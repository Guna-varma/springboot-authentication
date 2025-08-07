package com.app.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponseDTO {
    private String error;
    private String message;
    private List<String> details;
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public ErrorResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponseDTO(String error, String message, List<String> details, String path) {
        this();
        this.error = error;
        this.message = message;
        this.details = details;
        this.path = path;
    }

    // Getters and setters
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}


//
//import com.fasterxml.jackson.annotation.JsonFormat;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//public class ErrorResponseDTO {
//    private String error;
//    private String message;
//    private List<String> details;
//    private String path;
//
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime timestamp;
//
//    // Constructors
//    public ErrorResponseDTO() {
//        this.timestamp = LocalDateTime.now();
//    }
//
//    public ErrorResponseDTO(String error, String message, List<String> details, String path) {
//        this();
//        this.error = error;
//        this.message = message;
//        this.details = details;
//        this.path = path;
//    }
//
//    // Getters and Setters
//    public String getError() { return error; }
//    public void setError(String error) { this.error = error; }
//
//    public String getMessage() { return message; }
//    public void setMessage(String message) { this.message = message; }
//
//    public List<String> getDetails() { return details; }
//    public void setDetails(List<String> details) { this.details = details; }
//
//    public String getPath() { return path; }
//    public void setPath(String path) { this.path = path; }
//
//    public LocalDateTime getTimestamp() { return timestamp; }
//    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
//}
