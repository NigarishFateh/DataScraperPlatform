/**
 * Looks up the right scraper client by source (Google, Microsoft, IBM).
 */
package com.datascraper.orchestrator.client;

import com.datascraper.orchestrator.model.ScraperSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ScraperClientRegistry {

    private final Map<ScraperSource, ScraperClient> clientsBySource;

    public ScraperClientRegistry(List<ScraperClient> clients) {
        this.clientsBySource = clients.stream()
                .collect(Collectors.toUnmodifiableMap(ScraperClient::source, Function.identity()));
    }

    public ScraperClient getClient(ScraperSource source) {
        ScraperClient client = clientsBySource.get(source);
        if (client == null) {
            throw new IllegalArgumentException("No scraper client registered for source: " + source);
        }
        return client;
    }

}
