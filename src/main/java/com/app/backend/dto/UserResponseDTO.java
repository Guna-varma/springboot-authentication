package com.app.backend.dto;

import lombok.*;
import com.app.backend.entity.AuthProvider;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponseDTO {
    private Long id;
    private String fullName;
    private String email;
    private String gender;
    private String phoneNumber;
    private RoleDTO role;
    private boolean enabled;
    private AuthProvider provider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoleDTO {
        private Long id;
        private String name;
    }
}

