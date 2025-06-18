package com.app.backend.service;


import com.app.backend.dto.*;
import com.app.backend.entity.*;
import com.app.backend.repository.*;
import com.app.backend.util.EmailUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import com.app.backend.dto.*;
import com.app.backend.entity.*;
import com.app.backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.app.backend.entity.AuthProvider;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailUtil emailUtil;

    @Transactional
    public AuthResponse signup(SignUpRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
/// CODE NEED TO BE updating.....18062025 12:59ist
        Role userRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new RuntimeException("Default role not found"));


        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .gender(request.getGender())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .build();

        userRepository.save(user);

        String jwt = jwtService.generateToken(user.getEmail());

        UserResponseDTO responseUser = UserResponseDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .gender(user.getGender())
                .phoneNumber(user.getPhoneNumber())
                .role(new UserResponseDTO.RoleDTO(userRole.getId(), userRole.getName()))
                .build();

        return AuthResponse.builder()
                .token(jwt)
                .user(responseUser)
                .build();
    }

    public AuthResponse signin(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String jwt = jwtService.generateToken(user.getEmail());

        UserResponseDTO responseUser = UserResponseDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .gender(user.getGender())
                .phoneNumber(user.getPhoneNumber())
                .role(new UserResponseDTO.RoleDTO(
                        user.getRole().getId(),
                        user.getRole().getName()
                ))
                .build();

        return AuthResponse.builder()
                .token(jwt)
                .user(responseUser)
                .build();
    }

    public void sendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = String.valueOf((int) (Math.random() * 900000) + 100000); // 6-digit OTP
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);

        otpTokenRepository.deleteByEmail(email); // remove old OTP if exists

        OtpToken otpToken = OtpToken.builder()
                .email(email)
                .otp(otp)
                .expiryTime(expiryTime)
                .build();

        otpTokenRepository.save(otpToken);
        emailUtil.sendOtpEmail(email, otp); // Custom utility
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        boolean validOtp = otpTokenRepository
                .findByEmailAndOtp(request.getEmail(), request.getOtp())
                .filter(token -> token.getExpiryTime().isAfter(java.time.LocalDateTime.now()))
                .isPresent();

        if (!validOtp) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpTokenRepository.deleteByEmail(request.getEmail());
    }
}

//
//@Service
//@RequiredArgsConstructor
//public class AuthService {
//
//    private final UserRepository userRepository;
//    private final RoleRepository roleRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final JwtService jwtService;
//    private final OtpTokenRepository otpTokenRepository;
//
//    @Transactional
//    public AuthResponse signup(SignUpRequest request) {
//        if (!request.getPassword().equals(request.getConfirmPassword())) {
//            throw new RuntimeException("Passwords do not match");
//        }
//
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new RuntimeException("Email already registered");
//        }
//
//        Role userRole = roleRepository.findByName("STUDENT")
//                .orElseThrow(() -> new RuntimeException("Default role not found"));
//
//        User user = User.builder()
//                .fullName(request.getFullName())
//                .email(request.getEmail())
//                .gender(request.getGender())
//                .phoneNumber(request.getPhoneNumber())
//                .password(passwordEncoder.encode(request.getPassword()))
//                .role(userRole)
//                .enabled(true)
//                .build();
//
//        userRepository.save(user);
//
//        String jwt = jwtService.generateToken(user.getEmail());
//
//        // Map User to DTO manually
//        UserResponseDTO responseUser = UserResponseDTO.builder()
//                .id(user.getId())
//                .fullName(user.getFullName())
//                .email(user.getEmail())
//                .gender(user.getGender())
//                .phoneNumber(user.getPhoneNumber())
//                .role(new UserResponseDTO.RoleDTO(userRole.getId(), userRole.getName()))
//                .build();
//
//        return AuthResponse.builder()
//                .token(jwt)
//                .user(responseUser)
//                .build();
//    }
//
//
//    public AuthResponse signin(AuthRequest request) {
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
//            throw new RuntimeException("Invalid credentials");
//        }
//
//        String role = user.getRole().stream().findFirst().map(Role::getName).orElse("UNKNOWN");
//        String jwt = jwtService.generateToken(user.getEmail());
//
//        return AuthResponse.builder()
//                .token(jwt)
//                .role(role)
//                .build();
//    }
//
//    public void resetPassword(ResetPasswordRequest request) {
//        boolean validOtp = otpTokenRepository
//                .findByEmailAndOtp(request.getEmail(), request.getOtp())
//                .filter(token -> token.getExpiryTime().isAfter(java.time.LocalDateTime.now()))
//                .isPresent();
//
//        if (!validOtp) {
//            throw new RuntimeException("Invalid or expired OTP");
//        }
//
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
//        userRepository.save(user);
//
//        otpTokenRepository.deleteByEmail(request.getEmail());
//    }
//}
