package com.apostle.data.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    private String id;

    private String token;

    private LocalDateTime expiration;

    private boolean used = false;

    private User user;
}
