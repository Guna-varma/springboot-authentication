package com.app.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cors-test")
public class TestController {

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/practiceImages")
    public ResponseEntity<?> getTestImages() {
        // Redirect to your actual endpoint
        try {
            // You can inject DocumentService and call it directly
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "CORS test successful");
            response.put("data", Arrays.asList()); // Empty for now
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
