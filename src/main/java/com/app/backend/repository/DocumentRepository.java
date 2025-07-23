package com.app.backend.repository;

import com.app.backend.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    // Main method for getting all documents ordered by upload date
    @Query("SELECT d FROM DocumentEntity d ORDER BY d.uploadedAt DESC")
    List<DocumentEntity> findAllByOrderByUploadedAtDesc();

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
}




//
//@Repository
//public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
//
//    List<DocumentEntity> findAllByOrderByUploadedAtDesc();
//
//    List<DocumentEntity> findByDocumentTypeOrderByUploadedAtDesc(DocumentEntity.DocumentType documentType);
//
//    List<DocumentEntity> findByUploadedByUserIdOrderByUploadedAtDesc(Long userId);
//
//    boolean existsBySha256ChecksumAndUploadedByUserId(String checksum, Long userId);
//
//    // Delete multiple documents by IDs
//    @Modifying
//    @Transactional
//    @Query("DELETE FROM DocumentEntity d WHERE d.id IN :ids")
//    void deleteByIdIn(@Param("ids") List<Long> ids);
//
//    // Find existing IDs from a list
//    @Query("SELECT d.id FROM DocumentEntity d WHERE d.id IN :ids")
//    List<Long> findExistingIds(@Param("ids") List<Long> ids);
//
//    // Count documents by user
//    long countByUploadedByUserId(Long userId);
//
//    // Find documents by type and user
//    List<DocumentEntity> findByDocumentTypeAndUploadedByUserIdOrderByUploadedAtDesc(
//            DocumentEntity.DocumentType documentType, Long userId);
//}



//package com.app.backend.repository;
//
//import com.app.backend.entity.ImageEntity;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//public interface ImageRepository extends JpaRepository<ImageEntity, Long> {
//    // You can define custom queries if needed
//}
