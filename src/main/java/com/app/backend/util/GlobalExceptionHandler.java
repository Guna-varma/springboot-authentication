package com.app.backend.util;

import com.app.backend.dto.ApiResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.IOException;
//import java.nio.file.AccessDeniedException as FileAccessDeniedException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    // ===============================
    // VALIDATION EXCEPTIONS
    // ===============================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("Validation error on request: {} {}", request.getMethod(), request.getRequestURI());

        Map<String, String> fieldErrors = new HashMap<>();
        List<String> globalErrors = new ArrayList<>();

        // Field-specific errors
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        // Global errors (not tied to specific fields)
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                globalErrors.add(error.getDefaultMessage())
        );

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("fieldErrors", fieldErrors);
        if (!globalErrors.isEmpty()) {
            errorDetails.put("globalErrors", globalErrors);
        }
        errorDetails.put("errorCount", fieldErrors.size() + globalErrors.size());

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                false,
                "Validation failed. Please check the provided data.",
                errorDetails,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        log.warn("Constraint violation on request: {} {}", request.getMethod(), request.getRequestURI());

        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing // Keep first violation message for duplicate keys
                ));

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false,
                "Request validation failed",
                violations,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ===============================
    // FILE UPLOAD EXCEPTIONS
    // ===============================

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        log.warn("File size exceeded for request: {} {} - Max size: {}",
                request.getMethod(), request.getRequestURI(), ex.getMaxUploadSize());

        Map<String, Object> details = new HashMap<>();
        details.put("maxAllowedSize", formatFileSize(ex.getMaxUploadSize()));
        details.put("errorCode", "FILE_SIZE_EXCEEDED");

        ApiResponseDTO<Map<String, Object>> response = new ApiResponseDTO<>(
                false,
                "File size exceeds the maximum allowed limit of " + formatFileSize(ex.getMaxUploadSize()),
                details,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleMultipartException(
            MultipartException ex, HttpServletRequest request) {

        log.warn("Multipart processing error on request: {} {}", request.getMethod(), request.getRequestURI(), ex);

        String message = "File upload error occurred";
        if (ex.getMessage() != null && ex.getMessage().contains("FileSizeLimitExceededException")) {
            message = "One or more files exceed the maximum allowed size";
        } else if (ex.getMessage() != null && ex.getMessage().contains("SizeLimitExceededException")) {
            message = "Total upload size exceeds the maximum allowed limit";
        }

        Map<String, String> details = Map.of(
                "errorCode", "MULTIPART_ERROR",
                "suggestion", "Please check file sizes and formats"
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false, message, details, getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ===============================
    // BUSINESS LOGIC EXCEPTIONS
    // ===============================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Invalid argument for request: {} {} - Error: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        // Determine appropriate status code based on error message
        HttpStatus status = determineStatusFromMessage(ex.getMessage());

        Map<String, String> details = Map.of(
                "errorCode", status == HttpStatus.NOT_FOUND ? "RESOURCE_NOT_FOUND" : "INVALID_REQUEST",
                "path", request.getRequestURI()
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false, ex.getMessage(), details, getCurrentTimestamp()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {

        log.error("Illegal state error on request: {} {}", request.getMethod(), request.getRequestURI(), ex);

        Map<String, String> details = Map.of(
                "errorCode", "ILLEGAL_STATE",
                "path", request.getRequestURI()
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false,
                "Operation cannot be completed due to current system state",
                details,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ===============================
    // SECURITY EXCEPTIONS
    // ===============================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied for request: {} {} - User may lack required permissions",
                request.getMethod(), request.getRequestURI());

        Map<String, String> details = Map.of(
                "errorCode", "ACCESS_DENIED",
                "path", request.getRequestURI(),
                "suggestion", "Please ensure you have the required permissions for this operation"
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false,
                "Access denied. You don't have permission to perform this operation.",
                details,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleAuthentication(
            Exception ex, HttpServletRequest request) {

        log.warn("Authentication error on request: {} {}", request.getMethod(), request.getRequestURI());

        Map<String, String> details = Map.of(
                "errorCode", "AUTHENTICATION_FAILED",
                "path", request.getRequestURI()
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false,
                "Authentication failed. Please verify your credentials.",
                details,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // ===============================
    // DATA ACCESS EXCEPTIONS
    // ===============================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.error("Data integrity violation on request: {} {}", request.getMethod(), request.getRequestURI(), ex);

        String message = "Data constraint violation occurred";
        String errorCode = "DATA_INTEGRITY_ERROR";

        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("Duplicate entry")) {
                message = "Duplicate data detected. This record already exists.";
                errorCode = "DUPLICATE_ENTRY";
            } else if (ex.getMessage().contains("foreign key constraint")) {
                message = "Related data not found. Please ensure all referenced data exists.";
                errorCode = "FOREIGN_KEY_CONSTRAINT";
            }
        }

        Map<String, String> details = Map.of(
                "errorCode", errorCode,
                "path", request.getRequestURI()
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false, message, details, getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ===============================
    // HTTP/REQUEST EXCEPTIONS
    // ===============================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Invalid JSON in request: {} {}", request.getMethod(), request.getRequestURI());

        Map<String, String> details = Map.of(
                "errorCode", "INVALID_JSON",
                "path", request.getRequestURI(),
                "suggestion", "Please ensure the request body contains valid JSON"
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false,
                "Invalid request format. Please check your JSON syntax.",
                details,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.warn("Type mismatch for parameter '{}' on request: {} {}",
                ex.getName(), request.getMethod(), request.getRequestURI());

        Map<String, String> details = Map.of(
                "errorCode", "INVALID_PARAMETER_TYPE",
                "parameter", ex.getName(),
                "expectedType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                "providedValue", Objects.toString(ex.getValue(), "null")
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false,
                String.format("Invalid value for parameter '%s'. Expected %s but received: %s",
                        ex.getName(),
                        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid type",
                        ex.getValue()),
                details,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("Missing required parameter '{}' on request: {} {}",
                ex.getParameterName(), request.getMethod(), request.getRequestURI());

        Map<String, String> details = Map.of(
                "errorCode", "MISSING_PARAMETER",
                "parameterName", ex.getParameterName(),
                "parameterType", ex.getParameterType()
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false,
                String.format("Required parameter '%s' is missing", ex.getParameterName()),
                details,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {

        log.warn("No handler found for request: {} {}", request.getMethod(), request.getRequestURI());

        Map<String, String> details = Map.of(
                "errorCode", "ENDPOINT_NOT_FOUND",
                "method", request.getMethod(),
                "path", request.getRequestURI()
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false,
                String.format("Endpoint not found: %s %s", request.getMethod(), request.getRequestURI()),
                details,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ===============================
    // SYSTEM EXCEPTIONS
    // ===============================

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleIOException(
            IOException ex, HttpServletRequest request) {

        log.error("IO error on request: {} {}", request.getMethod(), request.getRequestURI(), ex);

        Map<String, String> details = Map.of(
                "errorCode", "IO_ERROR",
                "path", request.getRequestURI()
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false,
                "File operation failed. Please try again.",
                details,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(java.nio.file.AccessDeniedException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleFileAccessDenied(
            java.nio.file.AccessDeniedException ex, HttpServletRequest request) {

        log.error("File access denied on request: {} {}", request.getMethod(), request.getRequestURI(), ex);

        Map<String, String> details = Map.of(
                "errorCode", "FILE_ACCESS_DENIED",
                "path", request.getRequestURI()
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false,
                "File access denied. Please check file permissions.",
                details,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(NoSuchAlgorithmException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleNoSuchAlgorithm(
            NoSuchAlgorithmException ex, HttpServletRequest request) {

        log.error("Cryptographic algorithm not available on request: {} {}",
                request.getMethod(), request.getRequestURI(), ex);

        Map<String, String> details = Map.of(
                "errorCode", "CRYPTO_ERROR",
                "path", request.getRequestURI()
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false,
                "Security operation failed. Please contact support.",
                details,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ===============================
    // RUNTIME EXCEPTIONS
    // ===============================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleRuntime(
            RuntimeException ex, HttpServletRequest request) {

        log.error("Runtime error on request: {} {}", request.getMethod(), request.getRequestURI(), ex);

        Map<String, String> details = Map.of(
                "errorCode", "RUNTIME_ERROR",
                "path", request.getRequestURI()
        );

        // Don't expose internal error details in production
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = "A runtime error occurred while processing your request";
        }

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false, message, details, getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ===============================
    // GENERIC EXCEPTION (FALLBACK)
    // ===============================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleGeneric(
            Exception ex, HttpServletRequest request) {

        // Generate unique error ID for tracking
        String errorId = UUID.randomUUID().toString().substring(0, 8);

        log.error("Unexpected error [{}] on request: {} {}",
                errorId, request.getMethod(), request.getRequestURI(), ex);

        Map<String, String> details = Map.of(
                "errorCode", "INTERNAL_SERVER_ERROR",
                "errorId", errorId,
                "path", request.getRequestURI(),
                "suggestion", "Please contact support if this error persists"
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                false,
                GENERIC_ERROR_MESSAGE,
                details,
                getCurrentTimestamp()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private HttpStatus determineStatusFromMessage(String message) {
        if (message == null) return HttpStatus.BAD_REQUEST;

        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("not found") || lowerMessage.contains("does not exist")) {
            return HttpStatus.NOT_FOUND;
        }
        if (lowerMessage.contains("duplicate") || lowerMessage.contains("already exists")) {
            return HttpStatus.CONFLICT;
        }
        if (lowerMessage.contains("unauthorized") || lowerMessage.contains("access denied")) {
            return HttpStatus.FORBIDDEN;
        }

        return HttpStatus.BAD_REQUEST;
    }


    public static class DocumentNotFoundException extends RuntimeException {
        public DocumentNotFoundException(String message) {
            super(message);
        }
    }

    public class DocumentAccessDeniedException extends RuntimeException {
        public DocumentAccessDeniedException(String message) {
            super(message);
        }
    }


}