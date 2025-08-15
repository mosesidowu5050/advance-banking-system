package com.apostle.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl {
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.email.reset-token-expiration-minutes:10}")
    private int resetTokenExpirationMinutes;

    private static final String REGISTRATION_SUBJECT = "Welcome to Apostle Bank";
    private static final String REGISTRATION_BODY = "Thank you for registering with us. Your account number is: %s%n" +
            "This account number will be used for future transactions, please keep it safe.";

    public void sendPasswordResetEmail(String toEmail, String token) throws MessagingException {
        validateInputs(toEmail, token);
        String subject = "Your Password Reset Code";
        String body = String.format("Use the following reset code: %s%nThis code will expire in %d minutes.",
                token, resetTokenExpirationMinutes);

        try {
            sendEmail(toEmail, subject, body);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }


    public void sendAccountNumberEmail(String toEmail, String accountNumber) throws MessagingException {
        validateInputs(toEmail, accountNumber);
            sendEmail(toEmail, REGISTRATION_SUBJECT, String.format(REGISTRATION_BODY, accountNumber));

    }

    private void sendEmail(String toEmail, String subject, String body) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
        helper.setFrom(from);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, false);
        javaMailSender.send(message);
    }

    private void validateInputs(String email, String content) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email must not be null or empty");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content must not be null or empty");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}