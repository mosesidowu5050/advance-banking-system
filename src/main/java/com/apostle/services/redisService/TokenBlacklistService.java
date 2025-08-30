package com.apostle.services.redisService;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String token, long expirationMillis) {
        long ttl = expirationMillis - System.currentTimeMillis();
        if (ttl > 0) {
            redisTemplate.opsForValue().set(token, "revoked", ttl, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey(token);
    }
}
