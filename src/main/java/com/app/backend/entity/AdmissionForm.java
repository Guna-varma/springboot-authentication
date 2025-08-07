package com.app.backend.entity;

import com.app.backend.entity.ApplicationStatus;
import com.app.backend.entity.ClassLevel;
import com.app.backend.entity.AdmissionGender;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "academy_admission_form")
public class AdmissionForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", unique = true, nullable = false, length = 20)
    private String applicationNumber;

    @NotBlank(message = "Student name is required")
    @Size(min = 2, max = 100, message = "Student name must be between 2 and 100 characters")
    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private AdmissionGender gender;

    @NotNull(message = "Class applied is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "class_applied", nullable = false, length = 50)
    private ClassLevel classApplied;

    @Size(max = 200, message = "Previous school name cannot exceed 200 characters")
    @Column(name = "previous_school", length = 200)
    private String previousSchool;

    @NotBlank(message = "Father's name is required")
    @Size(min = 2, max = 100, message = "Father's name must be between 2 and 100 characters")
    @Column(name = "father_name", nullable = false, length = 100)
    private String fatherName;

    @NotBlank(message = "Mother's name is required")
    @Size(min = 2, max = 100, message = "Mother's name must be between 2 and 100 characters")
    @Column(name = "mother_name", nullable = false, length = 100)
    private String motherName;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be 10 digits")
    @Column(name = "contact_number", nullable = false, length = 15)
    private String contactNumber;

    @NotBlank(message = "Email address is required")
    @Email(message = "Email address must be valid")
    @Size(max = 100, message = "Email address cannot exceed 100 characters")
    @Column(name = "email_address", nullable = false, length = 100)
    private String emailAddress;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @NotNull(message = "Declaration must be accepted")
    @Column(name = "declaration_accepted", nullable = false)
    private Boolean declarationAccepted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false, length = 20)
    private ApplicationStatus applicationStatus = ApplicationStatus.SUBMITTED;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy = "SYSTEM";

    @Column(name = "updated_by", length = 50)
    private String updatedBy = "SYSTEM";

    // Constructors
    public AdmissionForm() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public AdmissionGender getGender() { return gender; }
    public void setGender(AdmissionGender gender) { this.gender = gender; }

    public ClassLevel getClassApplied() { return classApplied; }
    public void setClassApplied(ClassLevel classApplied) { this.classApplied = classApplied; }

    public String getPreviousSchool() { return previousSchool; }
    public void setPreviousSchool(String previousSchool) { this.previousSchool = previousSchool; }

    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Boolean getDeclarationAccepted() { return declarationAccepted; }
    public void setDeclarationAccepted(Boolean declarationAccepted) { this.declarationAccepted = declarationAccepted; }

    public ApplicationStatus getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(ApplicationStatus applicationStatus) { this.applicationStatus = applicationStatus; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}

