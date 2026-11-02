package com.faculdade.authsystem.service;

import com.faculdade.authsystem.domain.User;
import com.faculdade.authsystem.dto.request.LoginRequest;
import com.faculdade.authsystem.dto.request.RegisterRequest;
import com.faculdade.authsystem.dto.response.LoginResponse;
import com.faculdade.authsystem.dto.response.RegisterResponse;
import com.faculdade.authsystem.exception.EmailAlreadyExistsException;
import com.faculdade.authsystem.exception.InvalidCredentialsException;
import com.faculdade.authsystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl service;

    private static RegisterRequest registerRequest(String email) {
        return new RegisterRequest("  Maria Silva ", email, "Senha@123", "Senha@123");
    }

    @Test
    void registerNormalizesEmailAndStoresOnlyTheHash() {
        when(userRepository.existsByEmail("maria@exemplo.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha@123")).thenReturn("$2a$hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterResponse response = service.register(registerRequest("  Maria@Exemplo.COM "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertEquals("maria@exemplo.com", saved.getValue().getEmail());
        assertEquals("Maria Silva", saved.getValue().getName());
        assertEquals("$2a$hash", saved.getValue().getPasswordHash());
        assertNotEquals("Senha@123", saved.getValue().getPasswordHash());
        assertEquals("maria@exemplo.com", response.email());
    }

    @Test
    void registerRejectsExistingEmail() {
        when(userRepository.existsByEmail("maria@exemplo.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> service.register(registerRequest("MARIA@exemplo.com")));

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void loginReturnsBearerTokenAndUserIdentity() {
        User user = new User("Maria", "maria@exemplo.com", "$2a$hash");
        when(userRepository.findByEmail("maria@exemplo.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = service.login(new LoginRequest(" Maria@Exemplo.com", "Senha@123"));

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("maria@exemplo.com", "Senha@123"));
        assertEquals("token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertEquals("Maria", response.usuario().nome());
        assertEquals("maria@exemplo.com", response.usuario().email());
    }

    @Test
    void loginMapsAnyAuthenticationFailureToInvalidCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("maria@exemplo.com", "errada")));

        verify(jwtService, never()).generateToken(any());
    }
}
