package com.faculdade.authsystem.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void acceptsStrongPassword() {
        assertTrue(validator.isValid("Senha@123", null));
    }

    @Test
    void ignoresNullBecauseNotBlankHandlesIt() {
        assertTrue(validator.isValid(null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Sa@1",         // curta
            "senha@123",    // sem maiúscula
            "SENHA@123",    // sem minúscula
            "Senha@abc",    // sem número
            "Senha1234",    // sem símbolo
            "Senha 1234"    // espaço não conta como símbolo
    })
    void rejectsWeakPasswords(String password) {
        assertFalse(validator.isValid(password, null));
    }

    @Test
    void rejectsPasswordLongerThan72() {
        assertFalse(validator.isValid("Aa1@" + "x".repeat(69), null));
    }

    @Test
    void acceptsPasswordWithExactly72() {
        assertTrue(validator.isValid("Aa1@" + "x".repeat(68), null));
    }
}
