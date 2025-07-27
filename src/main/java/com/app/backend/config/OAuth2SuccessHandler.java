package com.app.backend.config;

import com.app.backend.entity.AuthProvider;
import com.app.backend.entity.Gender;
import com.app.backend.entity.Role;
import com.app.backend.entity.User;
import com.app.backend.redisToken.RedisRefreshTokenService;
import com.app.backend.repository.RoleRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final RedisRefreshTokenService redisRefreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Optional<User> optionalUser = userRepository.findByEmail(email);

        User user = optionalUser.orElseGet(() -> {
            Role defaultRole = roleRepository.findByName("REGISTERED_USER")
                    .orElseThrow(() -> new RuntimeException("Default role not found"));

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(name);
            newUser.setGender(Gender.NOT_SPECIFIED);
            newUser.setEnabled(true);
            newUser.setProvider(AuthProvider.GOOGLE);
            newUser.setRole(defaultRole);
            newUser.setCreatedAt(new Date());
            newUser.setUpdatedAt(new Date());
            newUser.setPassword(UUID.randomUUID().toString());

            return userRepository.save(newUser);
        });

        String jwt = jwtService.generateToken(user.getEmail());
        String refreshToken = redisRefreshTokenService.createRefreshToken(email);

        String frontendUrl = appProperties.getFrontend().getUrl();
        boolean isProd = frontendUrl.contains("vercel.app");

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(isProd);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(refreshTokenCookie);

        // 🔁 Redirect logic based on environment
        String origin = request.getHeader("Origin");
        String host = request.getHeader("Host");

        String redirectUrl;

        if ((origin != null && origin.contains("localhost")) || (host != null && host.contains("localhost"))) {
            redirectUrl = frontendUrl + "/oauth2/redirect?token=" + jwt;
        } else {
            redirectUrl = frontendUrl + "/oauth2/redirect?token=" + jwt;
        }

        log.info("OAuth2 login success for: {}", email);
        log.info("Origin: {}", origin);
        log.info("Host: {}", host);
        log.info("Final redirect URL: {}", redirectUrl);

        response.sendRedirect(redirectUrl);
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
//import java.util.Date;
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
//            User newUser = new User();
//            newUser.setEmail(email);
//            newUser.setFullName(name);
//            newUser.setGender(Gender.NOT_SPECIFIED);
//            newUser.setEnabled(true);
//            newUser.setProvider(AuthProvider.GOOGLE);
//            newUser.setRole(defaultRole);
//            newUser.setCreatedAt(new Date());
//            newUser.setUpdatedAt(new Date());
//            newUser.setPassword(UUID.randomUUID().toString());
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
////        String redirectUrl = "http://localhost:3000/oauth2/redirect?token=" + jwt;
////        response.sendRedirect(redirectUrl);
//    }
//}