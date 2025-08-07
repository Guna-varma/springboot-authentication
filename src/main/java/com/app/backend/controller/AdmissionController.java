package com.app.backend.controller;

import com.app.backend.dto.*;
import com.app.backend.entity.AdmissionForm;
import com.app.backend.entity.AdmissionGender;
import com.app.backend.entity.ApplicationStatus;
import com.app.backend.entity.ClassLevel;
import com.app.backend.service.AdmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admissions")
@Tag(name = "Admission Management", description = "APIs for managing student admission forms")
@Validated
public class AdmissionController {

    private static final Logger logger = LoggerFactory.getLogger(AdmissionController.class);
    private final AdmissionService admissionService;
    private static final String APP_NUM_REGEX = "^ACE\\d{10}$";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public AdmissionController(AdmissionService admissionService) {
        this.admissionService = admissionService;
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit admission form", description = "Submit a new student admission application")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Application submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Duplicate application"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AdmissionResponseDTO> submitAdmissionForm(@Valid @RequestBody AdmissionFormDTO admissionFormDTO) {
        logger.info("Received admission form submission for student: {}", admissionFormDTO.getStudentName());

        AdmissionResponseDTO response = admissionService.submitAdmissionForm(admissionFormDTO);

        logger.info("Admission form processed successfully with application number: {}", response.getApplicationNumber());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/status/{applicationNumber}")
    @Operation(
            summary     = "Get application status",
            description = "Retrieve application status by application number"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application found"),
            @ApiResponse(responseCode = "400", description = "Invalid application number format"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<ApiResponseDTO<?>> getApplicationStatus(
            @PathVariable
            @Pattern(regexp = APP_NUM_REGEX,
                    message = "Application number must start with 'ACE' followed by 10 digits")
            String applicationNumber) {

        logger.info("Checking status for application number: {}", applicationNumber);

        Optional<AdmissionForm> opt = admissionService.getApplicationByNumber(applicationNumber);

        if (opt.isPresent()) {
            ApiResponseDTO<AdmissionForm> body = new ApiResponseDTO<>(
                    true,
                    "Application retrieved successfully",
                    opt.get(),
                    TS.format(LocalDateTime.now())
            );
            return ResponseEntity.ok(body);
        }

        ApiResponseDTO<Map<String, String>> body = new ApiResponseDTO<>(
                false,
                "Application not found",
                Map.of(
                        "errorCode", "RESOURCE_NOT_FOUND",
                        "path", "/api/admissions/status/" + applicationNumber
                ),
                TS.format(LocalDateTime.now())
        );
        logger.warn("Application not found: {}", applicationNumber);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> healthCheck() {
        Map<String, String> healthData = Map.of(
                "status", "UP",
                "service", "Admission Service",
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "version", "1.0.0"
        );

        ApiResponseDTO<Map<String, String>> response = new ApiResponseDTO<>(
                true,
                "Admission service is running successfully!",
                healthData,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @Operation(
            summary = "Get all admission applications",
            description = "Retrieve paginated list of admission applications with filtering, sorting, and search capabilities"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponseDTO<?>> getAllAdmissions(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page number must be non-negative")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be at least 1")
            @Max(value = 100, message = "Page size cannot exceed 100")
            int size,

            @RequestParam(defaultValue = "submittedAt")
            @Pattern(regexp = "^(id|studentName|applicationNumber|submittedAt|updatedAt|applicationStatus|classApplied|gender)$",
                    message = "Invalid sort field")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'")
            String sortDirection,

            @RequestParam(required = false)
            @Size(min = 1, max = 100, message = "Search query must be between 1 and 100 characters")
            String search,

            @RequestParam(required = false)
            ApplicationStatus status,

            @RequestParam(required = false)
            ClassLevel classLevel,

            @RequestParam(required = false)
            AdmissionGender gender,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate submittedAfter,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate submittedBefore,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate updatedAfter,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate updatedBefore) {

        try {
            logger.info("Fetching admissions - Page: {}, Size: {}, SortBy: {}, Direction: {}, Search: {}, Status: {}",
                    page, size, sortBy, sortDirection, search, status);

            AdmissionSearchCriteria criteria = AdmissionSearchCriteria.builder()
                    .search(search)
                    .status(status)
                    .classLevel(classLevel)
                    .gender(gender)
                    .submittedAfter(submittedAfter)
                    .submittedBefore(submittedBefore)
                    .updatedAfter(updatedAfter)
                    .updatedBefore(updatedBefore)
                    .build();

            PagedAdmissionResponse pagedResponse = admissionService.getAllAdmissions(
                    page, size, sortBy, sortDirection, criteria);

            ApiResponseDTO<PagedAdmissionResponse> response = new ApiResponseDTO<>(
                    true,
                    String.format("Retrieved %d admission applications successfully",
                            pagedResponse.getTotalElements()),
                    pagedResponse,
                    TS.format(LocalDateTime.now())
            );

            logger.info("Successfully retrieved {} admission applications", pagedResponse.getTotalElements());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());

            ApiResponseDTO<Map<String, String>> errorResponse = new ApiResponseDTO<>(
                    false,
                    "Invalid request parameters",
                    Map.of(
                            "errorCode", "INVALID_PARAMETERS",
                            "details", e.getMessage(),
                            "path", "/api/admissions/all"
                    ),
                    TS.format(LocalDateTime.now())
            );

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("Error retrieving admission applications", e);

            ApiResponseDTO<Map<String, String>> errorResponse = new ApiResponseDTO<>(
                    false,
                    "Failed to retrieve admission applications",
                    Map.of(
                            "errorCode", "INTERNAL_SERVER_ERROR",
                            "path", "/api/admissions/all"
                    ),
                    TS.format(LocalDateTime.now())
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/statistics")
    @Operation(
            summary = "Get admission statistics",
            description = "Get comprehensive statistics about admission applications"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getAdmissionStatistics() {
        try {
            logger.info("Fetching admission statistics");

            AdmissionStatistics stats = admissionService.getAdmissionStatistics();

            ApiResponseDTO<AdmissionStatistics> response = new ApiResponseDTO<>(
                    true,
                    "Admission statistics retrieved successfully",
                    stats,
                    TS.format(LocalDateTime.now())
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving admission statistics", e);
            return createErrorResponse(
                    "Failed to retrieve admission statistics",
                    "INTERNAL_SERVER_ERROR",
                    "/api/admissions/statistics",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PutMapping("/updateStatus/{applicationNumber}")
    @Operation(
            summary = "Update application status",
            description = "Update the status of an admission application"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status or application number"),
            @ApiResponse(responseCode = "404", description = "Application not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponseDTO<?>> updateApplicationStatus(
            @PathVariable
            @Pattern(regexp = APP_NUM_REGEX,
                    message = "Application number must start with 'ACE' followed by 10 digits")
            String applicationNumber,

            @Valid @RequestBody UpdateStatusDTO updateStatusDTO) {

        try {
            logger.info("Updating status for application: {} to {}",
                    applicationNumber, updateStatusDTO.getNewStatus());

            AdmissionForm updatedApplication = admissionService.updateApplicationStatus(
                    applicationNumber,
                    updateStatusDTO.getNewStatus(),
                    updateStatusDTO.getComments()
            );

            ApiResponseDTO<AdmissionForm> response = new ApiResponseDTO<>(
                    true,
                    String.format("Application status updated to %s successfully",
                            updateStatusDTO.getNewStatus()),
                    updatedApplication,
                    TS.format(LocalDateTime.now())
            );

            logger.info("Status updated successfully for application: {}", applicationNumber);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status update request: {}", e.getMessage());

            ApiResponseDTO<Map<String, String>> errorResponse = new ApiResponseDTO<>(
                    false,
                    "Invalid status update request",
                    Map.of(
                            "errorCode", "INVALID_REQUEST",
                            "details", e.getMessage(),
                            "path", "/api/admissions/status/" + applicationNumber
                    ),
                    TS.format(LocalDateTime.now())
            );

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("Error updating application status", e);
            return createErrorResponse(
                    "Failed to update application status",
                    "INTERNAL_SERVER_ERROR",
                    "/api/admissions/status/" + applicationNumber,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PutMapping("/updateBulkStatus")
    @Operation(
            summary = "Bulk update application status",
            description = "Update status for multiple applications at once"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk update completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponseDTO<?>> bulkUpdateStatus(
            @Valid @RequestBody BulkStatusUpdateDTO bulkUpdateDTO) {

        try {
            logger.info("Bulk updating status for {} applications",
                    bulkUpdateDTO.getApplicationNumbers().size());

            BulkUpdateResult result = admissionService.bulkUpdateStatus(
                    bulkUpdateDTO.getApplicationNumbers(),
                    bulkUpdateDTO.getNewStatus(),
                    bulkUpdateDTO.getComments()
            );

            ApiResponseDTO<BulkUpdateResult> response = new ApiResponseDTO<>(
                    true,
                    String.format("Bulk update completed: %d successful, %d failed",
                            result.getSuccessCount(), result.getFailedCount()),
                    result,
                    TS.format(LocalDateTime.now())
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in bulk status update", e);
            return createErrorResponse(
                    "Failed to perform bulk status update",
                    "INTERNAL_SERVER_ERROR",
                    "/api/admissions/bulk-status",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private ResponseEntity<ApiResponseDTO<?>> createErrorResponse(
            String message, String errorCode, String path, HttpStatus status) {

        ApiResponseDTO<Map<String, String>> errorResponse = new ApiResponseDTO<>(
                false,
                message,
                Map.of(
                        "errorCode", errorCode,
                        "path", path
                ),
                TS.format(LocalDateTime.now())
        );
        return ResponseEntity.status(status).body(errorResponse);
    }
}