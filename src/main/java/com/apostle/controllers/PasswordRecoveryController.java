package com.apostle.controllers;


import com.apostle.dtos.requests.ForgotPasswordRequest;
import com.apostle.dtos.requests.ResetPasswordRequest;
import com.apostle.services.PasswordRecoveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    public PasswordRecoveryController(PasswordRecoveryService passwordRecoveryService) {
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest){
        try {
            return ResponseEntity.ok(passwordRecoveryService.resetPassword(resetPasswordRequest));
        }catch (Exception exception){
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> sendResetToken(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest){
        try {
            passwordRecoveryService.sendResetToken(forgotPasswordRequest);
            return ResponseEntity.ok("Password reset email sent");
        }catch (Exception exception){
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }
}
