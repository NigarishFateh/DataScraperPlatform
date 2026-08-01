/**
 * Implements country and city listing using the location repository.
 */
package com.datascraper.location.service.impl;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.location.domain.Country;
import com.datascraper.location.dto.CityResponse;
import com.datascraper.location.dto.CountryResponse;
import com.datascraper.location.exception.LocationNotFoundException;
import com.datascraper.location.repository.LocationRepository;
import com.datascraper.location.service.LocationService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    public LocationServiceImpl(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Override
    public PageResponse<CountryResponse> searchCountries(String search, int page, int pageSize) {
        int safePage = Math.max(page, 0);
        int safePageSize = Math.min(Math.max(pageSize, 1), 250);
        Page<Country> result =
                locationRepository.searchCountries(search, safePage, safePageSize);
        List<CountryResponse> items = result.getContent().stream()
                .map(country -> new CountryResponse(country.code(), country.name()))
                .toList();
        return PageResponse.of(items, safePage, safePageSize, result.getTotalElements());
    }

    @Override
    public List<CityResponse> listCities(String countryCode, String search) {
        String normalizedCode = countryCode == null ? "" : countryCode.trim();
        String normalizedSearch = search == null ? "" : search.trim();

        if (!normalizedCode.isEmpty()
                && locationRepository.findCountryByCode(normalizedCode).isEmpty()) {
            throw new LocationNotFoundException("Unknown country code: " + normalizedCode);
        }

        return locationRepository.findCities(normalizedCode, normalizedSearch).stream()
                .map(city -> new CityResponse(city.id(), city.name(), city.countryCode()))
                .toList();
    }
}
