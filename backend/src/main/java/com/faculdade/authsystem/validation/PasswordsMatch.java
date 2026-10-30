package com.faculdade.authsystem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida, no nível da classe, que {@code senha} e {@code confirmarSenha} são iguais (RN04).
 * O erro é associado ao campo {@code confirmarSenha}.
 */
@Documented
@Constraint(validatedBy = PasswordsMatchValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordsMatch {

    String message() default "As senhas não coincidem.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
