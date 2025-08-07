package com.app.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionStatistics {

    @JsonProperty("totalApplications")
    private long totalApplications;

    @JsonProperty("statusBreakdown")
    private Map<String, Long> statusBreakdown;

    @JsonProperty("classBreakdown")
    private Map<String, Long> classBreakdown;

    @JsonProperty("genderBreakdown")
    private Map<String, Long> genderBreakdown;

    @JsonProperty("monthlyApplications")
    private Map<String, Long> monthlyApplications;

    @JsonProperty("recentApplicationsCount")
    private long recentApplicationsCount; // Last 7 days

    @JsonProperty("averageProcessingTime")
    private double averageProcessingTime; // In hours
}
