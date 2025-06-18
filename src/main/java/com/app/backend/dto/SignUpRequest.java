package com.app.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignUpRequest {
    private String fullName;
    private String email;
    private String gender;
    private String phoneNumber;
    private String password;
    private String confirmPassword;
}
