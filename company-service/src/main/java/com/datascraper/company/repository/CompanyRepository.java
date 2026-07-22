package com.datascraper.company.repository;

import com.datascraper.company.domain.Company;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class CompanyRepository {

    private static final List<String> INDUSTRIES = List.of(
            "Software", "Cloud", "FinTech", "HealthTech", "Consulting", "SaaS"
    );

    private static final List<String> NAME_PREFIX = List.of(
            "Nimbus", "Nordic", "Alpine", "Baltic", "Horizon", "Vertex", "Quantum",
            "Aurora", "Helix", "Pulse", "Stack", "Forge", "Lattice", "Orbit", "Cedar"
    );

    private static final List<String> NAME_SUFFIX = List.of(
            "Labs", "Systems", "Soft", "Digital", "Works", "Cloud", "Analytics",
            "Security", "Apps", "Partners"
    );

    private static final List<String> CATEGORY_IDS = List.of(
            "software-dev", "web-dev", "cloud", "devops", "cyber", "ai", "ml", "data-eng",
            "erp", "crm", "fintech", "healthtech", "edtech", "recruitment", "consulting",
            "digital-xform", "automation", "blockchain", "mobile", "saas", "api"
    );

    private static final Map<String, List<String>> CITY_NAMES_BY_COUNTRY = Map.ofEntries(
            Map.entry("DE", List.of("Berlin", "Munich", "Hamburg", "Frankfurt", "Cologne", "Stuttgart", "Düsseldorf")),
            Map.entry("NL", List.of("Amsterdam", "Rotterdam", "The Hague", "Utrecht", "Eindhoven")),
            Map.entry("BE", List.of("Brussels", "Antwerp", "Ghent", "Leuven")),
            Map.entry("FR", List.of("Paris", "Lyon", "Marseille", "Toulouse", "Nantes", "Lille")),
            Map.entry("ES", List.of("Madrid", "Barcelona", "Valencia", "Seville", "Bilbao")),
            Map.entry("IT", List.of("Milan", "Rome", "Turin", "Bologna", "Florence")),
            Map.entry("PT", List.of("Lisbon", "Porto", "Braga", "Coimbra")),
            Map.entry("NO", List.of("Oslo", "Bergen", "Trondheim")),
            Map.entry("SE", List.of("Stockholm", "Gothenburg", "Malmö", "Uppsala")),
            Map.entry("DK", List.of("Copenhagen", "Aarhus", "Odense")),
            Map.entry("FI", List.of("Helsinki", "Espoo", "Tampere", "Oulu")),
            Map.entry("IE", List.of("Dublin", "Cork", "Galway", "Limerick")),
            Map.entry("PL", List.of("Warsaw", "Kraków", "Wrocław", "Gdańsk", "Poznań")),
            Map.entry("AT", List.of("Vienna", "Graz", "Linz", "Salzburg")),
            Map.entry("CH", List.of("Zurich", "Geneva", "Basel", "Bern", "Lausanne"))
    );

    private final Map<String, Company> byId = new ConcurrentHashMap<>();

    public CompanyRepository() {
        seedCompanies();
    }

    public List<Company> findAll() {
        return byId.values().stream()
                .sorted(Comparator.comparing(Company::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Optional<Company> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Company save(Company company) {
        byId.put(company.getId(), company);
        return company;
    }

    public void deleteById(String id) {
        byId.remove(id);
    }

    public List<Company> search(List<String> cityIds, String search) {
        if (cityIds == null || cityIds.isEmpty()) {
            return List.of();
        }
        var citySet = cityIds.stream().map(String::trim).collect(Collectors.toSet());
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        return findAll().stream()
                .filter(company -> citySet.contains(company.getCityId()))
                .filter(company -> matchesSearch(company, q))
                .toList();
    }

    private boolean matchesSearch(Company company, String q) {
        if (q.isEmpty()) {
            return true;
        }
        return company.getName().toLowerCase(Locale.ROOT).contains(q)
                || company.getWebsite().toLowerCase(Locale.ROOT).contains(q)
                || company.getIndustry().toLowerCase(Locale.ROOT).contains(q);
    }

    private void seedCompanies() {
        for (Map.Entry<String, List<String>> entry : CITY_NAMES_BY_COUNTRY.entrySet()) {
            String countryCode = entry.getKey();
            for (String cityName : entry.getValue()) {
                String cityId = toCityId(countryCode, cityName);
                int count = 4 + (hash(cityId) % 5);
                for (int index = 0; index < count; index++) {
                    Company company = buildCompany(cityId, countryCode, index);
                    save(company);
                }
            }
        }
    }

    private Company buildCompany(String cityId, String countryCode, int index) {
        String seed = cityId + "-" + index;
        int h = hash(seed);
        String name = NAME_PREFIX.get(h % NAME_PREFIX.size()) + " " + NAME_SUFFIX.get((h >> 3) % NAME_SUFFIX.size());
        int categoryCount = 2 + (h % 4);
        List<String> categoryIds = new ArrayList<>();
        for (int c = 0; c < categoryCount; c++) {
            categoryIds.add(CATEGORY_IDS.get((h + c * 5) % CATEGORY_IDS.size()));
        }
        List<String> uniqueCategories = categoryIds.stream().distinct().toList();
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        return new Company(
                "co-" + seed,
                name,
                "https://www." + slug + ".example",
                INDUSTRIES.get(h % INDUSTRIES.size()),
                cityId,
                countryCode,
                uniqueCategories
        );
    }

    private static String toCityId(String countryCode, String cityName) {
        return countryCode + "-" + cityName.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private static int hash(String input) {
        int h = 0;
        for (int i = 0; i < input.length(); i++) {
            h = (h * 31 + input.charAt(i));
        }
        return Math.abs(h);
    }
}
