package com.apostle.services.redisService;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    public void addTokenForUser(String userId, String token, long ttlSeconds) {
        String key = "user:" + userId + ":tokens";
        redisTemplate.opsForSet().add(key, token);
        redisTemplate.expire(key, java.time.Duration.ofSeconds(ttlSeconds));
    }

    public List<String> getTokensForUser(String userId) {
        String key = "user:" + userId + ":tokens";
        Set<String> tokens = redisTemplate.opsForSet().members(key);
        if (tokens == null) return List.of();
        return tokens.stream().collect(Collectors.toList());
    }

    public void blacklistToken(String token, long ttlSeconds) {
        String key = "blacklist:" + token;
        redisTemplate.opsForValue().set(key, "revoked", java.time.Duration.ofSeconds(ttlSeconds));
    }

    public void removeUserActiveTokens(String userId) {
        String key = "user:" + userId + ":tokens";
        redisTemplate.delete(key);
    }
}
