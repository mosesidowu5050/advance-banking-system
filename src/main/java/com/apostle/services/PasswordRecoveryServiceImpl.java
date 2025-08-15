package com.apostle.services;

import com.apostle.data.model.PasswordResetToken;
import com.apostle.data.model.User;
import com.apostle.data.repositories.PasswordResetTokenRepository;
import com.apostle.data.repositories.UserRepository;
import com.apostle.dtos.requests.ForgotPasswordRequest;
import com.apostle.dtos.requests.ResetPasswordRequest;
import com.apostle.dtos.responses.ResetPasswordResponse;
import com.apostle.exceptions.InvalidResetTokenException;
import com.apostle.exceptions.UserNotFoundException;
import jakarta.mail.MessagingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService{

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final Validator validator;
    private final EmailServiceImpl emailService;
//    public PasswordRecoveryServiceImpl(UserRepository userRepository,
//                                       PasswordResetTokenRepository passwordResetTokenRepository,
//                                       BCryptPasswordEncoder bCryptPasswordEncoder,
//                                       Validator validator, EmailServiceImpl emailService) {
//        this.userRepository = userRepository;
//        this.passwordResetTokenRepository = passwordResetTokenRepository;
//        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
//        this.validator = validator;
//
//        this.emailService = emailService;
//    }

    @Override
    public void sendResetToken(ForgotPasswordRequest forgotPasswordRequest) {
        String email = forgotPasswordRequest.getEmail().toLowerCase();
        boolean emailExists = userRepository.findUserByEmail(email).isPresent();
        if (!emailExists){
            throw new UserNotFoundException("User not found");
        }
//        String token = UUID.randomUUID().toString();
        String token = UUID.randomUUID().toString().substring(0, 6);
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(token);
        passwordResetToken.setUser(userRepository.findUserByEmail(email).get());
        passwordResetToken.setExpiration(LocalDateTime.now().plusMinutes(10));
        passwordResetTokenRepository.save(passwordResetToken);


        try {
            emailService.sendPasswordResetEmail(email, token);
        }catch (MessagingException ex){
            throw new RuntimeException("Error sending email", ex);
        }


    }

    @Override
    public ResetPasswordResponse resetPassword(ResetPasswordRequest resetPasswordRequest) {
        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(resetPasswordRequest);
        if (!violations.isEmpty()){
            throw new ConstraintViolationException(violations);
        }

        PasswordResetToken token = passwordResetTokenRepository.findByToken(resetPasswordRequest.getToken()).orElseThrow(()-> new InvalidResetTokenException("Invalid token"));
        if (token.getExpiration().isBefore(LocalDateTime.now())){
            throw new InvalidResetTokenException("Token expired");
        }

        if (token.isUsed()){
            throw new InvalidResetTokenException("Token already used");
        }

        User user = token.getUser();
        user.setPassword(bCryptPasswordEncoder.encode(resetPasswordRequest.getNewPassword()));
        userRepository.save(user);
        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        return new ResetPasswordResponse("Password reset successful");

    }
}
