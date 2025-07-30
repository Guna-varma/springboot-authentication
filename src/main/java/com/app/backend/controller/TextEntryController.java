package com.app.backend.controller;

import com.app.backend.dto.ApiResponseDTO;
import com.app.backend.dto.PageResponseDTO;
import com.app.backend.dto.TextEntryDTO;
import com.app.backend.dto.TextEntryResponseDTO;
import com.app.backend.service.TextEntryService;
import com.app.backend.util.TextEntryNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping("/api/text")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TextEntryController {

    private final TextEntryService textEntryService;

    // ============= CRUD Operations =============

    @PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
    @PostMapping("/create")
    public ResponseEntity<ApiResponseDTO<TextEntryResponseDTO>> createTextEntry(
            @Valid @RequestBody TextEntryDTO dto,
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            String userEmail = authentication.getName();
            TextEntryResponseDTO created = textEntryService.createTextEntry(dto, userEmail);

            log.info("Created TextEntry with id: {} by user: {} from IP: {}",
                    created.getId(), userEmail, getClientIp(request));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDTO<>(
                            true,
                            "Text entry created successfully",
                            created,
                            getCurrentTimestamp()
                    ));

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid input for TextEntry creation: {}", ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>(false, ex.getMessage(), null, getCurrentTimestamp()));
        } catch (Exception ex) {
            log.error("Failed to create TextEntry", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to create text entry", null, getCurrentTimestamp()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponseDTO<TextEntryResponseDTO>> updateTextEntry(
            @PathVariable @NotNull @Min(1) Long id,
            @Valid @RequestBody TextEntryDTO dto,
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            String userEmail = authentication.getName();
            TextEntryResponseDTO updated = textEntryService.updateTextEntry(id, dto, userEmail);

            log.info("Updated TextEntry with id: {} by user: {} from IP: {}",
                    id, userEmail, getClientIp(request));

            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true,
                    "Text entry updated successfully",
                    updated,
                    getCurrentTimestamp()
            ));

        } catch (TextEntryNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, ex.getMessage(), null, getCurrentTimestamp()));
        } catch (RuntimeException ex) {
            if (ex.getMessage().contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponseDTO<>(false, ex.getMessage(), null, getCurrentTimestamp()));
            }
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>(false, ex.getMessage(), null, getCurrentTimestamp()));
        } catch (Exception ex) {
            log.error("Failed to update TextEntry with id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to update text entry", null, getCurrentTimestamp()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteTextEntry(
            @PathVariable @NotNull @Min(1) Long id,
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            String userEmail = authentication.getName();
            textEntryService.deleteTextEntry(id, userEmail);

            log.info("Deleted TextEntry with id: {} by user: {} from IP: {}",
                    id, userEmail, getClientIp(request));

            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true,
                    "Text entry deleted successfully",
                    null,
                    getCurrentTimestamp()
            ));

        } catch (TextEntryNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, ex.getMessage(), null, getCurrentTimestamp()));
        } catch (RuntimeException ex) {
            if (ex.getMessage().contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponseDTO<>(false, ex.getMessage(), null, getCurrentTimestamp()));
            }
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>(false, ex.getMessage(), null, getCurrentTimestamp()));
        } catch (Exception ex) {
            log.error("Failed to delete TextEntry with id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to delete text entry", null, getCurrentTimestamp()));
        }
    }

    // ============= Public Read Operations =============

    @GetMapping("/public/get/{id}")
    public ResponseEntity<ApiResponseDTO<TextEntryResponseDTO>> getTextEntry(
            @PathVariable @NotNull @Min(1) Long id,
            HttpServletRequest request
    ) {
        try {
            long startTime = System.currentTimeMillis();
            TextEntryResponseDTO textEntry = textEntryService.findByIdCached(id);
            long duration = System.currentTimeMillis() - startTime;

            String cacheStatus = duration < 20 ? "🚀 CACHE HIT" : "🐌 CACHE MISS";
            log.info("Retrieved TextEntry - ID: {} - {}ms - {} from IP: {}",
                    id, duration, cacheStatus, getClientIp(request));

            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true,
                    "Text entry found",
                    textEntry,
                    getCurrentTimestamp()
            ));

        } catch (TextEntryNotFoundException ex) {
            log.warn("TextEntry not found for id: {} from IP: {}", id, getClientIp(request));
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>(false, ex.getMessage(), null, getCurrentTimestamp()));
        } catch (Exception ex) {
            log.error("Failed to retrieve TextEntry with id: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to retrieve text entry", null, getCurrentTimestamp()));
        }
    }

    @GetMapping("/public/getAll")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<TextEntryResponseDTO>>> listTextEntries(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request
    ) {
        try {
            long startTime = System.currentTimeMillis();

            if (size > 100) {
                size = 100;
                log.warn("Page size limited to 100 for performance from IP: {}", getClientIp(request));
            }

            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Sort sort = Sort.by(direction, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            PageResponseDTO<TextEntryResponseDTO> entries;

            if (startDate != null && endDate != null) {
                LocalDateTime start = LocalDateTime.parse(startDate);
                LocalDateTime end = LocalDateTime.parse(endDate);
                entries = textEntryService.findByDateRangePaginated(start, end, pageable);
                log.info("Listed TextEntries by date range {} to {} from IP: {}", startDate, endDate, getClientIp(request));
            } else if (search != null && !search.isBlank()) {
                entries = textEntryService.searchInMessagePaginated(search.trim(), pageable);
                log.info("Searched TextEntries with term '{}' from IP: {}", search, getClientIp(request));
            } else if (owner != null && !owner.isBlank()) {
                entries = textEntryService.findByOwnerPaginated(owner.trim(), pageable);
                log.info("Listed TextEntries by owner '{}' from IP: {}", owner, getClientIp(request));
            } else {
                entries = textEntryService.findAllTextEntriesPaginated(pageable);
                log.info("Listed all TextEntries from IP: {}", getClientIp(request));
            }

            long duration = System.currentTimeMillis() - startTime;

            // ✅ ASCII-safe cache status for headers
//            String cacheStatus = duration < 25 ? "CACHE_HIT" : "CACHE_MISS";
            String cacheStatus = duration < 50 ? "CACHE_HIT" : "CACHE_MISS";
            String cacheEmoji = duration < 50 ? "🚀" : "🐌";

            log.info("Listed entries - {}ms - {} {} from IP: {}",
                    duration, cacheEmoji, cacheStatus, getClientIp(request));

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(entries.getTotalElements()))
                    .header("X-Page-Number", String.valueOf(page))
                    .header("X-Page-Size", String.valueOf(size))
                    .header("X-Total-Pages", String.valueOf(entries.getTotalPages()))
                    .header("X-Cache-Status", cacheStatus)
                    .header("X-Response-Time-Ms", String.valueOf(duration))
                    .body(new ApiResponseDTO<>(
                            true,
                            String.format("Retrieved %d text entries (%s)", entries.getNumberOfElements(), cacheStatus),
                            entries,
                            getCurrentTimestamp()
                    ));

        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>(false, "Invalid date format. Use ISO format (YYYY-MM-DDTHH:MM:SS)", null, getCurrentTimestamp()));
        } catch (Exception ex) {
            log.error("Failed to retrieve text entries list", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to retrieve text entries", null, getCurrentTimestamp()));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-entries")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<TextEntryResponseDTO>>> getUserTextEntries(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            if (size > 100) size = 100;

            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            Sort sort = Sort.by(direction, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            String userEmail = authentication.getName();
            PageResponseDTO<TextEntryResponseDTO> entries = textEntryService.findByUserPaginated(userEmail, pageable);

            log.info("Listed user's TextEntries - user: {} from IP: {}", userEmail, getClientIp(request));

            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(entries.getTotalElements()))
                    .header("X-Page-Number", String.valueOf(page))
                    .header("X-Page-Size", String.valueOf(size))
                    .header("X-Total-Pages", String.valueOf(entries.getTotalPages()))
                    .body(new ApiResponseDTO<>(
                            true,
                            String.format("Retrieved %d text entries", entries.getNumberOfElements()),
                            entries,
                            getCurrentTimestamp()
                    ));

        } catch (Exception ex) {
            log.error("Failed to retrieve user's text entries", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to retrieve text entries", null, getCurrentTimestamp()));
        }
    }

    @GetMapping("/public/countEntries")
    public ResponseEntity<ApiResponseDTO<Long>> getTextEntriesCount(
            @RequestParam(required = false) String owner,
            HttpServletRequest request
    ) {
        try {
            long count = owner != null && !owner.isBlank()
                    ? textEntryService.getCountByOwnerCached(owner.trim())
                    : textEntryService.getTotalCountCached();

            log.info("Text entries count requested: {} from IP: {}", count, getClientIp(request));

            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true,
                    "Count retrieved successfully",
                    count,
                    getCurrentTimestamp()
            ));

        } catch (Exception ex) {
            log.error("Failed to get text entries count", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to get count", null, getCurrentTimestamp()));
        }
    }

    // ============= Admin Operations =============

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cache/clear")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> clearCache(
            @RequestParam(required = false) String confirmToken,
            @RequestParam(defaultValue = "textEntries") String cacheName,
            @RequestParam(defaultValue = "false") boolean clearAll,
            @RequestParam(defaultValue = "false") boolean warmUp,
            HttpServletRequest request
    ) {
        try {
            String expectedToken = "CONFIRM_CLEAR";
            if (!expectedToken.equals(confirmToken)) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponseDTO<>(false, "Confirmation token required: " + expectedToken, null, getCurrentTimestamp()));
            }

            Map<String, Object> result = textEntryService.performCacheOperation(clearAll, cacheName, warmUp);

            log.warn("CACHE OPERATION COMPLETED by ADMIN from IP: {} - Details: {}",
                    getClientIp(request), result);

            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true,
                    "Cache operation completed successfully",
                    result,
                    getCurrentTimestamp()
            ));

        } catch (Exception ex) {
            log.error("Failed to perform cache operation", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to perform cache operation", null, getCurrentTimestamp()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/cache/stats")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getCacheStatistics(HttpServletRequest request) {
        try {
            Map<String, Object> stats = textEntryService.getCacheStatistics();

            log.info("Cache statistics requested by ADMIN from IP: {}", getClientIp(request));

            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true,
                    "Cache statistics retrieved successfully",
                    stats,
                    getCurrentTimestamp()
            ));
        } catch (Exception ex) {
            log.error("Failed to get cache statistics", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to get cache statistics", null, getCurrentTimestamp()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cache/toggle")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> toggleCacheImplementation(
            @RequestParam boolean useAutoCache,
            HttpServletRequest request
    ) {
        try {
            Map<String, Object> result = textEntryService.toggleCacheImplementation(useAutoCache);

            log.info("Cache implementation toggled to {} by ADMIN from IP: {}",
                    useAutoCache ? "AUTO" : "MANUAL", getClientIp(request));

            return ResponseEntity.ok(new ApiResponseDTO<>(
                    true,
                    "Cache implementation toggled successfully",
                    result,
                    getCurrentTimestamp()
            ));
        } catch (Exception ex) {
            log.error("Failed to toggle cache implementation", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Failed to toggle cache implementation", null, getCurrentTimestamp()));
        }
    }

    // ============= Utility Methods =============

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String getCurrentTimestamp() {
        return Instant.now().toString();
    }
}
