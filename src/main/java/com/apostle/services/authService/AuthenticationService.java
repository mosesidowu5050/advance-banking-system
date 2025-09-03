package com.apostle.services.authService;

import com.apostle.dtos.requests.LoginRequest;
import com.apostle.dtos.requests.RegisterRequest;
import com.apostle.dtos.responses.LoginResponse;
import com.apostle.dtos.responses.RegisterResponses;

public interface AuthenticationService {
    RegisterResponses register(RegisterRequest registerRequest);
    LoginResponse login(LoginRequest loginRequest);

    void logout(String accessToken);
}
