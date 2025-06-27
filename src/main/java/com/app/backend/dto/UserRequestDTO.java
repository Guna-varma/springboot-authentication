package com.app.backend.dto;

import com.app.backend.entity.AuthProvider;
import com.app.backend.entity.Gender;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDTO {

    @NotBlank(message = "Full name is required")
    @Size(max = 50, message = "Full name must not exceed 50 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be 10 digits")
    private String phoneNumber;

    private boolean enabled;

    @NotNull(message = "Role ID is required")
    private Long roleId;

    @NotNull(message = "Auth provider is required")
    private AuthProvider provider;
}

