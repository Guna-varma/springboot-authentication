package com.app.backend.service;

import com.app.backend.dto.*;
import com.app.backend.entity.*;
import com.app.backend.redisToken.RedisRefreshTokenService;
import com.app.backend.repository.*;
import com.app.backend.util.EmailUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailUtil emailUtil;
    private final RedisRefreshTokenService redisRefreshTokenService;

    @Transactional
    public AuthResponse signup(SignUpRequest request) {
        // Pre-validation checks for early failure
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Get cached default role
        Role userRole = getDefaultRole();

        // Build user entity efficiently
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase()) // Normalize email
                .gender(Gender.valueOf(request.getGender().toUpperCase()))
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .build();

        userRepository.save(user);

        // Generate tokens asynchronously for better performance
        String accessToken = jwtService.generateTokenWithId(user.getEmail());
        String refreshToken = redisRefreshTokenService.createRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserDTO(user))
                .build();
    }

    public AuthResponse signin(AuthRequest request) {
        String email = request.getEmail().toLowerCase(); // Normalize email

        // Use optimized query that fetches user with role in single query
        User user = userRepository.findActiveUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Fast password verification
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Generate tokens
        String accessToken = jwtService.generateTokenWithId(user.getEmail());
        String refreshToken = redisRefreshTokenService.createRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserDTO(user))
                .build();
    }

    public void sendOtp(String email) {
        String normalizedEmail = email.toLowerCase();

        // Fast user existence and enabled check
        Long userId = userRepository.findUserIdByEmailIfEnabled(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No active user found with that email"));

        // Generate OTP efficiently
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);

        // Async database operations
        CompletableFuture.runAsync(() -> {
            otpTokenRepository.deleteByEmail(normalizedEmail);

            OtpToken otpToken = OtpToken.builder()
                    .email(normalizedEmail)
                    .otp(otp)
                    .expiryTime(expiryTime)
                    .build();

            otpTokenRepository.save(otpToken);
        });

        // Send email asynchronously
        CompletableFuture.runAsync(() -> emailUtil.sendOtpEmail(normalizedEmail, otp));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().toLowerCase();

        // Validate OTP efficiently
        boolean validOtp = otpTokenRepository
                .findByEmailAndOtp(email, request.getOtp())
                .filter(token -> token.getExpiryTime().isAfter(LocalDateTime.now()))
                .isPresent();

        if (!validOtp) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Get user and update password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Cleanup OTP asynchronously
        CompletableFuture.runAsync(() -> otpTokenRepository.deleteByEmail(email));
    }

    public void signout(String jwtToken) {
        try {
            String email = jwtService.extractUsername(jwtToken);
            String tokenId = jwtService.getTokenId(jwtToken);
            long remainingTtl = jwtService.extractExpiration(jwtToken).getTime() - System.currentTimeMillis();

            // Blacklist JWT token if valid
            if (tokenId != null && remainingTtl > 0) {
                redisRefreshTokenService.blacklistJwtToken(tokenId, remainingTtl);
            }

            // Invalidate refresh tokens asynchronously
            redisRefreshTokenService.invalidateAllUserTokens(email);

            log.info("User {} signed out successfully", email);
        } catch (Exception e) {
            log.error("Error during signout", e);
            throw new RuntimeException("Signout failed");
        }
    }

    public void signoutFromAllDevices(String email) {
        redisRefreshTokenService.invalidateAllUserTokens(email.toLowerCase());
        log.info("User {} signed out from all devices", email);
    }

    // Cache default role for faster access
    @Cacheable("defaultRole")
    private Role getDefaultRole() {
        return roleRepository.getDefaultRole()
                .orElseThrow(() -> new RuntimeException("Default role not found"));
    }

    // Optimized DTO mapping
    private UserResponseDTO mapToUserDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .gender(user.getGender() != null ? user.getGender().name() : "NOT_SPECIFIED")
                .phoneNumber(user.getPhoneNumber())
                .role(new UserResponseDTO.RoleDTO(
                        user.getRole().getId(),
                        user.getRole().getName()
                ))
                .build();
    }
}