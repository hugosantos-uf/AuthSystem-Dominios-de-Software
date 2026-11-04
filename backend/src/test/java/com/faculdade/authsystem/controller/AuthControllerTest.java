package com.faculdade.authsystem.controller;

import com.faculdade.authsystem.config.SecurityConfig;
import com.faculdade.authsystem.dto.response.LoginResponse;
import com.faculdade.authsystem.dto.response.RegisterResponse;
import com.faculdade.authsystem.exception.EmailAlreadyExistsException;
import com.faculdade.authsystem.exception.GlobalExceptionHandler;
import com.faculdade.authsystem.exception.InvalidCredentialsException;
import com.faculdade.authsystem.security.RestAccessDeniedHandler;
import com.faculdade.authsystem.security.RestAuthenticationEntryPoint;
import com.faculdade.authsystem.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef-test-secret"
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AuthControllerTest {

    private static final String VALID_REGISTER = """
            {"nome":"Maria Silva","email":"maria@exemplo.com","senha":"Senha@123","confirmarSenha":"Senha@123"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

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
}
