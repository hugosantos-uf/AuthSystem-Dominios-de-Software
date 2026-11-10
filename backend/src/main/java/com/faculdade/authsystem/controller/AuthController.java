package com.faculdade.authsystem.controller;

import com.faculdade.authsystem.config.OpenApiConfig;
import com.faculdade.authsystem.dto.request.LoginRequest;
import com.faculdade.authsystem.dto.request.RegisterRequest;
import com.faculdade.authsystem.dto.response.ErrorResponse;
import com.faculdade.authsystem.dto.response.LoginResponse;
import com.faculdade.authsystem.dto.response.RegisterResponse;
import com.faculdade.authsystem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro, login e validação do token")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastra um novo usuário")
    @ApiResponse(responseCode = "201", description = "Usuário criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON malformado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuário e devolve o token de acesso")
    @ApiResponse(responseCode = "200", description = "Autenticado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * O principal é o id do usuário ({@code sub} do JWT), definido pelo {@code JwtAuthenticationFilter}.
     * Sem token válido a requisição nem chega aqui: o Spring Security responde 401.
     */
    @GetMapping("/me")
    @Operation(summary = "Valida o token de acesso e devolve a identificação do usuário",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
    @ApiResponse(responseCode = "200", description = "Token válido")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<LoginResponse.UserIdentity> me(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(authService.currentUser(userId));
    }
}
