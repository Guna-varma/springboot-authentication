package com.app.backend.redisToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);
    private static final Duration JWT_BLACKLIST_BUFFER = Duration.ofMinutes(5);

    // Lua script for atomic token creation
    private static final String CREATE_TOKEN_SCRIPT = """
        local tokenKey = KEYS[1]
        local userTokensKey = KEYS[2]
        local email = ARGV[1]
        local token = ARGV[2]
        local expiry = ARGV[3]
        
        redis.call('SETEX', tokenKey, expiry, email)
        redis.call('SADD', userTokensKey, token)
        redis.call('EXPIRE', userTokensKey, expiry)
        
        return 'OK'
        """;

    // Lua script for atomic token deletion
    private static final String DELETE_TOKEN_SCRIPT = """
        local tokenKey = KEYS[1]
        local userTokensKey = KEYS[2]
        local token = ARGV[1]
        
        local email = redis.call('GET', tokenKey)
        if email then
            redis.call('DEL', tokenKey)
            redis.call('SREM', userTokensKey, token)
            return email
        end
        return nil
        """;

    public String createRefreshToken(String userEmail) {
        String refreshToken = UUID.randomUUID().toString();
        String tokenKey = refreshKey(refreshToken);
        String userTokensKey = userTokensKey(userEmail);

        try {
            // Use Lua script for atomic operation
            DefaultRedisScript<String> script = new DefaultRedisScript<>(CREATE_TOKEN_SCRIPT, String.class);

            redisTemplate.execute(script,
                    List.of(tokenKey, userTokensKey),
                    userEmail, refreshToken, String.valueOf(REFRESH_TOKEN_EXPIRY.getSeconds())
            );

            log.debug("Created refresh token for user: {}", userEmail);
            return refreshToken;

        } catch (Exception e) {
            log.error("Failed to create refresh token for user: {}", userEmail, e);
            throw new RuntimeException("Failed to create refresh token");
        }
    }

    public String getUserEmailFromToken(String token) {
        try {
            String key = refreshKey(token);
            String email = redisTemplate.opsForValue().get(key);
            if (email == null) {
                throw new RuntimeException("Invalid or expired refresh token");
            }
            return email;
        } catch (Exception e) {
            log.error("Failed to get user email from token", e);
            throw new RuntimeException("Invalid refresh token");
        }
    }

    public void deleteRefreshToken(String token) {
        try {
            String tokenKey = refreshKey(token);

            // Get email first to determine user tokens key
            String email = redisTemplate.opsForValue().get(tokenKey);
            if (email != null) {
                String userTokensKey = userTokensKey(email);

                // Use Lua script for atomic deletion
                DefaultRedisScript<String> script = new DefaultRedisScript<>(DELETE_TOKEN_SCRIPT, String.class);
                redisTemplate.execute(script,
                        List.of(tokenKey, userTokensKey),
                        token
                );

                log.debug("Deleted refresh token for user: {}", email);
            }
        } catch (Exception e) {
            log.error("Failed to delete refresh token", e);
        }
    }

    // Asynchronous bulk invalidation for better performance
    public void invalidateAllUserTokens(String email) {
        CompletableFuture.runAsync(() -> {
            String userTokensKey = userTokensKey(email);

            try {
                Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);

                if (tokens != null && !tokens.isEmpty()) {
                    try {
                        // Convert to byte arrays outside pipeline for better performance
                        List<byte[]> tokenKeys = tokens.stream()
                                .map(token -> refreshKey(token).getBytes(StandardCharsets.UTF_8))
                                .collect(Collectors.toList());

                        byte[] userTokensKeyBytes = userTokensKey.getBytes(StandardCharsets.UTF_8);

                        // Execute pipeline with proper typing
                        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                            // Delete all individual token keys
                            tokenKeys.forEach(connection::del);

                            // Delete the user tokens set
                            connection.del(userTokensKeyBytes);
                            return null;
                        });

                        log.info("Invalidated {} refresh tokens for user: {}", tokens.size(), email);
                    } catch (Exception e) {
                        log.error("Failed to invalidate tokens for user: {}", email, e);
                        throw new RuntimeException("Token invalidation failed", e);
                    }
                }


//                if (tokens != null && !tokens.isEmpty()) {
//                    // Create pipeline for batch operations
//                    redisTemplate.executePipelined((connection) -> {
//                        // Delete all individual token keys
//                        tokens.forEach(token ->
//                                connection.del(refreshKey(token).getBytes())
//                        );
//
//                        // Delete the user tokens set
//                        connection.del(userTokensKey.getBytes());
//                        return null;
//                    });
//
//                    log.info("Invalidated {} refresh tokens for user: {}", tokens.size(), email);
//                }
            } catch (Exception e) {
                log.error("Failed to invalidate all tokens for user: {}", email, e);
            }
        });
    }

    // Optimized validation with pipeline
    public boolean validate(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(refreshKey(token)));
        } catch (Exception e) {
            log.error("Failed to validate refresh token", e);
            return false;
        }
    }

    // High-performance JWT blacklisting
    public void blacklistJwtToken(String tokenId, long remainingTtlMs) {
        if (tokenId != null && remainingTtlMs > 0) {
            CompletableFuture.runAsync(() -> {
                try {
                    // Add buffer time to prevent race conditions
                    long adjustedTtl = remainingTtlMs + JWT_BLACKLIST_BUFFER.toMillis();

                    redisTemplate.opsForValue().set(
                            blacklistKey(tokenId),
                            "blacklisted",
                            Duration.ofMillis(adjustedTtl)
                    );
                    log.debug("Blacklisted JWT token: {}", tokenId);
                } catch (Exception e) {
                    log.error("Failed to blacklist JWT token: {}", tokenId, e);
                }
            });
        }
    }

    public boolean isJwtBlacklisted(String tokenId) {
        if (tokenId == null) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(tokenId)));
        } catch (Exception e) {
            log.error("Failed to check JWT blacklist for token: {}", tokenId, e);
            return false; // Fail open for availability
        }
    }

    // Optimized key generation methods
    private String refreshKey(String token) {
        return "rt:" + token; // Shorter key for memory optimization
    }

    private String userTokensKey(String email) {
        return "ut:" + email.hashCode(); // Use hash for shorter keys
    }

    private String blacklistKey(String tokenId) {
        return "bl:" + tokenId;
    }
}