package com.app.backend.controller;

import com.app.backend.dto.*;
import com.app.backend.entity.DocumentEntity;
import com.app.backend.repository.DocumentRepository;
import com.app.backend.service.DocumentService;
import com.app.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    // GET SINGLE DOCUMENT
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(@PathVariable @NotNull Long id) {
        try {
            DocumentEntity document = documentService.getDocumentById(id);
            ByteArrayResource resource = new ByteArrayResource(document.getData());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            String.format("inline; filename=\"%s\"", document.getFilename()))
                    .header("X-Document-Type", document.getDocumentType().toString())
                    .header("X-Upload-Date", document.getUploadedAt().toString())
                    .contentLength(document.getSize())
                    .body(resource);

        } catch (IllegalArgumentException ex) {
            log.warn("Document not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, ex.getMessage(), null, getCurrentTimestamp()));
        } catch (Exception ex) {
            log.error("Failed to retrieve document: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to retrieve document", null, getCurrentTimestamp()));
        }
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



//    @PreAuthorize("hasRole('ADMIN')")
//    @DeleteMapping("/multiple")
//    public ResponseEntity<ApiResponseDTO<Void>> deleteMultipleDocuments(
//            @RequestBody @NotNull List<Long> documentIds
//    ) {
//        try {
//            documentService.deleteMultipleDocuments(documentIds);
//            return ResponseEntity.ok(new ApiResponseDTO<>(
//                    true,
//                    String.format("Successfully deleted %d documents", documentIds.size()),
//                    null,
//                    getCurrentTimestamp()
//            ));
//
//        } catch (IllegalArgumentException ex) {
//            return ResponseEntity.badRequest().body(new ApiResponseDTO<>(
//                    false, ex.getMessage(), null, getCurrentTimestamp()
//            ));
//        } catch (Exception ex) {
//            log.error("Failed to delete multiple documents", ex);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(new ApiResponseDTO<>(false, "Failed to delete documents", null, getCurrentTimestamp()));
//        }
//    }

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
}
