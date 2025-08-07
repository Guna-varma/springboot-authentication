package com.app.backend.service;

import com.app.backend.entity.AdmissionForm;
import com.app.backend.entity.ApplicationStatus;
import com.app.backend.dto.BulkUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:gunavarma.website@gmail.com}")
    private String fromEmail;

    @Value("${spring.mail.from-name:Academy Admissions}")
    private String fromName;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.email.retry-attempts:3}")
    private int retryAttempts;

    @Value("${app.email.retry-delay:1000}")
    private long retryDelay;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Existing confirmation email method
    @Async
    public void sendConfirmationEmail(AdmissionForm admissionForm) {
        if (!emailEnabled) {
            logger.info("Email service disabled, skipping confirmation email for: {}",
                    admissionForm.getEmailAddress());
            return;
        }

        try {
            validateAdmissionForm(admissionForm);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromEmail));
            message.setTo(admissionForm.getEmailAddress());
            message.setSubject("Admission Application Confirmation - " + admissionForm.getApplicationNumber());
            message.setText(buildConfirmationEmailContent(admissionForm));

            sendEmailWithRetry(message, "confirmation", admissionForm.getEmailAddress());

        } catch (Exception e) {
            logger.error("Failed to send confirmation email to: {}", admissionForm.getEmailAddress(), e);
        }
    }

    // New status update email method
    @Async
    public void sendStatusUpdateEmail(AdmissionForm admissionForm,
                                      ApplicationStatus previousStatus,
                                      String comments) {
        if (!emailEnabled) {
            logger.info("Email service disabled, skipping status update email for: {}",
                    admissionForm.getEmailAddress());
            return;
        }

        try {
            validateAdmissionForm(admissionForm);
            validateStatusUpdate(admissionForm, previousStatus);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromEmail));
            message.setTo(admissionForm.getEmailAddress());
            message.setSubject(buildStatusUpdateSubject(admissionForm));
            message.setText(buildStatusUpdateEmailContent(admissionForm, previousStatus, comments));

            sendEmailWithRetry(message, "status update", admissionForm.getEmailAddress());

            logger.info("Status update email sent successfully to: {} for application: {}",
                    admissionForm.getEmailAddress(), admissionForm.getApplicationNumber());

        } catch (Exception e) {
            logger.error("Failed to send status update email to: {} for application: {}",
                    admissionForm.getEmailAddress(), admissionForm.getApplicationNumber(), e);
        }
    }

    // Bulk status update email method
    @Async
    public CompletableFuture<BulkEmailResult> sendBulkStatusUpdateEmails(
            List<AdmissionForm> applications,
            ApplicationStatus newStatus,
            String comments) {

        if (!emailEnabled) {
            logger.info("Email service disabled, skipping bulk status update emails");
            return CompletableFuture.completedFuture(
                    BulkEmailResult.builder()
                            .totalEmails(applications.size())
                            .successCount(0)
                            .failedCount(applications.size())
                            .skippedCount(applications.size())
                            .build()
            );
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        logger.info("Starting bulk email sending for {} applications with status: {}",
                applications.size(), newStatus);

        List<CompletableFuture<Void>> emailTasks = applications.stream()
                .map(application -> CompletableFuture.runAsync(() -> {
                    try {
                        // Validate application
                        if (!isValidForEmail(application)) {
                            skippedCount.incrementAndGet();
                            logger.warn("Skipping email for invalid application: {}",
                                    application.getApplicationNumber());
                            return;
                        }

                        // Send individual email
                        SimpleMailMessage message = new SimpleMailMessage();
                        message.setFrom(String.format("%s <%s>", fromName, fromEmail));
                        message.setTo(application.getEmailAddress());
                        message.setSubject(buildBulkStatusUpdateSubject(application, newStatus));
                        message.setText(buildBulkStatusUpdateEmailContent(application, newStatus, comments));

                        sendEmailWithRetry(message, "bulk status update", application.getEmailAddress());
                        successCount.incrementAndGet();

                        logger.debug("Bulk status update email sent to: {} for application: {}",
                                application.getEmailAddress(), application.getApplicationNumber());

                    } catch (Exception e) {
                        failedCount.incrementAndGet();
                        logger.error("Failed to send bulk status update email to: {} for application: {}",
                                application.getEmailAddress(), application.getApplicationNumber(), e);
                    }
                }))
                .toList();

        // Wait for all emails to complete
        CompletableFuture<Void> allEmails = CompletableFuture.allOf(
                emailTasks.toArray(new CompletableFuture[0])
        );

        return allEmails.thenApply(v -> {
            BulkEmailResult result = BulkEmailResult.builder()
                    .totalEmails(applications.size())
                    .successCount(successCount.get())
                    .failedCount(failedCount.get())
                    .skippedCount(skippedCount.get())
                    .build();

            logger.info("Bulk email sending completed - Total: {}, Success: {}, Failed: {}, Skipped: {}",
                    result.getTotalEmails(), result.getSuccessCount(),
                    result.getFailedCount(), result.getSkippedCount());

            return result;
        });
    }

    // Email content builders
    private String buildConfirmationEmailContent(AdmissionForm admissionForm) {
        return String.format(
                "Dear %s,\n\n" +
                        "Thank you for submitting your admission application to our academy.\n\n" +
                        "Application Details:\n" +
                        "Application Number: %s\n" +
                        "Student Name: %s\n" +
                        "Class Applied: %s\n" +
                        "Submission Date: %s\n\n" +
                        "Your application is currently under review. We will contact you within 2-3 business days " +
                        "regarding the next steps in the admission process.\n\n" +
                        "You can track your application status at: %s/application-status\n\n" +
                        "Please keep your application number safe for future reference.\n\n" +
                        "If you have any questions, please contact us at admissions@academy.com\n\n" +
                        "Best regards,\n" +
                        "Academy Admissions Team",
                getParentName(admissionForm),
                admissionForm.getApplicationNumber(),
                admissionForm.getStudentName(),
                getClassDisplayName(admissionForm.getClassApplied()),
                admissionForm.getSubmittedAt().format(DATE_FORMATTER),
                frontendUrl
        );
    }

    private String buildStatusUpdateEmailContent(AdmissionForm admissionForm,
                                                 ApplicationStatus previousStatus,
                                                 String comments) {
        String statusMessage = getStatusMessage(admissionForm.getApplicationStatus());
        String nextSteps = getNextSteps(admissionForm.getApplicationStatus());

        StringBuilder content = new StringBuilder();
        content.append(String.format("Dear %s,\n\n", getParentName(admissionForm)));
        content.append(String.format("We are writing to inform you about an update to your child's admission application.\n\n"));

        content.append("Application Details:\n");
        content.append(String.format("Application Number: %s\n", admissionForm.getApplicationNumber()));
        content.append(String.format("Student Name: %s\n", admissionForm.getStudentName()));
        content.append(String.format("Class Applied: %s\n", getClassDisplayName(admissionForm.getClassApplied())));

        if (previousStatus != null) {
            content.append(String.format("Previous Status: %s\n", getStatusDisplayName(previousStatus)));
        }
        content.append(String.format("Current Status: %s\n", getStatusDisplayName(admissionForm.getApplicationStatus())));
        content.append(String.format("Updated On: %s\n\n", LocalDateTime.now().format(DATE_FORMATTER)));

        content.append(String.format("Status Update: %s\n\n", statusMessage));

        if (StringUtils.hasText(comments)) {
            content.append(String.format("Additional Comments:\n%s\n\n", comments));
        }

        content.append(String.format("Next Steps:\n%s\n\n", nextSteps));

        content.append(String.format("You can track your application status at: %s/application-status\n\n", frontendUrl));
        content.append("If you have any questions, please contact us at admissions@academy.com\n\n");
        content.append("Best regards,\nAcademy Admissions Team");

        return content.toString();
    }

    private String buildBulkStatusUpdateEmailContent(AdmissionForm admissionForm,
                                                     ApplicationStatus newStatus,
                                                     String comments) {
        return buildStatusUpdateEmailContent(admissionForm, admissionForm.getApplicationStatus(), comments);
    }

    private String buildStatusUpdateSubject(AdmissionForm admissionForm) {
        String statusText = getStatusDisplayName(admissionForm.getApplicationStatus());
        return String.format("Application Update: %s - %s", statusText, admissionForm.getApplicationNumber());
    }

    private String buildBulkStatusUpdateSubject(AdmissionForm admissionForm, ApplicationStatus newStatus) {
        String statusText = getStatusDisplayName(newStatus);
        return String.format("Application Update: %s - %s", statusText, admissionForm.getApplicationNumber());
    }

    // Validation methods
    private void validateAdmissionForm(AdmissionForm admissionForm) {
        if (admissionForm == null) {
            throw new IllegalArgumentException("Admission form cannot be null");
        }
        if (!StringUtils.hasText(admissionForm.getEmailAddress())) {
            throw new IllegalArgumentException("Email address is required");
        }
        if (!StringUtils.hasText(admissionForm.getApplicationNumber())) {
            throw new IllegalArgumentException("Application number is required");
        }
        if (!StringUtils.hasText(admissionForm.getStudentName())) {
            throw new IllegalArgumentException("Student name is required");
        }
        if (!isValidEmail(admissionForm.getEmailAddress())) {
            throw new IllegalArgumentException("Invalid email address format");
        }
    }

    private void validateStatusUpdate(AdmissionForm admissionForm, ApplicationStatus previousStatus) {
        if (admissionForm.getApplicationStatus() == null) {
            throw new IllegalArgumentException("Current application status cannot be null");
        }

        // Optional: Add business logic validation
        if (previousStatus != null && previousStatus == admissionForm.getApplicationStatus()) {
            logger.warn("Status update email triggered but status hasn't changed for application: {}",
                    admissionForm.getApplicationNumber());
        }
    }

    private boolean isValidForEmail(AdmissionForm application) {
        return application != null
                && StringUtils.hasText(application.getEmailAddress())
                && StringUtils.hasText(application.getApplicationNumber())
                && StringUtils.hasText(application.getStudentName())
                && isValidEmail(application.getEmailAddress());
    }

    private boolean isValidEmail(String email) {
        return StringUtils.hasText(email) &&
                email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // Email sending with retry logic
    private void sendEmailWithRetry(SimpleMailMessage message, String emailType, String recipient) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < retryAttempts) {
            try {
                mailSender.send(message);
                logger.debug("{} email sent successfully to: {} on attempt {}",
                        emailType, recipient, attempts + 1);
                return;
            } catch (Exception e) {
                attempts++;
                lastException = e;

                if (attempts < retryAttempts) {
                    logger.warn("Failed to send {} email to: {} on attempt {}. Retrying in {}ms",
                            emailType, recipient, attempts, retryDelay);
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        logger.error("Failed to send {} email to: {} after {} attempts",
                emailType, recipient, retryAttempts, lastException);
        throw new RuntimeException(String.format("Failed to send %s email after %d attempts",
                emailType, retryAttempts), lastException);
    }

    // Helper methods
    private String getParentName(AdmissionForm admissionForm) {
        if (StringUtils.hasText(admissionForm.getFatherName()) && StringUtils.hasText(admissionForm.getMotherName())) {
            return admissionForm.getFatherName() + " / " + admissionForm.getMotherName();
        } else if (StringUtils.hasText(admissionForm.getFatherName())) {
            return admissionForm.getFatherName();
        } else if (StringUtils.hasText(admissionForm.getMotherName())) {
            return admissionForm.getMotherName();
        }
        return "Parent/Guardian";
    }

    private String getClassDisplayName(Object classLevel) {
        if (classLevel == null) return "N/A";

        // Assuming your ClassLevel enum has a getDisplayName() method
        // If not, you can use toString() or implement display name logic
        try {
            return classLevel.getClass().getMethod("getDisplayName").invoke(classLevel).toString();
        } catch (Exception e) {
            return classLevel.toString().replace("_", " ");
        }
    }

    private String getStatusDisplayName(ApplicationStatus status) {
        if (status == null) return "Unknown";

        switch (status) {
            case SUBMITTED: return "Application Submitted";
            case UNDER_REVIEW: return "Under Review";
            case APPROVED: return "Approved";
            case REJECTED: return "Rejected";
            case WAITLISTED: return "Waitlisted";
            default: return status.toString().replace("_", " ");
        }
    }

    private String getStatusMessage(ApplicationStatus status) {
        switch (status) {
            case SUBMITTED:
                return "Your application has been successfully submitted and is awaiting initial review.";
            case UNDER_REVIEW:
                return "Your application is currently being reviewed by our admissions committee.";
            case APPROVED:
                return "Congratulations! Your application has been approved.";
            case REJECTED:
                return "Unfortunately, we are unable to offer admission at this time.";
            case WAITLISTED:
                return "Your application has been placed on our waitlist.";
            default:
                return "Your application status has been updated.";
        }
    }

    private String getNextSteps(ApplicationStatus status) {
        switch (status) {
            case SUBMITTED:
                return "We will review your application and contact you within 5-7 business days.";
            case UNDER_REVIEW:
                return "Please wait while we complete our review process. We will contact you soon with updates.";
            case APPROVED:
                return "Please check your email for admission confirmation and next steps for enrollment.";
            case REJECTED:
                return "Thank you for your interest. You may reapply in the next academic session.";
            case WAITLISTED:
                return "We will contact you if a spot becomes available. Please keep checking your email.";
            default:
                return "Please contact our admissions office if you have any questions.";
        }
    }

    // Result DTO for bulk email operations
    public static class BulkEmailResult {
        private final int totalEmails;
        private final int successCount;
        private final int failedCount;
        private final int skippedCount;

        private BulkEmailResult(Builder builder) {
            this.totalEmails = builder.totalEmails;
            this.successCount = builder.successCount;
            this.failedCount = builder.failedCount;
            this.skippedCount = builder.skippedCount;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public int getTotalEmails() { return totalEmails; }
        public int getSuccessCount() { return successCount; }
        public int getFailedCount() { return failedCount; }
        public int getSkippedCount() { return skippedCount; }

        public static class Builder {
            private int totalEmails;
            private int successCount;
            private int failedCount;
            private int skippedCount;

            public Builder totalEmails(int totalEmails) {
                this.totalEmails = totalEmails;
                return this;
            }

            public Builder successCount(int successCount) {
                this.successCount = successCount;
                return this;
            }

            public Builder failedCount(int failedCount) {
                this.failedCount = failedCount;
                return this;
            }

            public Builder skippedCount(int skippedCount) {
                this.skippedCount = skippedCount;
                return this;
            }

            public BulkEmailResult build() {
                return new BulkEmailResult(this);
            }
        }
    }
}





//package com.app.backend.service;
//
//import com.app.backend.entity.AdmissionForm;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//@Service
//public class EmailService {
//
//    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
//
//    private final JavaMailSender mailSender;
//
//    @Value("${spring.mail.username:noreply@academy.com}")
//    private String fromEmail;
//
//    @Autowired
//    public EmailService(JavaMailSender mailSender) {
//        this.mailSender = mailSender;
//    }
//
//    @Async
//    public void sendConfirmationEmail(AdmissionForm admissionForm) {
//        try {
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setFrom(fromEmail);
//            message.setTo(admissionForm.getEmailAddress());
//            message.setSubject("Admission Application Confirmation - " + admissionForm.getApplicationNumber());
//            message.setText(buildConfirmationEmailContent(admissionForm));
//
//            mailSender.send(message);
//            logger.info("Confirmation email sent successfully to: {}", admissionForm.getEmailAddress());
//        } catch (Exception e) {
//            logger.error("Failed to send confirmation email to: {}", admissionForm.getEmailAddress(), e);
//        }
//    }
//
//    private String buildConfirmationEmailContent(AdmissionForm admissionForm) {
//        return String.format(
//                "Dear %s,\n\n" +
//                        "Thank you for submitting your admission application to our academy.\n\n" +
//                        "Application Details:\n" +
//                        "Application Number: %s\n" +
//                        "Student Name: %s\n" +
//                        "Class Applied: %s\n" +
//                        "Submission Date: %s\n\n" +
//                        "Your application is currently under review. We will contact you within 5-7 business days " +
//                        "regarding the next steps in the admission process.\n\n" +
//                        "Please keep your application number safe for future reference.\n\n" +
//                        "If you have any questions, please contact us at admissions@academy.com\n\n" +
//                        "Best regards,\n" +
//                        "Academy Admissions Team",
//                admissionForm.getFatherName() + " / " + admissionForm.getMotherName(),
//                admissionForm.getApplicationNumber(),
//                admissionForm.getStudentName(),
//                admissionForm.getClassApplied().getDisplayName(),
//                admissionForm.getSubmittedAt().toLocalDate()
//        );
//    }
//}

