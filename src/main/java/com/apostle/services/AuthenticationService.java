package com.apostle.services;

import com.apostle.dtos.requests.LoginRequest;
import com.apostle.dtos.requests.RegisterRequest;
import com.apostle.dtos.responses.LoginResponse;
import com.apostle.dtos.responses.RegisterResponses;
import org.springframework.stereotype.Service;

public interface AuthenticationService {
    RegisterResponses register(RegisterRequest registerRequest);
    LoginResponse login(LoginRequest loginRequest);
}
