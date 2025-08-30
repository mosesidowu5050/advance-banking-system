package com.apostle.services.redisService;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String token, long expirationInSeconds) {
        redisTemplate.opsForValue().set(token, "blacklisted", expirationInSeconds, TimeUnit.SECONDS);
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey(token);
    }


}
