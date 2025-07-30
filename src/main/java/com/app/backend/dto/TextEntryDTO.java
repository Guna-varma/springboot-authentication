package com.app.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextEntryDTO {

    @NotBlank(message = "Message cannot be blank")
    @Size(max = 2048, message = "Message cannot exceed 2048 characters")
    private String message;
}
