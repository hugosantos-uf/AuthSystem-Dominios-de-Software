package com.faculdade.authsystem.service;

import com.faculdade.authsystem.config.JwtProperties;
import com.faculdade.authsystem.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef-test-secret";
    private static final UUID USER_ID = UUID.fromString("3f0c5b0e-6a64-4b1e-9d52-2f1f0b6a9c11");

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, 3600));

    private static User user() {
        User user = new User("Maria Silva", "maria@exemplo.com", "$2a$hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private static String token(String secret, Instant expiration) {
        return Jwts.builder()
                .subject(USER_ID.toString())
                .issuedAt(Date.from(expiration.minusSeconds(3600)))
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void tokenGeneratedByTheServiceIsValidAndCarriesTheUserId() {
        assertEquals(Optional.of(USER_ID), jwtService.validate(jwtService.generateToken(user())));
    }

    @Test
    void tokenWithChangedPayloadIsRejected() {
        String[] parts = jwtService.generateToken(user()).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace(USER_ID.toString(), UUID.randomUUID().toString());
        String tampered = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        assertEquals(Optional.empty(), jwtService.validate(tampered));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String forged = token("outro-segredo-qualquer-com-32-bytes-ou-mais", Instant.now().plusSeconds(3600));

        assertEquals(Optional.empty(), jwtService.validate(forged));
    }

    @Test
    void expiredTokenIsRejected() {
        assertEquals(Optional.empty(), jwtService.validate(token(SECRET, Instant.now().minusSeconds(60))));
    }

    @Test
    void unsignedTokenIsRejected() {
        // Ataque "alg: none": token sem assinatura nunca é aceito.
        String unsigned = Jwts.builder()
                .subject(USER_ID.toString())
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .compact();

        assertEquals(Optional.empty(), jwtService.validate(unsigned));
    }

    @Test
    void malformedOrEmptyTokenIsRejected() {
        assertEquals(Optional.empty(), jwtService.validate("isto-nao-e-um-jwt"));
        assertEquals(Optional.empty(), jwtService.validate(""));
    }

    @Test
    void secretShorterThan32BytesIsRefusedAtStartup() {
        assertThrows(IllegalStateException.class, () -> new JwtService(new JwtProperties("curto", 3600)));
    }
}
