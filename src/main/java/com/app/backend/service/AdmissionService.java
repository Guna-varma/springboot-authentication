package com.app.backend.service;

import com.app.backend.dto.*;
import com.app.backend.entity.AdmissionForm;
import com.app.backend.entity.ApplicationStatus;
import com.app.backend.entity.ClassLevel;
import com.app.backend.entity.AdmissionGender;
import com.app.backend.repository.AdmissionFormRepository;
import com.app.backend.util.ApplicationNumberGenerator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import jakarta.persistence.criteria.Predicate;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AdmissionService {

    private static final Logger logger = LoggerFactory.getLogger(AdmissionService.class);

    private final AdmissionFormRepository admissionFormRepository;
    private final ValidationService validationService;
    private final EmailService emailService;
    private final ApplicationNumberGenerator applicationNumberGenerator;

    @Autowired
    public AdmissionService(AdmissionFormRepository admissionFormRepository,
                            ValidationService validationService,
                            EmailService emailService,
                            ApplicationNumberGenerator applicationNumberGenerator) {
        this.admissionFormRepository = admissionFormRepository;
        this.validationService = validationService;
        this.emailService = emailService;
        this.applicationNumberGenerator = applicationNumberGenerator;
    }

    @Transactional
    public AdmissionResponseDTO submitAdmissionForm(AdmissionFormDTO dto) {

        logger.info("Processing admission form for student: {}", dto.getStudentName());

        try {
            // 1. field-level validation
            validationService.validateAdmissionForm(dto);

            // 2. business-rule validation (duplicates)
            validateUniqueConstraints(dto);   // may throw IllegalArgumentException

            // 3. persist
            AdmissionForm admissionForm = createAdmissionFormEntity(dto);
            admissionForm.setApplicationNumber(applicationNumberGenerator.generateApplicationNumber());
            AdmissionForm saved = admissionFormRepository.save(admissionForm);

            // 4. async e-mail
            emailService.sendConfirmationEmail(saved);

            return new AdmissionResponseDTO(
                    saved.getApplicationNumber(),
                    saved.getStudentName(),
                    "Application submitted successfully! You will receive a confirmation email shortly.",
                    saved.getApplicationStatus().getDisplayName(),
                    saved.getSubmittedAt()
            );

        } catch (DataIntegrityViolationException ex) {
            logger.warn("Duplicate key while saving admission form", ex);
            // propagate as IllegalArgumentException so the existing handler shows a nice 400 response
            throw new IllegalArgumentException("An application with this email or contact number already exists.");
        }

    }

    @Transactional(readOnly = true)
    public Optional<AdmissionForm> getApplicationByNumber(String applicationNumber) {
        return admissionFormRepository.findByApplicationNumber(applicationNumber);
    }

    private void validateUniqueConstraints(AdmissionFormDTO dto) {
        if (admissionFormRepository.existsByEmailAddress(dto.getEmail())) {
            throw new IllegalArgumentException("An application with this email address already exists.");
        }

        if (admissionFormRepository.existsByContactNumber(dto.getContact())) {
            throw new IllegalArgumentException("An application with this contact number already exists.");
        }
    }

    private AdmissionForm createAdmissionFormEntity(AdmissionFormDTO dto) {
        AdmissionForm admissionForm = new AdmissionForm();

        admissionForm.setStudentName(dto.getStudentName().trim());
        admissionForm.setDateOfBirth(dto.getDob());
        admissionForm.setGender(AdmissionGender.fromDisplayName(dto.getGender()));
        admissionForm.setClassApplied(ClassLevel.fromDisplayName(dto.getClassApplied()));
        admissionForm.setPreviousSchool(dto.getPreviousSchool() != null ? dto.getPreviousSchool().trim() : null);
        admissionForm.setFatherName(dto.getFatherName().trim());
        admissionForm.setMotherName(dto.getMotherName().trim());
        admissionForm.setContactNumber(dto.getContact().trim());
        admissionForm.setEmailAddress(dto.getEmail().trim().toLowerCase());
        admissionForm.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        admissionForm.setDeclarationAccepted(dto.getDeclarationAccepted());
        admissionForm.setApplicationStatus(ApplicationStatus.SUBMITTED);

        return admissionForm;
    }

    @Transactional(readOnly = true)
    public PagedAdmissionResponse getAllAdmissions(int page, int size, String sortBy,
                                                   String sortDirection, AdmissionSearchCriteria criteria) {
        try {
            // Create sort object
            Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Sort sort = Sort.by(direction, sortBy);

            // Create pageable
            Pageable pageable = PageRequest.of(page, size, sort);

            // Get specifications for filtering
            Specification<AdmissionForm> spec = createSpecification(criteria);

            // Execute query - this will now work
            Page<AdmissionForm> pageResult = admissionFormRepository.findAll(spec, pageable);

            // Build response
            return PagedAdmissionResponse.builder()
                    .admissions(pageResult.getContent())
                    .currentPage(pageResult.getNumber())
                    .totalPages(pageResult.getTotalPages())
                    .totalElements(pageResult.getTotalElements())
                    .pageSize(pageResult.getSize())
                    .hasNext(pageResult.hasNext())
                    .hasPrevious(pageResult.hasPrevious())
                    .isFirst(pageResult.isFirst())
                    .isLast(pageResult.isLast())
                    .numberOfElements(pageResult.getNumberOfElements())
                    .empty(pageResult.isEmpty())
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving paginated admissions", e);
            throw new RuntimeException("Failed to retrieve admission applications", e);
        }
    }

    @Transactional(readOnly = true)
    public AdmissionStatistics getAdmissionStatistics() {
        try {
            long totalApplications = admissionFormRepository.count();

            // Status breakdown
            Map<String, Long> statusBreakdown = Arrays.stream(ApplicationStatus.values())
                    .collect(Collectors.toMap(
                            Enum::name,
                            status -> admissionFormRepository.countByApplicationStatus(status)
                    ));

            // Class breakdown
            Map<String, Long> classBreakdown = Arrays.stream(ClassLevel.values())
                    .collect(Collectors.toMap(
                            Enum::name,
                            level -> admissionFormRepository.countByClassApplied(level)
                    ));

            // Gender breakdown
            Map<String, Long> genderBreakdown = Arrays.stream(AdmissionGender.values())
                    .collect(Collectors.toMap(
                            Enum::name,
                            gender -> admissionFormRepository.countByGender(gender)
                    ));

            // Monthly applications (last 12 months)
            LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12);
            Map<String, Long> monthlyApplications = admissionFormRepository
                    .findMonthlyApplicationCounts(twelveMonthsAgo);

            // Recent applications (last 7 days)
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            long recentApplicationsCount = admissionFormRepository
                    .countBySubmittedAtGreaterThanEqual(sevenDaysAgo);

            // Average processing time (submitted to last updated)
            double averageProcessingTime = admissionFormRepository.calculateAverageProcessingTime();

            return AdmissionStatistics.builder()
                    .totalApplications(totalApplications)
                    .statusBreakdown(statusBreakdown)
                    .classBreakdown(classBreakdown)
                    .genderBreakdown(genderBreakdown)
                    .monthlyApplications(monthlyApplications)
                    .recentApplicationsCount(recentApplicationsCount)
                    .averageProcessingTime(averageProcessingTime)
                    .build();

        } catch (Exception e) {
            log.error("Error calculating admission statistics", e);
            throw new RuntimeException("Failed to calculate admission statistics", e);
        }
    }

    private Specification<AdmissionForm> createSpecification(AdmissionSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search in student name, father name, mother name, or application number
            if (criteria.getSearch() != null && !criteria.getSearch().trim().isEmpty()) {
                String searchTerm = "%" + criteria.getSearch().toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("studentName")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fatherName")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("motherName")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("applicationNumber")), searchTerm)
                );
                predicates.add(searchPredicate);
            }

            // Status filter
            if (criteria.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("applicationStatus"), criteria.getStatus()));
            }

            // Class level filter
            if (criteria.getClassLevel() != null) {
                predicates.add(criteriaBuilder.equal(root.get("classApplied"), criteria.getClassLevel()));
            }

            // Gender filter
            if (criteria.getGender() != null) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), criteria.getGender()));
            }

            // Date range filters
            if (criteria.getSubmittedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("submittedAt"), criteria.getSubmittedAfter().atStartOfDay()));
            }

            if (criteria.getSubmittedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("submittedAt"), criteria.getSubmittedBefore().atTime(23, 59, 59)));
            }

            if (criteria.getUpdatedAfter() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("updatedAt"), criteria.getUpdatedAfter().atStartOfDay()));
            }

            if (criteria.getUpdatedBefore() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("updatedAt"), criteria.getUpdatedBefore().atTime(23, 59, 59)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional
    public AdmissionForm updateApplicationStatus(String applicationNumber,
                                                 ApplicationStatus newStatus,
                                                 String comments) {
        AdmissionForm application = admissionFormRepository
                .findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationNumber));

        ApplicationStatus previousStatus = application.getApplicationStatus();

        // Update status and metadata
        application.setApplicationStatus(newStatus);
        application.setUpdatedBy(getCurrentUser());

        AdmissionForm savedApplication = admissionFormRepository.save(application);

        // Send status update email asynchronously
        try {
            emailService.sendStatusUpdateEmail(savedApplication, previousStatus, comments);
            logger.info("Status update email triggered for application: {}", applicationNumber);
        } catch (Exception e) {
            logger.error("Failed to trigger status update email for application: {}", applicationNumber, e);
            // Don't fail the transaction for email issues
        }

        return savedApplication;
    }

    @Transactional
    public BulkUpdateResult bulkUpdateStatus(List<String> applicationNumbers,
                                             ApplicationStatus newStatus,
                                             String comments) {
        int successCount = 0;
        int failedCount = 0;
        List<String> successfulApplications = new ArrayList<>();
        Map<String, String> failedApplications = new HashMap<>();
        List<AdmissionForm> updatedApplications = new ArrayList<>();

        for (String appNumber : applicationNumbers) {
            try {
                AdmissionForm updatedApp = updateApplicationStatusInternal(appNumber, newStatus, comments);
                updatedApplications.add(updatedApp);
                successfulApplications.add(appNumber);
                successCount++;
            } catch (Exception e) {
                failedApplications.put(appNumber, e.getMessage());
                failedCount++;
                logger.error("Failed to update status for application: {}", appNumber, e);
            }
        }

        // Send bulk emails for successfully updated applications
        if (!updatedApplications.isEmpty()) {
            try {
                emailService.sendBulkStatusUpdateEmails(updatedApplications, newStatus, comments)
                        .thenAccept(emailResult -> {
                            logger.info("Bulk email sending completed for {} applications. Success: {}, Failed: {}, Skipped: {}",
                                    emailResult.getTotalEmails(), emailResult.getSuccessCount(),
                                    emailResult.getFailedCount(), emailResult.getSkippedCount());
                        })
                        .exceptionally(ex -> {
                            logger.error("Error in bulk email sending process", ex);
                            return null;
                        });
            } catch (Exception e) {
                logger.error("Failed to trigger bulk status update emails", e);
                // Don't fail the transaction for email issues
            }
        }

        return BulkUpdateResult.builder()
                .successCount(successCount)
                .failedCount(failedCount)
                .successfulApplications(successfulApplications)
                .failedApplications(failedApplications)
                .build();
    }

    // Internal method without email sending to avoid duplicate emails in bulk updates
    private AdmissionForm updateApplicationStatusInternal(String applicationNumber,
                                                          ApplicationStatus newStatus,
                                                          String comments) {
        AdmissionForm application = admissionFormRepository
                .findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationNumber));

        application.setApplicationStatus(newStatus);
        application.setUpdatedBy(getCurrentUser());

        return admissionFormRepository.save(application);
    }


