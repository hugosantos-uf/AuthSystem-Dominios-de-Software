package com.faculdade.authsystem.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Formato padrão de todas as respostas de erro da API.
 * {@code errors} só é preenchido em {@code VALIDATION_ERROR}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId,
        List<FieldErrorDetail> errors
) {

    public record FieldErrorDetail(String field, String message) {
    }
}
