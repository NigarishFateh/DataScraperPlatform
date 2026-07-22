package com.datascraper.location.repository;

import com.datascraper.location.domain.City;
import com.datascraper.location.domain.Country;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory European catalog (Phase 6).
 * Phase 13 replaces with PostgreSQL without changing LocationService.
 */
@Repository
public class LocationRepository {

    private static final List<Country> COUNTRIES = List.of(
            new Country("DE", "Germany"),
            new Country("NL", "Netherlands"),
            new Country("BE", "Belgium"),
            new Country("FR", "France"),
            new Country("ES", "Spain"),
            new Country("IT", "Italy"),
            new Country("PT", "Portugal"),
            new Country("NO", "Norway"),
            new Country("SE", "Sweden"),
            new Country("DK", "Denmark"),
            new Country("FI", "Finland"),
            new Country("IE", "Ireland"),
            new Country("PL", "Poland"),
            new Country("AT", "Austria"),
            new Country("CH", "Switzerland")
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

    private final List<City> cities = CITY_NAMES_BY_COUNTRY.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                    .map(name -> new City(toCityId(entry.getKey(), name), name, entry.getKey())))
            .toList();

    public List<Country> findAllCountries() {
        return COUNTRIES;
    }

    public Optional<Country> findCountryByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return COUNTRIES.stream()
                .filter(country -> country.code().equals(normalized))
                .findFirst();
    }

    public List<City> findCitiesByCountry(String countryCode, String search) {
        String normalizedCode = countryCode.trim().toUpperCase(Locale.ROOT);
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        return cities.stream()
                .filter(city -> city.countryCode().equals(normalizedCode))
                .filter(city -> q.isEmpty() || city.name().toLowerCase(Locale.ROOT).contains(q))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .collect(Collectors.toList());
    }

    private static String toCityId(String countryCode, String cityName) {
        return countryCode + "-" + cityName.toLowerCase(Locale.ROOT).replace(' ', '-');
    }
}
