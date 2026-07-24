/**
 * Tests that cache keys stay stable and normalize website URLs.
 */
package com.datascraper.orchestrator.cache;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.enums.ScraperType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScraperCacheKeyBuilderTest {

    @Test
    void buildsStableKeyForSameCompanyAndUrl() {
        ScraperContext context = new ScraperContext(
                "job-1", "co-sap", "SAP", "https://www.SAP.com/", List.of(), "corr-1");

        String key1 = ScraperCacheKeyBuilder.build("intel:scraper", ScraperType.COMPANY_WEBSITE, context);
        String key2 = ScraperCacheKeyBuilder.build("intel:scraper", ScraperType.COMPANY_WEBSITE, context);

        assertThat(key1).isEqualTo(key2);
        assertThat(key1).startsWith("intel:scraper:COMPANY_WEBSITE:co-sap:");
    }

    @Test
    void differentScraperTypesProduceDifferentKeys() {
        ScraperContext context = new ScraperContext(
                "job-1", "co-sap", "SAP", "https://www.sap.com", List.of(), "corr-1");

        String websiteKey = ScraperCacheKeyBuilder.build("intel:scraper", ScraperType.COMPANY_WEBSITE, context);
        String techKey = ScraperCacheKeyBuilder.build("intel:scraper", ScraperType.TECHNOLOGY_STACK, context);

        assertThat(websiteKey).isNotEqualTo(techKey);
    }

    @Test
    void normalizesTrailingSlashAndScheme() {
        assertThat(ScraperCacheKeyBuilder.normalizeUrl("https://www.sap.com/"))
                .isEqualTo(ScraperCacheKeyBuilder.normalizeUrl("https://www.sap.com"));
        assertThat(ScraperCacheKeyBuilder.normalizeUrl("www.sap.com"))
                .isEqualTo(ScraperCacheKeyBuilder.normalizeUrl("https://www.sap.com"));
    }
}
