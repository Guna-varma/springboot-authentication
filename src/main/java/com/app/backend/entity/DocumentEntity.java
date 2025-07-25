package com.app.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_user_upload_date", columnList = "uploadedByUserId, uploadedAt"),
        @Index(name = "idx_document_type", columnList = "documentType"),
        @Index(name = "idx_checksum_user", columnList = "sha256Checksum, uploadedByUserId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(length = 100, nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private Long uploadedByUserId;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    // ✅ FIXED: Add lazy loading to prevent automatic loading of binary data
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] data;

    @Column(length = 64)
    private String sha256Checksum;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum DocumentType {
        IMAGE, PDF
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (uploadedAt == null) {
            uploadedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructor for metadata-only queries (without binary data)
    public DocumentEntity(Long id, String filename, String contentType, Long size,
                          Long uploadedByUserId, LocalDateTime uploadedAt,
                          DocumentType documentType, String sha256Checksum) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.uploadedByUserId = uploadedByUserId;
        this.uploadedAt = uploadedAt;
        this.documentType = documentType;
        this.sha256Checksum = sha256Checksum;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}