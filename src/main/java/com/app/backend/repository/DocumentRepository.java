package com.app.backend.repository;

import com.app.backend.dto.DocumentProjection;
import com.app.backend.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    // ✅ Metadata-only query (excludes binary data)
    @Query("SELECT new com.app.backend.entity.DocumentEntity(d.id, d.filename, d.contentType, d.size, d.uploadedByUserId, d.uploadedAt, d.documentType, d.sha256Checksum) FROM DocumentEntity d ORDER BY d.uploadedAt DESC")
    List<DocumentEntity> findAllMetadataByOrderByUploadedAtDesc();

    // ✅ Paginated metadata query
    @Query("SELECT new com.app.backend.entity.DocumentEntity(d.id, d.filename, d.contentType, d.size, d.uploadedByUserId, d.uploadedAt, d.documentType, d.sha256Checksum) FROM DocumentEntity d ORDER BY d.uploadedAt DESC")
    Page<DocumentEntity> findAllMetadataPageable(Pageable pageable);

    // ✅ Search by filename (metadata only)
    @Query("SELECT new com.app.backend.entity.DocumentEntity(d.id, d.filename, d.contentType, d.size, d.uploadedByUserId, d.uploadedAt, d.documentType, d.sha256Checksum) FROM DocumentEntity d WHERE LOWER(d.filename) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY d.uploadedAt DESC")
    List<DocumentEntity> findMetadataByFilenameContaining(@Param("search") String search);

    // ✅ Filter by type (metadata only)
    @Query("SELECT new com.app.backend.entity.DocumentEntity(d.id, d.filename, d.contentType, d.size, d.uploadedByUserId, d.uploadedAt, d.documentType, d.sha256Checksum) FROM DocumentEntity d WHERE d.documentType = :type ORDER BY d.uploadedAt DESC")
    List<DocumentEntity> findMetadataByDocumentType(@Param("type") DocumentEntity.DocumentType type);

    // Original query for when you need actual binary data
    @Query("SELECT d FROM DocumentEntity d WHERE d.id = :id")
    Optional<DocumentEntity> findByIdWithData(@Param("id") Long id);

    // Fallback method in case the main query fails
    @Query("SELECT d FROM DocumentEntity d ORDER BY d.id DESC")
    List<DocumentEntity> findAllByOrderByIdDesc();

    // ✅ Alternative: Using projection interface (even better approach)
    @Query("SELECT d.id as id, d.filename as filename, d.contentType as contentType, d.size as size, d.uploadedByUserId as uploadedByUserId, d.uploadedAt as uploadedAt, d.documentType as documentType, d.sha256Checksum as sha256Checksum FROM DocumentEntity d ORDER BY d.uploadedAt DESC")
    List<DocumentProjection> findAllDocumentProjections();

    // Keep the original for when you actually need the binary data
    @Query("SELECT d FROM DocumentEntity d ORDER BY d.uploadedAt DESC")
    List<DocumentEntity> findAllByOrderByUploadedAtDesc();

    // Paginated metadata-only query
    @Query("SELECT new com.app.backend.entity.DocumentEntity(d.id, d.filename, d.contentType, d.size, d.uploadedByUserId, d.uploadedAt, d.documentType, d.sha256Checksum) FROM DocumentEntity d ORDER BY d.uploadedAt DESC")
    Page<DocumentEntity> findAllMetadataByOrderByUploadedAtDesc(Pageable pageable);

    // Count query (safe to use)
    @Query("SELECT COUNT(d) FROM DocumentEntity d")
    long countAllDocuments();

    // Document type filtering with ordering
    @Query("SELECT d FROM DocumentEntity d WHERE d.documentType = :documentType ORDER BY d.uploadedAt DESC")
    List<DocumentEntity> findByDocumentTypeOrderByUploadedAtDesc(@Param("documentType") DocumentEntity.DocumentType documentType);

    // User-specific documents with ordering
    @Query("SELECT d FROM DocumentEntity d WHERE d.uploadedByUserId = :userId ORDER BY d.uploadedAt DESC")
    List<DocumentEntity> findByUploadedByUserIdOrderByUploadedAtDesc(@Param("userId") Long userId);

    // Duplicate detection
    boolean existsBySha256ChecksumAndUploadedByUserId(String checksum, Long userId);

    // Batch deletion
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentEntity d WHERE d.id IN :ids")
    void deleteByIdIn(@Param("ids") List<Long> ids);

    // Check existing IDs
    @Query("SELECT d.id FROM DocumentEntity d WHERE d.id IN :ids")
    List<Long> findExistingIds(@Param("ids") List<Long> ids);

    // Count user documents
    long countByUploadedByUserId(Long userId);

    // Combined filter: type + user
    @Query("SELECT d FROM DocumentEntity d WHERE d.documentType = :documentType AND d.uploadedByUserId = :userId ORDER BY d.uploadedAt DESC")
    List<DocumentEntity> findByDocumentTypeAndUploadedByUserIdOrderByUploadedAtDesc(
            @Param("documentType") DocumentEntity.DocumentType documentType,
            @Param("userId") Long userId);

    // Count by document type - ONLY ADD THIS if it's missing
    @Query("SELECT COUNT(d) FROM DocumentEntity d WHERE d.documentType = :type")
    long countByDocumentType(@Param("type") DocumentEntity.DocumentType type);

    @Query("SELECT COUNT(d) > 0 FROM DocumentEntity d WHERE d.id = :id AND d.uploadedByUserId = :userId")
    boolean existsByIdAndUploadedByUserId(@Param("id") Long id, @Param("userId") Long userId);

    // In DocumentRepository
    @Query("SELECT d FROM DocumentEntity d WHERE " +
            "d.documentType = 'IMAGE' AND " +
            "(LOWER(d.filename) LIKE %:keyword1% OR LOWER(d.filename) LIKE %:keyword2%) " +
            "ORDER BY d.uploadedAt DESC")
    List<DocumentEntity> findPracticeImagesPaginated(
            @Param("keyword1") String keyword1,
            @Param("keyword2") String keyword2,
            Pageable pageable
    );

}