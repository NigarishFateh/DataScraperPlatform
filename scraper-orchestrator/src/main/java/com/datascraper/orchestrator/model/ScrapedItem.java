/**
 * One scraped row with title, description, URL, and optional fields.
 */
package com.datascraper.orchestrator.model;

import java.util.Map;

public record ScrapedItem(
        String title,
        String description,
        String url,
        String location,
        String value,
        Map<String, String> metadata
) {
}
