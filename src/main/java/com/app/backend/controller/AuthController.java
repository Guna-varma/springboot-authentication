package com.app.backend.controller;

import com.app.backend.dto.*;
import com.app.backend.entity.User;
import com.app.backend.redisToken.RedisRefreshTokenService;
import com.app.backend.repository.UserRepository;
import com.app.backend.service.AuthService;
import com.app.backend.service.JwtService;
import com.app.backend.service.RateLimitService;
import com.app.backend.util.WebUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    private final RateLimitService rateLimitService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignUpRequest request,
                                               HttpServletRequest httpRequest) {
        // Rate limiting for signup
        String clientIp = WebUtils.getClientIpAddress(httpRequest);
        if (!rateLimitService.isAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Remaining", "0")
                    .header("Retry-After", "60")
                    .build();
        }

        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingRequests(clientIp)))
                .body(authService.signup(request));
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@RequestBody AuthRequest request,
                                               HttpServletRequest httpRequest) {
        // Rate limiting for signin
        String clientIp = WebUtils.getClientIpAddress(httpRequest);
        if (!rateLimitService.isAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Remaining", "0")
                    .header("Retry-After", "60")
                    .build();
        }

        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingRequests(clientIp)))
                .body(authService.signin(request));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestBody EmailRequest request,
                                          HttpServletRequest httpRequest) {
        // Strict rate limiting for OTP
        String clientIp = WebUtils.getClientIpAddress(httpRequest);
        if (!rateLimitService.isAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Remaining", "0")
                    .header("Retry-After", "60")
                    .body("Rate limit exceeded");
        }

        authService.sendOtp(request.getEmail());
        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingRequests(clientIp)))
                .body("OTP sent successfully");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request,
                                                HttpServletRequest httpRequest) {
        String clientIp = WebUtils.getClientIpAddress(httpRequest);
        if (!rateLimitService.isAllowed(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Remaining", "0")
                    .header("Retry-After", "60")
                    .body("Rate limit exceeded");
        }

        authService.resetPassword(request);
        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(rateLimitService.getRemainingRequests(clientIp)))
                .body("Password reset successful");
    }

    @PostMapping("/signout")
    public ResponseEntity<Map<String, String>> signout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request,
            HttpServletResponse response) {

        String token = authHeader.replace("Bearer ", "");

        // Asynchronous signout for better performance
        CompletableFuture.runAsync(() -> authService.signout(token));

        // Clear refresh token cookie immediately
        clearRefreshTokenCookie(response, request);

        return ResponseEntity.ok(Map.of("message", "Successfully signed out"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Fast token extraction
            String refreshToken = extractRefreshTokenFromCookies(request);
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh token not found"));
            }

            // Validate token existence in Redis (fast)
            if (!redisRefreshTokenService.validate(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired refresh token"));
            }

            // Get user email and validate user (optimized query)
            String email = redisRefreshTokenService.getUserEmailFromToken(refreshToken);
            User user = userRepository.findActiveUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found or disabled"));

            // Generate new tokens
            String newAccessToken = jwtService.generateTokenWithId(email);
            String newRefreshToken = redisRefreshTokenService.createRefreshToken(email);

            // Atomic token rotation
            redisRefreshTokenService.deleteRefreshToken(refreshToken);
            setRefreshTokenCookie(response, newRefreshToken, request);

            log.debug("Token refreshed successfully for user: {}", email);

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

    @PostMapping("/cleanup-tokens")
    public ResponseEntity<Map<String, String>> cleanupUserTokens(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);

        // Asynchronous cleanup
        CompletableFuture.runAsync(() -> redisRefreshTokenService.invalidateAllUserTokens(email));

        return ResponseEntity.ok(Map.of(
                "message", "All refresh tokens invalidated",
                "email", email
        ));
    }

    // Optimized helper methods
    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.trim().isEmpty())
                .findFirst()
                .orElse(null);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, HttpServletRequest request) {
        boolean isProd = !request.getServerName().contains("localhost");

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(isProd)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response, HttpServletRequest request) {
        boolean isProd = !request.getServerName().contains("localhost");

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(isProd)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
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
}