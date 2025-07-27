package com.app.backend.filter;

import com.app.backend.service.CustomUserDetailsService;
import com.app.backend.service.JwtService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}





//package com.app.backend.filter;
//
//import com.app.backend.service.CustomUserDetailsService;
//import com.app.backend.service.JwtService;
//import jakarta.servlet.*;
//import jakarta.servlet.http.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.authentication.*;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class JwtFilter extends OncePerRequestFilter {
//
//    private final JwtService jwtService;
//    private final CustomUserDetailsService userDetailsService;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws ServletException, IOException {
//
//        String path = request.getRequestURI();
//        log.debug("Processing request: {}", path);
//
//        // Skip JWT processing for public endpoints
//        if (isPublicEndpoint(path)) {
//            log.debug("Skipping JWT validation for public endpoint: {}", path);
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        String authHeader = request.getHeader("Authorization");
//        log.debug("Authorization header: {}", authHeader != null ? "Bearer ***" : "null");
//
//        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
//            try {
//                String jwt = authHeader.substring(7);
//                log.debug("Extracted JWT token (length: {})", jwt.length());
//
//                String username = jwtService.extractUsername(jwt);
//                log.debug("Extracted username: {}", username);
//
//                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                    var userDetails = userDetailsService.loadUserByUsername(username);
//
//                    if (jwtService.isTokenValid(jwt, userDetails)) {
//                        UsernamePasswordAuthenticationToken authToken =
//                                new UsernamePasswordAuthenticationToken(
//                                        userDetails, null, userDetails.getAuthorities());
//
//                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                        SecurityContextHolder.getContext().setAuthentication(authToken);
//                        log.debug("Successfully authenticated user: {}", username);
//                    } else {
//                        log.warn("Invalid JWT token for user: {}", username);
//                    }
//                }
//            } catch (Exception e) {
//                log.error("JWT processing error: {}", e.getMessage());
//                // Clear any partial authentication
//                SecurityContextHolder.clearContext();
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }
//
//    private boolean isPublicEndpoint(String path) {
//        return path.startsWith("/api/auth/") ||
//                path.startsWith("/api/otp/") ||
//                path.startsWith("/swagger-ui/") ||
//                path.startsWith("/v3/api-docs/") ||
//                path.startsWith("/actuator/") ||
//                path.startsWith("/api/document/public/") ||
//                path.equals("/error") ||
//                path.startsWith("/oauth2/") ||
//                path.startsWith("/login/");
//    }
//}






