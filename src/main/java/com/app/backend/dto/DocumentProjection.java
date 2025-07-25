package com.app.backend.dto;

import com.app.backend.entity.DocumentEntity;
import java.time.LocalDateTime;

public interface DocumentProjection {
    Long getId();
    String getFilename();
    String getContentType();
    Long getSize();
    Long getUploadedByUserId();
    LocalDateTime getUploadedAt();
    DocumentEntity.DocumentType getDocumentType();
    String getSha256Checksum();
}
