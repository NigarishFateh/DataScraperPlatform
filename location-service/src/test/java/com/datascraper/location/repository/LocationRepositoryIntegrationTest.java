/**
 * Tests that the location repository loads the seeded country and city catalog.
 */
package com.datascraper.location.repository;

import com.datascraper.location.domain.City;
import com.datascraper.location.domain.Country;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LocationRepositoryIntegrationTest {

    @Autowired
    private LocationRepository locationRepository;

    @Test
    void loadsSeededEuropeanCatalog() {
        List<Country> countries = locationRepository.findAllCountries();
        assertThat(countries).hasSize(15);
        assertThat(countries).extracting(Country::code).contains("DE", "FR", "CH");

        List<City> berlinCities = locationRepository.findCitiesByCountry("DE", "ber");
        assertThat(berlinCities).extracting(City::id).contains("DE-berlin");
    }
}
