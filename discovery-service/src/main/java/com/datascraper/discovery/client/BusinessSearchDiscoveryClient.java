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

/**
 * Facade over paid/real business-search providers (Google Places, SerpAPI Maps).
 * Query shape is always category keyword + city + country.
 */
@Component
public class BusinessSearchDiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(BusinessSearchDiscoveryClient.class);

    private final GooglePlacesDiscoveryClient googlePlacesDiscoveryClient;
    private final SerpApiMapsDiscoveryClient serpApiMapsDiscoveryClient;

    public BusinessSearchDiscoveryClient(
            GooglePlacesDiscoveryClient googlePlacesDiscoveryClient,
            SerpApiMapsDiscoveryClient serpApiMapsDiscoveryClient
    ) {
        this.googlePlacesDiscoveryClient = googlePlacesDiscoveryClient;
        this.serpApiMapsDiscoveryClient = serpApiMapsDiscoveryClient;
    }

    public boolean isConfigured() {
        return googlePlacesDiscoveryClient.isConfigured() || serpApiMapsDiscoveryClient.isConfigured();
    }

    public String configuredProviders() {
        List<String> names = new ArrayList<>();
        if (googlePlacesDiscoveryClient.isConfigured()) {
            names.add("google-places");
        }
        if (serpApiMapsDiscoveryClient.isConfigured()) {
            names.add("serpapi-maps");
        }
        return names.isEmpty() ? "none" : String.join(",", names);
    }

    public List<WebSearchHit> discover(ResolvedDiscoveryCriteria criteria) {
        Map<String, WebSearchHit> unique = new LinkedHashMap<>();

        if (googlePlacesDiscoveryClient.isConfigured()) {
            for (WebSearchHit hit : safe(() -> googlePlacesDiscoveryClient.discover(criteria), "google-places")) {
                unique.putIfAbsent(dedupeKey(hit), hit);
                if (unique.size() >= criteria.maxResults()) {
                    return new ArrayList<>(unique.values());
                }
            }
        }

        if (unique.size() < criteria.maxResults() && serpApiMapsDiscoveryClient.isConfigured()) {
            for (WebSearchHit hit : safe(() -> serpApiMapsDiscoveryClient.discover(criteria), "serpapi-maps")) {
                unique.putIfAbsent(dedupeKey(hit), hit);
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
