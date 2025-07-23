package com.app.backend.dto;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PartialDeleteResponseDTO {
    private List<Long> requestedIds;
    private List<Long> deletedIds;
    private List<Long> notFoundIds;
    private int totalRequested;
    private int successfullyDeleted;
    private int notFound;
    private String summary;
    private boolean allDeleted;
    private boolean partialDeletion;

    // Constructor for builder pattern completion
    public static class PartialDeleteResponseDTOBuilder {
        public PartialDeleteResponseDTO build() {
            PartialDeleteResponseDTO dto = new PartialDeleteResponseDTO();
            dto.requestedIds = this.requestedIds;
            dto.deletedIds = this.deletedIds;
            dto.notFoundIds = this.notFoundIds;
            dto.totalRequested = this.totalRequested;
            dto.successfullyDeleted = this.successfullyDeleted;
            dto.notFound = this.notFound;
            dto.summary = this.summary;
            dto.allDeleted = this.notFound == 0;
            dto.partialDeletion = this.successfullyDeleted > 0 && this.notFound > 0;
            return dto;
        }
    }
}
