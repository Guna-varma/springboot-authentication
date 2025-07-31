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







//package com.app.backend.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//import java.util.Arrays;
//
//@Configuration
//public class CorsConfig implements WebMvcConfigurer {
//
//    // ✅ Define allowed origins as a method to reuse
//    private String[] getAllowedOrigins() {
//        String allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
//        if (allowedOriginsEnv != null && !allowedOriginsEnv.trim().isEmpty()) {
//            return allowedOriginsEnv.split(",");
//        } else {
//            return new String[]{
//                    "http://localhost:3000",
//                    "http://127.0.0.1:3000",
//                    "https://gs-hub.vercel.app",
//                    "https://*.vercel.app"
//            };
//        }
//    }
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        String[] allowedOrigins = getAllowedOrigins();
//
//        // ✅ TEXT ENTRY ENDPOINTS - Public (no credentials needed)
//        registry.addMapping("/api/text/public/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false)  // ✅ Public endpoint
//                .maxAge(3600);
//
//        // ✅ TEXT ENTRY ENDPOINTS - Protected (need credentials)
//        registry.addMapping("/api/text/create")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("POST", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/update/*")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("PUT", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/delete/*")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/my-entries")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/cache/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "POST", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        // ✅ Authentication endpoints
//        registry.addMapping("/api/auth/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        // ✅ User endpoints
//        registry.addMapping("/api/users/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        // ✅ Document endpoints
//        registry.addMapping("/api/document/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        // ✅ Public document endpoints
//        registry.addMapping("/api/document/public/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false)
//                .maxAge(3600);
//
//                // ✅ Protected document endpoints - need credentials for authentication
//        registry.addMapping("/api/document/all")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Admin endpoint needs credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/upload/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("POST", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Upload needs credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/metadata")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Protected metadata needs credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/my-documents")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ User documents need credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/stats")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Admin stats need credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/*/download")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/delete/*")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/delete/multiple")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/debug/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Debug endpoints need credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/public/view/*")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false)
//                .maxAge(3600);
//
//        // ✅ Public document endpoints - no credentials needed
//        registry.addMapping("/api/document/public/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false) // ✅ Public endpoints don't need credentials
//                .maxAge(3600);
//
//        // ✅ Practice images - no credentials needed
//        registry.addMapping("/api/document/public/practiceImages")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false) // ✅ Public endpoint
//                .maxAge(3600);
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        String[] allowedOrigins = getAllowedOrigins();
//
//        // ✅ Public Configuration
//        CorsConfiguration publicConfig = new CorsConfiguration();
//        publicConfig.setAllowedOrigins(Arrays.asList(allowedOrigins));  // ✅ Fixed null issue
//        publicConfig.setAllowedHeaders(Arrays.asList("*"));
//        publicConfig.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));
//        publicConfig.setAllowCredentials(false);
//        publicConfig.setMaxAge(3600L);
//
//        // ✅ Protected Configuration
//        CorsConfiguration protectedConfig = new CorsConfiguration();
//        protectedConfig.setAllowedOrigins(Arrays.asList(allowedOrigins));
//        protectedConfig.setAllowedHeaders(Arrays.asList("*"));
//        protectedConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        protectedConfig.setAllowCredentials(true);
//        protectedConfig.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//
//        // ✅ Public text endpoints
//        source.registerCorsConfiguration("/api/text/public/**", publicConfig);
//
//        // ✅ Protected text endpoints
//        source.registerCorsConfiguration("/api/text/create", protectedConfig);
//        source.registerCorsConfiguration("/api/text/update/*", protectedConfig);
//        source.registerCorsConfiguration("/api/text/delete/*", protectedConfig);
//        source.registerCorsConfiguration("/api/text/my-entries", protectedConfig);
//        source.registerCorsConfiguration("/api/text/cache/**", protectedConfig);
//
//        // ✅ Authentication & User endpoints
//        source.registerCorsConfiguration("/api/auth/**", protectedConfig);
//        source.registerCorsConfiguration("/api/users/**", protectedConfig);
//
//        // ✅ Document endpoints
//        source.registerCorsConfiguration("/api/document/all", protectedConfig);
//        source.registerCorsConfiguration("/api/document/upload/**", protectedConfig);
//        source.registerCorsConfiguration("/api/document/metadata", protectedConfig);
//        source.registerCorsConfiguration("/api/document/my-documents", protectedConfig);
//        source.registerCorsConfiguration("/api/document/stats", protectedConfig);
//        source.registerCorsConfiguration("/api/document/*/download", protectedConfig);
//        source.registerCorsConfiguration("/api/document/delete/*", protectedConfig);
//        source.registerCorsConfiguration("/api/document/debug/**", protectedConfig);
//        // Protected Document endpoints
//        source.registerCorsConfiguration("/api/document/public/view/*", publicConfig);
//        source.registerCorsConfiguration("/api/document/delete/multiple", protectedConfig);
//
//        // ✅ Public document endpoints
//        source.registerCorsConfiguration("/api/document/public/**", publicConfig);
//
//        // Public Document endpoints
////        source.registerCorsConfiguration("/api/document/public/**", protectedConfig);
//
//        return source;
//    }
//}



