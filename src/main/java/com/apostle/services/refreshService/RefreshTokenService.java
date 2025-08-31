package com.apostle.services.refreshService;

import com.apostle.data.model.RefreshToken;
import com.apostle.data.model.Role;
import com.apostle.data.repositories.RefreshTokenRepository;
import com.apostle.utils.RefreshTokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refreshExpiration}")
    private long refreshExpiration;


    public String createRefreshToken(String userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(RefreshTokenGenerator.generateRefreshToken());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpiration)); // 7 days

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    public boolean validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now()))
            throw new RuntimeException("Expired or revoked refresh token");

        return true;
    }


    public void revokeAllRefreshTokensForUser(String userId) {
                List<RefreshToken> tokens = refreshTokenRepository.findAllByUserId(userId);
                for (RefreshToken token : tokens) {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                }
            }


    public String getUserIdFromRefreshToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .map(RefreshToken::getUserId)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    }

    public Role getRoleFromRefreshToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .map(RefreshToken::getRole)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    }
}
