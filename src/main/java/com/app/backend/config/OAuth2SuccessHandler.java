package com.app.backend.config;

import com.app.backend.entity.AuthProvider;
import com.app.backend.entity.Role;
import com.app.backend.entity.User;
import com.app.backend.repository.RoleRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException,ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Optional<User> optionalUser = userRepository.findByEmail(email);

        User user = optionalUser.orElseGet(() -> {
            Role defaultRole = roleRepository.findByName("STUDENT")  // ✅ Fetch role by name
                    .orElseThrow(() -> new RuntimeException("Default role not found"));

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(name);
            newUser.setEnabled(true);
            newUser.setProvider(AuthProvider.GOOGLE); // GOOGLE provider
            newUser.setRole(defaultRole);             // ✅ Set default role

            return userRepository.save(newUser);
        });

        String jwt = jwtService.generateToken(user.getEmail());

        // ✅ Return JWT as JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"token\": \"" + jwt + "\"}");

//        User user = userRepository.findByEmail(email).orElseGet(() -> {
//            User newUser = new User();
//            newUser.setEmail(email);
//            newUser.setFullName(name);
//            newUser.setPassword(""); // Not needed for OAuth
//            newUser.setGender("Not Specified");
//
//            Role defaultRole = roleRepository.findByName("STUDENT")
//                    .orElseThrow(() -> new RuntimeException("Default role not found"));
//            newUser.setRole(defaultRole);
//
//            return userRepository.save(newUser);
//        });


//        String token = jwtService.generateToken(user.getEmail());
//
//        // Send token in response (you can redirect or return JSON as needed)
//        response.setContentType("application/json");
//        response.getWriter().write("{\"token\": \"" + token + "\"}");
    }
}

//@Component
//@RequiredArgsConstructor
//public class OAuth2SuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {
//
//    private final UserRepository userRepository;
//    private final RoleRepository roleRepository;
//    private final JwtService jwtService;
//
//    @Override
//    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
//            throws IOException, ServletException {
//
//        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
//        String email = oAuth2User.getAttribute("email");
//        String name = oAuth2User.getAttribute("name");
//
//        User user = userRepository.findByEmail(email)
//                .orElseGet(() -> {
//                    Role defaultRole = roleRepository.findByName("STUDENT").orElseThrow();
//                    User newUser = User.builder()
//                            .email(email)
//                            .fullName(name)
//                            .enabled(true)
//                            .roles(Set.of(defaultRole))
//                            .build();
//                    return userRepository.save(newUser);
//                });
//
//        String jwt = jwtService.generateToken(user.getEmail());
//        response.sendRedirect("/swagger-ui/index.html?token=" + jwt); // or redirect to frontend
//    }
//}

