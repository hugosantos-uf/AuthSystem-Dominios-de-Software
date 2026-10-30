package com.faculdade.authsystem.service;

import com.faculdade.authsystem.domain.User;
import com.faculdade.authsystem.dto.request.LoginRequest;
import com.faculdade.authsystem.dto.request.RegisterRequest;
import com.faculdade.authsystem.dto.response.LoginResponse;
import com.faculdade.authsystem.dto.response.RegisterResponse;
import com.faculdade.authsystem.exception.EmailAlreadyExistsException;
import com.faculdade.authsystem.exception.InvalidCredentialsException;
import com.faculdade.authsystem.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        User user = new User(request.nome().trim(), email, passwordEncoder.encode(request.senha()));
        // saveAndFlush: uma violação da constraint UNIQUE (concorrência) estoura aqui e vira 409 no handler.
        return RegisterResponse.from(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.senha()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        return LoginResponse.bearer(jwtService.generateToken(user), jwtService.getExpirationSeconds());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
