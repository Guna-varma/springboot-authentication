package com.app.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "app.documents")
@Component
@Data
public class DocumentConfigProperties {

    private long maxImageSize = 7 * 1024 * 1024; // 7MB
    private long maxPdfSize = 10 * 1024 * 1024;  // 10MB
    private int maxBatchSize = 10;
    private List<String> allowedImageTypes = List.of("image/png", "image/jpeg", "image/jpg");
    private List<String> allowedPdfTypes = List.of("application/pdf");
    private String uploadPath = System.getProperty("java.io.tmpdir") + "/uploads";
}

