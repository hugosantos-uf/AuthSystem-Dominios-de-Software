package com.faculdade.authsystem.exception;

public class EmailAlreadyExistsException extends ApiException {

    public EmailAlreadyExistsException() {
        super(ErrorCode.EMAIL_ALREADY_EXISTS, "E-mail já cadastrado.");
    }
}
