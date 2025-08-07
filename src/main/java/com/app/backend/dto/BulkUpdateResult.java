package com.app.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateResult {
    private int successCount;
    private int failedCount;
    private List<String> successfulApplications;
    private Map<String, String> failedApplications; // applicationNumber -> error message
}
