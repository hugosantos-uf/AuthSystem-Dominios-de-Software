package com.faculdade.authsystem.service;

import com.faculdade.authsystem.dto.request.LoginRequest;
import com.faculdade.authsystem.dto.request.RegisterRequest;
import com.faculdade.authsystem.dto.response.LoginResponse;
import com.faculdade.authsystem.dto.response.RegisterResponse;

import java.util.UUID;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    /** Identificação do dono de um token já validado. */
    LoginResponse.UserIdentity currentUser(UUID userId);
}
