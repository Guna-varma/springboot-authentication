package com.app.backend.service;

import com.app.backend.entity.OtpToken;
import com.app.backend.repository.OtpTokenRepository;
import com.app.backend.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final EmailUtil emailUtil;

    public void sendOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));

        OtpToken token = OtpToken.builder()
                .email(email)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .build();

        otpTokenRepository.deleteByEmail(email);
        otpTokenRepository.save(token);

        emailUtil.sendOtpEmail(email, otp);
    }

    public boolean verifyOtp(String email, String otp) {
        return otpTokenRepository.findByEmailAndOtp(email, otp)
                .filter(token -> token.getExpiryTime().isAfter(LocalDateTime.now()))
                .isPresent();
    }
}

