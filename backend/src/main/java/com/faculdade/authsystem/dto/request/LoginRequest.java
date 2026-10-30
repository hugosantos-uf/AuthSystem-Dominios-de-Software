package com.faculdade.authsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        String senha
) {

    /** Evita que a senha apareça em logs (RNF01). */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", senha=***]";
    }
}
