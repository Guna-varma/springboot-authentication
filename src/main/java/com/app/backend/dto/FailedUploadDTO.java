package com.app.backend.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FailedUploadDTO {
    private String filename;
    private String errorMessage;
    private String errorCode;
    private Long fileSize;

    // Constructor for backward compatibility
    public FailedUploadDTO(String filename, String errorMessage) {
        this.filename = filename;
        this.errorMessage = errorMessage;
    }
}
