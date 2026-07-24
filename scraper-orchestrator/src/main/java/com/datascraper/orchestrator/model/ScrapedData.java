/**
 * Holds one scrape result from a source, including items and metadata.
 */
package com.datascraper.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonCreator;

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
