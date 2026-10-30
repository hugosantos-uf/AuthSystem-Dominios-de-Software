package com.faculdade.authsystem.exception;

import com.faculdade.authsystem.dto.response.ErrorResponse;
import com.faculdade.authsystem.dto.response.ErrorResponse.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ponto único de conversão de exceções em {@link ErrorResponse}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String UNIQUE_EMAIL_CONSTRAINT = "uk_users_email";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldErrorDetail(e.getField(), e.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, "Dados inválidos. Corrija os campos indicados.",
                request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformed(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        return build(ErrorCode.MALFORMED_REQUEST, "Corpo da requisição ausente ou JSON inválido.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                         HttpServletRequest request) {
        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteúdo não suportado. Use application/json.",
                request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                HttpServletRequest request) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, "Método HTTP não suportado para este recurso.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return build(ErrorCode.NOT_FOUND, "Recurso não encontrado.", request);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return build(ex.getCode(), ex.getMessage(), request);
    }

    /**
     * A checagem {@code existsByEmail} não cobre concorrência: a constraint UNIQUE do banco é a garantia real.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        if (isUniqueEmailViolation(ex)) {
            return build(ErrorCode.EMAIL_ALREADY_EXISTS, "E-mail já cadastrado.", request);
        }
        return handleUnexpected(ex, request);
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(AuthenticationException ex,
                                                              HttpServletRequest request) {
        return build(ErrorCode.INVALID_CREDENTIALS, "E-mail ou senha inválidos.", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex,
                                                              HttpServletRequest request) {
        return build(ErrorCode.UNAUTHORIZED, "Autenticação necessária.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.FORBIDDEN, "Acesso negado.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String traceId = newTraceId();
        log.error("Erro inesperado [traceId={}] em {}", traceId, request.getRequestURI(), ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus()).body(new ErrorResponse(
                Instant.now(),
                ErrorCode.INTERNAL_ERROR.getStatus().value(),
                ErrorCode.INTERNAL_ERROR.name(),
                "Erro interno. Informe o traceId ao suporte.",
                request.getRequestURI(),
                traceId,
                null));
    }

    private boolean isUniqueEmailViolation(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && message.contains(UNIQUE_EMAIL_CONSTRAINT)) {
                return true;
            }
        }
        return false;
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message, HttpServletRequest request) {
        return build(code, message, request, null);
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message, HttpServletRequest request,
                                                List<FieldErrorDetail> errors) {
        String traceId = newTraceId();
        log.warn("{} [traceId={}] em {}: {}", code, traceId, request.getRequestURI(), message);
        return ResponseEntity.status(code.getStatus()).body(new ErrorResponse(
                Instant.now(),
                code.getStatus().value(),
                code.name(),
                message,
                request.getRequestURI(),
                traceId,
                errors));
    }

    private String newTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
