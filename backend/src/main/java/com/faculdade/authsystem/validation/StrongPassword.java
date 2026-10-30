package com.faculdade.authsystem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RN03: 8 a 72 caracteres, com ao menos 1 maiúscula, 1 minúscula, 1 número e 1 caractere especial.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "A senha deve ter ao menos 8 caracteres, com maiúscula, minúscula, número e símbolo.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
