/**
 * Carries request data for Google credential login.
 */
package com.datascraper.auth.dto;

import jakarta.validation.constraints.AssertTrue;

/**
 * Exchange a Google credential for platform tokens.
 * Provide either accessToken (chrome.identity.getAuthToken) or idToken.
 */
public record GoogleAuthRequest(
        String accessToken,
        String idToken
) {
    @AssertTrue(message = "Either accessToken or idToken is required")
    public boolean hasCredential() {
        return (accessToken != null && !accessToken.isBlank())
                || (idToken != null && !idToken.isBlank());
    }
}
