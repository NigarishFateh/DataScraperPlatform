/**
 * Carries request data for refreshing tokens or logging out.
 */
package com.datascraper.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank String refreshToken
) {
}
