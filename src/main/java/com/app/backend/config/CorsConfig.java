package com.app.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // ✅ Define allowed origins as a method to reuse
    private String[] getAllowedOrigins() {
        String allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
        if (allowedOriginsEnv != null && !allowedOriginsEnv.trim().isEmpty()) {
            return allowedOriginsEnv.split(",");
        } else {
            return new String[]{
                    "http://localhost:3000",
                    "http://127.0.0.1:3000",
                    "https://gs-hub.vercel.app",
                    "https://*.vercel.app"
            };
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = getAllowedOrigins();

        // ✅ Public document endpoints - ALLOW CREDENTIALS for browser compatibility
        registry.addMapping("/api/document/public/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)  // ✅ Changed from false to true
                .maxAge(3600);

        // ✅ Specific mapping for public view endpoint
        registry.addMapping("/api/document/public/view/*")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)  // ✅ Changed from false to true
                .maxAge(3600);

        // ✅ TEXT ENTRY ENDPOINTS - Public (allow credentials for browser compatibility)
        registry.addMapping("/api/text/public/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)  // ✅ Changed from false to true for consistency
                .maxAge(3600);

        // ✅ TEXT ENTRY ENDPOINTS - Protected (need credentials)
        registry.addMapping("/api/text/create")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/text/update/*")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("PUT", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/text/delete/*")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/text/my-entries")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/text/cache/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // ✅ Authentication endpoints
        registry.addMapping("/api/auth/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // ✅ User endpoints
        registry.addMapping("/api/users/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // ✅ Protected Document endpoints
        registry.addMapping("/api/document/all")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/document/upload/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/document/metadata")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/document/my-documents")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/document/stats")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/document/*/download")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/document/delete/*")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/document/debug/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        String[] allowedOrigins = getAllowedOrigins();

        // ✅ Public Configuration - ALLOW CREDENTIALS
        CorsConfiguration publicConfig = new CorsConfiguration();
        publicConfig.setAllowedOrigins(Arrays.asList(allowedOrigins));
        publicConfig.setAllowedHeaders(Arrays.asList("*"));
        publicConfig.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));
        publicConfig.setAllowCredentials(true);  // ✅ Changed from false to true
        publicConfig.setMaxAge(3600L);

        // ✅ Protected Configuration
        CorsConfiguration protectedConfig = new CorsConfiguration();
        protectedConfig.setAllowedOrigins(Arrays.asList(allowedOrigins));
        protectedConfig.setAllowedHeaders(Arrays.asList("*"));
        protectedConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        protectedConfig.setAllowCredentials(true);
        protectedConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // ✅ Public endpoints with credentials allowed
        source.registerCorsConfiguration("/api/text/public/**", publicConfig);
        source.registerCorsConfiguration("/api/document/public/**", publicConfig);
        source.registerCorsConfiguration("/api/document/public/view/*", publicConfig);

        // ✅ Protected endpoints
        source.registerCorsConfiguration("/api/text/create", protectedConfig);
        source.registerCorsConfiguration("/api/text/update/*", protectedConfig);
        source.registerCorsConfiguration("/api/text/delete/*", protectedConfig);
        source.registerCorsConfiguration("/api/text/my-entries", protectedConfig);
        source.registerCorsConfiguration("/api/text/cache/**", protectedConfig);

        source.registerCorsConfiguration("/api/auth/**", protectedConfig);
        source.registerCorsConfiguration("/api/users/**", protectedConfig);

        source.registerCorsConfiguration("/api/document/all", protectedConfig);
        source.registerCorsConfiguration("/api/document/upload/**", protectedConfig);
        source.registerCorsConfiguration("/api/document/metadata", protectedConfig);
        source.registerCorsConfiguration("/api/document/my-documents", protectedConfig);
        source.registerCorsConfiguration("/api/document/stats", protectedConfig);
        source.registerCorsConfiguration("/api/document/*/download", protectedConfig);
        source.registerCorsConfiguration("/api/document/delete/*", protectedConfig);
        source.registerCorsConfiguration("/api/document/debug/**", protectedConfig);

        return source;
    }
}