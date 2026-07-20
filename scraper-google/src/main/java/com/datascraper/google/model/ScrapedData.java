package com.datascraper.google.model;

import java.time.Instant;
import java.util.List;

public record ScrapedData(
        String source,
        DataCategory category,
        Instant scrapedAt,
        String pageTitle,
        int totalItems,
        List<ScrapedItem> items
) {
}
