package com.apostle.services;

import com.apostle.dtos.requests.ForgotPasswordRequest;
import com.apostle.dtos.requests.ResetPasswordRequest;
import com.apostle.dtos.responses.ResetPasswordResponse;
import org.springframework.stereotype.Service;

@Service
public interface PasswordRecoveryService {
    void sendResetToken(ForgotPasswordRequest forgotPasswordRequest);
    ResetPasswordResponse resetPassword(ResetPasswordRequest resetPasswordRequest);
}
