package com.app.backend.config;

import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import com.app.backend.filter.JwtFilter;
import com.app.backend.service.CustomOAuth2UserService;
import com.app.backend.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .cors(Customizer.withDefaults())
////                .cors(cors -> cors.disable()) // ✅ Disable Spring Security CORS to use our custom filter
//                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers(
//                                "/api/auth/**",
//                                "/api/otp/**",
//                                "/api/cors-test/**",
//                                "/swagger-ui/**",
//                                "/v3/api-docs/**",
//                                "/actuator/**",
//                                "/api/document/public/**",
//                                "/api/document/public/debug/test",
//                                "/api/document/public/practiceImages", // ✅ Exact match
//                                "/api/document/public/practiceImages/**" // ✅ With parameters
//                        ).permitAll()
//                        .requestMatchers("/actuator/**").hasRole("ADMIN")
//                        .requestMatchers("/api/users/me").authenticated()
//                        .requestMatchers("/api/users/updatePersonalInfo").authenticated()
//                        .requestMatchers("/api/users/**").hasRole("ADMIN")
//                        .anyRequest().authenticated()
//                )
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .oauth2Login(oauth2 -> oauth2
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(customOAuth2UserService)
//                        )
//                        .successHandler(oAuth2SuccessHandler)
//                );
//
//        // Register JWT filter before UsernamePasswordAuthenticationFilter
//        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults()) // ✅ Use our custom CORS configuration
                .authorizeHttpRequests(authorize -> authorize
                        // ✅ Public endpoints - no authentication required
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/otp/**",
                                "/api/test/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/**",
                                "/api/document/public/**",           // ✅ All public document endpoints
                                "/api/document/public/practiceImages",      // ✅ Practice images endpoint
                                "/api/document/public/debug/test"    // ✅ Debug endpoint
                        ).permitAll()

                        // ✅ Admin-only endpoints
                        .requestMatchers(
                                "/api/document/all",
                                "/api/document/stats",
                                "/api/document/debug/**",
                                "/api/document/delete/**",
                                "/api/document/multiple"
                        ).hasRole("ADMIN")

                        // ✅ Upload endpoints - specific roles
                        .requestMatchers("/api/document/upload/**")
                        .hasAnyRole("ADMIN", "HEALTHCARE_PROVIDER", "TUTOR")

                        // ✅ User-specific endpoints
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/users/updatePersonalInfo").authenticated()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // ✅ Document access endpoints
                        .requestMatchers(
                                "/api/document/metadata",
                                "/api/document/my-documents",
                                "/api/document/*/view",
                                "/api/document/*/download"
                        ).authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                );

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder
                .userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder());
        return authBuilder.build();
    }
}



