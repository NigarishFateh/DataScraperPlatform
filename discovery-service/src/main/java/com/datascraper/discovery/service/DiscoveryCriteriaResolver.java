package com.datascraper.discovery.service;

import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.discovery.client.CategoryCatalogClient;
import com.datascraper.discovery.client.LocationCatalogClient;
import com.datascraper.discovery.dto.CategoryDto;
import com.datascraper.discovery.dto.CityDto;
import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DiscoveryCriteriaResolver.class);

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
            )),
            Map.entry("NL", List.of(
                    "nl-rotterdam", "nl-eindhoven", "nl-groningen", "nl-maastricht",
                    "nl-utrecht", "nl-the-hague", "nl-tilburg", "nl-almere",
                    "nl-breda", "nl-nijmegen", "nl-haarlem", "nl-arnhem",
                    "nl-leiden", "nl-delft", "nl-amersfoort", "nl-zwolle",
                    "nl-apeldoorn", "nl-enschede", "nl-dordrecht", "nl-leeuwarden",
                    "nl-alkmaar", "nl-venlo", "nl-deventer", "nl-amsterdam"
            )),
            Map.entry("DZ", List.of(
                    "dz-algiers", "dz-oran", "dz-constantine", "dz-annaba",
                    "dz-blida", "dz-setif", "dz-batna", "dz-tlemcen"
            )),
            Map.entry("FR", List.of(
                    "fr-paris", "fr-lyon", "fr-marseille", "fr-toulouse",
                    "fr-bordeaux", "fr-lille", "fr-nantes", "fr-nice"
            )),
            Map.entry("ES", List.of(
                    "es-madrid", "es-barcelona", "es-valencia", "es-seville",
                    "es-malaga", "es-bilbao", "es-zaragoza", "es-murcia"
            )),
            Map.entry("IT", List.of(
                    "it-rome", "it-milan", "it-naples", "it-turin",
                    "it-florence", "it-bologna", "it-genoa", "it-venice"
            )),
            Map.entry("CA", List.of(
                    "ca-toronto", "ca-montreal", "ca-vancouver", "ca-calgary",
                    "ca-ottawa", "ca-edmonton", "ca-winnipeg", "ca-quebec-city"
            )),
            Map.entry("AU", List.of(
                    "au-sydney", "au-melbourne", "au-brisbane", "au-perth",
                    "au-adelaide", "au-canberra", "au-gold-coast", "au-newcastle"
            ))
    );

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.ofEntries(
            Map.entry("ai", List.of("artificial intelligence", "AI company", "machine learning", "deep learning", "LLM")),
            Map.entry("ml", List.of("machine learning", "ML company", "data science", "AI")),
            Map.entry("deep-learning", List.of("deep learning", "neural network", "AI research")),
            Map.entry("nlp", List.of("natural language processing", "NLP", "text analytics", "LLM")),
            Map.entry("computer-vision", List.of("computer vision", "image recognition", "visual AI")),
            Map.entry("software", List.of("software", "SaaS", "IT services", "software company")),
            Map.entry("software-dev", List.of("software development", "custom software", "IT company", "software house")),
            Map.entry("web-dev", List.of("web development", "web agency", "website development")),
            Map.entry("mobile-dev", List.of("mobile app development", "iOS Android development", "app studio")),
            Map.entry("saas", List.of("SaaS", "software as a service", "cloud software")),
            Map.entry("cloud", List.of("cloud computing", "cloud services", "cloud provider")),
            Map.entry("devops", List.of("DevOps", "CI/CD", "platform engineering")),
            Map.entry("cybersecurity", List.of("cybersecurity", "infosec", "security software", "cyber security")),
            Map.entry("infosec", List.of("information security", "cybersecurity", "security consulting")),
            Map.entry("network-security", List.of("network security", "firewall", "SOC")),
            Map.entry("identity-access", List.of("identity access management", "IAM", "SSO")),
            Map.entry("data-science", List.of("data science", "analytics company", "data analytics")),
            Map.entry("data-eng", List.of("data engineering", "data platform", "ETL")),
            Map.entry("bi-software", List.of("business intelligence", "BI software", "analytics platform")),
            Map.entry("it", List.of("information technology", "IT company", "technology services")),
            Map.entry("it-services", List.of("IT services", "managed IT", "technology services")),
            Map.entry("it-support", List.of("IT support", "help desk", "technical support")),
            Map.entry("it-consulting", List.of("IT consulting", "technology consulting", "digital consulting")),
            Map.entry("msp", List.of("managed service provider", "MSP", "managed IT services")),
            Map.entry("outsourcing", List.of("IT outsourcing", "software outsourcing", "offshore development")),
            Map.entry("qa-testing", List.of("software testing", "QA services", "test automation")),
            Map.entry("ui-ux", List.of("UI UX design", "product design", "design agency")),
            Map.entry("automation", List.of("automation", "RPA", "process automation")),
            Map.entry("rpa", List.of("robotic process automation", "RPA", "workflow automation")),
            Map.entry("blockchain", List.of("blockchain", "crypto company", "web3")),
            Map.entry("web3", List.of("web3", "blockchain", "decentralized")),
            Map.entry("iot", List.of("internet of things", "IoT", "connected devices")),
            Map.entry("robotics", List.of("robotics", "robotics company", "automation robotics")),
            Map.entry("game-dev", List.of("game development", "game studio", "video games")),
            Map.entry("hosting", List.of("web hosting", "hosting provider", "cloud hosting")),
            Map.entry("open-source", List.of("open source", "open source software")),
            Map.entry("digital-xform", List.of("digital transformation", "digital consultancy")),
            Map.entry("fintech", List.of("fintech", "financial technology", "payments")),
            Map.entry("healthtech", List.of("healthtech", "health technology", "digital health")),
            Map.entry("edtech", List.of("edtech", "education technology", "e-learning platform")),
            Map.entry("martech", List.of("martech", "marketing technology", "marketing software")),
            Map.entry("api", List.of("API development", "API platform", "integration platform")),
            Map.entry("low-code", List.of("low code", "no code", "app builder")),
            Map.entry("observability", List.of("observability", "monitoring", "APM")),
            Map.entry("database", List.of("database software", "database company", "data management")),
            Map.entry("cleaning", List.of(
                    "cleaning company",
                    "janitorial",
                    "facility cleaning",
                    "schoonmaakbedrijf",
                    "schoonmaak bedrijf"
            )),
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
            String catalogName = locationCatalogClient.findCountryName(code);
            countryNames.add(catalogName != null && !catalogName.isBlank() ? catalogName : code);
        }

        List<String> cityIds = new ArrayList<>();
        List<String> cityNames = new ArrayList<>();
        Set<String> seenCityKeys = new LinkedHashSet<>();
        List<String> requestedCityIds = request.cityIds() == null ? List.of() : request.cityIds();
        List<String> companyNames = normalizeCompanyNames(request.companyNames());
        boolean namedMode = !companyNames.isEmpty();

        if (!requestedCityIds.isEmpty()) {
            for (String cityId : requestedCityIds) {
                CityDto city = locationCatalogClient.findCityById(cityId);
                if (city != null && city.name() != null && !city.name().isBlank()) {
                    addCity(cityIds, cityNames, seenCityKeys, city.id(), city.name());
                } else if (cityId != null && cityId.contains("-")) {
                    addCity(cityIds, cityNames, seenCityKeys, cityId, humanizeCityId(cityId));
                }
            }
        } else if (!countryCodes.isEmpty() && namedMode) {
            // Custom scrape with no city: major cities nationwide, not Amsterdam-only.
            for (String countryCode : countryCodes) {
                addPriorityCities(countryCode, cityIds, cityNames, seenCityKeys, locationCatalogClient);
            }
            log.info("Named scrape city plan countries={} cities={}", countryCodes, cityNames);
        } else if (!countryCodes.isEmpty() && !namedMode) {
            // No city selected → nationwide: priority/major cities first, then remaining catalog cities.
            for (String countryCode : countryCodes) {
                List<CityDto> cities = locationCatalogClient.listCitiesByCountry(countryCode);
                Map<String, CityDto> byId = new HashMap<>();
                for (CityDto city : cities) {
                    if (city != null && city.id() != null) {
                        byId.put(city.id().toLowerCase(Locale.ROOT), city);
                    }
                }

                addPriorityCitiesFromMap(cityIds, cityNames, seenCityKeys, byId, countryCode);

                for (CityDto city : cities) {
                    if (city == null || city.id() == null || city.name() == null || city.name().isBlank()) {
                        continue;
                    }
                    String key = city.id().toLowerCase(Locale.ROOT);
                    if (!byId.containsKey(key)) {
                        continue; // already added via priority
                    }
                    byId.remove(key);
                    addCity(cityIds, cityNames, seenCityKeys, city.id(), city.name());
                }
            }
            log.info(
                    "Nationwide discovery city plan countries={} cities={} (priority-first)",
                    countryCodes,
                    cityNames.size()
            );
        }

        Set<String> keywords = new LinkedHashSet<>();
        if (namedMode) {
            keywords.addAll(companyNames);
        } else {
            for (String countryCode : countryCodes) {
                for (String categoryId : requestedIds) {
                    keywords.addAll(localizedKeywords(categoryId, countryCode));
                }
            }
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
        }

        int maxResults;
        if (namedMode) {
            int requested = request.maxResults() <= 0 ? companyNames.size() : request.maxResults();
            if (requested <= companyNames.size()) {
                maxResults = companyNames.size();
            } else {
                maxResults = Math.min(requested, companyNames.size() * 80);
            }
        } else {
            maxResults = request.maxResults();
        }

        return new ResolvedDiscoveryCriteria(
                requestedIds,
                categoryNames,
                countryCodes,
                countryNames,
                List.copyOf(cityIds),
                List.copyOf(cityNames),
                List.copyOf(keywords),
                maxResults,
                companyNames
        );
    }

    /**
     * When a selected city returns nothing (small towns), retry major cities in the same country.
     * Already-searched city IDs are skipped. If every major city was already tried, city lists
     * are emptied so Places/Apollo query the country as a whole.
     */
    public ResolvedDiscoveryCriteria expandToMajorCities(ResolvedDiscoveryCriteria criteria) {
        if (criteria == null || criteria.countryCodes().isEmpty()) {
            return criteria;
        }
        Set<String> alreadyTried = new LinkedHashSet<>();
        for (String cityId : criteria.cityIds()) {
            if (cityId != null && !cityId.isBlank()) {
                alreadyTried.add(cityId.toLowerCase(Locale.ROOT));
            }
        }

        List<String> cityIds = new ArrayList<>();
        List<String> cityNames = new ArrayList<>();
        Set<String> seenCityKeys = new LinkedHashSet<>();
        for (String countryCode : criteria.countryCodes()) {
            addPriorityCities(countryCode, cityIds, cityNames, seenCityKeys, locationCatalogClient);
        }

        List<String> filteredIds = new ArrayList<>();
        List<String> filteredNames = new ArrayList<>();
        for (int i = 0; i < cityIds.size(); i++) {
            if (alreadyTried.contains(cityIds.get(i).toLowerCase(Locale.ROOT))) {
                continue;
            }
            filteredIds.add(cityIds.get(i));
            filteredNames.add(cityNames.get(i));
        }

        log.info(
                "Major-city fallback countries={} cities={} (skipped already-tried={})",
                criteria.countryCodes(),
                filteredNames,
                alreadyTried
        );
        return new ResolvedDiscoveryCriteria(
                criteria.categoryIds(),
                criteria.categoryNames(),
                criteria.countryCodes(),
                criteria.countryNames(),
                List.copyOf(filteredIds),
                List.copyOf(filteredNames),
                criteria.searchKeywords(),
                criteria.maxResults(),
                criteria.companyNames()
        );
    }

    private static final Set<String> FRANCOPHONE_COUNTRIES = Set.of(
            "DZ", "MA", "TN", "FR", "BE", "SN", "CI", "CM", "CD", "MG", "HT", "LU"
    );

    private static List<String> localizedKeywords(String categoryId, String countryCode) {
        if (categoryId == null || countryCode == null || !FRANCOPHONE_COUNTRIES.contains(countryCode.toUpperCase(Locale.ROOT))) {
            return List.of();
        }
        String key = categoryId.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "ai", "ml", "deep-learning", "nlp", "computer-vision" ->
                    List.of("informatique", "intelligence artificielle", "société de logiciels");
            case "software", "software-dev", "it", "it-services", "it-consulting", "saas" ->
                    List.of("informatique", "logiciel", "développement logiciel");
            default -> List.of("entreprise", "société");
        };
    }

    private static void addPriorityCities(
            String countryCode,
            List<String> cityIds,
            List<String> cityNames,
            Set<String> seenCityKeys,
            LocationCatalogClient locationCatalogClient
    ) {
        List<CityDto> cities = locationCatalogClient.listCitiesByCountry(countryCode);
        Map<String, CityDto> byId = new HashMap<>();
        for (CityDto city : cities) {
            if (city != null && city.id() != null) {
                byId.put(city.id().toLowerCase(Locale.ROOT), city);
            }
        }
        addPriorityCitiesFromMap(cityIds, cityNames, seenCityKeys, byId, countryCode);
    }

    private static void addPriorityCitiesFromMap(
            List<String> cityIds,
            List<String> cityNames,
            Set<String> seenCityKeys,
            Map<String, CityDto> byId,
            String countryCode
    ) {
        List<String> priority = PRIORITY_CITY_IDS.getOrDefault(countryCode, List.of());
        for (String priorityId : priority) {
            CityDto city = byId.remove(priorityId.toLowerCase(Locale.ROOT));
            if (city == null || city.name() == null || city.name().isBlank()) {
                addCity(cityIds, cityNames, seenCityKeys, priorityId, humanizeCityId(priorityId));
                continue;
            }
            addCity(cityIds, cityNames, seenCityKeys, city.id(), city.name());
        }
    }

    private static List<String> normalizeCompanyNames(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static void addCity(
            List<String> cityIds,
            List<String> cityNames,
            Set<String> seenCityKeys,
            String cityId,
            String cityName
    ) {
        if (cityId == null || cityId.isBlank() || cityName == null || cityName.isBlank()) {
            return;
        }
        String key = cityId.toLowerCase(Locale.ROOT);
        if (!seenCityKeys.add(key)) {
            return;
        }
        cityIds.add(cityId);
        cityNames.add(cityName);
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
