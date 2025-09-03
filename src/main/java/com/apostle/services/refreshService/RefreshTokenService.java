package com.apostle.services.refreshService;

import com.apostle.data.model.RefreshToken;
import com.apostle.data.model.Role;
import com.apostle.data.repositories.RefreshTokenRepository;
import com.apostle.exceptions.InvalidLoginException;
import com.apostle.services.jwtService.JwtService;
import com.apostle.services.redisService.RedisService;
import com.apostle.utils.RefreshTokenGenerator;
import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisService redisService;
    private final JwtService jwtService;

    @Value("${jwt.refreshExpiration}")
    private long refreshExpiration;


    public String createRefreshToken(String userId, Role userRole) {
        String rawToken = RefreshTokenGenerator.generateRefreshToken();
        String hashedToken = RefreshTokenGenerator.hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setRevoked(false);
        refreshToken.setRole(userRole);
        refreshToken.setToken(hashedToken);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpiration));

        refreshTokenRepository.save(refreshToken);

        long ttlSeconds = refreshExpiration / 1000;
        redisService.cacheRefreshToken(userId, hashedToken, ttlSeconds);

        return rawToken;
    }

    public boolean validateRefreshToken(String token) {
        String hashedToken = RefreshTokenGenerator.hashToken(token);
        RefreshToken refreshToken = refreshTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new InvalidLoginException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidLoginException("Expired or revoked refresh token");
        }

        return true;
    }


    public void revokeAllRefreshTokensForUser(String userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserId(userId);
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }

        redisService.deleteCachedRefreshToken(userId);
    }



    public void revokeAllAccessTokensForUser(String userId) {
        List<String> activeToken = redisService.getTokensForUser(userId);
        for (String token : activeToken) {
            long expiration = jwtService.getExpiration(token);
            long now = System.currentTimeMillis() / 1000;
            long ttl = expiration - now;
            redisService.blacklistToken(token, ttl);
        }
        redisService.removeUserActiveTokens(userId);
    }


    public String getUserIdFromRefreshToken(String refreshToken) {
        String hashedToken = RefreshTokenGenerator.hashToken(refreshToken);
        return refreshTokenRepository.findByToken(hashedToken)
                .map(RefreshToken::getUserId)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    }

    public Role getRoleFromRefreshToken(String refreshToken) {
        String hashedToken = RefreshTokenGenerator.hashToken(refreshToken);
        return refreshTokenRepository.findByToken(hashedToken)
                .map(RefreshToken::getRole)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    }


    public Map<String, String> refreshAccessToken(String refreshToken) {
        if (!validateRefreshToken(refreshToken)) throw new InvalidLoginException("Invalid or expired refresh token");

        String userId = getUserIdFromRefreshToken(refreshToken);
        Role role = getRoleFromRefreshToken(refreshToken);
        revokeAllRefreshTokensForUser(userId);

        String newAccessToken = jwtService.generateJwtToken(userId, role);
        String newRefreshToken = createRefreshToken(userId, role);
        long ttl = jwtService.getRemainingValidity(newAccessToken);
        redisService.addTokenForUser(userId, newAccessToken, ttl);

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        );
    }

}
