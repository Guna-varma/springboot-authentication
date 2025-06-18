package com.app.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRequest {
    private String email;
    private String otp; // optional for sending, required for verification
}

