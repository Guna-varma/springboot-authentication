package com.app.backend.service;

import com.app.backend.entity.AuthProvider;
import com.app.backend.entity.Gender;
import com.app.backend.entity.Role;
import com.app.backend.entity.User;
import com.app.backend.repository.RoleRepository;
import com.app.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Role defaultRole = roleRepository.findByName("STUDENT")
                    .orElseThrow(() -> new RuntimeException("Default role not found"));

            return User.builder()
                    .email(email)
                    .fullName(name)
                    .provider(AuthProvider.GOOGLE)
                    .gender(Gender.NOT_SPECIFIED)
                    .enabled(true)
                    .role(defaultRole)
                    .build();
        });

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName())),
                oAuth2User.getAttributes(),
                "email"
        );
    }
}
