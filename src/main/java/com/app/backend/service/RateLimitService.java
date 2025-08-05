package com.app.backend.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Component
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int maxRequestsPerMinute = 60; // Same limit as before
    private final Duration timeWindow = Duration.ofMinutes(1); // 1 minute window

    /**
     * Creates a new bucket for a client with the configured rate limits
     */
    private Bucket createNewBucket() {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(maxRequestsPerMinute, Refill.intervally(maxRequestsPerMinute, timeWindow)))
                .build();
    }

    /**
     * Gets or creates a bucket for the given client IP
     */
    private Bucket getBucket(String clientIp) {
        return buckets.computeIfAbsent(clientIp, k -> createNewBucket());
    }

    /**
     * Checks if a request is allowed for the given client IP
     * Same method signature as before - no breaking changes!
     */
    public boolean isAllowed(String clientIp) {
        Bucket bucket = getBucket(clientIp);
        return bucket.tryConsume(1);
    }

    /**
     * Gets remaining requests for the given client IP
     * Same method signature as before - no breaking changes!
     */
    public int getRemainingRequests(String clientIp) {
        Bucket bucket = getBucket(clientIp);
        return (int) bucket.getAvailableTokens();
    }

    /**
     * Optional: Get additional rate limit info (you can add this for enhanced functionality)
     */
    public long getSecondsUntilRefill(String clientIp) {
        // This is a bonus method - won't break existing code since it's new
        return timeWindow.getSeconds(); // Simplified - in reality, you'd calculate actual time
    }

    /**
     * Optional: Cleanup method to remove unused buckets (prevents memory leaks)
     */
    public void cleanupUnusedBuckets() {
        // Remove buckets that haven't been used recently
        // This is optional but recommended for production
        buckets.entrySet().removeIf(entry -> entry.getValue().getAvailableTokens() == maxRequestsPerMinute);
    }
}