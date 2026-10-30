package com.faculdade.authsystem.validation;

import com.faculdade.authsystem.dto.request.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request == null || request.senha() == null || request.confirmarSenha() == null) {
            // Ausência é tratada por @NotBlank nos próprios campos.
            return true;
        }
        if (request.senha().equals(request.confirmarSenha())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("confirmarSenha")
                .addConstraintViolation();
        return false;
    }
}
