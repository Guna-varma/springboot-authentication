package com.app.backend.service;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Component
public class RateLimitService {

    private final Map<String, List<Long>> requestCounts = new ConcurrentHashMap<>();
    private final int maxRequestsPerMinute = 60; // Adjust as needed
    private final long timeWindowMs = 60000; // 1 minute

    public boolean isAllowed(String clientIp) {
        long currentTime = System.currentTimeMillis();

        requestCounts.putIfAbsent(clientIp, new ArrayList<>());
        List<Long> timestamps = requestCounts.get(clientIp);

        synchronized (timestamps) {
            // Remove old timestamps
            timestamps.removeIf(timestamp -> currentTime - timestamp > timeWindowMs);

            if (timestamps.size() >= maxRequestsPerMinute) {
                return false;
            }

            timestamps.add(currentTime);
            return true;
        }
    }

    public int getRemainingRequests(String clientIp) {
        List<Long> timestamps = requestCounts.get(clientIp);
        if (timestamps == null) {
            return maxRequestsPerMinute;
        }

        synchronized (timestamps) {
            long currentTime = System.currentTimeMillis();
            timestamps.removeIf(timestamp -> currentTime - timestamp > timeWindowMs);
            return Math.max(0, maxRequestsPerMinute - timestamps.size());
        }
    }
}
