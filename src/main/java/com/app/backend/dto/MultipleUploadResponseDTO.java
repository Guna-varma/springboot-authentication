package com.app.backend.dto;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MultipleUploadResponseDTO {
    private List<DocumentMetadataDTO> successfulUploads;
    private List<FailedUploadDTO> failedUploads;
    private String summary;
    private int totalFiles;
    private int successCount;
    private int failureCount;

    // Constructor for backward compatibility
    public MultipleUploadResponseDTO(List<DocumentMetadataDTO> successfulUploads,
                                     List<FailedUploadDTO> failedUploads,
                                     String summary) {
        this.successfulUploads = successfulUploads;
        this.failedUploads = failedUploads;
        this.summary = summary;
        this.totalFiles = (successfulUploads != null ? successfulUploads.size() : 0) +
                (failedUploads != null ? failedUploads.size() : 0);
        this.successCount = successfulUploads != null ? successfulUploads.size() : 0;
        this.failureCount = failedUploads != null ? failedUploads.size() : 0;
    }
}
