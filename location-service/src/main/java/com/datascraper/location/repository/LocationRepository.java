/**
 * Loads countries and cities from the database into domain objects.
 */
package com.datascraper.location.repository;

import com.datascraper.location.domain.City;
import com.datascraper.location.domain.Country;
import com.datascraper.location.entity.CityEntity;
import com.datascraper.location.entity.CountryEntity;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * PostgreSQL-backed location catalog (Phase 13).
 */
@Repository
public class LocationRepository {

    private final CountryJpaRepository countryJpaRepository;
    private final CityJpaRepository cityJpaRepository;

    public LocationRepository(CountryJpaRepository countryJpaRepository, CityJpaRepository cityJpaRepository) {
        this.countryJpaRepository = countryJpaRepository;
        this.cityJpaRepository = cityJpaRepository;
    }

    public List<Country> findAllCountries() {
        return countryJpaRepository.findAll(Sort.by("name")).stream()
                .map(this::toCountry)
                .toList();
    }

    public Optional<Country> findCountryByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return countryJpaRepository.findById(code.trim().toUpperCase(Locale.ROOT))
                .map(this::toCountry);
    }

    public List<City> findCitiesByCountry(String countryCode, String search) {
        String normalizedCode = countryCode.trim().toUpperCase(Locale.ROOT);
        String q = search == null ? "" : search.trim();

        List<CityEntity> entities = q.isEmpty()
                ? cityJpaRepository.findByCountryCodeOrderByNameAsc(normalizedCode)
                : cityJpaRepository.searchByCountryAndName(normalizedCode, q);

        return entities.stream().map(this::toCity).toList();
    }

    private Country toCountry(CountryEntity entity) {
        return new Country(entity.getCode(), entity.getName());
    }

    private City toCity(CityEntity entity) {
        return new City(entity.getId(), entity.getName(), entity.getCountryCode());
    }
}
