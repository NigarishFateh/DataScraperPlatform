/**
 * Holds one scraped row of Microsoft page data.
 */
package com.datascraper.microsoft.model;

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