//package com.app.backend.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//import java.util.Arrays;
//
//@Configuration
//public class CorsConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//
//        String[] allowedOrigins = {
//                "http://localhost:3000",
//                "http://127.0.0.1:3000",
//                "https://gs-hub.vercel.app",           // ✅ Your production frontend
//                "https://*.vercel.app"                 // ✅ All Vercel preview deployments
//        };
//
//        // ✅ Authentication endpoints - need credentials
//        registry.addMapping("/api/auth/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Auth needs credentials
//                .maxAge(3600);
//
//        // ✅ User endpoints - need credentials
//        registry.addMapping("/api/users/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ User data needs credentials
//                .maxAge(3600);
//
//        // ✅ Protected document endpoints - need credentials for authentication
//        registry.addMapping("/api/document/all")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Admin endpoint needs credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/upload/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("POST", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Upload needs credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/metadata")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Protected metadata needs credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/my-documents")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ User documents need credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/stats")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Admin stats need credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/*/download")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/delete/*")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/delete/multiple")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/debug/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Debug endpoints need credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/public/view/*")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false)
//                .maxAge(3600);
//
//        // ✅ Public document endpoints - no credentials needed
//        registry.addMapping("/api/document/public/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false) // ✅ Public endpoints don't need credentials
//                .maxAge(3600);
//
//        // ✅ Practice images - no credentials needed
//        registry.addMapping("/api/document/public/practiceImages")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false) // ✅ Public endpoint
//                .maxAge(3600);
//
//        // ✅ TEXT ENTRY ENDPOINTS - Protected (need credentials for authentication)
//        registry.addMapping("/api/text/create")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("POST", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Create needs ADMIN/TUTOR credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/update/*")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("PUT", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Update needs ADMIN/TUTOR credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/delete/*")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Delete needs ADMIN/TUTOR credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/my-entries")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ User's own entries need authentication
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/cache/clear")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("POST", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Cache clear needs ADMIN credentials
//                .maxAge(3600);
//
//        // ✅ TEXT ENTRY ENDPOINTS - Public (no credentials needed)
//        registry.addMapping("/api/text/public/getAll")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false)
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/public/**")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false)
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/public/get/*")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false)
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/public/countEntries")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false)
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/my-entries")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/cache/clear")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("POST", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/cache/stats")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//
//        registry.addMapping("/api/text/cache/toggle")
//                .allowedOrigins(allowedOrigins)
//                .allowedMethods("POST", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Public endpoint
//                .maxAge(3600);
//
//    }
//
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration protectedConfig = new CorsConfiguration();
//
////        protectedConfig.setAllowedOrigins(Arrays.asList(allowedOrigins));
//
//
//        // ✅ PRODUCTION: Set allowed origins from environment
//        String allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
//        if (allowedOriginsEnv != null) {
//            protectedConfig.setAllowedOrigins(Arrays.asList(allowedOriginsEnv.split(",")));
//        } else {
//            protectedConfig.setAllowedOrigins(Arrays.asList(
//                    "http://localhost:3000",
//                    "http://127.0.0.1:3000",
//                    "https://gs-hub.vercel.app",
//                    "https://*.vercel.app"
//            ));
//        }
//
//        protectedConfig.setAllowedHeaders(Arrays.asList("*"));
//        protectedConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        protectedConfig.setAllowCredentials(true);
//        protectedConfig.setMaxAge(3600L);
//
//        CorsConfiguration publicConfig = new CorsConfiguration();
//
//        publicConfig.setAllowedOrigins(Arrays.asList(allowedOriginsEnv));
//        publicConfig.setAllowedHeaders(Arrays.asList("*"));
//        publicConfig.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));
//        publicConfig.setAllowCredentials(false);
//        publicConfig.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//
//        // Authentication & User endpoints
//        source.registerCorsConfiguration("/api/auth/**", protectedConfig);
//        source.registerCorsConfiguration("/api/users/**", protectedConfig);
//
//        // Protected Document endpoints
//        source.registerCorsConfiguration("/api/document/all", protectedConfig);
//        source.registerCorsConfiguration("/api/document/upload/**", protectedConfig);
//        source.registerCorsConfiguration("/api/document/metadata", protectedConfig);
//        source.registerCorsConfiguration("/api/document/my-documents", protectedConfig);
//        source.registerCorsConfiguration("/api/document/stats", protectedConfig);
//        source.registerCorsConfiguration("/api/document/preview/*/view", protectedConfig);
//        source.registerCorsConfiguration("/api/document/*/download", protectedConfig);
//        source.registerCorsConfiguration("/api/document/debug/**", protectedConfig);
//        source.registerCorsConfiguration("/api/document/delete/*", protectedConfig);
//        source.registerCorsConfiguration("/api/document/delete/multiple", protectedConfig);
//
//        // Public Document endpoints
//        source.registerCorsConfiguration("/api/document/public/**", protectedConfig);
//
//        // Protected text entry endpoints
//        source.registerCorsConfiguration("/api/text/create", protectedConfig);
//        source.registerCorsConfiguration("/api/text/update/*", protectedConfig);
//        source.registerCorsConfiguration("/api/text/delete/*", protectedConfig);
//        source.registerCorsConfiguration("/api/text/my-entries", protectedConfig);
//        source.registerCorsConfiguration("/api/text/cache/**", protectedConfig);
////        source.registerCorsConfiguration("/api/text/cache/clear", protectedConfig);
////        source.registerCorsConfiguration("/api/text/my-entries", protectedConfig);
//
////        source.registerCorsConfiguration("/api/text/cache/toggle", protectedConfig);
//
//        // Public text entry endpoints
//        source.registerCorsConfiguration("/api/text/public/**", publicConfig);
//
//        source.registerCorsConfiguration("/api/text/public/getAll", publicConfig);
////        source.registerCorsConfiguration("/api/text/public/get/*", publicConfig);
////        source.registerCorsConfiguration("/api/text/public/countEntries", publicConfig);
//
//
//        return source;
//    }
//}