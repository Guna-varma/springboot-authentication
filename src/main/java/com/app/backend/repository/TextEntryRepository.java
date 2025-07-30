package com.app.backend.repository;

import com.app.backend.entity.TextEntry;
import com.app.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TextEntryRepository extends JpaRepository<TextEntry, Long> {

    // ============= Single Entity Queries with User =============

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user WHERE t.id = :id")
    Optional<TextEntry> findByIdWithUser(@Param("id") Long id);

    // ============= List Queries with User (for caching) =============

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user ORDER BY t.createdAt DESC")
    List<TextEntry> findAllWithUserSimple();

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user WHERE LOWER(t.owner) LIKE LOWER(CONCAT('%', :owner, '%')) ORDER BY t.createdAt DESC")
    List<TextEntry> findByOwnerWithUserSimple(@Param("owner") String owner);

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user WHERE LOWER(t.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY t.createdAt DESC")
    List<TextEntry> findByMessageContainingWithUserSimple(@Param("searchTerm") String searchTerm);

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<TextEntry> findByCreatedAtBetweenWithUserSimple(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user WHERE t.user = :user ORDER BY t.createdAt DESC")
    List<TextEntry> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    // ============= Paginated Queries with User (for non-cached operations) =============

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user ORDER BY t.createdAt DESC")
    Page<TextEntry> findAllWithUser(Pageable pageable);

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user WHERE LOWER(t.owner) LIKE LOWER(CONCAT('%', :owner, '%'))")
    Page<TextEntry> findByOwnerWithUser(@Param("owner") String owner, Pageable pageable);

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user WHERE LOWER(t.message) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<TextEntry> findByMessageContainingWithUser(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user WHERE t.createdAt BETWEEN :startDate AND :endDate")
    Page<TextEntry> findByCreatedAtBetweenWithUser(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // ============= User-based Queries =============

    @Query("SELECT t FROM TextEntry t WHERE t.user = :user")
    Page<TextEntry> findByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT t FROM TextEntry t WHERE t.user.id = :userId")
    Page<TextEntry> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM TextEntry t WHERE t.user.email = :email ORDER BY t.createdAt DESC")
    List<TextEntry> findByUserEmail(@Param("email") String email);

    // ============= Count Queries =============

    long countByOwnerIgnoreCase(String owner);

    long countByUser(User user);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(t) FROM TextEntry t WHERE t.createdAt >= :sinceDate")
    long countRecentEntries(@Param("sinceDate") LocalDateTime sinceDate);

    @Query("SELECT COUNT(t) FROM TextEntry t WHERE LOWER(t.message) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    long countByMessageContaining(@Param("searchTerm") String searchTerm);

    // ============= Advanced Search Queries =============

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user WHERE " +
            "(:owner IS NULL OR LOWER(t.owner) LIKE LOWER(CONCAT('%', :owner, '%'))) AND " +
            "(:searchTerm IS NULL OR LOWER(t.message) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:startDate IS NULL OR t.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR t.createdAt <= :endDate) " +
            "ORDER BY t.createdAt DESC")
    List<TextEntry> findWithMultipleFilters(
            @Param("owner") String owner,
            @Param("searchTerm") String searchTerm,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user WHERE " +
            "(:owner IS NULL OR LOWER(t.owner) LIKE LOWER(CONCAT('%', :owner, '%'))) AND " +
            "(:searchTerm IS NULL OR LOWER(t.message) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:startDate IS NULL OR t.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR t.createdAt <= :endDate)")
    Page<TextEntry> findWithMultipleFilters(
            @Param("owner") String owner,
            @Param("searchTerm") String searchTerm,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // ============= Recent Entries Queries =============

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user WHERE t.createdAt >= :sinceDate ORDER BY t.createdAt DESC")
    List<TextEntry> findRecentEntries(@Param("sinceDate") LocalDateTime sinceDate, Pageable pageable);

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user WHERE t.createdAt >= :sinceDate ORDER BY t.createdAt DESC")
    List<TextEntry> findRecentEntriesSimple(@Param("sinceDate") LocalDateTime sinceDate);

    // ============= Statistics Queries =============

    @Query("SELECT DATE(t.createdAt) as date, COUNT(t) as count FROM TextEntry t " +
            "WHERE t.createdAt >= :startDate AND t.createdAt <= :endDate " +
            "GROUP BY DATE(t.createdAt) ORDER BY DATE(t.createdAt)")
    List<Object[]> getEntriesCountByDate(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t.owner, COUNT(t) as count FROM TextEntry t " +
            "GROUP BY t.owner ORDER BY COUNT(t) DESC")
    List<Object[]> getEntriesCountByOwner();

    @Query("SELECT EXTRACT(HOUR FROM t.createdAt) as hour, COUNT(t) as count FROM TextEntry t " +
            "WHERE t.createdAt >= :startDate " +
            "GROUP BY EXTRACT(HOUR FROM t.createdAt) ORDER BY EXTRACT(HOUR FROM t.createdAt)")
    List<Object[]> getEntriesCountByHour(@Param("startDate") LocalDateTime startDate);

    // ============= Maintenance Queries =============

    @Modifying
    @Transactional
    @Query("DELETE FROM TextEntry t WHERE t.createdAt < :cutoffDate")
    int deleteOldEntries(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Transactional
    @Query("UPDATE TextEntry t SET t.message = :newMessage WHERE t.id = :id")
    int updateMessage(@Param("id") Long id, @Param("newMessage") String newMessage);

    // ============= Existence Checks =============

    boolean existsByIdAndUser(Long id, User user);

    boolean existsByIdAndUserId(Long id, Long userId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TextEntry t WHERE t.id = :id AND t.user = :user")
    boolean existsByIdAndUserCustom(@Param("id") Long id, @Param("user") User user);

    // ============= Top/Most Popular Queries =============

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user ORDER BY LENGTH(t.message) DESC")
    List<TextEntry> findLongestEntries(Pageable pageable);

    @Query("SELECT t FROM TextEntry t JOIN FETCH t.user ORDER BY LENGTH(t.message) ASC")
    List<TextEntry> findShortestEntries(Pageable pageable);

    // ============= Legacy Support (for backward compatibility) =============

    Page<TextEntry> findByOwnerIgnoreCase(String owner, Pageable pageable);

    Page<TextEntry> findByOwnerContainingIgnoreCase(String owner, Pageable pageable);

    @Query("SELECT t FROM TextEntry t WHERE LOWER(t.message) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<TextEntry> findByMessageContaining(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT t FROM TextEntry t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    Page<TextEntry> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}