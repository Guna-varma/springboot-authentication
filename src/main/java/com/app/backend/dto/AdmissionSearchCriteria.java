package com.app.backend.dto;

import com.app.backend.entity.AdmissionGender;
import com.app.backend.entity.ApplicationStatus;
import com.app.backend.entity.ClassLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionSearchCriteria {
    private String search;
    private ApplicationStatus status;
    private ClassLevel classLevel;
    private AdmissionGender gender;
    private LocalDate submittedAfter;
    private LocalDate submittedBefore;
    private LocalDate updatedAfter;
    private LocalDate updatedBefore;
}

