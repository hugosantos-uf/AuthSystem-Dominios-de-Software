package com.faculdade.authsystem.service;

import com.faculdade.authsystem.config.JwtProperties;
import com.faculdade.authsystem.domain.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Gera e valida o token de acesso (JWT HS256) com {@code sub}, {@code iat} e {@code exp}.
 * A validação não consulta nenhum estado no servidor: confere a assinatura com o segredo e o {@code exp}.
 */
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(JwtProperties properties) {
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET deve ter no mínimo 32 bytes (256 bits).");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expirationSeconds = properties.expirationSeconds();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Devolve o id do usuário ({@code sub}) se o token tiver assinatura válida e não estiver expirado.
     * Token adulterado, assinado com outro segredo, sem assinatura, expirado ou malformado → vazio.
     */
    public Optional<UUID> validate(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return subject == null ? Optional.empty() : Optional.of(UUID.fromString(subject));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
