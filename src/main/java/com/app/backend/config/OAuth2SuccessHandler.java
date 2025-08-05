package com.app.backend.config;

import com.app.backend.dto.OAuth2Data;
import com.app.backend.dto.OAuth2Response;
import com.app.backend.dto.UserInfo;
import com.app.backend.entity.AuthProvider;
import com.app.backend.entity.Gender;
import com.app.backend.entity.Role;
import com.app.backend.entity.User;
import com.app.backend.redisToken.RedisRefreshTokenService;
import com.app.backend.repository.RoleRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final RedisRefreshTokenService redisRefreshTokenService;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.refresh-token.expiry-days:7}")
    private int refreshTokenExpiryDays;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = extractEmail(oAuth2User);
            String name = extractName(oAuth2User);

            if (email == null || email.trim().isEmpty()) {
                handleError(response, "Email not provided by OAuth2 provider");
                return;
            }

            // Get or create user
            User user = getOrCreateUser(email, name);

            // Generate tokens
            String jwt = jwtService.generateToken(user.getEmail());
            String refreshToken = redisRefreshTokenService.createRefreshToken(email);

            // Set refresh token cookie
            setRefreshTokenCookie(response, refreshToken);

            // Determine response type based on request source
            if (isDirectBackendAccess(request)) {
                // Return JSON for direct backend access
                handleJsonResponse(response, jwt, user);
            } else {
                // Redirect to frontend for UI-initiated login
                handleFrontendRedirect(response, jwt);
            }

            log.info("OAuth2 authentication successful for user: {} with provider: {}",
                    email, user.getProvider());

        } catch (Exception e) {
            log.error("OAuth2 authentication failed", e);
            handleError(response, "Authentication failed: " + e.getMessage());
        }
    }

    private boolean isDirectBackendAccess(HttpServletRequest request) {
        // Check if request came directly to backend (no referrer from frontend)
        String referer = request.getHeader("Referer");
        String userAgent = request.getHeader("User-Agent");

        // If no referrer or referrer doesn't contain frontend URL, it's direct backend access
        if (referer == null) {
            log.debug("No referrer found - treating as direct backend access");
            return true;
        }

        // If referrer contains backend URL (like localhost:8080), it's direct access
        if (referer.contains("localhost:8080") || referer.contains(request.getServerName() + ":" + request.getServerPort())) {
            log.debug("Referrer from backend - treating as direct backend access");
            return true;
        }

        log.debug("Referrer from frontend: {} - treating as frontend-initiated", referer);
        return false;
    }

    private void handleJsonResponse(HttpServletResponse response, String jwt, User user) throws IOException {
        // Create response object
        OAuth2Response authResponse = OAuth2Response.builder()
                .success(true)
                .message("Authentication successful")
                .data(OAuth2Data.builder()
                        .accessToken(jwt)
//                        .tokenType("Bearer")
                        .expiresIn(jwtService.getExpirationTime())
                        .user(UserInfo.builder()
//                                .id(user.getId())
                                .email(user.getEmail())
                                .fullName(user.getFullName())
                                .gender(user.getGender().name())
//                                .phoneNumber(user.getPhoneNumber())
                                .role(user.getRole().getName())
                                .provider(user.getProvider().name())
                                .enabled(user.isEnabled())
//                                .createdAt(user.getCreatedAt())
                                .build())
                        .build())
                .build();

        // Set response headers for JSON
        setJsonResponseHeaders(response);

        // Write JSON response
        objectMapper.writeValue(response.getWriter(), authResponse);

        log.info("Returned JSON response for direct backend access");
    }

    private void handleFrontendRedirect(HttpServletResponse response, String jwt) throws IOException {
        // Redirect to frontend with token
        String redirectUrl = frontendUrl + "/oauth2/redirect?token=" + jwt;

        log.info("Redirecting to frontend: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private String extractEmail(OAuth2User oAuth2User) {
        return oAuth2User.getAttribute("email");
    }

    private String extractName(OAuth2User oAuth2User) {
        String name = oAuth2User.getAttribute("name");
        return name != null ? name : "Unknown User";
    }

    private User getOrCreateUser(String email, String name) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        return optionalUser.orElseGet(() -> {
            log.info("Creating new user for email: {}", email);

            Role defaultRole = roleRepository.findByName("REGISTERED_USER")
                    .orElseThrow(() -> new RuntimeException("Default role 'REGISTERED_USER' not found"));

            User newUser = User.builder()
                    .email(email)
                    .fullName(name)
                    .gender(Gender.NOT_SPECIFIED)
                    .enabled(true)
                    .provider(AuthProvider.GOOGLE)
                    .role(defaultRole)
                    .password(UUID.randomUUID().toString())
                    .build();

            User savedUser = userRepository.save(newUser);
            log.info("Successfully created new user with ID: {} for email: {}",
                    savedUser.getId(), email);

            return savedUser;
        });
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // Set to false for localhost development
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(refreshTokenExpiryDays * 24 * 60 * 60);

        response.addCookie(refreshTokenCookie);
        log.debug("Refresh token cookie set with expiry: {} days", refreshTokenExpiryDays);
    }

    private void setJsonResponseHeaders(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", frontendUrl);
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    private void handleError(HttpServletResponse response, String errorMessage) throws IOException {
        OAuth2Response errorResponse = OAuth2Response.builder()
                .success(false)
                .message(errorMessage)
                .data(null)
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        setJsonResponseHeaders(response);
        objectMapper.writeValue(response.getWriter(), errorResponse);

        log.error("OAuth2 authentication error: {}", errorMessage);
    }
}