//    @Transactional
//    public AdmissionForm updateApplicationStatus(String applicationNumber,
//                                                 ApplicationStatus newStatus,
//                                                 String comments) {
//        AdmissionForm application = admissionFormRepository
//                .findByApplicationNumber(applicationNumber)
//                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationNumber));
//
//        // Update status and metadata
//        application.setApplicationStatus(newStatus);
//        application.setUpdatedBy(getCurrentUser()); // Implement this method
//
//        // You might want to add a comments field to your entity
//        // application.setComments(comments);
//
//        return admissionFormRepository.save(application);
//    }
//
//    @Transactional
//    public BulkUpdateResult bulkUpdateStatus(List<String> applicationNumbers,
//                                             ApplicationStatus newStatus,
//                                             String comments) {
//        int successCount = 0;
//        int failedCount = 0;
//        List<String> successfulApplications = new ArrayList<>();
//        Map<String, String> failedApplications = new HashMap<>();
//
//        for (String appNumber : applicationNumbers) {
//            try {
//                updateApplicationStatus(appNumber, newStatus, comments);
//                successfulApplications.add(appNumber);
//                successCount++;
//            } catch (Exception e) {
//                failedApplications.put(appNumber, e.getMessage());
//                failedCount++;
//            }
//        }
//
//        return BulkUpdateResult.builder()
//                .successCount(successCount)
//                .failedCount(failedCount)
//                .successfulApplications(successfulApplications)
//                .failedApplications(failedApplications)
//                .build();
//    }

    private String getCurrentUser() {
        // For now, return a default value
        // Later, you can integrate with your authentication system
        return "ADMIN"; // or get from security context
    }


}

