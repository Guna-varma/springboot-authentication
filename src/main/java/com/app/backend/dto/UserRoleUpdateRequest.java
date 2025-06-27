package com.app.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateRequest {

    @NotBlank(message = "Role name is required")
    @Pattern(regexp = "^(ADMIN|TUTOR|STAFF|STUDENT)$", message = "Invalid role")
    private String roleName;
}
