package com.apostle.services;

import com.apostle.data.model.RefreshToken;
import com.apostle.data.repositories.RefreshTokenRepository;
import com.apostle.utils.RefreshTokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public String createRefreshToken(String userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(RefreshTokenGenerator.generateRefreshToken());
        refreshToken.setExpiryDate(Instant.now().plusSeconds(60L * 60L * 24L * 7L)); // 7 days

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    public boolean validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Expired or revoked refresh token");
        }

        return true;
    }

    public void revokeUserTokens(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
