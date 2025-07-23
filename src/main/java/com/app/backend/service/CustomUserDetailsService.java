package com.app.backend.service;

import com.app.backend.entity.User;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserDetails(user);

    }

    private List<SimpleGrantedAuthority> getAuthorities(com.app.backend.entity.Role role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.getName()));
    }

}