/**
 * Tests that the location repository loads the seeded global country and city catalog.
 */
package com.datascraper.location.repository;

import com.datascraper.location.domain.City;
import com.datascraper.location.domain.Country;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LocationRepositoryIntegrationTest {

    @Autowired
    private LocationRepository locationRepository;

    @Test
    void loadsGlobalCountryCatalog() {
        List<Country> countries = locationRepository.findAllCountries();
        assertThat(countries).hasSizeGreaterThanOrEqualTo(200);
        assertThat(countries).extracting(Country::code)
                .contains("US", "CA", "IN", "AU", "BR", "JP", "CN", "DE", "FR", "GB");
    }

    @Test
    void searchesCountriesByName() {
        Page<Country> page = locationRepository.searchCountries("united", 0, 20);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).extracting(Country::code).contains("US", "GB");
    }

    @Test
    void findsCitiesWithinCountry() {
        List<City> berlinCities = locationRepository.findCities("DE", "ber");
        assertThat(berlinCities).extracting(City::name).contains("Berlin");
    }

    @Test
    void searchesCitiesGloballyWithoutCountry() {
        List<City> cities = locationRepository.findCities(null, "mumbai");
        assertThat(cities).extracting(City::id).contains("in-mumbai");
    }
}
