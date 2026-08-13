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
import java.util.Comparator;
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
            "places.types",
            "places.userRatingCount",
            "places.rating",
            "nextPageToken"
    );
    private static final int MAX_CITIES = 500;
    private static final int MAX_KEYWORDS = 2;
    private static final int MAX_PAGES_PER_QUERY = 3;
    private static final int MIN_CITIES_BEFORE_RANK_CUT = 12;
    private static final int NAMED_PAGES_NATIONWIDE = 3;
    private static final int NAMED_PAGES_PER_CITY = 1;
    private static final int NAMED_MIN_BRANCHES_PER_BRAND = 60;

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
            Map.entry("funeral-services", "funeral_home"),
            Map.entry("bakery", "bakery"),
            Map.entry("florist", "florist"),
            Map.entry("locksmith", "locksmith"),
            Map.entry("roofing", "roofing_contractor"),
            Map.entry("physiotherapy", "physiotherapist")
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

        if (criteria.hasCompanyNames()) {
            return discoverByCompanyNames(criteria);
        }

        List<RankedHit> ranked = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<String> keywords = limit(criteria.searchKeywords(), MAX_KEYWORDS);
        String country = criteria.countryNames().isEmpty() ? "" : criteria.countryNames().get(0);
        String includedType = resolveIncludedType(criteria);
        String regionCode = criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0);

        List<CityGeo> cities = buildCities(criteria, country);
        int overfetch = Math.max(criteria.maxResults() * 3, criteria.maxResults() + 40);
        int minCities = Math.min(cities.size(), MIN_CITIES_BEFORE_RANK_CUT);
        int citiesVisited = 0;

        for (CityGeo city : cities) {
            citiesVisited++;
            for (String keyword : keywords) {
                if (keyword == null || keyword.isBlank()) {
                    continue;
                }
                String textQuery = buildTextQuery(keyword, city.cityName(), country);
                try {
                    List<RankedHit> batch = searchText(
                            textQuery, includedType, regionCode, city, criteria, seen, true, 1, overfetch);
                    ranked.addAll(batch);
                    log.info("Google Places '{}' -> {} places (pool {})", textQuery, batch.size(), ranked.size());
                } catch (Exception ex) {
                    log.warn("Google Places search failed for '{}': {}", textQuery, ex.getMessage());
                }
                sleepQuietly(200);
            }
            if (ranked.size() >= overfetch && citiesVisited >= minCities) {
                break;
            }
        }
        return takeLargestNationwide(ranked, criteria.maxResults());
    }

    /**
     * Custom scrape / branch expansion: Places text search per company name (no category type filter).
     * Allows maps-only branches that have an address even when website is missing.
     */
    public List<WebSearchHit> discoverByCompanyNames(ResolvedDiscoveryCriteria criteria) {
        List<WebSearchHit> hits = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String country = criteria.countryNames().isEmpty() ? "" : criteria.countryNames().get(0);
        String regionCode = criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0);
        List<CityGeo> cities = buildCities(criteria, country);
        if (cities.isEmpty()) {
            cities = List.of(new CityGeo(null, country == null || country.isBlank() ? "Global" : country));
        }
        int brandCount = Math.max(1, criteria.companyNames().size());
        int brandCap = Math.max(
                NAMED_MIN_BRANCHES_PER_BRAND,
                criteria.maxResults() / brandCount
        );

        for (String companyName : criteria.companyNames()) {
            if (companyName == null || companyName.isBlank()) {
                continue;
            }
            if (hits.size() >= criteria.maxResults()) {
                break;
            }
            int brandStart = hits.size();
            List<CityGeo> locations = branchSearchLocations(cities, country, companyName);
            for (int i = 0; i < locations.size(); i++) {
                if (hits.size() - brandStart >= brandCap || hits.size() >= criteria.maxResults()) {
                    break;
                }
                CityGeo location = locations.get(i);
                boolean nationwide = i == 0;
                int pages = nationwide ? NAMED_PAGES_NATIONWIDE : NAMED_PAGES_PER_CITY;
                int locationCap = nationwide ? Math.min(30, brandCap) : brandCap;
                int keptHere = 0;
                for (String textQuery : branchQueries(companyName.trim(), location, country, regionCode, nationwide)) {
                    if (keptHere >= locationCap
                            || hits.size() - brandStart >= brandCap
                            || hits.size() >= criteria.maxResults()) {
                        break;
                    }
                    int remaining = Math.min(
                            locationCap - keptHere,
                            Math.min(
                                    brandCap - (hits.size() - brandStart),
                                    criteria.maxResults() - hits.size()
                            )
                    );
                    int collectCap = nationwide ? Math.min(60, Math.max(20, remaining)) : Math.min(20, remaining);
                    try {
                        List<RankedHit> batch = searchText(
                                textQuery, null, regionCode, location, criteria, seen, true, pages, collectCap);
                        int kept = 0;
                        for (RankedHit ranked : batch) {
                            if (keptHere >= locationCap
                                    || hits.size() - brandStart >= brandCap
                                    || hits.size() >= criteria.maxResults()) {
                                break;
                            }
                            if (!nameLooksLikeBrand(ranked.hit().name(), companyName)) {
                                continue;
                            }
                            hits.add(ranked.hit());
                            keptHere++;
                            kept++;
                        }
                        log.info("Google Places name '{}' -> {} places (kept {})", textQuery, batch.size(), kept);
                    } catch (Exception ex) {
                        log.warn("Google Places name search failed for '{}': {}", textQuery, ex.getMessage());
                    }
                    sleepQuietly(120);
                }
            }
        }
        return hits.size() > criteria.maxResults() ? hits.subList(0, criteria.maxResults()) : hits;
    }

    private List<RankedHit> searchText(
            String textQuery,
            String includedType,
            String regionCode,
            CityGeo city,
            ResolvedDiscoveryCriteria criteria,
            Set<String> seen,
            boolean allowWithoutWebsite,
            int maxPages,
            int collectCap
    ) throws Exception {
        List<RankedHit> hits = new ArrayList<>();
        String pageToken = null;
        int pages = Math.max(1, Math.min(maxPages, MAX_PAGES_PER_QUERY));
        int cap = Math.max(1, collectCap);
        for (int page = 0; page < pages; page++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("textQuery", textQuery);
            body.put("languageCode", languageCodeFor(regionCode));
            body.put("pageSize", 20);
            if (regionCode != null && !regionCode.isBlank()) {
                body.put("regionCode", regionCode);
            }
            if (includedType != null && !includedType.isBlank()) {
                body.put("includedType", includedType);
                body.put("strictTypeFiltering", false);
            }
            if (pageToken != null && !pageToken.isBlank()) {
                body.put("pageToken", pageToken);
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

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode places = root.path("places");
            if (places.isArray()) {
                for (JsonNode place : places) {
                    RankedHit hit = toRankedHit(place, city, criteria, seen, allowWithoutWebsite);
                    if (hit != null) {
                        hits.add(hit);
                    }
                    if (hits.size() >= cap) {
                        return hits;
                    }
                }
            }

            pageToken = text(root, "nextPageToken");
            if (pageToken.isBlank()) {
                break;
            }
            sleepQuietly(350);
        }
        return hits;
    }

    private RankedHit toRankedHit(
            JsonNode place,
            CityGeo city,
            ResolvedDiscoveryCriteria criteria,
            Set<String> seen,
            boolean allowWithoutWebsite
    ) {
        String name = text(place.path("displayName"), "text");
        if (name.isBlank()) {
            return null;
        }
        String placeId = text(place, "id");
        String resourceName = text(place, "name");
        if (resourceName.isBlank() && !placeId.isBlank()) {
            resourceName = placeId.startsWith("places/") ? placeId : "places/" + placeId;
        }
        if (!placeId.isBlank() && !seen.add(placeId)) {
            return null;
        }
        if (placeId.isBlank() && !seen.add(name.toLowerCase(Locale.ROOT) + "|" + city.cityName())) {
            return null;
        }

        String address = text(place, "formattedAddress");
        String phone = firstNonBlank(
                text(place, "internationalPhoneNumber"),
                text(place, "nationalPhoneNumber")
        );

        String website = WebsiteUrlSupport.normalizeHttpUrl(text(place, "websiteUri"));
        if (!WebsiteUrlSupport.isUsableCompanyWebsite(website) && !resourceName.isBlank()) {
            website = fetchWebsiteFromPlaceDetails(resourceName);
        }
        if (!WebsiteUrlSupport.isUsableCompanyWebsite(website)) {
            if (!allowWithoutWebsite || address.isBlank()) {
                log.debug("Skipping Google place '{}' — no company website (maps-only)", name);
                return null;
            }
            website = null;
        }

        String mapsUri = text(place, "googleMapsUri");
        String sourceUrl = WebsiteUrlSupport.isUsableCompanyWebsite(website)
                ? (WebsiteUrlSupport.isMapOrDirectoryUrl(mapsUri) ? website : firstNonBlank(mapsUri, website))
                : firstNonBlank(mapsUri, address);
        if (sourceUrl == null || sourceUrl.isBlank()) {
            sourceUrl = website;
        }

        String resolvedCity = extractCityFromAddress(address, city.cityName(), firstCountryName(criteria));

        WebSearchHit hit = new WebSearchHit(
                name,
                website,
                sourceUrl,
                firstCountry(criteria),
                city.cityId(),
                resolvedCity,
                "google-places",
                blankToNull(address),
                blankToNull(phone),
                blankToNull(placeId)
        );
        int ratingCount = place.path("userRatingCount").asInt(0);
        double rating = place.path("rating").asDouble(0);
        return new RankedHit(hit, ratingCount, rating);
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

    private static List<WebSearchHit> takeLargestNationwide(List<RankedHit> ranked, int maxResults) {
        if (ranked.isEmpty()) {
            return List.of();
        }
        int cap = Math.max(1, maxResults);
        return ranked.stream()
                .sorted(Comparator
                        .comparingInt(RankedHit::userRatingCount).reversed()
                        .thenComparing(Comparator.comparingDouble(RankedHit::rating).reversed()))
                .map(RankedHit::hit)
                .limit(cap)
                .toList();
    }

    /**
     * Country-wide query first, then remaining cities rotated per brand so Amsterdam
     * is not always searched first.
     */
    private static List<CityGeo> branchSearchLocations(List<CityGeo> cities, String country, String companyName) {
        List<CityGeo> cityLoop = new ArrayList<>();
        for (CityGeo city : cities) {
            if (city.cityName() != null && country != null && city.cityName().equalsIgnoreCase(country)) {
                continue;
            }
            cityLoop.add(city);
        }
        if (!cityLoop.isEmpty()) {
            int rotate = Math.floorMod(companyName.toLowerCase(Locale.ROOT).hashCode(), cityLoop.size());
            if (rotate > 0) {
                List<CityGeo> rotated = new ArrayList<>(cityLoop.size());
                rotated.addAll(cityLoop.subList(rotate, cityLoop.size()));
                rotated.addAll(cityLoop.subList(0, rotate));
                cityLoop = rotated;
            }
        }
        List<CityGeo> locations = new ArrayList<>();
        locations.add(new CityGeo(null, country == null || country.isBlank() ? "Global" : country));
        locations.addAll(cityLoop);
        return locations;
    }

    private static List<String> branchQueries(
            String companyName,
            CityGeo location,
            String country,
            String regionCode,
            boolean nationwide
    ) {
        List<String> queries = new ArrayList<>();
        if (nationwide) {
            queries.add(buildTextQuery(companyName, null, country));
            if (regionCode != null && "NL".equalsIgnoreCase(regionCode)) {
                queries.add(companyName + " vestigingen, " + country);
            } else {
                queries.add(companyName + " locations, " + country);
            }
        } else {
            queries.add(buildTextQuery(companyName, location.cityName(), country));
        }
        return queries;
    }

    private static boolean nameLooksLikeBrand(String placeName, String brand) {
        if (placeName == null || placeName.isBlank() || brand == null || brand.isBlank()) {
            return false;
        }
        String hay = placeName.toLowerCase(Locale.ROOT);
        String needle = brand.toLowerCase(Locale.ROOT).trim();
        if (needle.length() >= 3 && hay.contains(needle)) {
            return true;
        }
        String compact = needle.replace("'", "").replace("’", "").replace(" ", "");
        String hayCompact = hay.replace("'", "").replace("’", "").replace(" ", "");
        return compact.length() >= 4 && hayCompact.contains(compact);
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

    private static String extractCityFromAddress(String address, String fallbackCity, String countryName) {
        if (address != null && !address.isBlank()) {
            if (isUsableCityName(fallbackCity, countryName)
                    && address.toLowerCase(Locale.ROOT).contains(fallbackCity.toLowerCase(Locale.ROOT))) {
                return fallbackCity;
            }
            String[] parts = address.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = stripPostalPrefix(parts[i].trim());
                if (isUsableCityName(candidate, countryName)) {
                    return candidate;
                }
            }
        }
        return isUsableCityName(fallbackCity, countryName) ? fallbackCity : null;
    }

    private static String stripPostalPrefix(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceFirst("(?i)^\\d{4}\\s*[A-Z]{2}\\s+", "").trim();
        cleaned = cleaned.replaceFirst("^\\d{5}\\s+", "").trim();
        return cleaned;
    }

    private static boolean isUsableCityName(String value, String countryName) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("://") || lower.contains("/") || lower.startsWith("www.")) {
            return false;
        }
        if (countryName != null && !countryName.isBlank() && lower.equals(countryName.trim().toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (lower.equals("netherlands") || lower.equals("nederland") || lower.equals("holland")
                || lower.equals("algeria") || lower.equals("algérie") || lower.equals("algerie")
                || lower.equals("global") || lower.equals("nl") || lower.equals("dz")) {
            return false;
        }
        if (value.matches("(?i)\\d{4}\\s*[A-Z]{2}") || value.matches("\\d+")) {
            return false;
        }
        return value.length() <= 40;
    }

    private static String languageCodeFor(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return "en";
        }
        return switch (regionCode.toUpperCase(Locale.ROOT)) {
            case "DZ", "MA", "TN", "FR", "BE", "SN", "CI", "CM", "CD", "MG", "HT", "LU" -> "fr";
            case "NL" -> "nl";
            case "DE", "AT", "CH" -> "de";
            case "ES", "MX", "AR", "CL", "CO" -> "es";
            case "IT" -> "it";
            case "PT", "BR" -> "pt";
            case "SA", "EG", "AE", "QA", "KW", "BH", "OM", "JO", "LB", "IQ" -> "ar";
            default -> "en";
        };
    }

    private static String firstCountry(ResolvedDiscoveryCriteria criteria) {
        return criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0);
    }

    private static String firstCountryName(ResolvedDiscoveryCriteria criteria) {
        return criteria.countryNames().isEmpty() ? null : criteria.countryNames().get(0);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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

    private record RankedHit(WebSearchHit hit, int userRatingCount, double rating) {
    }
}
