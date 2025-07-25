package com.app.backend.controller;

import com.app.backend.dto.*;
import com.app.backend.entity.DocumentEntity;
import com.app.backend.repository.DocumentRepository;
import com.app.backend.service.DocumentService;
import com.app.backend.security.CustomUserDetails;
import com.app.backend.service.RateLimitService;
import com.app.backend.util.GlobalExceptionHandler;
import com.app.backend.util.WebUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    private final DocumentRepository documentRepository;

    private final RateLimitService rateLimitService;

    // SINGLE FILE UPLOAD
    @PreAuthorize("hasAnyRole('ADMIN','HEALTHCARE_PROVIDER','TUTOR')")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponseDTO<DocumentMetadataDTO>> uploadSingleDocument(
            @RequestParam("file") @NotNull MultipartFile file,
            Authentication authentication
    ) {
        try {
            Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
            DocumentEntity saved = documentService.saveDocument(file, userId);

            DocumentMetadataDTO metadata = new DocumentMetadataDTO(
                    saved.getId(), saved.getFilename(), saved.getContentType(),
                    saved.getSize(), saved.getUploadedByUserId(), saved.getUploadedAt(),
                    saved.getDocumentType(), saved.getSha256Checksum()
            );

            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true,
                    "Document uploaded successfully",
                    metadata,
                    getCurrentTimestamp()
            ));

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid upload request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                    false, ex.getMessage(), null, getCurrentTimestamp()
            ));
        } catch (Exception ex) {
            log.error("Upload failed for user: {}",
                    ((CustomUserDetails) authentication.getPrincipal()).getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(
                            false, "Upload failed: " + ex.getMessage(), null, getCurrentTimestamp()
                    ));
        }
    }

    // MULTIPLE FILES UPLOAD
    @PreAuthorize("hasAnyRole('ADMIN','HEALTHCARE_PROVIDER','TUTOR')")
    @PostMapping("/upload/multiple")
    public ResponseEntity<ApiResponseDTO<MultipleUploadResponseDTO>> uploadMultipleDocuments(
            @RequestParam("files") @NotNull List<MultipartFile> files,
            Authentication authentication
    ) {
        try {
            Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
            MultipleUploadResponseDTO result = documentService.saveMultipleDocuments(files, userId);

            boolean allSuccessful = result.getFailedUploads().isEmpty();
            HttpStatus status = allSuccessful ? HttpStatus.OK : HttpStatus.MULTI_STATUS;

            return ResponseEntity.status(status).body(new ApiResponseDTO<>(
                    allSuccessful,
                    result.getSummary(),
                    result,
                    getCurrentTimestamp()
            ));

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid multiple upload request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                    false, ex.getMessage(), null, getCurrentTimestamp()
            ));
        } catch (Exception ex) {
            log.error("Multiple upload failed for user: {}",
                    ((CustomUserDetails) authentication.getPrincipal()).getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(
                            false, "Multiple upload failed: " + ex.getMessage(), null, getCurrentTimestamp()
                    ));
        }
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/view")
    @Transactional(readOnly = true)
    public ResponseEntity<?> viewDocument(@PathVariable @NotNull @Min(1) Long id, HttpServletRequest request) {

        try {
            // Check user access
            Long currentUserId = getCurrentUserId();
            if (!documentService.hasUserAccessForView(id, currentUserId)) {
                log.warn("Unauthorized view attempt to document: {} by user: {}", id, currentUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponseDTO<>(false, "Access denied", null, getCurrentTimestamp()));
            }

            DocumentEntity document = documentService.getDocumentById(id);

            if (document.getData() == null || document.getData().length == 0) {
                log.warn("Document data is empty for ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "Document not found", null, getCurrentTimestamp()));
            }

            ByteArrayResource resource = new ByteArrayResource(document.getData());
            String sanitizedFilename = sanitizeFilename(document.getFilename());

            // Log view activity
            log.info("Document viewed - ID: {}, Filename: {}, User: {}",
                    id, sanitizedFilename, currentUserId);

            return ResponseEntity.ok()
                    .contentType(parseContentType(document.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            String.format("inline; filename=\"%s\"", sanitizedFilename)) // ✅ KEY: "inline" not "attachment"
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600") // Cache for better performance
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Frame-Options", "SAMEORIGIN") // Allow iframe within same origin
                    .contentLength(document.getSize())
                    .body(resource);

        } catch (Exception ex) {
            log.error("Failed to view document: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to load document", null, getCurrentTimestamp()));
        }
    }


    @GetMapping("/public/{id}/view")
    @Transactional(readOnly = true)
    public ResponseEntity<?> viewDocumentPublic(@PathVariable @NotNull @Min(1) Long id, HttpServletRequest request) {

        try {
            // Rate limiting check (production security)
            String clientIp = WebUtils.getClientIpAddress(request);
            if (!rateLimitService.isAllowed(clientIp)) {
                log.warn("Rate limit exceeded for IP: {} accessing document: {}", clientIp, id);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new ApiResponseDTO<>(false, "Rate limit exceeded", null, getCurrentTimestamp()));
            }

            DocumentEntity document = documentService.getDocumentByIdPublic(id);

            if (document.getData() == null || document.getData().length == 0) {
                log.warn("Document data is empty for ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "Document not found", null, getCurrentTimestamp()));
            }

            ByteArrayResource resource = new ByteArrayResource(document.getData());
            String sanitizedFilename = sanitizeFilename(document.getFilename());

            // Log public access activity (important for monitoring)
            log.info("Public document viewed - ID: {}, Filename: {}, IP: {}, UserAgent: {}",
                    id, sanitizedFilename, clientIp, request.getHeader("User-Agent"));

            return ResponseEntity.ok()
                    .contentType(parseContentType(document.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            String.format("inline; filename=\"%s\"", sanitizedFilename))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Frame-Options", "SAMEORIGIN")
                    .header("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingRequests(clientIp)))
                    .contentLength(document.getSize())
                    .body(resource);

        } catch (GlobalExceptionHandler.DocumentNotFoundException ex) {
            log.warn("Document not found for public access: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, "Document not found", null, getCurrentTimestamp()));
        } catch (Exception ex) {
            log.error("Failed to serve public document: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to load document", null, getCurrentTimestamp()));
        }
    }

//    @CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
    @GetMapping("/practiceImages")
    public ResponseEntity<ApiResponseDTO<List<DocumentMetadataDTO>>> getPracticeImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request
    ) {
        try {
            // Rate limiting for public endpoint
            String clientIp = WebUtils.getClientIpAddress(request);
            if (!rateLimitService.isAllowed(clientIp)) {
                log.warn("Rate limit exceeded for practice images request from IP: {}", clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new ApiResponseDTO<>(false, "Rate limit exceeded", null, getCurrentTimestamp()));
            }

            // Get practice images through service
            List<DocumentEntity> practiceImages = documentService.getPracticeImages(page, size);

            List<DocumentMetadataDTO> practiceImageDTOs = practiceImages.stream()
                    .map(this::convertToMetadataDTO)
                    .collect(Collectors.toList());

            log.info("Practice images retrieved: {} images from IP: {}",
                    practiceImageDTOs.size(), clientIp);

            return ResponseEntity.ok()
                    .header("Access-Control-Allow-Origin", "*")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Frame-Options", "SAMEORIGIN")
                    .header("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingRequests(clientIp)))
                    .body(new ApiResponseDTO<>(
                            true,
                            String.format("Retrieved %d practice images", practiceImageDTOs.size()),
                            practiceImageDTOs,
                            getCurrentTimestamp()
                    ));

        } catch (Exception ex) {
            log.error("Failed to retrieve practice images", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(
                            false,
                            "Failed to retrieve practice images: " + ex.getMessage(),
                            null,
                            getCurrentTimestamp()
                    ));
        }
    }




    // Health check endpoint for public API
    @GetMapping("/public/health")
    public ResponseEntity<ApiResponseDTO<String>> publicHealthCheck() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .body(new ApiResponseDTO<>(true, "Public API is healthy", "OK", getCurrentTimestamp()));
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/metadata")
    public ResponseEntity<ApiResponseDTO<List<DocumentMetadataDTO>>> getAllDocumentsMetadata(
            @RequestParam(required = false) String type
    ) {
        try {
            List<DocumentMetadataDTO> documents;

            if (type != null && !type.trim().isEmpty()) {
                try {
                    DocumentEntity.DocumentType documentType = DocumentEntity.DocumentType.valueOf(type.toUpperCase());
                    List<DocumentEntity> entities = documentService.getDocumentsByType(documentType);
                    documents = entities.stream()
                            .map(this::convertToMetadataDTO)
                            .toList();

                    log.info("Successfully retrieved {} documents of type: {}", documents.size(), documentType);

                } catch (IllegalArgumentException ex) {
                    log.warn("Invalid document type provided: {}", type);
                    return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                            false, "Invalid document type: " + type + ". Valid types are: IMAGE, PDF",
                            null, getCurrentTimestamp()
                    ));
                }
            } else {
                documents = documentService.getAllDocumentsMetadata();
            }

            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true,
                    String.format("Retrieved %d documents", documents.size()),
                    documents,
                    getCurrentTimestamp()
            ));

        } catch (Exception ex) {
            log.error("Failed to retrieve documents metadata with type: {}", type, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(
                            false,
                            "Failed to retrieve documents: " + ex.getMessage(),
                            null,
                            getCurrentTimestamp()
                    ));
        }
    }


    // ✅ ADD THIS NEW PUBLIC ENDPOINT
    @GetMapping("/public/metadata")
    public ResponseEntity<ApiResponseDTO<List<DocumentMetadataDTO>>> getPublicDocumentsMetadata(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request
    ) {
        try {
            // Rate limiting for public endpoint
            String clientIp = WebUtils.getClientIpAddress(request);
            if (!rateLimitService.isAllowed(clientIp)) {
                log.warn("Rate limit exceeded for public metadata request from IP: {}", clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new ApiResponseDTO<>(false, "Rate limit exceeded", null, getCurrentTimestamp()));
            }

            List<DocumentMetadataDTO> documents;

            if (type != null && !type.trim().isEmpty()) {
                try {
                    DocumentEntity.DocumentType documentType = DocumentEntity.DocumentType.valueOf(type.toUpperCase());
                    List<DocumentEntity> entities = documentService.getPublicDocumentsByType(documentType, page, size);
                    documents = entities.stream()
                            .map(this::convertToMetadataDTO)
                            .toList();

                    log.info("Public API: Retrieved {} documents of type: {} from IP: {}",
                            documents.size(), documentType, clientIp);

                } catch (IllegalArgumentException ex) {
                    log.warn("Invalid document type in public request: {}", type);
                    return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                            false, "Invalid document type: " + type + ". Valid types are: IMAGE, PDF",
                            null, getCurrentTimestamp()
                    ));
                }
            } else {
                documents = documentService.getPublicDocumentsMetadata(page, size);
            }

            return ResponseEntity.ok()
                    .header("Access-Control-Allow-Origin", "*")
                    .header("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingRequests(clientIp)))
                    .body(new ApiResponseDTO<>(
                            true,
                            String.format("Retrieved %d public documents", documents.size()),
                            documents,
                            getCurrentTimestamp()
                    ));

        } catch (Exception ex) {
            log.error("Failed to retrieve public documents metadata with type: {}", type, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(
                            false,
                            "Failed to retrieve documents: " + ex.getMessage(),
                            null,
                            getCurrentTimestamp()
                    ));
        }
    }



    @GetMapping("/debug/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getDocumentCount() {
        try {
            long count = documentRepository.count();
            List<DocumentEntity> allDocs = documentRepository.findAll();

            return ResponseEntity.ok(Map.of(
                    "totalDocuments", count,
                    "documents", allDocs.stream()
                            .map(doc -> Map.of(
                                    "id", doc.getId(),
                                    "filename", doc.getFilename(),
                                    "type", doc.getDocumentType()
                            ))
                            .collect(Collectors.toList())
            ));
        } catch (Exception ex) {
            log.error("Debug count failed", ex);
            return ResponseEntity.status(500).body("Error: " + ex.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-documents")
    public ResponseEntity<ApiResponseDTO<List<DocumentMetadataDTO>>> getUserDocuments(
            Authentication authentication,
            @RequestParam(required = false) String type
    ) {
        try {
            Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
            log.debug("Retrieving documents for user: {}", userId);

            List<DocumentEntity> documents = documentService.getDocumentsByUser(userId);

            if (type != null && !type.trim().isEmpty()) {
                try {
                    DocumentEntity.DocumentType documentType = DocumentEntity.DocumentType.valueOf(type.toUpperCase());
                    documents = documents.stream()
                            .filter(doc -> doc.getDocumentType() == documentType)
                            .toList();
                    log.debug("Filtered {} documents by type: {}", documents.size(), documentType);
                } catch (IllegalArgumentException ex) {
                    log.warn("Invalid document type provided: {}", type);
                    return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                            false,
                            "Invalid document type: " + type + ". Valid types are: IMAGE, PDF",
                            null,
                            getCurrentTimestamp()
                    ));
                }
            }

            List<DocumentMetadataDTO> metadata = documents.stream()
                    .map(this::convertToMetadataDTO)
                    .toList();

            log.info("Successfully retrieved {} documents for user: {}", metadata.size(), userId);

            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true,
                    String.format("Retrieved %d documents for user", metadata.size()),
                    metadata,
                    getCurrentTimestamp()
            ));

        } catch (Exception ex) {
            log.error("Failed to retrieve user documents", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(
                            false,
                            "Failed to retrieve user documents: " + ex.getMessage(),
                            null,
                            getCurrentTimestamp()
                    ));
        }
    }


    // DELETE SINGLE DOCUMENT
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteDocument(@PathVariable @NotNull Long id) {
        try {
            documentService.deleteDocumentById(id);
            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true, "Document deleted successfully", null, getCurrentTimestamp()
            ));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, ex.getMessage(), null, getCurrentTimestamp()));
        } catch (Exception ex) {
            log.error("Failed to delete document: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to delete document", null, getCurrentTimestamp()));
        }
    }

    // DELETE MULTIPLE DOCUMENTS
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/multiple")
    public ResponseEntity<ApiResponseDTO<PartialDeleteResponseDTO>> deleteMultipleDocuments(
            @RequestBody @NotNull List<Long> documentIds
    ) {
        try {
            PartialDeleteResponseDTO result = documentService.deleteMultipleDocuments(documentIds);

            // Determine response status and success flag
            boolean isSuccess = result.getSuccessfullyDeleted() > 0;
            HttpStatus status = result.isAllDeleted() ? HttpStatus.OK :
                    result.isPartialDeletion() ? HttpStatus.MULTI_STATUS :
                            HttpStatus.NOT_FOUND;

            return ResponseEntity.status(status).body(new ApiResponseDTO<>(
                    isSuccess,
                    result.getSummary(),
                    result,
                    getCurrentTimestamp()
            ));

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid delete request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                    false, ex.getMessage(), null, getCurrentTimestamp()
            ));
        } catch (Exception ex) {
            log.error("Failed to delete multiple documents: {}", documentIds, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(
                            false,
                            "Failed to delete documents: " + ex.getMessage(),
                            null,
                            getCurrentTimestamp()
                    ));
        }
    }

    // Helper methods
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private DocumentMetadataDTO convertToMetadataDTO(DocumentEntity document) {
        return new DocumentMetadataDTO(
                document.getId(),
                document.getFilename(),
                document.getContentType(),
                document.getSize(),
                document.getUploadedByUserId(),
                document.getUploadedAt(),
                document.getDocumentType(),
                document.getSha256Checksum()
        );
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/download")
    @Transactional(readOnly = true)  // ✅ THIS IS THE KEY FIX for PostgreSQL LOB error
    public ResponseEntity<?> downloadDocument(
            @PathVariable @NotNull @Min(1) Long id,
            HttpServletRequest request) {

        try {
            // Add user authorization check
            Long currentUserId = getCurrentUserId();
            if (!documentService.hasUserAccessForDownload(id, currentUserId)) {
                log.warn("Unauthorized access attempt to document: {} by user: {}", id, currentUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponseDTO<>(false, "Access denied", null, getCurrentTimestamp()));
            }

            DocumentEntity document = documentService.getDocumentById(id);

            // Validate document data exists
            if (document.getData() == null || document.getData().length == 0) {
                log.warn("Document data is empty for ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "Document data not found", null, getCurrentTimestamp()));
            }

            ByteArrayResource resource = new ByteArrayResource(document.getData());
            String sanitizedFilename = sanitizeFilename(document.getFilename());

            // Audit log
            log.info("Document downloaded - ID: {}, Filename: {}, Size: {}MB, User: {}",
                    id, sanitizedFilename,
                    String.format("%.2f", document.getSize() / (1024.0 * 1024.0)),
                    currentUserId);

            return ResponseEntity.ok()
                    .contentType(parseContentType(document.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            String.format("attachment; filename=\"%s\"", sanitizedFilename))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header("X-Content-Type-Options", "nosniff")
                    .header("X-Frame-Options", "DENY")
                    .header("X-Document-Type", document.getDocumentType().toString())
                    .header("X-Upload-Date", document.getUploadedAt().toString())
                    .contentLength(document.getSize())
                    .body(resource);

        } catch (GlobalExceptionHandler.DocumentNotFoundException ex) {
            log.warn("Document not found for download: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, "Document not found", null, getCurrentTimestamp()));
        } catch (GlobalExceptionHandler.DocumentAccessDeniedException ex) {
            log.warn("Access denied for document: {} by user: {}", id, getCurrentUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDTO<>(false, "Access denied", null, getCurrentTimestamp()));
        } catch (Exception ex) {
            log.error("Failed to download document: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to download document", null, getCurrentTimestamp()));
        }
    }

    // Helper methods for the controller
    private String sanitizeFilename(String filename) {
        if (filename == null) return "document";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_{2,}", "_")
                .trim();
    }

    private MediaType parseContentType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            log.warn("Invalid content type: {}, using default", contentType);
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).getId();
        }
        throw new SecurityException("Unable to determine current user");
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<PagedDocumentResponseDTO>> getAllDocumentsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type
    ) {
        log.info("Admin document pagination request: page={}, size={}, search='{}', type='{}'",
                page, size, search, type);

        try {
            // Validate parameters
            if (page < 0) {
                return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                        false,
                        "Page number must be non-negative",
                        null,
                        getCurrentTimestamp()
                ));
            }

            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                        false,
                        "Page size must be between 1 and 100",
                        null,
                        getCurrentTimestamp()
                ));
            }

            PagedDocumentResponseDTO pagedResponse = documentService.getAllDocumentsPaginated(
                    page, size, search, type);

            log.info("Successfully retrieved page {} with {} documents", page, pagedResponse.getContent().size());

            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true,
                    String.format("Retrieved page %d of documents (%d items)",
                            page + 1, pagedResponse.getContent().size()),
                    pagedResponse,
                    getCurrentTimestamp()
            ));

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid request parameters: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
                    false,
                    "Invalid request: " + ex.getMessage(),
                    null,
                    getCurrentTimestamp()
            ));
        } catch (RuntimeException ex) {
            log.error("Service error during pagination: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(
                            false,
                            "Failed to retrieve documents: " + ex.getMessage(),
                            null,
                            getCurrentTimestamp()
                    ));
        } catch (Exception ex) {
            log.error("Unexpected error during pagination", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(
                            false,
                            "An unexpected error occurred",
                            null,
                            getCurrentTimestamp()
                    ));
        }
    }

    @GetMapping("/debug/detailed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDetailedDebugInfo() {
        try {
            long count = documentRepository.count();
            long customCount = documentRepository.countAllDocuments();

            List<DocumentEntity> sample = documentRepository.findAll().stream()
                    .limit(3)
                    .collect(Collectors.toList());

            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("totalCount", count);
            debugInfo.put("customCount", customCount);
            debugInfo.put("sampleDocuments", sample.stream()
                    .map(doc -> Map.of(
                            "id", doc.getId(),
                            "filename", doc.getFilename(),
                            "uploadedAt", doc.getUploadedAt(),
                            "type", doc.getDocumentType()
                    ))
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(debugInfo);
        } catch (Exception ex) {
            log.error("Debug endpoint failed", ex);
            return ResponseEntity.status(500)
                    .body(Map.of("error", ex.getMessage(), "stackTrace", ex.getStackTrace()));
        }
    }



    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponseDTO<DocumentStatsResponseDTO>> getDocumentStats() {
        try {
            DocumentStatsResponseDTO stats = documentService.getDocumentStats();
            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true,
                    "Document statistics retrieved successfully",
                    stats,
                    getCurrentTimestamp()
            ));
        } catch (Exception ex) {
            log.error("Failed to retrieve document statistics", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(
                            false,
                            "Failed to retrieve statistics: " + ex.getMessage(),
                            null,
                            getCurrentTimestamp()
                    ));
        }
    }


}