//package com.app.backend.config;
//
//import com.app.backend.entity.AuthProvider;
//import com.app.backend.entity.Gender;
//import com.app.backend.entity.Role;
//import com.app.backend.entity.User;
//import com.app.backend.redisToken.RedisRefreshTokenService;
//import com.app.backend.repository.RoleRepository;
//import com.app.backend.repository.UserRepository;
//import com.app.backend.service.JwtService;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.Optional;
//import java.util.UUID;
//import lombok.extern.slf4j.Slf4j;
//
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
//
//    private final UserRepository userRepository;
//    private final RoleRepository roleRepository;
//    private final JwtService jwtService;
//    private final RedisRefreshTokenService redisRefreshTokenService;
//
//
//    @Override
//    public void onAuthenticationSuccess(HttpServletRequest request,
//                                        HttpServletResponse response,
//                                        Authentication authentication) throws IOException, ServletException {
//
//        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
//        String email = oAuth2User.getAttribute("email");
//        String name = oAuth2User.getAttribute("name");
//
//        Optional<User> optionalUser = userRepository.findByEmail(email);
//
//        User user = optionalUser.orElseGet(() -> {
//            Role defaultRole = roleRepository.findByName("REGISTERED_USER")
//                    .orElseThrow(() -> new RuntimeException("Default role not found"));
//
//            User newUser = User.builder()
//                    .email(email)
//                    .fullName(name)
//                    .gender(Gender.NOT_SPECIFIED)
//                    .enabled(true)
//                    .provider(AuthProvider.GOOGLE)
//                    .role(defaultRole)
//                    .password(UUID.randomUUID().toString()) // Random password for OAuth users
//                    .build();
//
//            return userRepository.save(newUser);
//        });
//
//        String jwt = jwtService.generateToken(user.getEmail());
//        String refreshToken = redisRefreshTokenService.createRefreshToken(email);
//
//        // Detect if running in production (Railway)
//        boolean isProd = request.getServerName().contains("railway.app");
//
//        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
//        refreshTokenCookie.setHttpOnly(true);
//        refreshTokenCookie.setSecure(isProd); // HTTPS in production, not in local dev
////        refreshTokenCookie.setSecure(true); // Set to false in local dev if needed
//        refreshTokenCookie.setPath("/");
//        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
//        response.addCookie(refreshTokenCookie);
//
//        // Redirect to frontend with token
//        String frontendUrl = isProd
//                ? "https://gs-hub.vercel.app" // 🔁 your Vercel frontend
//                : "http://localhost:3000";    // local dev
//
//        String redirectUrl = frontendUrl + "/oauth2/redirect?token=" + jwt;
//
//        log.info("OAuth2 login success for: {}", email);
//        log.info("Redirecting to: {}", redirectUrl);
//
//        response.sendRedirect(redirectUrl);
//
//    }
//}
