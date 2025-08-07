package com.app.backend.service;

import com.app.backend.dto.AdmissionFormDTO;
import com.app.backend.entity.AdmissionGender;
import com.app.backend.entity.ClassLevel;
import com.app.backend.entity.Gender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

@Service
public class ValidationService {

    public void validateAdmissionForm(AdmissionFormDTO dto) {
        validateAge(dto.getDob(), dto.getClassApplied());
        validateGender(dto.getGender());
        validateClassLevel(dto.getClassApplied());
        validateContactNumber(dto.getContact());
        validateEmail(dto.getEmail());
    }

    private void validateAge(LocalDate dob, String classApplied) {
        int age = Period.between(dob, LocalDate.now()).getYears();

        // Define age ranges for different classes
        switch (classApplied.toUpperCase()) {
            case "NURSERY":
                if (age < 3 || age > 4) {
                    throw new IllegalArgumentException("Age should be between 3-4 years for Nursery");
                }
                break;
            case "LKG":
                if (age < 4 || age > 5) {
                    throw new IllegalArgumentException("Age should be between 4-5 years for LKG");
                }
                break;
            case "UKG":
                if (age < 5 || age > 6) {
                    throw new IllegalArgumentException("Age should be between 5-6 years for UKG");
                }
                break;
            default:
                // For other classes, allow more flexibility
                if (age < 3 || age > 18) {
                    throw new IllegalArgumentException("Invalid age for the selected class");
                }
        }
    }

    private void validateGender(String gender) {
        try {
            AdmissionGender.fromDisplayName(gender);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid gender selection");
        }
    }

    private void validateClassLevel(String classLevel) {
        try {
            ClassLevel.fromDisplayName(classLevel);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid class selection");
        }
    }

    private void validateContactNumber(String contact) {
        if (!contact.matches("^[6-9][0-9]{9}$")) {
            throw new IllegalArgumentException("Invalid Indian mobile number format");
        }
    }

    private void validateEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!email.matches(emailRegex)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}
