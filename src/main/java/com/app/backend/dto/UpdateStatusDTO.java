package com.app.backend.dto;

import com.app.backend.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusDTO {

    @NotNull(message = "New status is required")
    private ApplicationStatus newStatus;

    @Size(max = 500, message = "Comments cannot exceed 500 characters")
    private String comments;
}
