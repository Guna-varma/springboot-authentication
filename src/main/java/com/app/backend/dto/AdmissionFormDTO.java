package com.app.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class AdmissionFormDTO {

    @NotBlank(message = "Student name is required")
    @Size(min = 2, max = 100, message = "Student name must be between 2 and 100 characters")
    private String studentName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Class applied is required")
    private String classApplied;

    @Size(max = 200, message = "Previous school name cannot exceed 200 characters")
    private String previousSchool;

    @NotBlank(message = "Father's name is required")
    @Size(min = 2, max = 100, message = "Father's name must be between 2 and 100 characters")
    private String fatherName;

    @NotBlank(message = "Mother's name is required")
    @Size(min = 2, max = 100, message = "Mother's name must be between 2 and 100 characters")
    private String motherName;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be 10 digits")
    private String contact;

    @NotBlank(message = "Email address is required")
    @Email(message = "Email address must be valid")
    @Size(max = 100, message = "Email address cannot exceed 100 characters")
    private String email;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    @NotNull(message = "Declaration must be accepted")
    @AssertTrue(message = "Declaration must be accepted to submit the form")
    private Boolean declarationAccepted;

    // Constructors
    public AdmissionFormDTO() {}

    // Getters and Setters
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getClassApplied() { return classApplied; }
    public void setClassApplied(String classApplied) { this.classApplied = classApplied; }

    public String getPreviousSchool() { return previousSchool; }
    public void setPreviousSchool(String previousSchool) { this.previousSchool = previousSchool; }

    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Boolean getDeclarationAccepted() { return declarationAccepted; }
    public void setDeclarationAccepted(Boolean declarationAccepted) { this.declarationAccepted = declarationAccepted; }
}
