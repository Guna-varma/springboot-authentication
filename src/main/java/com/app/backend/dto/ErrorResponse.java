package com.app.backend.dto;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.*;

import java.util.Map;

@Hidden
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private Map<String, String> errors; // can be null
}
