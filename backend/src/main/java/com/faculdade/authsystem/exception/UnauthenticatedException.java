package com.faculdade.authsystem.exception;

/**
 * O token é válido, mas não corresponde mais a um usuário existente.
 * Responde como qualquer outra falha de autenticação em rota protegida (401 UNAUTHORIZED).
 */
public class UnauthenticatedException extends ApiException {

    public UnauthenticatedException() {
        super(ErrorCode.UNAUTHORIZED, "Autenticação necessária.");
    }
}
