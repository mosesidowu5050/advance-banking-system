package com.apostle.data.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "refresh_tokens")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshToken {

        @Id
        private String id;
        private String userId;
        private String token;
        private Instant expiryDate;
        private boolean revoked = false;
        private Role role;
}
