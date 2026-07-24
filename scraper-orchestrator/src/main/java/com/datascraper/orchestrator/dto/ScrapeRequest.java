/**
 * Request body listing which sources and data categories to scrape.
 */
package com.datascraper.orchestrator.dto;

import com.datascraper.orchestrator.model.DataCategory;
import com.datascraper.orchestrator.model.ScraperSource;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ScrapeRequest(
        @NotEmpty List<ScraperSource> sources,
        @NotEmpty List<DataCategory> categories
) {

    public static ScrapeRequest defaults() {
        return new ScrapeRequest(
                List.of(ScraperSource.GOOGLE, ScraperSource.MICROSOFT, ScraperSource.IBM),
                List.of(DataCategory.JOBS)
        );
    }

}
