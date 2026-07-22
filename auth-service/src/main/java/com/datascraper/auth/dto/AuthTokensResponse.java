package com.datascraper.auth.dto;

import java.util.UUID;

public record AuthTokensResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {
    public static AuthTokensResponse bearer(
            String accessToken,
            String refreshToken,
            long expiresInSeconds,
            UserResponse user
    ) {
        return new AuthTokensResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }

    public record UserResponse(
            UUID id,
            String email,
            String displayName,
            String pictureUrl
    ) {
    }
}
