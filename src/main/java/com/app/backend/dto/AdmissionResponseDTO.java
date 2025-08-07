package com.app.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class AdmissionResponseDTO {
    private String applicationNumber;
    private String studentName;
    private String message;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submittedAt;

    // Constructors
    public AdmissionResponseDTO() {}

    public AdmissionResponseDTO(String applicationNumber, String studentName, String message, String status, LocalDateTime submittedAt) {
        this.applicationNumber = applicationNumber;
        this.studentName = studentName;
        this.message = message;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    // Getters and Setters
    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
