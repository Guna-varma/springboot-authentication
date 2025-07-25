package com.app.backend.config;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;


import  com.app.backend.filter.JwtFilter;
import com.app.backend.service.CustomOAuth2UserService;
import com.app.backend.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(
                            "/api/auth/**",
                            "/api/otp/**",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/actuator/**",
                            "/api/document/**",
                            "/api/document/public/**",
                            "/api/document/practiceImages"
                    ).permitAll()
                      .requestMatchers("/actuator/**").hasRole("ADMIN")
                      .requestMatchers("/api/users/me").authenticated()             // everyone who’s logged in
                      .requestMatchers("/api/users/updatePersonalInfo").authenticated()        // ✅ ADD THIS
                      .requestMatchers("/api/users/**").hasRole("ADMIN")                        // ✅ Keep this AFTER more specific matchers
//                    .requestMatchers("/api/users/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                    )
                    .successHandler(oAuth2SuccessHandler)
            );

    // ✅ Register your JWT filter BEFORE UsernamePasswordAuthenticationFilter
    http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.addAllowedOriginPattern("http://localhost:3000");
        configuration.addAllowedOriginPattern("https://*.vercel.app");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
