package com.datascraper.discovery.client;

import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import com.datascraper.discovery.dto.WebSearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Facade over B2B / business-search providers.
 * Order: Apollo.io (primary) → Google Places → SerpAPI Maps.
 */
@Component
public class BusinessSearchDiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(BusinessSearchDiscoveryClient.class);

    private final ApolloDiscoveryClient apolloDiscoveryClient;
    private final GooglePlacesDiscoveryClient googlePlacesDiscoveryClient;
    private final SerpApiMapsDiscoveryClient serpApiMapsDiscoveryClient;

    public BusinessSearchDiscoveryClient(
            ApolloDiscoveryClient apolloDiscoveryClient,
            GooglePlacesDiscoveryClient googlePlacesDiscoveryClient,
            SerpApiMapsDiscoveryClient serpApiMapsDiscoveryClient
    ) {
        this.apolloDiscoveryClient = apolloDiscoveryClient;
        this.googlePlacesDiscoveryClient = googlePlacesDiscoveryClient;
        this.serpApiMapsDiscoveryClient = serpApiMapsDiscoveryClient;
    }

    public boolean isConfigured() {
        return apolloDiscoveryClient.isConfigured()
                || googlePlacesDiscoveryClient.isConfigured()
                || serpApiMapsDiscoveryClient.isConfigured();
    }

    public String configuredProviders() {
        List<String> names = new ArrayList<>();
        if (apolloDiscoveryClient.isConfigured()) {
            names.add("apollo");
        }
        if (googlePlacesDiscoveryClient.isConfigured()) {
            names.add("google-places");
        }
        if (serpApiMapsDiscoveryClient.isConfigured()) {
            names.add("serpapi-maps");
        }
        return names.isEmpty() ? "none" : String.join(",", names);
    }

    public List<WebSearchHit> discover(ResolvedDiscoveryCriteria criteria) {
        return discover(criteria, null);
    }

    public List<WebSearchHit> discover(ResolvedDiscoveryCriteria criteria, IntConsumer onProgress) {
        Map<String, WebSearchHit> unique = new LinkedHashMap<>();

        if (apolloDiscoveryClient.isConfigured()) {
            for (WebSearchHit hit : safe(
                    () -> apolloDiscoveryClient.discover(criteria, count -> {
                        if (onProgress != null) {
                            onProgress.accept(Math.max(count, unique.size()));
                        }
                    }),
                    "apollo"
            )) {
                unique.putIfAbsent(dedupeKey(hit), hit);
                if (onProgress != null) {
                    onProgress.accept(unique.size());
                }
                if (unique.size() >= criteria.maxResults()) {
                    return new ArrayList<>(unique.values());
                }
            }
        }

        // Named custom scrapes: Apollo org lookup only. Places/Maps keyword fallback
        // pollutes results with unrelated local businesses; brand stubs cover misses.
        if (criteria.hasCompanyNames()) {
            return new ArrayList<>(unique.values());
        }

        if (unique.size() < criteria.maxResults() && googlePlacesDiscoveryClient.isConfigured()) {
            for (WebSearchHit hit : safe(() -> googlePlacesDiscoveryClient.discover(criteria), "google-places")) {
                unique.putIfAbsent(dedupeKey(hit), hit);
                if (onProgress != null) {
                    onProgress.accept(unique.size());
                }
                if (unique.size() >= criteria.maxResults()) {
                    return new ArrayList<>(unique.values());
                }
            }
        }

        if (unique.size() < criteria.maxResults() && serpApiMapsDiscoveryClient.isConfigured()) {
            for (WebSearchHit hit : safe(() -> serpApiMapsDiscoveryClient.discover(criteria), "serpapi-maps")) {
                unique.putIfAbsent(dedupeKey(hit), hit);
                if (onProgress != null) {
                    onProgress.accept(unique.size());
                }
                if (unique.size() >= criteria.maxResults()) {
                    break;
                }
            }
        }

        log.info(
                "Business search providers=[{}] returned {} hits for categories={} cities={}",
                configuredProviders(),
                unique.size(),
                criteria.categoryNames(),
                criteria.cityNames()
        );
        return new ArrayList<>(unique.values());
    }

    private List<WebSearchHit> safe(SourceCall call, String source) {
        try {
            return call.get();
        } catch (Exception ex) {
            log.warn("Business search source '{}' failed: {}", source, ex.getMessage());
            return List.of();
        }
    }

    private static String dedupeKey(WebSearchHit hit) {
        String website = hit.website() == null ? "" : hit.website().trim().toLowerCase(Locale.ROOT);
        if (!website.isBlank()) {
            website = website.replaceFirst("^https?://", "").replaceFirst("^www\\.", "");
            while (website.endsWith("/")) {
                website = website.substring(0, website.length() - 1);
            }
            return "w:" + website;
        }
        String name = hit.name() == null ? "" : hit.name().trim().toLowerCase(Locale.ROOT);
        String city = hit.cityName() == null ? "" : hit.cityName().trim().toLowerCase(Locale.ROOT);
        return "n:" + name + "|" + city;
    }

    @FunctionalInterface
    private interface SourceCall {
        List<WebSearchHit> get() throws Exception;
    }
}
