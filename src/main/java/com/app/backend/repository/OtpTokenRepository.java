package com.app.backend.repository;


import com.app.backend.entity.OtpToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByEmailAndOtp(String email, String otp);

    @Modifying
    @Transactional
    @Query("DELETE FROM OtpToken t WHERE t.email = :email")
    void deleteByEmail(@Param("email") String email);

}

