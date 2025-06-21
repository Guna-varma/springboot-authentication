package com.app.backend.redisToken;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisRefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);

    public String createRefreshToken(String userEmail) {
        String refreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(refreshKey(refreshToken), userEmail, REFRESH_TOKEN_EXPIRY);
        return refreshToken;
    }

    public String getUserEmailFromToken(String token) {
        String key = refreshKey(token);
        String email = redisTemplate.opsForValue().get(key);
        if (email == null) {
            throw new RuntimeException("Invalid or expired refresh token");
        }
        return email;
    }

    public void deleteRefreshToken(String token) {
        redisTemplate.delete(refreshKey(token));
    }

    public boolean validate(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(refreshKey(token)));
    }

    private String refreshKey(String token) {
        return "refresh-token:" + token;
    }
}
