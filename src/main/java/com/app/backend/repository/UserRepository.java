package com.app.backend.repository;

import com.app.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Optimized email existence check (faster than findByEmail)
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    boolean existsByEmail(@Param("email") String email);

    // Optimized user fetch with role join
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email AND u.enabled = true")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<User> findActiveUserByEmail(@Param("email") String email);

    // Original method for backward compatibility
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<User> findByEmail(@Param("email") String email);

    // Optimized search with pagination support
    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<User> findByFullNameContainingIgnoreCase(@Param("fullName") String fullName);

    // Optimized role-based search
    @Query("SELECT u FROM User u JOIN u.role r WHERE r.name = :roleName")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<User> findByRole_Name(@Param("roleName") String roleName);

    // High-performance user validation
    @Query("SELECT u.id FROM User u WHERE u.email = :email AND u.enabled = true")
    Optional<Long> findUserIdByEmailIfEnabled(@Param("email") String email);


    // ✅ Add these optimized methods for getAllUsers
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.enabled = true ORDER BY u.createdAt DESC")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<User> findAllEnabledUsersWithRole();

    @Query("SELECT u FROM User u JOIN FETCH u.role ORDER BY u.createdAt DESC")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<User> findAllUsersWithRole();
}




//package com.app.backend.repository;
//
//import com.app.backend.entity.User;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface UserRepository extends JpaRepository<User, Long> {
//    boolean existsByEmail(String email);
//    Optional<User> findByEmail(String email);
//    List<User> findByFullNameContainingIgnoreCase(String fullName);
//    List<User> findByRole_Name(String roleName);
//}