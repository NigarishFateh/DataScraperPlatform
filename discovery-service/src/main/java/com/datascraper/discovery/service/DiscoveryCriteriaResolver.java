package com.datascraper.discovery.service;

import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.discovery.client.CategoryCatalogClient;
import com.datascraper.discovery.client.LocationCatalogClient;
import com.datascraper.discovery.dto.CategoryDto;
import com.datascraper.discovery.dto.CityDto;
import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DiscoveryCriteriaResolver {

    private static final int MAX_EXPANDED_CITIES = 8;

    private static final Map<String, List<String>> PRIORITY_CITY_IDS = Map.ofEntries(
            Map.entry("PK", List.of(
                    "pk-karachi", "pk-lahore", "pk-islamabad", "pk-rawalpindi",
                    "pk-faisalabad", "pk-multan", "pk-peshawar", "pk-quetta"
            )),
            Map.entry("US", List.of(
                    "us-new-york", "us-los-angeles", "us-chicago", "us-houston",
                    "us-san-francisco", "us-seattle", "us-boston", "us-miami"
            )),
            Map.entry("IN", List.of(
                    "in-mumbai", "in-delhi", "in-bangalore", "in-hyderabad",
                    "in-chennai", "in-pune", "in-kolkata", "in-gurgaon"
            )),
            Map.entry("GB", List.of(
                    "gb-london", "gb-manchester", "gb-birmingham", "gb-leeds",
                    "gb-glasgow", "gb-edinburgh", "gb-bristol", "gb-liverpool"
            )),
            Map.entry("AE", List.of(
                    "ae-dubai", "ae-abu-dhabi", "ae-sharjah", "ae-ajman"
            )),
            Map.entry("DE", List.of(
                    "de-berlin", "de-munich", "de-hamburg", "de-frankfurt",
                    "de-cologne", "de-stuttgart", "de-dusseldorf"
            ))
    );

    private static final Map<String, String> COUNTRY_NAMES = Map.ofEntries(
            Map.entry("PK", "Pakistan"),
            Map.entry("US", "United States"),
            Map.entry("GB", "United Kingdom"),
            Map.entry("IN", "India"),
            Map.entry("DE", "Germany"),
            Map.entry("AE", "United Arab Emirates"),
            Map.entry("SA", "Saudi Arabia"),
            Map.entry("CA", "Canada"),
            Map.entry("AU", "Australia"),
            Map.entry("FR", "France"),
            Map.entry("NL", "Netherlands"),
            Map.entry("SG", "Singapore")
    );

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.ofEntries(
            Map.entry("ai", List.of("artificial intelligence", "AI", "machine learning", "deep learning", "LLM")),
            Map.entry("ml", List.of("machine learning", "ML", "data science", "AI")),
            Map.entry("software", List.of("software", "SaaS", "IT services", "software development")),
            Map.entry("software-dev", List.of("software development", "custom software", "IT company")),
            Map.entry("cybersecurity", List.of("cybersecurity", "infoSec", "security software")),
            Map.entry("fintech", List.of("fintech", "financial technology", "payments")),
            Map.entry("cleaning", List.of("cleaning company", "janitorial", "facility cleaning")),
            Map.entry("automation", List.of("automation", "RPA", "industrial automation")),
            Map.entry("dental", List.of("dental clinic", "dentist", "dental practice", "dental care", "odontology")),
            Map.entry("dental-lab", List.of("dental laboratory", "dental lab", "dental technician")),
            Map.entry("orthodontics", List.of("orthodontist", "orthodontic clinic", "braces clinic")),
            Map.entry("clinic", List.of("medical clinic", "clinic", "outpatient clinic")),
            Map.entry("hospital", List.of("hospital", "medical center", "healthcare hospital")),
            Map.entry("healthcare", List.of("healthcare", "medical clinic", "health clinic")),
            Map.entry("pharmacy", List.of("pharmacy", "drugstore", "chemist")),
            Map.entry("veterinary", List.of("veterinary clinic", "vet clinic", "animal hospital")),
            Map.entry("physiotherapy", List.of("physiotherapy", "physical therapy clinic")),
            Map.entry("dermatology", List.of("dermatology clinic", "dermatologist")),
            Map.entry("ophthalmology", List.of("eye clinic", "ophthalmologist", "optometrist")),
            Map.entry("solar-installers", List.of("solar installer", "solar panel installation", "solar company")),
            Map.entry("solar", List.of("solar energy company", "solar panels", "solar installer")),
            Map.entry("restaurant", List.of("restaurant", "dining", "eatery")),
            Map.entry("cafe", List.of("cafe", "coffee shop")),
            Map.entry("law-firm", List.of("law firm", "attorney", "advocates")),
            Map.entry("real-estate-agency", List.of("real estate agency", "property agents", "realtors")),
            Map.entry("construction", List.of("construction company", "building contractor", "general contractor")),
            Map.entry("plumbing", List.of("plumber", "plumbing services")),
            Map.entry("hvac", List.of("HVAC", "air conditioning", "heating cooling")),
            Map.entry("gym", List.of("gym", "fitness center", "fitness club")),
            Map.entry("beauty-salon", List.of("beauty salon", "hair salon")),
            Map.entry("hotel", List.of("hotel", "lodging", "accommodation")),
            Map.entry("advertising-agency", List.of(
                    "advertising agency",
                    "ad agency",
                    "creative agency",
                    "media agency",
                    "branding agency",
                    "advertising company"
            )),
            Map.entry("media-agency", List.of("media agency", "media buying", "communications agency")),
            Map.entry("creative-agency", List.of("creative agency", "design agency", "brand agency")),
            Map.entry("branding-agency", List.of("branding agency", "brand agency", "branding company")),
            Map.entry("digital-marketing", List.of("digital marketing agency", "digital marketing", "online marketing")),
            Map.entry("seo-agency", List.of("SEO agency", "SEO company", "search engine optimization")),
            Map.entry("pr-agency", List.of("PR agency", "public relations agency", "public relations"))
    );

    private final CategoryCatalogClient categoryCatalogClient;
    private final LocationCatalogClient locationCatalogClient;

    public DiscoveryCriteriaResolver(
            CategoryCatalogClient categoryCatalogClient,
            LocationCatalogClient locationCatalogClient
    ) {
        this.categoryCatalogClient = categoryCatalogClient;
        this.locationCatalogClient = locationCatalogClient;
    }

    public ResolvedDiscoveryCriteria resolve(DiscoveryRequest request) {
        List<String> requestedIds = request.categoryIds() == null ? List.of() : request.categoryIds();
        List<CategoryDto> categories = categoryCatalogClient.listByIds(requestedIds);

        Map<String, String> catalogNamesById = new HashMap<>();
        for (CategoryDto category : categories) {
            if (category == null || category.id() == null || category.id().isBlank()) {
                continue;
            }
            String name = category.name();
            catalogNamesById.put(
                    category.id().toLowerCase(Locale.ROOT),
                    name != null && !name.isBlank() ? name : category.id()
            );
        }

        List<String> categoryNames = new ArrayList<>();
        for (String categoryId : requestedIds) {
            if (categoryId == null || categoryId.isBlank()) {
                continue;
            }
            String catalogName = catalogNamesById.get(categoryId.toLowerCase(Locale.ROOT));
            categoryNames.add(catalogName != null ? catalogName : categoryId.trim());
        }

        List<String> countryCodes = request.countryCodes() == null ? List.of() : request.countryCodes().stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        List<String> countryNames = new ArrayList<>();
        for (String code : countryCodes) {
            countryNames.add(COUNTRY_NAMES.getOrDefault(code, code));
        }

        List<String> cityIds = new ArrayList<>();
        List<String> cityNames = new ArrayList<>();
        List<String> requestedCityIds = request.cityIds() == null ? List.of() : request.cityIds();

        if (!requestedCityIds.isEmpty()) {
            for (String cityId : requestedCityIds) {
                CityDto city = locationCatalogClient.findCityById(cityId);
                if (city != null && city.name() != null && !city.name().isBlank()) {
                    cityIds.add(city.id());
                    cityNames.add(city.name());
                } else if (cityId != null && cityId.contains("-")) {
                    cityIds.add(cityId);
                    cityNames.add(humanizeCityId(cityId));
                }
            }
        } else if (!countryCodes.isEmpty()) {
            // Nationwide scrape still resolves concrete cities so every query/result mentions a city.
            for (String countryCode : countryCodes) {
                List<CityDto> cities = locationCatalogClient.listCitiesByCountry(countryCode);
                Map<String, CityDto> byId = new HashMap<>();
                for (CityDto city : cities) {
                    if (city != null && city.id() != null) {
                        byId.put(city.id().toLowerCase(Locale.ROOT), city);
                    }
                }

                List<String> priority = PRIORITY_CITY_IDS.getOrDefault(countryCode, List.of());
                for (String priorityId : priority) {
                    CityDto city = byId.remove(priorityId.toLowerCase(Locale.ROOT));
                    if (city == null || city.name() == null || city.name().isBlank()) {
                        continue;
                    }
                    cityIds.add(city.id());
                    cityNames.add(city.name());
                    if (cityIds.size() >= MAX_EXPANDED_CITIES) {
                        break;
                    }
                }

                if (cityIds.size() < MAX_EXPANDED_CITIES) {
                    for (CityDto city : cities) {
                        if (city == null || city.id() == null || city.name() == null || city.name().isBlank()) {
                            continue;
                        }
                        if (!byId.containsKey(city.id().toLowerCase(Locale.ROOT))) {
                            continue; // already added via priority
                        }
                        byId.remove(city.id().toLowerCase(Locale.ROOT));
                        cityIds.add(city.id());
                        cityNames.add(city.name());
                        if (cityIds.size() >= MAX_EXPANDED_CITIES) {
                            break;
                        }
                    }
                }
                if (cityIds.size() >= MAX_EXPANDED_CITIES) {
                    break;
                }
            }
        }

        Set<String> keywords = new LinkedHashSet<>();
        for (String categoryId : requestedIds) {
            List<String> mapped = CATEGORY_KEYWORDS.get(categoryId == null ? "" : categoryId.toLowerCase(Locale.ROOT));
            if (mapped != null) {
                keywords.addAll(mapped);
            }
        }
        keywords.addAll(categoryNames);
        if (keywords.isEmpty()) {
            keywords.add("company");
        }

        return new ResolvedDiscoveryCriteria(
                requestedIds,
                categoryNames,
                countryCodes,
                countryNames,
                cityIds.stream().distinct().toList(),
                cityNames.stream().distinct().toList(),
                List.copyOf(keywords),
                request.maxResults()
        );
    }

    private static String humanizeCityId(String cityId) {
        String[] parts = cityId.split("-", 2);
        String raw = parts.length == 2 ? parts[1] : cityId;
        if (raw.isBlank()) {
            return cityId;
        }
        String spaced = raw.replace('-', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1).toLowerCase(Locale.ROOT);
    }
}
