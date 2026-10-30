package com.faculdade.authsystem.dto.response;

import com.faculdade.authsystem.domain.User;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(UUID id, String nome, String email, Instant criadoEm) {

    public static RegisterResponse from(User user) {
        return new RegisterResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
