package com.faculdade.authsystem.dto.request;

import com.faculdade.authsystem.validation.PasswordsMatch;
import com.faculdade.authsystem.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@PasswordsMatch
public record RegisterRequest(

        @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 2, max = 100, message = "O nome deve ter de 2 a 100 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @StrongPassword
        String senha,

        @NotBlank(message = "A confirmação de senha é obrigatória.")
        String confirmarSenha
) {

    /** Evita que as senhas apareçam em logs (RNF01). */
    @Override
    public String toString() {
        return "RegisterRequest[nome=" + nome + ", email=" + email + ", senha=***, confirmarSenha=***]";
    }
}
