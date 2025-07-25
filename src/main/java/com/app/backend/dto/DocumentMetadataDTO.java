package com.app.backend.dto;

import com.app.backend.entity.DocumentEntity;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentMetadataDTO {
    private Long id;
    private String filename;
    private String contentType;
    private Long size;
    private Long uploadedByUserId;
    private LocalDateTime uploadedAt;
    private DocumentEntity.DocumentType documentType;
    private String sha256Checksum;


    public DocumentMetadataDTO(Long id, String filename, String contentType, Long size, String string, LocalDateTime uploadedAt) {
    }

}