package com.app.backend.validation;

import com.app.backend.dto.SignUpRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, SignUpRequest> {

    @Override
    public boolean isValid(SignUpRequest request, ConstraintValidatorContext context) {
        if (request.getPassword() == null || request.getConfirmPassword() == null) {
            return false;
        }

        boolean isValid = request.getPassword().equals(request.getConfirmPassword());
        if (!isValid) {
            context.disableDefaultConstraintViolation(); // 👈 Turn off default
            context.buildConstraintViolationWithTemplate("Passwords do not match")
                    .addPropertyNode("confirmPassword") // 👈 Attach error to this field
                    .addConstraintViolation();
        }

        return isValid;
    }

}