package com.faculdade.authsystem.dto.response;

import com.faculdade.authsystem.domain.User;

import java.util.UUID;

/**
 * Resultado de uma autenticação bem-sucedida: o token de acesso e a identificação do usuário autenticado.
 */
public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserIdentity usuario) {

    public static LoginResponse bearer(String accessToken, long expiresIn, User user) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, UserIdentity.from(user));
    }

    public record UserIdentity(UUID id, String nome, String email) {

        public static UserIdentity from(User user) {
            return new UserIdentity(user.getId(), user.getName(), user.getEmail());
        }
    }
}
