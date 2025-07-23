package com.app.backend.entity;

import jakarta.persistence.*;
import lombok.*;
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
    private Long size; // in bytes

    @Column(nullable = false)
    private Long uploadedByUserId;

//    // Audit fields for production tracking
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Lob
    @Column(nullable = false)
    private byte[] data;

    // Optional: Add checksum for integrity verification
    @Column(length = 64)
    private String sha256Checksum;

    // Add with default value to handle existing records
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
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
    }

}



//package com.app.backend.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "images")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ImageEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String filename;
//
//    @Column(length = 100)
//    private String contentType;
//
//    private Long size; // in bytes
//
//    private Long uploadedByUserId;
//
//    private LocalDateTime uploadedAt;
//
//    @Lob
//    @Column(nullable = false)
//    private byte[] data;
//}
