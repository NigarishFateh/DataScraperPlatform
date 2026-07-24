/**
 * Models a server-side refresh session for a logged-in user.
 */
package com.datascraper.auth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Server-side refresh session. The client only stores the opaque token string.
 */
public class RefreshSession {

    private final String token;
    private final UUID userId;
    private final Instant expiresAt;
    private boolean revoked;

    public RefreshSession(String token, UUID userId, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public String getToken() {
        return token;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }
}
