/**
 * Carries a standard error response shared by services.
 */
package com.datascraper.common.error;

import java.time.Instant;

/**
 * Stable error envelope returned by services (and later Gateway).
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}
