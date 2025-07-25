package com.app.backend.controller;

import com.app.backend.dto.*;
import com.app.backend.entity.Gender;
import com.app.backend.entity.User;
import com.app.backend.redisToken.RedisRefreshTokenService;
import com.app.backend.redisToken.RefreshTokenRequest;
import com.app.backend.repository.UserRepository;
import com.app.backend.service.AuthService;
import com.app.backend.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final RedisRefreshTokenService redisRefreshTokenService;
    private final JwtService jwtService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.signin(request));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestBody EmailRequest request) {
        authService.sendOtp(request.getEmail());
        return ResponseEntity.ok("OTP sent successfully");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password reset successful");
    }

//    @PostMapping("/signout")
//    public ResponseEntity<String> signout(@RequestHeader("Authorization") String authHeader) {
//        String token = authHeader.replace("Bearer ", "");
//        authService.signout(token);
//        return ResponseEntity.ok("Successfully signed out");
//    }
    @PostMapping("/signout")
    public ResponseEntity<Map<String, String>> signout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request,
            HttpServletResponse response) {

        String token = authHeader.replace("Bearer ", "");
        authService.signout(token);

        // Clear refresh token cookie
        boolean isProd = !request.getServerName().contains("localhost");
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(isProd)
                .sameSite("Strict")
                .path("/") // Make available for all paths
                .maxAge(0) // Expire immediately
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of("message", "Successfully signed out"));
    }
//
//    @PostMapping("/refresh-token")
//    public ResponseEntity<AuthResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {
//        // 🔍 Get refreshToken from cookies
//        String refreshToken = null;
//        if (request.getCookies() != null) {
//            for (Cookie cookie : request.getCookies()) {
//                if (cookie.getName().equals("refreshToken")) {
//                    refreshToken = cookie.getValue();
//                    break;
//                }
//            }
//        }
//
//        if (refreshToken == null || !redisRefreshTokenService.validate(refreshToken)) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        String email = redisRefreshTokenService.getUserEmailFromToken(refreshToken);
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        // ✅ Generate new access & refresh tokens
//        String newAccessToken = jwtService.generateToken(email);
//        String newRefreshToken = redisRefreshTokenService.createRefreshToken(email);
//
//        // ✅ Delete old refresh token
//        redisRefreshTokenService.deleteRefreshToken(refreshToken);
//
//        boolean isProd = !request.getServerName().contains("localhost");
//        ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefreshToken)
//                .httpOnly(true)
////                .secure(true) //for production you set this as true
//                .secure(isProd)
//                .sameSite("Strict")
//                .path("/api/auth/refresh-token")
//                .maxAge(Duration.ofDays(7))
//                .build();
//        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
//
//        return ResponseEntity.ok(
//                AuthResponse.builder()
//                        .token(newAccessToken)
//                        .user(mapToUserDTO(user))
//                        .build()
//        );
//    }
//
//

@PostMapping("/refresh-token")
public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
    try {
        // 🔍 Get refreshToken from cookies
        String refreshToken = extractRefreshTokenFromCookies(request);

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token not found"));
        }

        // Validate refresh token exists in Redis
        if (!redisRefreshTokenService.validate(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }

        // Get user email and validate user exists
        String email = redisRefreshTokenService.getUserEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is still enabled
        if (!user.isEnabled()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User account is disabled"));
        }

        // ✅ Generate new tokens with ID for blacklisting support
        String newAccessToken = jwtService.generateTokenWithId(email);
        String newRefreshToken = redisRefreshTokenService.createRefreshToken(email);

        // ✅ Delete old refresh token (atomic operation)
        redisRefreshTokenService.deleteRefreshToken(refreshToken);

        // ✅ Set new refresh token cookie with correct path
        setRefreshTokenCookie(response, newRefreshToken, request);

        log.info("Token refreshed successfully for user: {}", email);

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .token(newAccessToken)
                        .user(mapToUserDTO(user))
                        .build()
        );

    } catch (Exception e) {
        log.error("Error during token refresh", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Token refresh failed"));
    }
}

    // Helper method to extract refresh token from cookies
    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.trim().isEmpty())
                .findFirst()
                .orElse(null);
    }

    // Helper method to set refresh token cookie
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, HttpServletRequest request) {
        boolean isProd = !request.getServerName().contains("localhost");

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(isProd) // HTTPS in production
                .sameSite("Strict")
                .path("/") // ✅ FIXED: Available for all paths, not just refresh endpoint
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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

    @PostMapping("/cleanup-tokens")
    public ResponseEntity<Map<String, String>> cleanupUserTokens(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);

        redisRefreshTokenService.invalidateAllUserTokens(email);

        return ResponseEntity.ok(Map.of(
                "message", "All refresh tokens invalidated",
                "email", email
        ));
    }


}
