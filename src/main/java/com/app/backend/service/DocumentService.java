package com.app.backend.service;

import com.app.backend.config.DocumentConfigProperties;
import com.app.backend.dto.*;
import com.app.backend.entity.DocumentEntity;
import com.app.backend.repository.DocumentRepository;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.app.backend.util.GlobalExceptionHandler;


@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentConfigProperties config;

    private static final Tika tika = new Tika();

    @Transactional
    public DocumentEntity saveDocument(MultipartFile file, Long userId) throws Exception {
        validateFile(file);

        String mimeType = detectMimeType(file);
        DocumentEntity.DocumentType documentType = determineDocumentType(mimeType);
        validateFileSize(file, documentType);

        String checksum = calculateSHA256(file.getBytes());

        // Check for duplicates
        if (documentRepository.existsBySha256ChecksumAndUploadedByUserId(checksum, userId)) {
            throw new IllegalArgumentException("Duplicate file detected. This file has already been uploaded.");
        }

        try {
            DocumentEntity document = DocumentEntity.builder()
                    .filename(sanitizeFilename(file.getOriginalFilename()))
                    .contentType(mimeType)
                    .size(file.getSize())
                    .uploadedByUserId(userId)
                    .uploadedAt(LocalDateTime.now())
                    .documentType(documentType)
                    .data(file.getBytes())
                    .sha256Checksum(checksum)
                    .build();

            DocumentEntity saved = documentRepository.save(document);

            log.info("Document uploaded successfully. ID: {}, Type: {}, Size: {}MB, User: {}",
                    saved.getId(), documentType,
                    String.format("%.2f", file.getSize() / (1024.0 * 1024.0)), userId);

            return saved;
        } catch (Exception ex) {
            log.error("Failed to save document to database for user: {}", userId, ex);
            throw new Exception("Failed to store document to database.", ex);
        }
    }

    @Transactional
    public MultipleUploadResponseDTO saveMultipleDocuments(List<MultipartFile> files, Long userId) {
        // Validate input
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided for upload.");
        }

        if (files.size() > config.getMaxBatchSize()) {
            throw new IllegalArgumentException(
                    String.format("Batch size %d exceeds maximum limit of %d files.",
                            files.size(), config.getMaxBatchSize()));
        }

        // **NEW: Validate file type consistency for multiple uploads**
        validateFileTypeConsistency(files);

        List<DocumentMetadataDTO> successfulUploads = new ArrayList<>();
        List<FailedUploadDTO> failedUploads = new ArrayList<>();

        log.info("Starting batch upload of {} files for user: {}", files.size(), userId);

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String filename = file.getOriginalFilename();

            try {
                // Skip empty files
                if (file.isEmpty()) {
                    failedUploads.add(FailedUploadDTO.builder()
                            .filename(filename != null ? filename : "File #" + (i + 1))
                            .errorMessage("File is empty")
                            .errorCode("EMPTY_FILE")
                            .fileSize(file.getSize())
                            .build());
                    continue;
                }

                // Validate filename
                if (filename == null || filename.trim().isEmpty()) {
                    failedUploads.add(FailedUploadDTO.builder()
                            .filename("File #" + (i + 1))
                            .errorMessage("File name is missing")
                            .errorCode("MISSING_FILENAME")
                            .fileSize(file.getSize())
                            .build());
                    continue;
                }

                DocumentEntity saved = saveDocument(file, userId);
                successfulUploads.add(convertToMetadataDTO(saved));

            } catch (IllegalArgumentException ex) {
                log.warn("Validation failed for file: {} - {}", filename, ex.getMessage());
                failedUploads.add(FailedUploadDTO.builder()
                        .filename(filename != null ? filename : "File #" + (i + 1))
                        .errorMessage(ex.getMessage())
                        .errorCode("VALIDATION_ERROR")
                        .fileSize(file.getSize())
                        .build());

            } catch (Exception ex) {
                log.error("Failed to upload file: {}", filename, ex);
                failedUploads.add(FailedUploadDTO.builder()
                        .filename(filename != null ? filename : "File #" + (i + 1))
                        .errorMessage("Upload failed: " + ex.getMessage())
                        .errorCode("UPLOAD_ERROR")
                        .fileSize(file.getSize())
                        .build());
            }
        }

        String summary = String.format("Upload completed: %d successful, %d failed out of %d total files",
                successfulUploads.size(), failedUploads.size(), files.size());

        log.info("Batch upload completed for user: {}. {}", userId, summary);

        return MultipleUploadResponseDTO.builder()
                .successfulUploads(successfulUploads)
                .failedUploads(failedUploads)
                .summary(summary)
                .totalFiles(files.size())
                .successCount(successfulUploads.size())
                .failureCount(failedUploads.size())
                .build();
    }

    // **NEW: File Type Consistency Validation Method**
    private void validateFileTypeConsistency(List<MultipartFile> files) {
        if (files.size() <= 1) {
            return; // Single file uploads don't need consistency check
        }

        Set<DocumentEntity.DocumentType> detectedTypes = new HashSet<>();
        List<String> fileDetails = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue; // Skip empty files for type detection
            }

            try {
                String mimeType = detectMimeType(file);
                DocumentEntity.DocumentType documentType = determineDocumentType(mimeType);
                detectedTypes.add(documentType);

                fileDetails.add(String.format("%s (%s)",
                        file.getOriginalFilename(),
                        documentType.toString()));

            } catch (Exception ex) {
                log.warn("Could not determine type for file: {} - {}",
                        file.getOriginalFilename(), ex.getMessage());
                // Continue processing other files
            }
        }

        // Check if we have mixed file types
        if (detectedTypes.size() > 1) {
            String imageFiles = fileDetails.stream()
                    .filter(detail -> detail.contains("(IMAGE)"))
                    .collect(Collectors.joining(", "));

            String pdfFiles = fileDetails.stream()
                    .filter(detail -> detail.contains("(PDF)"))
                    .collect(Collectors.joining(", "));

            String errorMessage = String.format(
                    "Mixed file types are not allowed in batch uploads. " +
                            "Please upload either all images OR all PDFs in a single request. " +
                            "Detected: Images [%s] and PDFs [%s]",
                    imageFiles.isEmpty() ? "none" : imageFiles,
                    pdfFiles.isEmpty() ? "none" : pdfFiles
            );

            log.warn("Mixed file types detected in batch upload. Images: {}, PDFs: {}",
                    imageFiles, pdfFiles);

            throw new IllegalArgumentException(errorMessage);
        }

        // Log the detected consistent type
        if (!detectedTypes.isEmpty()) {
            DocumentEntity.DocumentType batchType = detectedTypes.iterator().next();
            log.info("Batch upload validation passed. All files are of type: {}", batchType);
        }
    }

    // In DocumentService - for when you actually need the binary data
    public DocumentEntity getDocumentWithData(Long id) {
        return documentRepository.findByIdWithData(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + id));
    }


    @Transactional(readOnly = true)
    public boolean hasUserAccessForDownload(Long documentId, Long userId) {
        if (userId == null) return false;

        try {
            Optional<DocumentEntity> documentOpt = documentRepository.findByIdWithData(documentId);
            if (documentOpt.isEmpty()) {
                return false;
            }

            DocumentEntity document = documentOpt.get();

            // Check if user owns the document
            if (document.getUploadedByUserId().equals(userId)) {
                return true;
            }

            // Check if user has admin role
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

            return isAdmin;

        } catch (Exception ex) {
            log.error("Error checking document access for document: {} and user: {}", documentId, userId, ex);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean hasUserAccessForView(Long documentId, Long userId) {
        if (userId == null) return false;

        try {
            Optional<DocumentEntity> documentOpt = documentRepository.findByIdWithData(documentId);
            if (documentOpt.isEmpty()) {
                return false;
            }

            DocumentEntity document = documentOpt.get();

            // Owner always has access
            if (document.getUploadedByUserId().equals(userId)) {
                return true;
            }

            // Check if user has admin or view permissions
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean hasViewPermission = auth.getAuthorities().stream()
                    .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_ADMIN") ||
                                    authority.getAuthority().equals("ROLE_HEALTHCARE_PROVIDER") ||
                                    authority.getAuthority().equals("ROLE_TUTOR")
                    );

            return hasViewPermission;

        } catch (Exception ex) {
            log.error("Error checking document access for document: {} and user: {}", documentId, userId, ex);
            return false;
        }
    }


    // Make sure this method has @Transactional
    @Transactional(readOnly = true)
    public DocumentEntity getDocumentById(Long id) {
        return documentRepository.findByIdWithData(id)
                .orElseThrow(() -> new GlobalExceptionHandler.DocumentNotFoundException("Document not found with ID: " + id));
    }

    public List<DocumentEntity> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadedAtDesc();
    }

    public List<DocumentEntity> getDocumentsByType(DocumentEntity.DocumentType type) {
        try {
            return documentRepository.findByDocumentTypeOrderByUploadedAtDesc(type);
        } catch (Exception ex) {
            log.error("Error retrieving documents by type: {}", type, ex);
            // Fallback to manual filtering if repository method fails
            List<DocumentEntity> allDocuments = documentRepository.findAll();
            return allDocuments.stream()
                    .filter(doc -> doc.getDocumentType() == type)
                    .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()))
                    .collect(Collectors.toList());
        }
    }


    public List<DocumentEntity> getDocumentsByUser(Long userId) {
        try {
            log.debug("Attempting to retrieve documents for user: {}", userId);

            // Try the repository method first
            List<DocumentEntity> documents = documentRepository.findByUploadedByUserIdOrderByUploadedAtDesc(userId);

            log.info("Successfully retrieved {} documents for user: {}", documents.size(), userId);
            return documents;

        } catch (Exception ex) {
            log.error("Repository query failed for user: {}, falling back to manual filtering", userId, ex);

            // Fallback: Get all documents and filter manually
            List<DocumentEntity> allDocuments = documentRepository.findAll();
            List<DocumentEntity> userDocuments = allDocuments.stream()
                    .filter(doc -> doc.getUploadedByUserId().equals(userId))
                    .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()))
                    .collect(Collectors.toList());

            log.info("Fallback filtering successful: {} documents for user: {}", userDocuments.size(), userId);
            return userDocuments;
        }
    }


    @Transactional
    public void deleteDocumentById(Long id) {
        if (!documentRepository.existsById(id)) {
            throw new IllegalArgumentException("Document not found with ID: " + id);
        }

        documentRepository.deleteById(id);
        log.info("Document deleted successfully. ID: {}", id);
    }

    @Transactional
    public PartialDeleteResponseDTO deleteMultipleDocuments(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("No document IDs provided for deletion.");
        }

        // Find which IDs exist and which don't
        List<Long> existingIds = documentRepository.findExistingIds(ids);
        List<Long> nonExistingIds = ids.stream()
                .filter(id -> !existingIds.contains(id))
                .collect(Collectors.toList());

        // Delete the existing documents
        int deletedCount = 0;
        if (!existingIds.isEmpty()) {
            documentRepository.deleteByIdIn(existingIds);
            deletedCount = existingIds.size();
            log.info("Successfully deleted {} documents with IDs: {}", deletedCount, existingIds);
        }

        if (!nonExistingIds.isEmpty()) {
            log.warn("Attempted to delete non-existing documents with IDs: {}", nonExistingIds);
        }

        // Create detailed response
        return PartialDeleteResponseDTO.builder()
                .requestedIds(ids)
                .deletedIds(existingIds)
                .notFoundIds(nonExistingIds)
                .totalRequested(ids.size())
                .successfullyDeleted(deletedCount)
                .notFound(nonExistingIds.size())
                .summary(String.format("Deletion completed: %d successful, %d not found out of %d requested",
                        deletedCount, nonExistingIds.size(), ids.size()))
                .build();
    }


    public List<DocumentMetadataDTO> getAllDocumentsMetadata() {
        List<DocumentEntity> documents = documentRepository.findAll();
        return documents.stream()
                .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()))
                .map(this::convertToMetadataDTO)
                .collect(Collectors.toList());
    }

    public List<DocumentMetadataDTO> getDocumentsMetadataByUser(Long userId) {
        try {
            return documentRepository.findByUploadedByUserIdOrderByUploadedAtDesc(userId).stream()
                    .map(this::convertToMetadataDTO)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Error retrieving metadata for user: {}, using fallback", userId, ex);
            List<DocumentEntity> allDocuments = documentRepository.findAll();
            return allDocuments.stream()
                    .filter(doc -> doc.getUploadedByUserId().equals(userId))
                    .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()))
                    .map(this::convertToMetadataDTO)
                    .collect(Collectors.toList());
        }
    }

    public List<DocumentMetadataDTO> getDocumentsMetadataByTypeAndUser(
            DocumentEntity.DocumentType type, Long userId) {
        return documentRepository.findByDocumentTypeAndUploadedByUserIdOrderByUploadedAtDesc(type, userId)
                .stream()
                .map(this::convertToMetadataDTO)
                .collect(Collectors.toList());
    }

    public long getDocumentCountByUser(Long userId) {
        return documentRepository.countByUploadedByUserId(userId);
    }

    // Private helper methods (keep all existing ones)
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or missing.");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            throw new IllegalArgumentException("File name is missing.");
        }

        long maxFileSize = Math.max(config.getMaxImageSize(), config.getMaxPdfSize());
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("File size %.2fMB exceeds maximum allowed size of %.2fMB.",
                            file.getSize() / (1024.0 * 1024.0),
                            maxFileSize / (1024.0 * 1024.0)));
        }
    }

    private String detectMimeType(MultipartFile file) throws IOException {
        try {
            String mimeType = tika.detect(file.getInputStream());
            if (mimeType == null || mimeType.isEmpty()) {
                throw new IllegalArgumentException("Unable to determine file type.");
            }
            log.debug("Detected MIME type: {} for file: {}", mimeType, file.getOriginalFilename());
            return mimeType;
        } catch (IOException ex) {
            log.error("Error detecting MIME type for file: {}", file.getOriginalFilename(), ex);
            throw new IOException("Failed to detect file type.", ex);
        }
    }

    private DocumentEntity.DocumentType determineDocumentType(String mimeType) {
        if (config.getAllowedImageTypes().contains(mimeType)) {
            return DocumentEntity.DocumentType.IMAGE;
        } else if (config.getAllowedPdfTypes().contains(mimeType)) {
            return DocumentEntity.DocumentType.PDF;
        } else {
            String allowedTypes = String.join(", ", config.getAllowedImageTypes()) +
                    ", " + String.join(", ", config.getAllowedPdfTypes());
            throw new IllegalArgumentException(
                    String.format("Unsupported file type: %s. Allowed types: %s", mimeType, allowedTypes));
        }
    }

    private void validateFileSize(MultipartFile file, DocumentEntity.DocumentType type) {
        long maxSize = (type == DocumentEntity.DocumentType.IMAGE) ?
                config.getMaxImageSize() : config.getMaxPdfSize();

        String typeStr = type.toString().toLowerCase();

        if (file.getSize() > maxSize) {
            String limitStr = String.format("%.1fMB", maxSize / (1024.0 * 1024.0));
            String actualSize = String.format("%.2fMB", file.getSize() / (1024.0 * 1024.0));
            throw new IllegalArgumentException(
                    String.format("File size %s exceeds %s limit for %s files.",
                            actualSize, limitStr, typeStr));
        }

        log.debug("File size validation passed for {}: {}MB (limit: {}MB)",
                typeStr, file.getSize() / (1024.0 * 1024.0), maxSize / (1024.0 * 1024.0));
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed_file";

        String baseName = filename.contains(".") ?
                filename.substring(0, filename.lastIndexOf('.')) : filename;
        String extension = filename.contains(".") ?
                filename.substring(filename.lastIndexOf('.')) : "";

        String sanitizedBase = baseName.replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_{2,}", "_")
                .trim();

        String sanitizedFilename = sanitizedBase + extension;
        if (sanitizedFilename.length() > 200) {
            sanitizedFilename = sanitizedBase.substring(0, 200 - extension.length()) + extension;
        }

        log.debug("Sanitized filename: {} -> {}", filename, sanitizedFilename);
        return sanitizedFilename;
    }

    private String calculateSHA256(byte[] data) throws NoSuchAlgorithmException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            log.error("SHA-256 algorithm not available", ex);
            throw ex;
        }
    }

    public DocumentConfigProperties getConfiguration() {
        return config;
    }

    public Map<String, Object> getConfigurationInfo() {
        Map<String, Object> configInfo = new HashMap<>();
        configInfo.put("maxImageSize", String.format("%.1fMB", config.getMaxImageSize() / (1024.0 * 1024.0)));
        configInfo.put("maxPdfSize", String.format("%.1fMB", config.getMaxPdfSize() / (1024.0 * 1024.0)));
        configInfo.put("maxBatchSize", config.getMaxBatchSize());
        configInfo.put("allowedImageTypes", config.getAllowedImageTypes());
        configInfo.put("allowedPdfTypes", config.getAllowedPdfTypes());
        configInfo.put("uploadPath", config.getUploadPath());
        return configInfo;
    }

    public PagedDocumentResponseDTO getAllDocumentsPaginated(int page, int size, String search, String type) {
        log.info("Starting paginated document retrieval: page={}, size={}, search={}, type={}",
                page, size, search, type);

        try {
            // Validate input parameters
            if (page < 0) page = 0;
            if (size <= 0) size = 10;
            if (size > 100) size = 100;

            // ✅ FIXED: Use metadata-only query to avoid LOB loading
            List<DocumentEntity> allDocuments;
            try {
                // Use the metadata-only query instead
                allDocuments = documentRepository.findAllMetadataByOrderByUploadedAtDesc();
                log.debug("Retrieved {} document metadata records from database", allDocuments.size());
            } catch (Exception ex) {
                log.error("Database query failed: {}", ex.getMessage(), ex);
                throw new RuntimeException("Database connection error: " + ex.getMessage());
            }

            // Apply filters with null safety
            List<DocumentEntity> filteredDocuments = allDocuments.stream()
                    .filter(doc -> doc != null)
                    .filter(doc -> {
                        // Search filter
                        if (search != null && !search.trim().isEmpty()) {
                            String filename = doc.getFilename();
                            if (filename == null) return false;
                            return filename.toLowerCase().contains(search.toLowerCase());
                        }
                        return true;
                    })
                    .filter(doc -> {
                        // Type filter
                        if (type != null && !type.trim().isEmpty() && !type.equalsIgnoreCase("ALL")) {
                            try {
                                DocumentEntity.DocumentType docType = DocumentEntity.DocumentType.valueOf(type.toUpperCase());
                                return doc.getDocumentType() == docType;
                            } catch (IllegalArgumentException ex) {
                                log.warn("Invalid document type filter: {}", type);
                                return true;
                            }
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            log.debug("Filtered documents: {} out of {}", filteredDocuments.size(), allDocuments.size());

            // Calculate pagination
            int totalElements = filteredDocuments.size();
            int totalPages = totalElements == 0 ? 1 : (int) Math.ceil((double) totalElements / size);

            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);

            // Get page content
            List<DocumentEntity> pageContent = new ArrayList<>();
            if (startIndex < totalElements && endIndex > startIndex) {
                pageContent = filteredDocuments.subList(startIndex, endIndex);
            }

            // Convert to DTOs
            List<DocumentMetadataDTO> contentDTOs = pageContent.stream()
                    .map(this::convertToMetadataDTO)
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());

            PagedDocumentResponseDTO response = PagedDocumentResponseDTO.builder()
                    .content(contentDTOs)
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .first(page == 0)
                    .last(page >= totalPages - 1)
                    .empty(contentDTOs.isEmpty())
                    .build();

            log.info("Paginated query completed successfully: page={}, size={}, totalElements={}, totalPages={}, returnedItems={}",
                    page, size, totalElements, totalPages, contentDTOs.size());

            return response;

        } catch (Exception ex) {
            log.error("Error in paginated document retrieval: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve paginated documents: " + ex.getMessage(), ex);
        }
    }

    /**
     * Convert DocumentEntity to DocumentMetadataDTO with null safety
     */
    private DocumentMetadataDTO convertToMetadataDTO(DocumentEntity document) {
        if (document == null) {
            log.warn("Attempted to convert null DocumentEntity to DTO");
            return null;
        }

        try {
            return DocumentMetadataDTO.builder()
                    .id(document.getId())
                    .filename(document.getFilename() != null ? document.getFilename() : "Unknown")
                    .contentType(document.getContentType() != null ? document.getContentType() : "application/octet-stream")
                    .size(document.getSize() != null ? document.getSize() : 0L)
                    .uploadedByUserId(document.getUploadedByUserId())
                    .uploadedAt(document.getUploadedAt() != null ? document.getUploadedAt() : LocalDateTime.now())
                    .documentType(document.getDocumentType() != null ? document.getDocumentType() : DocumentEntity.DocumentType.PDF)
                    .sha256Checksum(document.getSha256Checksum())
                    .build();
        } catch (Exception ex) {
            log.error("Error converting DocumentEntity to DTO: {}", ex.getMessage());
            throw new RuntimeException("DTO conversion failed", ex);
        }
    }

    public DocumentStatsResponseDTO getDocumentStats() {
        try {
            log.debug("Calculating document statistics");

            long totalDocuments = documentRepository.count();

            // Count by type - using manual counting to avoid repository method dependencies
            List<DocumentEntity> allDocs = documentRepository.findAll();

            long imageCount = allDocs.stream()
                    .filter(doc -> doc.getDocumentType() == DocumentEntity.DocumentType.IMAGE)
                    .count();

            long pdfCount = allDocs.stream()
                    .filter(doc -> doc.getDocumentType() == DocumentEntity.DocumentType.PDF)
                    .count();

            // Calculate total size
            long totalSize = allDocs.stream()
                    .mapToLong(DocumentEntity::getSize)
                    .sum();

            log.info("Document stats calculated: total={}, images={}, pdfs={}, totalSize={}MB",
                    totalDocuments, imageCount, pdfCount, totalSize / (1024.0 * 1024.0));

            return DocumentStatsResponseDTO.builder()
                    .total(totalDocuments)
                    .images(imageCount)
                    .pdfs(pdfCount)
                    .totalSize(totalSize)
                    .build();

        } catch (Exception ex) {
            log.error("Error calculating document statistics", ex);
            throw new RuntimeException("Failed to calculate document statistics", ex);
        }
    }

    /**
     * Get all documents with fallback mechanism
     */
    private List<DocumentEntity> getAllDocumentsWithFallback() {
        try {
            log.debug("Attempting primary query: findAllByOrderByUploadedAtDesc");
            List<DocumentEntity> documents = documentRepository.findAllByOrderByUploadedAtDesc();
            log.debug("Primary query successful: {} documents retrieved", documents.size());
            return documents;
        } catch (Exception ex) {
            log.warn("Primary query failed, attempting fallback: {}", ex.getMessage());
            try {
                List<DocumentEntity> documents = documentRepository.findAllByOrderByIdDesc();
                log.info("Fallback query successful: {} documents retrieved", documents.size());
                return documents;
            } catch (Exception fallbackEx) {
                log.error("Both primary and fallback queries failed", fallbackEx);
                throw new RuntimeException("Database query failed: " + fallbackEx.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public DocumentEntity getPublicDocumentById(Long id) {
        // ✅ Dynamic approach - fetch document first, then validate accessibility
        DocumentEntity document = documentRepository.findByIdWithData(id)
                .orElseThrow(() -> new GlobalExceptionHandler.DocumentNotFoundException(
                        "Document not found with ID: " + id));

        // ✅ Use dynamic validation instead of static ID list
        if (!isDocumentPubliclyAccessible(document)) {
            log.warn("Document {} is not publicly accessible - filename: '{}', type: '{}'",
                    id, document.getFilename(), document.getDocumentType());
            throw new GlobalExceptionHandler.DocumentNotFoundException(
                    "Document is not publicly accessible with ID: " + id);
        }

        log.info("Public document accessed - ID: {}, Filename: {}", id, document.getFilename());
        return document;
    }


    private DocumentMetadataDTO getPublicImageMetadata(Long id) {
        try {
            Optional<DocumentEntity> docOpt = documentRepository.findById(id);
            if (docOpt.isEmpty()) {
                log.warn("Public practice image not found with ID: {}", id);
                return null;
            }

            DocumentEntity doc = docOpt.get();

            // Validate it's an image and publicly accessible
            if (doc.getDocumentType() != DocumentEntity.DocumentType.IMAGE) {
                log.warn("Document with ID: {} is not an image, skipping", id);
                return null;
            }

            if (!isDocumentPubliclyAccessible(doc)) {
                log.warn("Document with ID: {} is not publicly accessible, skipping", id);
                return null;
            }

            return new DocumentMetadataDTO(
                    doc.getId(),
                    doc.getFilename(),
                    doc.getContentType(),
                    doc.getSize(),
                    doc.getDocumentType().toString(),
                    doc.getUploadedAt()
            );

        } catch (Exception e) {
            log.error("Error fetching metadata for public image ID: {}", id, e);
            return null;
        }
    }

    // Method to check if a document has public access (for internal use)
    @Transactional(readOnly = true)
    public boolean hasPublicAccess(Long documentId) {
        try {
            DocumentEntity document = getPublicDocumentById(documentId);
            return document != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public DocumentEntity getDocumentByIdPublic(Long id) {
        Optional<DocumentEntity> documentOpt = documentRepository.findByIdWithData(id);

        if (documentOpt.isEmpty()) {
            throw new GlobalExceptionHandler.DocumentNotFoundException("Document not found with ID: " + id);
        }

        DocumentEntity document = documentOpt.get();

        return document;
    }


    private boolean isValidImageType(DocumentEntity document) {
        if (document.getDocumentType() != DocumentEntity.DocumentType.IMAGE) {
            return false;
        }

        String contentType = document.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }


    @Transactional(readOnly = true)
    public List<DocumentEntity> getPublicDocumentsByType(DocumentEntity.DocumentType documentType, int page, int size) {
        // Only return metadata (without binary data) for public access
        return documentRepository.findByDocumentTypeOrderByUploadedAtDesc(documentType)
                .stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentMetadataDTO> getPublicDocumentsMetadata(int page, int size) {
        // Return only metadata for public consumption
        return documentRepository.findAllByOrderByUploadedAtDesc()
                .stream()
                .skip((long) page * size)
                .limit(size)
                .map(doc -> new DocumentMetadataDTO(
                        doc.getId(), doc.getFilename(), doc.getContentType(),
                        doc.getSize(), doc.getUploadedByUserId(), doc.getUploadedAt(),
                        doc.getDocumentType(), doc.getSha256Checksum()
                ))
                .collect(Collectors.toList());
    }

    public List<DocumentEntity> getPracticeImages(int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);

            List<DocumentEntity> practiceImages = documentRepository.findPracticeImagesPaginated(
                    "practice",   // Search for 'practice' in filename
                    "spiritual",  // Search for 'spiritual' in filename
                    pageable
            );

            log.info("Practice images found via database query: {} images for page: {}, size: {}",
                    practiceImages.size(), page, size);

            return practiceImages;

        } catch (Exception ex) {
            log.error("Database query failed, falling back to in-memory filtering", ex);

            // ✅ Fallback to your original logic if database query fails
            return getPracticeImagesWithInMemoryFiltering(page, size);
        }
    }

    // Keep your original method as fallback
    private List<DocumentEntity> getPracticeImagesWithInMemoryFiltering(int page, int size) {
        List<DocumentEntity> allDocs = documentRepository.findAll();
        log.info("Fallback: Total documents in DB: {}", allDocs.size());

        List<DocumentEntity> publicDocs = allDocs.stream()
                .filter(this::isDocumentPubliclyAccessible)
                .collect(Collectors.toList());
        log.info("Fallback: Public accessible documents: {}", publicDocs.size());

        List<DocumentEntity> imageDocs = publicDocs.stream()
                .filter(doc -> doc.getDocumentType() == DocumentEntity.DocumentType.IMAGE)
                .collect(Collectors.toList());
        log.info("Fallback: Public image documents: {}", imageDocs.size());

        List<DocumentEntity> practiceImages = imageDocs.stream()
                .filter(doc -> doc.getFilename() != null &&
                        (doc.getFilename().toLowerCase().contains("practice") ||
                                doc.getFilename().toLowerCase().contains("spiritual")))
                .collect(Collectors.toList());
        log.info("Fallback: Practice images found: {}", practiceImages.size());

        return practiceImages.stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

//    public List<DocumentEntity> getPracticeImages(int page, int size) {
//        List<DocumentEntity> allDocs = documentRepository.findAll();
//        log.info("Total documents in DB: {}", allDocs.size());
//
//        List<DocumentEntity> publicDocs = allDocs.stream()
//                .filter(this::isDocumentPubliclyAccessible)
//                .collect(Collectors.toList());
//        log.info("Public accessible documents: {}", publicDocs.size());
//
//        List<DocumentEntity> imageDocs = publicDocs.stream()
//                .filter(doc -> doc.getDocumentType() == DocumentEntity.DocumentType.IMAGE)
//                .collect(Collectors.toList());
//        log.info("Public image documents: {}", imageDocs.size());
//
//        List<DocumentEntity> practiceImages = imageDocs.stream()
//                .filter(doc -> doc.getFilename() != null &&
//                        (doc.getFilename().toLowerCase().contains("practice") ||
//                                doc.getFilename().toLowerCase().contains("spiritual")))
//                .collect(Collectors.toList());
//        log.info("Practice images found: {}", practiceImages.size());
//
//        return practiceImages.stream()
//                .skip((long) page * size)
//                .limit(size)
//                .collect(Collectors.toList());
//    }


    @Transactional(readOnly = true)
    public List<DocumentMetadataDTO> getPublicPracticeImages() {
        log.info("Fetching public practice images for guest user");

        // ✅ Dynamic approach - query database instead of static IDs
        return documentRepository.findAll().stream()
                .filter(this::isDocumentPubliclyAccessible)
                .filter(doc -> doc.getDocumentType() == DocumentEntity.DocumentType.IMAGE)
                .filter(doc -> isPracticeImage(doc))
                .map(this::convertToMetadataDTO)
                .collect(Collectors.toList());
    }

    // ✅ Helper method to check if document is a practice image
    private boolean isPracticeImage(DocumentEntity document) {
        if (document.getFilename() == null) {
            return false;
        }

        String filename = document.getFilename().toLowerCase();
        return filename.contains("practice") ||
                filename.contains("spiritual") ||
                filename.contains("meditation") ||
                filename.contains("yoga");
    }

    // ✅ Update your isDocumentPubliclyAccessible method to be more flexible
    private boolean isDocumentPubliclyAccessible(DocumentEntity document) {
        if (document == null) {
            return false;
        }

        // Option 1: Check if it's a valid image type
        boolean isValidImage = document.getDocumentType() == DocumentEntity.DocumentType.IMAGE &&
                document.getContentType() != null &&
                document.getContentType().startsWith("image/");

        // Option 2: Check filename pattern for practice images
        boolean hasPracticeFilename = isPracticeImage(document);

        // Option 3: You could also add a database field 'is_public' later
        // boolean isMarkedPublic = document.getIsPublic() != null && document.getIsPublic();

        return isValidImage && hasPracticeFilename;
    }

}