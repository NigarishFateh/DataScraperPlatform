/**
 * Lists the kinds of scrapers the platform can run.
 */
package com.datascraper.common.enums;

/**
 * Scraper capabilities selected by the Factory (Strategy Pattern).
 * One enum value ≈ one data-source responsibility — not one company.
 */
public enum ScraperType {
    COMPANY_WEBSITE,
    TECHNOLOGY_STACK,
    NEWS,
    GITHUB,
    CONTACT
    // LINKEDIN intentionally omitted until a legal/API-approved adapter exists
}
