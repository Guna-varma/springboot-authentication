package com.app.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OAuth2Response {
    private boolean success;
    private String message;
    private OAuth2Data data;
}
