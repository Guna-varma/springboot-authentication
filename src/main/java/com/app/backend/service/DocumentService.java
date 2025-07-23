package com.app.backend.service;

import com.app.backend.config.DocumentConfigProperties;
import com.app.backend.dto.*;
import com.app.backend.entity.DocumentEntity;
import com.app.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    // ... [Keep all your existing methods unchanged] ...

    public DocumentEntity getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + id));
    }

    public List<DocumentEntity> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadedAtDesc();
    }

//    public List<DocumentEntity> getDocumentsByType(DocumentEntity.DocumentType type) {
//        return documentRepository.findByDocumentTypeOrderByUploadedAtDesc(type);
//    }

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


//    public List<DocumentEntity> getDocumentsByUser(Long userId) {
//        return documentRepository.findByUploadedByUserIdOrderByUploadedAtDesc(userId);
//    }

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

//    @Transactional
//    public void deleteMultipleDocuments(List<Long> ids) {
//        if (ids == null || ids.isEmpty()) {
//            throw new IllegalArgumentException("No document IDs provided for deletion.");
//        }
//
//        List<Long> existingIds = documentRepository.findExistingIds(ids);
//        List<Long> nonExistingIds = ids.stream()
//                .filter(id -> !existingIds.contains(id))
//                .collect(Collectors.toList());
//
//        if (!nonExistingIds.isEmpty()) {
//            throw new IllegalArgumentException("Documents not found with IDs: " + nonExistingIds);
//        }
//
//        documentRepository.deleteByIdIn(ids);
//        log.info("Multiple documents deleted successfully. Count: {}", ids.size());
//    }


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
}


