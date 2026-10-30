package com.faculdade.authsystem.exception;

/**
 * Mensagem única para e-mail inexistente ou senha incorreta (RN05), evitando enumeração de usuários.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS, "E-mail ou senha inválidos.");
    }
}
