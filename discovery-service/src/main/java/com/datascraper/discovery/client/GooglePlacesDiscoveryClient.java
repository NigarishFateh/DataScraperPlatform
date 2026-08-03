package com.datascraper.discovery.client;

import com.datascraper.discovery.config.AppProperties;
import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import com.datascraper.discovery.dto.WebSearchHit;
import com.datascraper.discovery.support.WebsiteUrlSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Google Places API (New) Text Search — primary source for category + city + country discovery.
 */
@Component
public class GooglePlacesDiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesDiscoveryClient.class);
    private static final String SEARCH_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String FIELD_MASK = String.join(",",
            "places.id",
            "places.name",
            "places.displayName",
            "places.formattedAddress",
            "places.websiteUri",
            "places.googleMapsUri",
            "places.nationalPhoneNumber",
            "places.internationalPhoneNumber",
            "places.types"
    );
    private static final int MAX_CITIES = 8;
    private static final int MAX_KEYWORDS = 2;

    /** Best-effort mapping from our category ids to Places includedType (Table A). */
    private static final Map<String, String> INCLUDED_TYPES = Map.ofEntries(
            Map.entry("dental", "dentist"),
            Map.entry("dental-lab", "dentist"),
            Map.entry("orthodontics", "dentist"),
            Map.entry("hospital", "hospital"),
            Map.entry("clinic", "doctor"),
            Map.entry("pharmacy", "pharmacy"),
            Map.entry("veterinary", "veterinary_care"),
            Map.entry("restaurant", "restaurant"),
            Map.entry("cafe", "cafe"),
            Map.entry("coffee-shop", "cafe"),
            Map.entry("hotel", "lodging"),
            Map.entry("gym", "gym"),
            Map.entry("beauty-salon", "beauty_salon"),
            Map.entry("barbershop", "hair_care"),
            Map.entry("spa", "spa"),
            Map.entry("law-firm", "lawyer"),
            Map.entry("real-estate-agency", "real_estate_agency"),
            Map.entry("plumbing", "plumber"),
            Map.entry("electrical-contractor", "electrician"),
            Map.entry("car-rental", "car_rental"),
            Map.entry("auto-repair", "car_repair"),
            Map.entry("bank", "bank"),
            Map.entry("atm", "atm"),
            Map.entry("school", "school"),
            Map.entry("university", "university"),
            Map.entry("mosque", "mosque"),
            Map.entry("church", "church"),
            Map.entry("supermarket", "supermarket"),
            Map.entry("grocery", "grocery_or_supermarket"),
            Map.entry("travel-agency", "travel_agency"),
            Map.entry("insurance", "insurance_agency"),
            Map.entry("accounting", "accounting"),
            Map.entry("laundry", "laundry"),
            Map.entry("moving-company", "moving_company"),
            Map.entry("storage-facility", "storage"),
            Map.entry("funeral-services", "funeral_home")
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GooglePlacesDiscoveryClient(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.apiKey = appProperties.getGooglePlacesApiKey() == null
                ? ""
                : appProperties.getGooglePlacesApiKey().trim();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public List<WebSearchHit> discover(ResolvedDiscoveryCriteria criteria) {
        if (!isConfigured()) {
            return List.of();
        }

        List<WebSearchHit> hits = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<String> keywords = limit(criteria.searchKeywords(), MAX_KEYWORDS);
        String country = criteria.countryNames().isEmpty() ? "" : criteria.countryNames().get(0);
        String includedType = resolveIncludedType(criteria);
        String regionCode = criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0);

        List<CityGeo> cities = buildCities(criteria, country);
        for (CityGeo city : cities) {
            for (String keyword : keywords) {
                if (keyword == null || keyword.isBlank()) {
                    continue;
                }
                String textQuery = buildTextQuery(keyword, city.cityName(), country);
                try {
                    List<WebSearchHit> batch = searchText(textQuery, includedType, regionCode, city, criteria, seen);
                    log.info("Google Places '{}' -> {} places", textQuery, batch.size());
                    hits.addAll(batch);
                } catch (Exception ex) {
                    log.warn("Google Places search failed for '{}': {}", textQuery, ex.getMessage());
                }
                if (hits.size() >= criteria.maxResults()) {
                    return hits.subList(0, criteria.maxResults());
                }
                sleepQuietly(200);
            }
        }
        return hits;
    }

    private List<WebSearchHit> searchText(
            String textQuery,
            String includedType,
            String regionCode,
            CityGeo city,
            ResolvedDiscoveryCriteria criteria,
            Set<String> seen
    ) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("textQuery", textQuery);
        body.put("languageCode", "en");
        body.put("pageSize", Math.min(20, Math.max(criteria.maxResults(), 10)));
        if (regionCode != null && !regionCode.isBlank()) {
            body.put("regionCode", regionCode);
        }
        if (includedType != null && !includedType.isBlank()) {
            body.put("includedType", includedType);
            body.put("strictTypeFiltering", false);
        }

        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(SEARCH_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode() + ": " + truncate(response.body(), 240));
        }

        JsonNode places = objectMapper.readTree(response.body()).path("places");
        List<WebSearchHit> hits = new ArrayList<>();
        if (!places.isArray()) {
            return hits;
        }

        for (JsonNode place : places) {
            String name = text(place.path("displayName"), "text");
            if (name.isBlank()) {
                continue;
            }
            String placeId = text(place, "id");
            String resourceName = text(place, "name");
            if (resourceName.isBlank() && !placeId.isBlank()) {
                resourceName = placeId.startsWith("places/") ? placeId : "places/" + placeId;
            }
            if (!placeId.isBlank() && !seen.add(placeId)) {
                continue;
            }
            if (placeId.isBlank() && !seen.add(name.toLowerCase(Locale.ROOT) + "|" + city.cityName())) {
                continue;
            }

            String website = WebsiteUrlSupport.normalizeHttpUrl(text(place, "websiteUri"));
            if (!WebsiteUrlSupport.isUsableCompanyWebsite(website) && !resourceName.isBlank()) {
                website = fetchWebsiteFromPlaceDetails(resourceName);
            }
            if (!WebsiteUrlSupport.isUsableCompanyWebsite(website)) {
                log.debug("Skipping Google place '{}' — no company website (maps-only)", name);
                continue;
            }

            String mapsUri = text(place, "googleMapsUri");
            String sourceUrl = WebsiteUrlSupport.isMapOrDirectoryUrl(mapsUri) ? website : mapsUri;
            if (sourceUrl == null || sourceUrl.isBlank()) {
                sourceUrl = website;
            }

            String address = text(place, "formattedAddress");
            String resolvedCity = extractCityFromAddress(address, city.cityName());

            hits.add(new WebSearchHit(
                    name,
                    website,
                    sourceUrl,
                    firstCountry(criteria),
                    city.cityId(),
                    resolvedCity,
                    "google-places"
            ));
        }
        return hits;
    }

    private String fetchWebsiteFromPlaceDetails(String resourceName) {
        try {
            String path = resourceName.startsWith("places/") ? resourceName : "places/" + resourceName;
            String url = "https://places.googleapis.com/v1/" + path;
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", "websiteUri,googleMapsUri")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return "";
            }
            String website = WebsiteUrlSupport.normalizeHttpUrl(
                    text(objectMapper.readTree(response.body()), "websiteUri")
            );
            sleepQuietly(120);
            return WebsiteUrlSupport.isUsableCompanyWebsite(website) ? website : "";
        } catch (Exception ex) {
            log.debug("Place details website lookup failed for {}: {}", resourceName, ex.getMessage());
            return "";
        }
    }

    private static String buildTextQuery(String keyword, String city, String country) {
        StringBuilder q = new StringBuilder(keyword.trim());
        if (city != null && !city.isBlank()) {
            q.append(" in ").append(city.trim());
        }
        if (country != null && !country.isBlank() && (city == null || !city.equalsIgnoreCase(country))) {
            q.append(", ").append(country.trim());
        }
        return q.toString();
    }

    private static List<CityGeo> buildCities(ResolvedDiscoveryCriteria criteria, String country) {
        List<CityGeo> cities = new ArrayList<>();
        int limit = Math.min(criteria.cityNames().size(), MAX_CITIES);
        for (int i = 0; i < limit; i++) {
            String cityName = criteria.cityNames().get(i);
            if (cityName == null || cityName.isBlank()) {
                continue;
            }
            String cityId = i < criteria.cityIds().size() ? criteria.cityIds().get(i) : null;
            cities.add(new CityGeo(cityId, cityName));
        }
        if (cities.isEmpty() && country != null && !country.isBlank()) {
            cities.add(new CityGeo(null, country));
        }
        return cities;
    }

    private static String resolveIncludedType(ResolvedDiscoveryCriteria criteria) {
        if (criteria.categoryIds() == null) {
            return null;
        }
        for (String id : criteria.categoryIds()) {
            if (id == null) {
                continue;
            }
            String mapped = INCLUDED_TYPES.get(id.toLowerCase(Locale.ROOT));
            if (mapped != null) {
                return mapped;
            }
        }
        return null;
    }

    private static String extractCityFromAddress(String address, String fallbackCity) {
        if (address == null || address.isBlank()) {
            return fallbackCity;
        }
        // Prefer explicit fallback city if it appears in the address.
        if (fallbackCity != null && !fallbackCity.isBlank()
                && address.toLowerCase(Locale.ROOT).contains(fallbackCity.toLowerCase(Locale.ROOT))) {
            return fallbackCity;
        }
        String[] parts = address.split(",");
        if (parts.length >= 2) {
            return parts[parts.length - 2].trim();
        }
        return fallbackCity;
    }

    private static String firstCountry(ResolvedDiscoveryCriteria criteria) {
        return criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0);
    }

    private static List<String> limit(List<String> values, int max) {
        if (values == null || values.isEmpty()) {
            return List.of("company");
        }
        return values.size() <= max ? values : values.subList(0, max);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private record CityGeo(String cityId, String cityName) {
    }
}
