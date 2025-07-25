package com.app.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatsResponseDTO {
    private long total;
    private long images;
    private long pdfs;
    private long totalSize;
}

