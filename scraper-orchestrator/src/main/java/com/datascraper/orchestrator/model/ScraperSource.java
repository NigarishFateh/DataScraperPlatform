/**
 * Enum of company scraper sources (Google, Microsoft, IBM).
 */
package com.datascraper.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ScraperSource {

    GOOGLE,
    MICROSOFT,
    IBM;

    @JsonCreator
    public static ScraperSource fromValue(String value) {
        return ScraperSource.valueOf(value.trim().toUpperCase());
    }

}