//package com.app.backend.service;
//
//import com.app.backend.config.DocumentConfigProperties;
//import com.app.backend.dto.*;
//import com.app.backend.entity.DocumentEntity;
//import com.app.backend.repository.DocumentRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.tika.Tika;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class DocumentService {
//
//    private final DocumentRepository documentRepository;
//    private final DocumentConfigProperties config;
//
//    private static final Tika tika = new Tika();
//
//    @Transactional
//    public DocumentEntity saveDocument(MultipartFile file, Long userId) throws Exception {
//        validateFile(file);
//
//        String mimeType = detectMimeType(file);
//        DocumentEntity.DocumentType documentType = determineDocumentType(mimeType);
//        validateFileSize(file, documentType);
//
//        String checksum = calculateSHA256(file.getBytes());
//
//        // Check for duplicates
//        if (documentRepository.existsBySha256ChecksumAndUploadedByUserId(checksum, userId)) {
//            throw new IllegalArgumentException("Duplicate file detected. This file has already been uploaded.");
//        }
//
//        try {
//            DocumentEntity document = DocumentEntity.builder()
//                    .filename(sanitizeFilename(file.getOriginalFilename()))
//                    .contentType(mimeType)
//                    .size(file.getSize())
//                    .uploadedByUserId(userId)
//                    .uploadedAt(LocalDateTime.now())
//                    .documentType(documentType)
//                    .data(file.getBytes())
//                    .sha256Checksum(checksum)
//                    .build();
//
//            DocumentEntity saved = documentRepository.save(document);
//
//            log.info("Document uploaded successfully. ID: {}, Type: {}, Size: {}MB, User: {}",
//                    saved.getId(), documentType,
//                    String.format("%.2f", file.getSize() / (1024.0 * 1024.0)), userId);
//
//            return saved;
//        } catch (Exception ex) {
//            log.error("Failed to save document to database for user: {}", userId, ex);
//            throw new Exception("Failed to store document to database.", ex);
//        }
//    }
//
//    @Transactional
//    public MultipleUploadResponseDTO saveMultipleDocuments(List<MultipartFile> files, Long userId) {
//        // Validate input
//        if (files == null || files.isEmpty()) {
//            throw new IllegalArgumentException("No files provided for upload.");
//        }
//
//        if (files.size() > config.getMaxBatchSize()) {
//            throw new IllegalArgumentException(
//                    String.format("Batch size %d exceeds maximum limit of %d files.",
//                            files.size(), config.getMaxBatchSize()));
//        }
//
//        List<DocumentMetadataDTO> successfulUploads = new ArrayList<>();
//        List<FailedUploadDTO> failedUploads = new ArrayList<>();
//
//        log.info("Starting batch upload of {} files for user: {}", files.size(), userId);
//
//        for (int i = 0; i < files.size(); i++) {
//            MultipartFile file = files.get(i);
//            String filename = file.getOriginalFilename();
//
//            try {
//                // Skip empty files
//                if (file.isEmpty()) {
//                    failedUploads.add(FailedUploadDTO.builder()
//                            .filename(filename != null ? filename : "File #" + (i + 1))
//                            .errorMessage("File is empty")
//                            .errorCode("EMPTY_FILE")
//                            .fileSize(file.getSize())
//                            .build());
//                    continue;
//                }
//
//                // Validate filename
//                if (filename == null || filename.trim().isEmpty()) {
//                    failedUploads.add(FailedUploadDTO.builder()
//                            .filename("File #" + (i + 1))
//                            .errorMessage("File name is missing")
//                            .errorCode("MISSING_FILENAME")
//                            .fileSize(file.getSize())
//                            .build());
//                    continue;
//                }
//
//                DocumentEntity saved = saveDocument(file, userId);
//                successfulUploads.add(convertToMetadataDTO(saved));
//
//            } catch (IllegalArgumentException ex) {
//                log.warn("Validation failed for file: {} - {}", filename, ex.getMessage());
//                failedUploads.add(FailedUploadDTO.builder()
//                        .filename(filename != null ? filename : "File #" + (i + 1))
//                        .errorMessage(ex.getMessage())
//                        .errorCode("VALIDATION_ERROR")
//                        .fileSize(file.getSize())
//                        .build());
//
//            } catch (Exception ex) {
//                log.error("Failed to upload file: {}", filename, ex);
//                failedUploads.add(FailedUploadDTO.builder()
//                        .filename(filename != null ? filename : "File #" + (i + 1))
//                        .errorMessage("Upload failed: " + ex.getMessage())
//                        .errorCode("UPLOAD_ERROR")
//                        .fileSize(file.getSize())
//                        .build());
//            }
//        }
//
//        String summary = String.format("Upload completed: %d successful, %d failed out of %d total files",
//                successfulUploads.size(), failedUploads.size(), files.size());
//
//        log.info("Batch upload completed for user: {}. {}", userId, summary);
//
//        return MultipleUploadResponseDTO.builder()
//                .successfulUploads(successfulUploads)
//                .failedUploads(failedUploads)
//                .summary(summary)
//                .totalFiles(files.size())
//                .successCount(successfulUploads.size())
//                .failureCount(failedUploads.size())
//                .build();
//    }
//
//    public DocumentEntity getDocumentById(Long id) {
//        return documentRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + id));
//    }
//
//    public List<DocumentEntity> getAllDocuments() {
//        return documentRepository.findAllByOrderByUploadedAtDesc();
//    }
//
//    public List<DocumentEntity> getDocumentsByType(DocumentEntity.DocumentType type) {
//        return documentRepository.findByDocumentTypeOrderByUploadedAtDesc(type);
//    }
//
//    public List<DocumentEntity> getDocumentsByUser(Long userId) {
//        return documentRepository.findByUploadedByUserIdOrderByUploadedAtDesc(userId);
//    }
//
//    @Transactional
//    public void deleteDocumentById(Long id) {
//        if (!documentRepository.existsById(id)) {
//            throw new IllegalArgumentException("Document not found with ID: " + id);
//        }
//
//        documentRepository.deleteById(id);
//        log.info("Document deleted successfully. ID: {}", id);
//    }
//
//    @Transactional
//    public void deleteMultipleDocuments(List<Long> ids) {
//        if (ids == null || ids.isEmpty()) {
//            throw new IllegalArgumentException("No document IDs provided for deletion.");
//        }
//
//        List<Long> existingIds = documentRepository.findExistingIds(ids);
//        List<Long> nonExistingIds = ids.stream()
//                .filter(id -> !existingIds.contains(id))
//                .collect(Collectors.toList());
//
//        if (!nonExistingIds.isEmpty()) {
//            throw new IllegalArgumentException("Documents not found with IDs: " + nonExistingIds);
//        }
//
//        documentRepository.deleteByIdIn(ids);
//        log.info("Multiple documents deleted successfully. Count: {}", ids.size());
//    }
//
////    public List<DocumentMetadataDTO> getAllDocumentsMetadata() {
////        return documentRepository.findAllByOrderByUploadedAtDesc().stream()
////                .map(this::convertToMetadataDTO)
////                .collect(Collectors.toList());
////    }
//
//    public List<DocumentMetadataDTO> getAllDocumentsMetadata() {
//        // Simple approach first
//        List<DocumentEntity> documents = documentRepository.findAll();
//
//        return documents.stream()
//                .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt())) // Sort in memory
//                .map(this::convertToMetadataDTO)
//                .collect(Collectors.toList());
//    }
//
//
//    public List<DocumentMetadataDTO> getDocumentsMetadataByUser(Long userId) {
//        return documentRepository.findByUploadedByUserIdOrderByUploadedAtDesc(userId).stream()
//                .map(this::convertToMetadataDTO)
//                .collect(Collectors.toList());
//    }
//
//    public List<DocumentMetadataDTO> getDocumentsMetadataByTypeAndUser(
//            DocumentEntity.DocumentType type, Long userId) {
//        return documentRepository.findByDocumentTypeAndUploadedByUserIdOrderByUploadedAtDesc(type, userId)
//                .stream()
//                .map(this::convertToMetadataDTO)
//                .collect(Collectors.toList());
//    }
//
//    public long getDocumentCountByUser(Long userId) {
//        return documentRepository.countByUploadedByUserId(userId);
//    }
//
//    // Private helper methods
//    private void validateFile(MultipartFile file) {
//        if (file == null || file.isEmpty()) {
//            throw new IllegalArgumentException("File is empty or missing.");
//        }
//
//        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
//            throw new IllegalArgumentException("File name is missing.");
//        }
//
//        // Additional validation for maximum file size
//        long maxFileSize = Math.max(config.getMaxImageSize(), config.getMaxPdfSize());
//        if (file.getSize() > maxFileSize) {
//            throw new IllegalArgumentException(
//                    String.format("File size %.2fMB exceeds maximum allowed size of %.2fMB.",
//                            file.getSize() / (1024.0 * 1024.0),
//                            maxFileSize / (1024.0 * 1024.0)));
//        }
//    }
//
//    private String detectMimeType(MultipartFile file) throws IOException {
//        try {
//            String mimeType = tika.detect(file.getInputStream());
//            if (mimeType == null || mimeType.isEmpty()) {
//                throw new IllegalArgumentException("Unable to determine file type.");
//            }
//            log.debug("Detected MIME type: {} for file: {}", mimeType, file.getOriginalFilename());
//            return mimeType;
//        } catch (IOException ex) {
//            log.error("Error detecting MIME type for file: {}", file.getOriginalFilename(), ex);
//            throw new IOException("Failed to detect file type.", ex);
//        }
//    }
//
//    private DocumentEntity.DocumentType determineDocumentType(String mimeType) {
//        if (config.getAllowedImageTypes().contains(mimeType)) {
//            return DocumentEntity.DocumentType.IMAGE;
//        } else if (config.getAllowedPdfTypes().contains(mimeType)) {
//            return DocumentEntity.DocumentType.PDF;
//        } else {
//            String allowedTypes = String.join(", ", config.getAllowedImageTypes()) +
//                    ", " + String.join(", ", config.getAllowedPdfTypes());
//            throw new IllegalArgumentException(
//                    String.format("Unsupported file type: %s. Allowed types: %s", mimeType, allowedTypes));
//        }
//    }
//
//    private void validateFileSize(MultipartFile file, DocumentEntity.DocumentType type) {
//        long maxSize = (type == DocumentEntity.DocumentType.IMAGE) ?
//                config.getMaxImageSize() : config.getMaxPdfSize();
//
//        String typeStr = type.toString().toLowerCase();
//
//        if (file.getSize() > maxSize) {
//            String limitStr = String.format("%.1fMB", maxSize / (1024.0 * 1024.0));
//            String actualSize = String.format("%.2fMB", file.getSize() / (1024.0 * 1024.0));
//            throw new IllegalArgumentException(
//                    String.format("File size %s exceeds %s limit for %s files.",
//                            actualSize, limitStr, typeStr));
//        }
//
//        log.debug("File size validation passed for {}: {}MB (limit: {}MB)",
//                typeStr, file.getSize() / (1024.0 * 1024.0), maxSize / (1024.0 * 1024.0));
//    }
//
//    private String sanitizeFilename(String filename) {
//        if (filename == null) return "unnamed_file";
//
//        // Remove potentially dangerous characters and preserve file extension
//        String baseName = filename.contains(".") ?
//                filename.substring(0, filename.lastIndexOf('.')) : filename;
//        String extension = filename.contains(".") ?
//                filename.substring(filename.lastIndexOf('.')) : "";
//
//        String sanitizedBase = baseName.replaceAll("[^a-zA-Z0-9._-]", "_")
//                .replaceAll("_{2,}", "_") // Replace multiple underscores with single
//                .trim();
//
//        // Ensure filename isn't too long (max 255 characters for most filesystems)
//        String sanitizedFilename = sanitizedBase + extension;
//        if (sanitizedFilename.length() > 200) {
//            sanitizedFilename = sanitizedBase.substring(0, 200 - extension.length()) + extension;
//        }
//
//        log.debug("Sanitized filename: {} -> {}", filename, sanitizedFilename);
//        return sanitizedFilename;
//    }
//
//    private String calculateSHA256(byte[] data) throws NoSuchAlgorithmException {
//        try {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hash = digest.digest(data);
//            return Base64.getEncoder().encodeToString(hash);
//        } catch (NoSuchAlgorithmException ex) {
//            log.error("SHA-256 algorithm not available", ex);
//            throw ex;
//        }
//    }
//
//    private DocumentMetadataDTO convertToMetadataDTO(DocumentEntity document) {
//        return new DocumentMetadataDTO(
//                document.getId(),
//                document.getFilename(),
//                document.getContentType(),
//                document.getSize(),
//                document.getUploadedByUserId(),
//                document.getUploadedAt(),
//                document.getDocumentType(),
//                document.getSha256Checksum()
//        );
//    }
//
//    // Utility methods for configuration access
//    public DocumentConfigProperties getConfiguration() {
//        return config;
//    }
//
//    public Map<String, Object> getConfigurationInfo() {
//        Map<String, Object> configInfo = new HashMap<>();
//        configInfo.put("maxImageSize", String.format("%.1fMB", config.getMaxImageSize() / (1024.0 * 1024.0)));
//        configInfo.put("maxPdfSize", String.format("%.1fMB", config.getMaxPdfSize() / (1024.0 * 1024.0)));
//        configInfo.put("maxBatchSize", config.getMaxBatchSize());
//        configInfo.put("allowedImageTypes", config.getAllowedImageTypes());
//        configInfo.put("allowedPdfTypes", config.getAllowedPdfTypes());
//        configInfo.put("uploadPath", config.getUploadPath());
//        return configInfo;
//    }
//}