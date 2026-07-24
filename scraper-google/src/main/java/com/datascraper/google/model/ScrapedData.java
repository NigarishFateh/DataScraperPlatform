/**
 * Holds one full Google scrape result with items and metadata.
 */
package com.datascraper.google.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ScrapedData(
        String source,
        DataCategory category,
        Instant scrapedAt,
        String pageTitle,
        int totalItems,
        List<ScrapedItem> items,
        Map<String, String> metadata
) {

    public ScrapedData {
        metadata = metadata == null ? Map.of() : metadata;
        items = items == null ? List.of() : items;
    }

}
