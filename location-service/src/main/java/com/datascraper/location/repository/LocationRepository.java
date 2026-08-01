/**
 * Loads countries and cities from the database into domain objects.
 */
package com.datascraper.location.repository;

import com.datascraper.location.domain.City;
import com.datascraper.location.domain.Country;
import com.datascraper.location.entity.CityEntity;
import com.datascraper.location.entity.CountryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * PostgreSQL-backed global location catalog.
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

    public Page<Country> searchCountries(String search, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("name"));
        String q = search == null ? "" : search.trim();
        Page<CountryEntity> result = q.isEmpty()
                ? countryJpaRepository.findAll(pageable)
                : countryJpaRepository.searchByNameOrCode(q, pageable);
        return result.map(this::toCountry);
    }

    public Optional<Country> findCountryByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return countryJpaRepository.findById(code.trim().toUpperCase(Locale.ROOT))
                .map(this::toCountry);
    }

    public List<City> findCities(String countryCode, String search) {
        String normalizedCode = countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.ROOT);
        String q = search == null ? "" : search.trim();

        if (normalizedCode.isEmpty() && q.isEmpty()) {
            return List.of();
        }

        List<CityEntity> entities;
        if (normalizedCode.isEmpty()) {
            entities = cityJpaRepository.searchByName(q);
        } else if (q.isEmpty()) {
            entities = cityJpaRepository.findByCountryCodeOrderByNameAsc(normalizedCode);
        } else {
            entities = cityJpaRepository.searchByCountryAndName(normalizedCode, q);
        }

        return entities.stream().map(this::toCity).toList();
    }

    private Country toCountry(CountryEntity entity) {
        return new Country(entity.getCode(), entity.getName());
    }

    private City toCity(CityEntity entity) {
        return new City(entity.getId(), entity.getName(), entity.getCountryCode());
    }
}
