package com.app.backend.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ApplicationNumberGenerator {

    private static final AtomicInteger counter = new AtomicInteger(1);
    private static final String PREFIX = "ACE";

    public String generateApplicationNumber() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String sequence = String.format("%06d", counter.getAndIncrement());
        return PREFIX + year + sequence;
    }
}

