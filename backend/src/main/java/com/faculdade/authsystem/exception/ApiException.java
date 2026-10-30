package com.faculdade.authsystem.exception;

import org.springframework.http.HttpStatus;

/**
 * Base das exceções de negócio. Carrega o {@link ErrorCode}, de onde vem o status HTTP.
 */
public abstract class ApiException extends RuntimeException {

    private final ErrorCode code;

    protected ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return code.getStatus();
    }
}
