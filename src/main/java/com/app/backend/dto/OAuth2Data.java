package com.app.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2Data {

    private String accessToken;

    private String tokenType;

    private Long expiresIn; // Token expiration time in seconds

    private UserInfo user;
}
