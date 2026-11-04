package com.faculdade.authsystem.controller;

import com.faculdade.authsystem.config.SecurityConfig;
import com.faculdade.authsystem.domain.User;
import com.faculdade.authsystem.dto.response.LoginResponse;
import com.faculdade.authsystem.dto.response.RegisterResponse;
import com.faculdade.authsystem.exception.EmailAlreadyExistsException;
import com.faculdade.authsystem.exception.GlobalExceptionHandler;
import com.faculdade.authsystem.exception.InvalidCredentialsException;
import com.faculdade.authsystem.exception.UnauthenticatedException;
import com.faculdade.authsystem.security.RestAccessDeniedHandler;
import com.faculdade.authsystem.security.RestAuthenticationEntryPoint;
import com.faculdade.authsystem.service.AuthService;
import com.faculdade.authsystem.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, properties = {
        "jwt.secret=" + AuthControllerTest.SECRET
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtService.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AuthControllerTest {

    static final String SECRET = "0123456789abcdef0123456789abcdef-test-secret";
    private static final UUID USER_ID = UUID.fromString("3f0c5b0e-6a64-4b1e-9d52-2f1f0b6a9c11");
    private static final LoginResponse.UserIdentity MARIA =
            new LoginResponse.UserIdentity(USER_ID, "Maria Silva", "maria@exemplo.com");

    private static final String VALID_REGISTER = """
            {"nome":"Maria Silva","email":"maria@exemplo.com","senha":"Senha@123","confirmarSenha":"Senha@123"}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    private String validToken() {
        User user = new User("Maria Silva", "maria@exemplo.com", "$2a$hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return jwtService.generateToken(user);
    }

    private static String tokenSignedWith(String secret, Instant expiration) {
        return Jwts.builder()
                .subject(USER_ID.toString())
                .issuedAt(Date.from(expiration.minusSeconds(3600)))
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void registerReturns201() throws Exception {
        when(authService.register(any())).thenReturn(
                new RegisterResponse(UUID.randomUUID(), "Maria Silva", "maria@exemplo.com", Instant.now()));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerWithDifferentPasswordsReturnsValidationErrorOnConfirmarSenha() throws Exception {
        String body = """
                {"nome":"Maria","email":"maria@exemplo.com","senha":"Senha@123","confirmarSenha":"Outra@123"}""";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("confirmarSenha"))
                .andExpect(jsonPath("$.errors[0].message").value("As senhas não coincidem."));
    }

    @Test
    void registerWithWeakPasswordAndInvalidEmailListsEveryField() throws Exception {
        String body = """
                {"nome":"M","email":"nao-e-email","senha":"fraca","confirmarSenha":"fraca"}""";

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void registerWithMissingFieldsReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void malformedJsonReturnsMalformedRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void missingBodyReturnsMalformedRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void unsupportedMediaTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void loginReturns200WithTokenAndUserIdentity() throws Exception {
        UUID id = UUID.randomUUID();
        when(authService.login(any())).thenReturn(new LoginResponse("token", "Bearer", 3600,
                new LoginResponse.UserIdentity(id, "Maria Silva", "maria@exemplo.com")));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"maria@exemplo.com","senha":"Senha@123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.usuario.id").value(id.toString()))
                .andExpect(jsonPath("$.usuario.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.usuario.email").value("maria@exemplo.com"));
    }

    @Test
    void loginWithInvalidCredentialsReturns401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"maria@exemplo.com","senha":"errada"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    }

    @Test
    void loginWithBlankFieldsReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","senha":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void protectedRouteReturnsJson401() throws Exception {
        mockMvc.perform(get("/api/qualquer-coisa"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void meWithValidTokenReturnsTheTokenOwner() throws Exception {
        when(authService.currentUser(USER_ID)).thenReturn(MARIA);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"));
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/auth/me"));

        verify(authService, never()).currentUser(any());
    }

    @Test
    void meWithTamperedTokenReturns401() throws Exception {
        // Troca o "sub" do payload pelo id de outro usuário e mantém a assinatura original.
        String[] parts = validToken().split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String payload = new String(decoder.decode(parts[1]), StandardCharsets.UTF_8)
                .replace(USER_ID.toString(), UUID.randomUUID().toString());
        String tampered = parts[0] + "." + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(authService, never()).currentUser(any());
    }

    @Test
    void meWithTokenSignedByAnotherSecretReturns401() throws Exception {
        String forged = tokenSignedWith("outro-segredo-qualquer-com-32-bytes-ou-mais", Instant.now().plusSeconds(3600));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void meWithExpiredTokenReturns401() throws Exception {
        String expired = tokenSignedWith(SECRET, Instant.now().minusSeconds(60));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void meWithNonBearerAuthorizationReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Basic bWFyaWE6c2VuaGE="))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void meWithValidTokenOfRemovedUserReturns401() throws Exception {
        when(authService.currentUser(USER_ID)).thenThrow(new UnauthenticatedException());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidTokenDoesNotBlockPublicLogin() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("token", "Bearer", 3600, MARIA));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer token-invalido")
                        .content("""
                                {"email":"maria@exemplo.com","senha":"Senha@123"}"""))
                .andExpect(status().isOk());
    }
}
