package com.apostle.services.redisService;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey(token);
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
            return new ArrayList<>(tokens);
        }

        public void blacklistToken(String token, long ttlSeconds) {
        redisTemplate.opsForValue().set(token, "blacklisted", ttlSeconds, TimeUnit.SECONDS);
        }

        public void removeUserActiveTokens(String userId) {
            String key = "user:" + userId + ":tokens";
            redisTemplate.delete(key);
        }

    public void cacheRefreshToken(String userId, String hashedToken, long ttlSeconds) {
        String key = "refreshToken:" + userId;
        redisTemplate.opsForValue().set(key, hashedToken, ttlSeconds, TimeUnit.SECONDS);
    }

    public String getCachedRefreshToken(String userId) {
        String key = "refreshToken:" + userId;
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteCachedRefreshToken(String userId) {
        redisTemplate.opsForValue().getOperations().delete("refreshToken:" + userId);
    }

}
