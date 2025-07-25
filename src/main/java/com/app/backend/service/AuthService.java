package com.app.backend.service;

import com.app.backend.dto.*;
import com.app.backend.entity.*;
import com.app.backend.redisToken.RedisRefreshTokenService;
import com.app.backend.repository.*;
import com.app.backend.util.EmailUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

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
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        Role userRole = roleRepository.findByName("REGISTERED_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .gender(Gender.valueOf(request.getGender().toUpperCase()))
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateTokenWithId(user.getEmail());
        String refreshToken = redisRefreshTokenService.createRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserDTO(user))
                .build();
    }

    public AuthResponse signin(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new RuntimeException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String accessToken = jwtService.generateTokenWithId(user.getEmail());
        String refreshToken = redisRefreshTokenService.createRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserDTO(user))
                .build();
    }

    public void sendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No user found with that email"));

        if (!user.isEnabled()) {
            throw new RuntimeException("User account is disabled");
        }

        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);

        otpTokenRepository.deleteByEmail(email);

        OtpToken otpToken = OtpToken.builder()
                .email(email)
                .otp(otp)
                .expiryTime(expiryTime)
                .build();

        otpTokenRepository.save(otpToken);
        emailUtil.sendOtpEmail(email, otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        boolean validOtp = otpTokenRepository
                .findByEmailAndOtp(request.getEmail(), request.getOtp())
                .filter(token -> token.getExpiryTime().isAfter(LocalDateTime.now()))
                .isPresent();

        if (!validOtp) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpTokenRepository.deleteByEmail(request.getEmail());
    }

    public void signout(String jwtToken) {
        try {
            String email = jwtService.extractUsername(jwtToken);
            String tokenId = jwtService.getTokenId(jwtToken);
            Date expiration = jwtService.extractExpiration(jwtToken);

            // Calculate remaining TTL
            long remainingTtl = expiration.getTime() - System.currentTimeMillis();

            // Blacklist the JWT token
            if (tokenId != null && remainingTtl > 0) {
                redisRefreshTokenService.blacklistJwtToken(tokenId, remainingTtl);
            }

            // Invalidate all refresh tokens for this user
            redisRefreshTokenService.invalidateAllUserTokens(email);

            log.info("User {} signed out successfully", email);
        } catch (Exception e) {
            log.error("Error during signout", e);
            throw new RuntimeException("Signout failed");
        }
    }

    public void signoutFromAllDevices(String email) {
        redisRefreshTokenService.invalidateAllUserTokens(email);
        log.info("User {} signed out from all devices", email);
    }

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

//
//
//import com.app.backend.dto.*;
//import com.app.backend.entity.*;
//import com.app.backend.redisToken.RedisRefreshTokenService;
//import com.app.backend.repository.*;
//import com.app.backend.util.EmailUtil;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.Set;
//import com.app.backend.dto.*;
//import com.app.backend.entity.*;
//import com.app.backend.repository.*;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import com.app.backend.entity.AuthProvider;
//
//@Service
//@RequiredArgsConstructor
//public class AuthService {
//
//    private final UserRepository userRepository;
//    private final RoleRepository roleRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final JwtService jwtService;
//    private final OtpTokenRepository otpTokenRepository;
//    private final EmailUtil emailUtil;
//    private final RedisRefreshTokenService redisRefreshTokenService;
//
//    @Transactional
//    public AuthResponse signup(SignUpRequest request) {
//        if (!request.getPassword().equals(request.getConfirmPassword())) {
//            throw new RuntimeException("Passwords do not match");
//        }
//
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new RuntimeException("Email already registered");
//        }
///// CODE NEED TO BE updating.....18062025 12:59ist
//        Role userRole = roleRepository.findByName("STUDENT")
//                .orElseThrow(() -> new RuntimeException("Default role not found"));
//
//
//        User user = User.builder()
//                .fullName(request.getFullName())
//                .email(request.getEmail())
//                .gender(request.getGender())
//                .phoneNumber(request.getPhoneNumber())
//                .password(passwordEncoder.encode(request.getPassword()))
//                .role(userRole)
//                .provider(AuthProvider.LOCAL)
//                .enabled(true)
//                .build();
//
//        userRepository.save(user);
//
//        String accessToken = jwtService.generateToken(user.getEmail());
//        String refreshToken = redisRefreshTokenService.createRefreshToken(user.getEmail());
//
//
//        return AuthResponse.builder()
//                .token(accessToken)
//                .refreshToken(refreshToken)
//                .refreshToken(refreshToken)
//                .user(mapToUserDTO(user))
//                .build();
//    }
//
//    public AuthResponse signin(AuthRequest request) {
//
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
//
//        if (!user.isEnabled()) {
//            throw new RuntimeException("User account is disabled");
//        }
//
//        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
//            throw new RuntimeException("Invalid email or password");
//        }
//
//        String accessToken = jwtService.generateToken(user.getEmail());
//        String refreshToken = redisRefreshTokenService.createRefreshToken(user.getEmail());
//
//        return AuthResponse.builder()
//                .token(accessToken)
//                .refreshToken(refreshToken)
//                .user(mapToUserDTO(user))
//                .build();
//    }
//
//    public void sendOtp(String email) {
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("No user found with that email"));
//
//        if (!user.isEnabled()) {
//            throw new RuntimeException("User account is disabled");
//        }
//
//        String otp = String.valueOf((int) (Math.random() * 900000) + 100000); // 6-digit OTP
//        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);
//
//        otpTokenRepository.deleteByEmail(email); // remove old OTP if exists
//
//        OtpToken otpToken = OtpToken.builder()
//                .email(email)
//                .otp(otp)
//                .expiryTime(expiryTime)
//                .build();
//
//        otpTokenRepository.save(otpToken);
//        emailUtil.sendOtpEmail(email, otp); // Custom utility
//    }
//
//    @Transactional
//    public void resetPassword(ResetPasswordRequest request) {
//        boolean validOtp = otpTokenRepository
//                .findByEmailAndOtp(request.getEmail(), request.getOtp())
//                .filter(token -> token.getExpiryTime().isAfter(java.time.LocalDateTime.now()))
//                .isPresent();
//
//        if (!validOtp) {
//            throw new RuntimeException("Invalid or expired OTP");
//        }
//
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
//        userRepository.save(user);
//
//        otpTokenRepository.deleteByEmail(request.getEmail());
//    }
//
//    public void signout(String token) {
//        // Optional: extract email from token and clear all refresh tokens from Redis
//        String email = jwtService.extractUsername(token);
////        redisRefreshTokenService.invalidateAllTokensForUser(email); // you can implement this if needed
//        System.out.println("Signed out token (client should discard it): " + token);
//    }
//
//    private UserResponseDTO mapToUserDTO(User user) {
//        return UserResponseDTO.builder()
//                .id(user.getId())
//                .fullName(user.getFullName())
//                .email(user.getEmail())
//                .gender(user.getGender())
//                .phoneNumber(user.getPhoneNumber())
//                .role(new UserResponseDTO.RoleDTO(
//                        user.getRole().getId(),
//                        user.getRole().getName()
//                ))
//                .build();
//    }
//}