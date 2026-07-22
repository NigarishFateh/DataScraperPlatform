package com.datascraper.auth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Application user — our source of truth after Google proves identity once.
 */
public class User {

    private final UUID id;
    private final String email;
    private final String displayName;
    private final String pictureUrl;
    private final String googleSubject;
    private final Instant createdAt;
    private Instant lastLoginAt;

    public User(
            UUID id,
            String email,
            String displayName,
            String pictureUrl,
            String googleSubject,
            Instant createdAt,
            Instant lastLoginAt
    ) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.pictureUrl = pictureUrl;
        this.googleSubject = googleSubject;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void markLogin(Instant at) {
        this.lastLoginAt = at;
    }
}
