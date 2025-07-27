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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // ✅ Authentication endpoints - need credentials
        registry.addMapping("/api/auth/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // ✅ Auth needs credentials
                .maxAge(3600);

        // ✅ User endpoints - need credentials
        registry.addMapping("/api/users/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // ✅ User data needs credentials
                .maxAge(3600);

        // ✅ Protected document endpoints - need credentials for authentication
        registry.addMapping("/api/document/all")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // ✅ Admin endpoint needs credentials
                .maxAge(3600);

        registry.addMapping("/api/document/upload/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // ✅ Upload needs credentials
                .maxAge(3600);

        registry.addMapping("/api/document/metadata")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // ✅ Protected metadata needs credentials
                .maxAge(3600);

        registry.addMapping("/api/document/my-documents")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // ✅ User documents need credentials
                .maxAge(3600);

        registry.addMapping("/api/document/stats")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // ✅ Admin stats need credentials
                .maxAge(3600);

        registry.addMapping("/api/document/*/download")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/document/delete/*")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/document/delete/multiple")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/api/document/debug/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // ✅ Debug endpoints need credentials
                .maxAge(3600);

//        // ✅ DELETE endpoints - need credentials
//        registry.addMapping("/api/document/*")
//                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
//                .allowedMethods("DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Delete needs admin credentials
//                .maxAge(3600);
//
//        registry.addMapping("/api/document/multiple")
//                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
//                .allowedMethods("DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ Bulk delete needs admin credentials
//                .maxAge(3600);

        registry.addMapping("/api/document/public/view/*")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);

        // ✅ Public document endpoints - no credentials needed
        registry.addMapping("/api/document/public/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false) // ✅ Public endpoints don't need credentials
                .maxAge(3600);

        // ✅ Practice images - no credentials needed
        registry.addMapping("/api/document/public/practiceImages")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false) // ✅ Public endpoint
                .maxAge(3600);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration protectedConfig = new CorsConfiguration();
        protectedConfig.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://127.0.0.1:3000"));
        protectedConfig.setAllowedHeaders(Arrays.asList("*"));
        protectedConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        protectedConfig.setAllowCredentials(true);
        protectedConfig.setMaxAge(3600L);

        CorsConfiguration publicConfig = new CorsConfiguration();
        publicConfig.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://127.0.0.1:3000"));
        publicConfig.setAllowedHeaders(Arrays.asList("*"));
        publicConfig.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));
        publicConfig.setAllowCredentials(false);
        publicConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Authentication & User endpoints
        source.registerCorsConfiguration("/api/auth/**", protectedConfig);
        source.registerCorsConfiguration("/api/users/**", protectedConfig);

        // Protected Document endpoints
        source.registerCorsConfiguration("/api/document/all", protectedConfig);
        source.registerCorsConfiguration("/api/document/upload/**", protectedConfig);
        source.registerCorsConfiguration("/api/document/metadata", protectedConfig);
        source.registerCorsConfiguration("/api/document/my-documents", protectedConfig);
        source.registerCorsConfiguration("/api/document/stats", protectedConfig);
        source.registerCorsConfiguration("/api/document/preview/*/view", protectedConfig);
        source.registerCorsConfiguration("/api/document/*/download", protectedConfig);
        source.registerCorsConfiguration("/api/document/debug/**", protectedConfig);
        source.registerCorsConfiguration("/api/document/delete/*", protectedConfig);
        source.registerCorsConfiguration("/api/document/delete/multiple", protectedConfig);

        // Public Document endpoints
        source.registerCorsConfiguration("/api/document/public/**", protectedConfig);

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
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        // ✅ Authentication endpoints - need credentials
//        registry.addMapping("/api/auth/**")
//                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ CRITICAL: Auth needs credentials
//                .maxAge(3600);
//
//        // ✅ User endpoints - need credentials
//        registry.addMapping("/api/users/**")
//                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true) // ✅ CRITICAL: User data needs credentials
//                .maxAge(3600);
//
//        // ✅ Public document endpoints - no credentials
//        registry.addMapping("/api/document/public/**")
//                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false) // ✅ Public endpoints don't need credentials
//                .maxAge(3600);
//
//        // ✅ Practice images - no credentials
//        registry.addMapping("/api/document/public/practiceImages/**")
//                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
//                .allowedMethods("GET", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(false)
//                .maxAge(3600);
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration authConfig = new CorsConfiguration();
//        authConfig.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://127.0.0.1:3000"));
//        authConfig.setAllowedHeaders(Arrays.asList("*"));
//        authConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        authConfig.setAllowCredentials(true); // ✅ Auth endpoints need credentials
//        authConfig.setMaxAge(3600L);
//
//        CorsConfiguration publicConfig = new CorsConfiguration();
//        publicConfig.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://127.0.0.1:3000"));
//        publicConfig.setAllowedHeaders(Arrays.asList("*"));
//        publicConfig.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));
//        publicConfig.setAllowCredentials(false); // ✅ Public endpoints don't need credentials
//        publicConfig.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/api/auth/**", authConfig);
//        source.registerCorsConfiguration("/api/users/**", authConfig);
//        source.registerCorsConfiguration("/api/document/public/**", publicConfig);
//        source.registerCorsConfiguration("/api/document/public/practiceImages/**", publicConfig);
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
//@Configuration
//public class CorsConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/api/**")
//                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000")
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // ✅ Include OPTIONS
//                .allowedHeaders("*")
//                .allowCredentials(false) // ✅ Important for public endpoints
//                .maxAge(3600);
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.addAllowedOrigin("http://localhost:3000");
//        configuration.addAllowedOrigin("http://127.0.0.1:3000");
//        configuration.addAllowedHeader("*");
//        configuration.addAllowedMethod("*"); // ✅ This includes OPTIONS automatically
//        configuration.setAllowCredentials(false);
//        configuration.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/api/**", configuration);
//        return source;
//    }
//}