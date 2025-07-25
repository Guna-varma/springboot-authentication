package com.app.backend.redisToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);

//    public String createRefreshToken(String userEmail) {
//        String refreshToken = UUID.randomUUID().toString();
//        String key = refreshKey(refreshToken);
//
//        // Store token with user email and TTL
//        redisTemplate.opsForValue().set(key, userEmail, REFRESH_TOKEN_EXPIRY);
//
//        // Also maintain a user-to-tokens mapping for bulk invalidation
//        String userTokensKey = userTokensKey(userEmail);
//        redisTemplate.opsForSet().add(userTokensKey, refreshToken);
//        redisTemplate.expire(userTokensKey, REFRESH_TOKEN_EXPIRY);
//
//        return refreshToken;
//    }

    public String createRefreshToken(String userEmail) {
        String refreshToken = UUID.randomUUID().toString();
        String key = refreshKey(refreshToken);

        try {
            // Store token with user email and TTL
            redisTemplate.opsForValue().set(key, userEmail, REFRESH_TOKEN_EXPIRY);

            // ✅ Maintain user-to-tokens mapping for bulk invalidation
            String userTokensKey = userTokensKey(userEmail);
            redisTemplate.opsForSet().add(userTokensKey, refreshToken);
            redisTemplate.expire(userTokensKey, REFRESH_TOKEN_EXPIRY);

            log.debug("Created refresh token for user: {}", userEmail);
            return refreshToken;

        } catch (Exception e) {
            log.error("Failed to create refresh token for user: {}", userEmail, e);
            throw new RuntimeException("Failed to create refresh token");
        }
    }

    public String getUserEmailFromToken(String token) {
        String key = refreshKey(token);
        String email = redisTemplate.opsForValue().get(key);
        if (email == null) {
            throw new RuntimeException("Invalid or expired refresh token");
        }
        return email;
    }

//    public void deleteRefreshToken(String token) {
//        String key = refreshKey(token);
//        String email = redisTemplate.opsForValue().get(key);
//
//        if (email != null) {
//            // Remove from individual token storage
//            redisTemplate.delete(key);
//
//            // Remove from user's token set
//            redisTemplate.opsForSet().remove(userTokensKey(email), token);
//        }
//    }

    public void deleteRefreshToken(String token) {
        String key = refreshKey(token);

        try {
            // Get email before deleting
            String email = redisTemplate.opsForValue().get(key);

            if (email != null) {
                // Remove from individual token storage
                redisTemplate.delete(key);

                // Remove from user's token set
                redisTemplate.opsForSet().remove(userTokensKey(email), token);

                log.debug("Deleted refresh token for user: {}", email);
            }
        } catch (Exception e) {
            log.error("Failed to delete refresh token", e);
        }
    }

//    public void invalidateAllUserTokens(String email) {
//        String userTokensKey = userTokensKey(email);
//        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
//
//        if (tokens != null && !tokens.isEmpty()) {
//            // Delete all individual token keys
//            List<String> keysToDelete = tokens.stream()
//                    .map(this::refreshKey)
//                    .collect(Collectors.toList());
//            redisTemplate.delete(keysToDelete);
//
//            // Delete the user tokens set
//            redisTemplate.delete(userTokensKey);
//        }
//    }

    // ✅ Bulk invalidation for signout from all devices
    public void invalidateAllUserTokens(String email) {
        String userTokensKey = userTokensKey(email);

        try {
            Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);

            if (tokens != null && !tokens.isEmpty()) {
                // Delete all individual token keys
                List<String> keysToDelete = tokens.stream()
                        .map(this::refreshKey)
                        .collect(Collectors.toList());

                redisTemplate.delete(keysToDelete);

                // Delete the user tokens set
                redisTemplate.delete(userTokensKey);

                log.info("Invalidated {} refresh tokens for user: {}", tokens.size(), email);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate all tokens for user: {}", email, e);
        }
    }

    // ✅ Enhanced validation with error handling
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

//    // JWT Blacklisting methods
//    public void blacklistJwtToken(String tokenId, long remainingTtlMs) {
//        if (remainingTtlMs > 0) {
//            redisTemplate.opsForValue().set(
//                    blacklistKey(tokenId),
//                    "blacklisted",
//                    Duration.ofMillis(remainingTtlMs)
//            );
//        }
//    }

    // JWT Blacklisting support
    public void blacklistJwtToken(String tokenId, long remainingTtlMs) {
        if (tokenId != null && remainingTtlMs > 0) {
            try {
                redisTemplate.opsForValue().set(
                        blacklistKey(tokenId),
                        "blacklisted",
                        Duration.ofMillis(remainingTtlMs)
                );
                log.debug("Blacklisted JWT token: {}", tokenId);
            } catch (Exception e) {
                log.error("Failed to blacklist JWT token: {}", tokenId, e);
            }
        }
    }

//    public boolean isJwtBlacklisted(String tokenId) {
//        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(tokenId)));
//    }

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

    private String refreshKey(String token) {
        return "refresh-token:" + token;
    }

    private String userTokensKey(String email) {
        return "user-tokens:" + email;
    }

    private String blacklistKey(String tokenId) {
        return "blacklist:" + tokenId;
    }
}



//package com.app.backend.redisToken;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class RedisRefreshTokenService {
//
//    private final StringRedisTemplate redisTemplate;
//    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);
//
//    public String createRefreshToken(String userEmail) {
//        String refreshToken = UUID.randomUUID().toString();
//        redisTemplate.opsForValue().set(refreshKey(refreshToken), userEmail, REFRESH_TOKEN_EXPIRY);
//        return refreshToken;
//    }
//
//    public String getUserEmailFromToken(String token) {
//        String key = refreshKey(token);
//        String email = redisTemplate.opsForValue().get(key);
//        if (email == null) {
//            throw new RuntimeException("Invalid or expired refresh token");
//        }
//        return email;
//    }
//
//    public void deleteRefreshToken(String token) {
//        redisTemplate.delete(refreshKey(token));
//    }
//
//    public boolean validate(String token) {
//        return Boolean.TRUE.equals(redisTemplate.hasKey(refreshKey(token)));
//    }
//
//    private String refreshKey(String token) {
//        return "refresh-token:" + token;
//    }
//}
