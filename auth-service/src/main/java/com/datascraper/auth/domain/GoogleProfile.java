package com.datascraper.auth.domain;

/**
 * Identity payload extracted from a verified Google token.
 */
public record GoogleProfile(
        String subject,
        String email,
        String name,
        String pictureUrl
) {
}