//package com.app.backend.config;
//
//import org.springframework.security.config.Customizer;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.cors.CorsConfigurationSource;
//
//import com.app.backend.filter.JwtFilter;
//import com.app.backend.service.CustomOAuth2UserService;
//import com.app.backend.service.CustomUserDetailsService;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//
//@Configuration
//@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final JwtFilter jwtFilter;
//    private final CustomUserDetailsService customUserDetailsService;
//    private final CustomOAuth2UserService customOAuth2UserService;
//    private final OAuth2SuccessHandler oAuth2SuccessHandler;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
////                .cors(Customizer.withDefaults()) // ✅ Use default CORS configuration
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers(
//                                "/api/auth/**",
//                                "/api/otp/**",
//                                "/swagger-ui/**",
//                                "/v3/api-docs/**",
//                                "/api/test/**",
//                                "/actuator/**",
//                                "/api/document/public/**",
//                                "/api/document/public/practiceImages/**", // ✅ Fixed pattern
//                                "/api/document/public/practiceImages"     // ✅ Without trailing slash
//                        ).permitAll()
//                        .requestMatchers("/actuator/**").hasRole("ADMIN")
//                        .requestMatchers("/api/users/me").authenticated()
//                        .requestMatchers("/api/users/updatePersonalInfo").authenticated()
//                        .requestMatchers("/api/users/**").hasRole("ADMIN")
//                        .anyRequest().authenticated()
//                )
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .oauth2Login(oauth2 -> oauth2
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(customOAuth2UserService)
//                        )
//                        .successHandler(oAuth2SuccessHandler)
//                );
//
//        // Register JWT filter before UsernamePasswordAuthenticationFilter
//        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//
////    @Bean
////    public CorsConfigurationSource corsConfigurationSource() {
////        CorsConfiguration configuration = new CorsConfiguration();
////
////        // ✅ CRITICAL: Set this to false for localhost development
////        configuration.setAllowCredentials(false);
////
////        // ✅ Add specific origins
////        configuration.addAllowedOrigin("http://localhost:3000");
////        configuration.addAllowedOrigin("http://127.0.0.1:3000");
////
////        // ✅ Production origins
////        configuration.addAllowedOrigin("https://gs-hub.vercel.app");
////        configuration.addAllowedOriginPattern("https://*.vercel.app");
////        configuration.addAllowedOriginPattern("https://*.railway.app");
////
////        // ✅ Allow all headers and methods
////        configuration.addAllowedHeader("*");
////        configuration.addAllowedMethod("*");
////
////        // ✅ Add specific headers that might be needed
////        configuration.addExposedHeader("Authorization");
////        configuration.addExposedHeader("X-Total-Count");
////        configuration.addExposedHeader("X-RateLimit-Remaining");
////
////        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
////        source.registerCorsConfiguration("/**", configuration);
////        return source;
////    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//
//        // ✅ CRITICAL: Use addAllowedOrigin instead of addAllowedOriginPattern for simple cases
//        configuration.addAllowedOrigin("http://localhost:3000");
//        configuration.addAllowedOrigin("http://127.0.0.1:3000");
//
//        // ✅ Production origins
//        configuration.addAllowedOrigin("https://gs-hub.vercel.app");
//        configuration.addAllowedOriginPattern("https://*.vercel.app");
//        configuration.addAllowedOriginPattern("https://*.railway.app");
//
//        // ✅ Allow all methods explicitly
//        configuration.addAllowedMethod("GET");
//        configuration.addAllowedMethod("POST");
//        configuration.addAllowedMethod("PUT");
//        configuration.addAllowedMethod("DELETE");
//        configuration.addAllowedMethod("OPTIONS"); // ✅ CRITICAL for preflight
//
//        // ✅ Allow all headers
//        configuration.addAllowedHeader("*");
//
//        // ✅ IMPORTANT: Set to false for public endpoints to avoid credential issues
//        configuration.setAllowCredentials(false);
//
//        // ✅ Expose relevant headers
//        configuration.addExposedHeader("Authorization");
//        configuration.addExposedHeader("X-Total-Count");
//        configuration.addExposedHeader("X-RateLimit-Remaining");
//
//        // ✅ Set max age for preflight cache
//        configuration.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
//
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
//        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
//        authBuilder
//                .userDetailsService(customUserDetailsService)
//                .passwordEncoder(passwordEncoder());
//        return authBuilder.build();
//    }
//}






//package com.app.backend.config;
//
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.cors.CorsConfigurationSource;
//
//import com.app.backend.filter.JwtFilter;
//import com.app.backend.service.CustomOAuth2UserService;
//import com.app.backend.service.CustomUserDetailsService;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//@Configuration
//@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final JwtFilter jwtFilter;
//    private final CustomUserDetailsService customUserDetailsService;
//    private final CustomOAuth2UserService customOAuth2UserService;
//    private final OAuth2SuccessHandler oAuth2SuccessHandler;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers(
//                                "/api/auth/**",
//                                "/api/otp/**",
//                                "/swagger-ui/**",
//                                "/v3/api-docs/**",
//                                "/actuator/**",
//                                "/api/document/public/**",
//                                "/api/document/public/practiceImages/**",
//                                "/api/document/public/practiceImages",
//                                "/api/document/public/metadata"
//                        ).permitAll()
//                        .requestMatchers("/actuator/**").hasRole("ADMIN")
//                        .requestMatchers("/api/users/me").authenticated()
//                        .requestMatchers("/api/users/updatePersonalInfo").authenticated()
//                        .requestMatchers("/api/users/**").hasRole("ADMIN")
//                        .anyRequest().authenticated()
//                )
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .oauth2Login(oauth2 -> oauth2
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(customOAuth2UserService)
//                        )
//                        .successHandler(oAuth2SuccessHandler)
//                );
//
//        // Register JWT filter before UsernamePasswordAuthenticationFilter
//        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowCredentials(true);
//
//        // Add your production URLs
//        configuration.addAllowedOriginPattern("http://localhost:3000"); // Dev
//        configuration.addAllowedOriginPattern("https://gs-hub.vercel.app"); // Production
//        configuration.addAllowedOriginPattern("https://*.vercel.app"); // Any Vercel preview deployments
//        configuration.addAllowedOriginPattern("https://*.railway.app"); // Railway backend
//
//        configuration.addAllowedHeader("*");
//        configuration.addAllowedMethod("*");
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
//
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
//        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
//        authBuilder
//                .userDetailsService(customUserDetailsService)
//                .passwordEncoder(passwordEncoder());
//        return authBuilder.build();
//    }
//}





//package com.app.backend.config;
//
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.cors.CorsConfigurationSource;
//
//import com.app.backend.filter.JwtFilter;
//import com.app.backend.service.CustomOAuth2UserService;
//import com.app.backend.service.CustomUserDetailsService;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//@Configuration
//@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final JwtFilter jwtFilter;
//    private final CustomUserDetailsService customUserDetailsService;
//    private final CustomOAuth2UserService customOAuth2UserService;
//    private final OAuth2SuccessHandler oAuth2SuccessHandler;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers(
//                                "/api/auth/**",
//                                "/api/otp/**",
//                                "/swagger-ui/**",
//                                "/v3/api-docs/**",
//                                "/actuator/**",
//                                "/api/document/public/**",
//                                "/api/document/public/practiceImages**",
//                                "/error",
//                                "/oauth2/**",
//                                "/login/**"
//                        ).permitAll()
//                        .requestMatchers("/actuator/**").hasRole("ADMIN")
//                        .requestMatchers("/api/users/me").authenticated()
//                        .requestMatchers("/api/users/updatePersonalInfo").authenticated()
//                        .requestMatchers("/api/users/**").hasRole("ADMIN")
//                        .requestMatchers("/api/document/**").authenticated()
//                        .anyRequest().authenticated()
//                )
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .oauth2Login(oauth2 -> oauth2
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(customOAuth2UserService)
//                        )
//                        .successHandler(oAuth2SuccessHandler)
//                );
//
//        // Register JWT filter before UsernamePasswordAuthenticationFilter
//        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowCredentials(true);
//
//        // Add your URLs
//        configuration.addAllowedOriginPattern("http://localhost:3000"); // React dev
//        configuration.addAllowedOriginPattern("http://localhost:8080"); // Backend dev
//        configuration.addAllowedOriginPattern("https://gs-hub.vercel.app"); // Production
//        configuration.addAllowedOriginPattern("https://*.vercel.app"); // Any Vercel preview deployments
//        configuration.addAllowedOriginPattern("https://*.railway.app"); // Railway backend
//
//        configuration.addAllowedHeader("*");
//        configuration.addAllowedMethod("*");
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
//        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
//        authBuilder
//                .userDetailsService(customUserDetailsService)
//                .passwordEncoder(passwordEncoder());
//        return authBuilder.build();
//    }
//}






