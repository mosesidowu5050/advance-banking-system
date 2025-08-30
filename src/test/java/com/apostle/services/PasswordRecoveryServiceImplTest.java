package com.apostle.services;

import com.apostle.data.model.PasswordResetToken;
import com.apostle.data.repositories.PasswordResetTokenRepository;
import com.apostle.data.repositories.UserRepository;
import com.apostle.dtos.requests.ForgotPasswordRequest;
import com.apostle.dtos.requests.RegisterRequest;
import com.apostle.dtos.requests.ResetPasswordRequest;
import com.apostle.dtos.responses.ResetPasswordResponse;
import com.apostle.exceptions.InvalidResetTokenException;
import com.apostle.services.authService.AuthenticationServiceImpl;
import com.apostle.services.passwordService.PasswordRecoveryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PasswordRecoveryServiceImplTest {

    @Autowired
    private PasswordRecoveryServiceImpl passwordRecoveryService;

    @Autowired
    private AuthenticationServiceImpl authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    private RegisterRequest createRegisterRequest(String email, String username, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
    @BeforeEach
    public void setUp() {
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

    }

    @Test
    public void testSendResetToken_createsTokenSuccessfully(){
        RegisterRequest registerRequest = createRegisterRequest("adahjohn419@gmail.com", "johndoe", "Password@2024");
        authenticationService.register(registerRequest);

        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail(registerRequest.getEmail());

        passwordRecoveryService.sendResetToken(forgotPasswordRequest);

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAll();

        assertEquals(1, tokens.size());
        assertEquals(forgotPasswordRequest.getEmail(), tokens.get(0).getUser().getEmail());
    }

    @Test
    public void testResetPassword_changesPasswordSuccessfully(){
        RegisterRequest registerRequest = createRegisterRequest("myemail@gmail.com", "myuser", "MyPassword@2025");
        authenticationService.register(registerRequest);

        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail(registerRequest.getEmail());
        passwordRecoveryService.sendResetToken(forgotPasswordRequest);


        PasswordResetToken token = passwordResetTokenRepository.findAll().get(0);
        assertNotNull(token);
        String tokenString = token.getToken();

        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest();
        resetPasswordRequest.setToken(tokenString);
        resetPasswordRequest.setNewPassword("MyNewPassword@2002");

        ResetPasswordResponse resetPasswordResponse = passwordRecoveryService.resetPassword(resetPasswordRequest);

        assertEquals("Password reset successful", resetPasswordResponse.getMessage());
    }

    @Test
    public void testResetPassword_failsForInvalidToken(){
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest();
        resetPasswordRequest.setToken("invalidToken");
        resetPasswordRequest.setNewPassword("NotChangedPassword@2002");

        Exception exception = assertThrows(InvalidResetTokenException.class, () -> passwordRecoveryService.resetPassword(resetPasswordRequest));
        assertEquals("Invalid token", exception.getMessage());
    }

    @Test
    public void testResetPassword_failsForUsedToken(){
        RegisterRequest registerRequest = createRegisterRequest("myemailjohn@gmail.com", "myuser", "MyPassword@2024");
        authenticationService.register(registerRequest);

        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail(registerRequest.getEmail());
        passwordRecoveryService.sendResetToken(forgotPasswordRequest);

        PasswordResetToken token = passwordResetTokenRepository.findAll().get(0);
        token.setUsed(true);
        passwordResetTokenRepository.save(token);


        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest();
        resetPasswordRequest.setToken(token.getToken());
        resetPasswordRequest.setNewPassword("MyPassword@2024");

        Exception exception = assertThrows(InvalidResetTokenException.class, () -> passwordRecoveryService.resetPassword(resetPasswordRequest));
        assertEquals("Token already used", exception.getMessage());
    }

    @Test
    public void testResetPassword_failsForExpiredToken(){
        RegisterRequest registerRequest = createRegisterRequest("myemailu@gmail.com", "myuser", "MyPassword@2024");
        authenticationService.register(registerRequest);

        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail(registerRequest.getEmail());
        passwordRecoveryService.sendResetToken(forgotPasswordRequest);

        PasswordResetToken token = passwordResetTokenRepository.findAll().get(0);
        token.setExpiration(LocalDateTime.now().minusMinutes(1));
        passwordResetTokenRepository.save(token);

        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest();
        resetPasswordRequest.setToken(token.getToken());
        resetPasswordRequest.setNewPassword("ExpiredPassword@2024");

        Exception exception = assertThrows(InvalidResetTokenException.class, () -> passwordRecoveryService.resetPassword(resetPasswordRequest));
        assertEquals("Token expired", exception.getMessage());
    }
}