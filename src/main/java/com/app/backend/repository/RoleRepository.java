package com.app.backend.repository;

import com.app.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query("SELECT r FROM Role r WHERE r.name = :name")
    @QueryHints({
            @QueryHint(name = "org.hibernate.cacheable", value = "true"),
            @QueryHint(name = "org.hibernate.cacheMode", value = "NORMAL")
    })
    Optional<Role> findByName(@Param("name") String name);

    // Pre-load default role for faster access
    @Query("SELECT r FROM Role r WHERE r.name = 'REGISTERED_USER'")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Role> getDefaultRole();
}



//package com.app.backend.repository;
//
//import com.app.backend.entity.Role;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//import java.util.Optional;
//
//@Repository
//public interface RoleRepository extends JpaRepository<Role, Long> {
//    Optional<Role> findByName(String name);
//}