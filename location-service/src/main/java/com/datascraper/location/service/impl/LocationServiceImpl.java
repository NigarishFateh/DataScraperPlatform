package com.datascraper.location.service.impl;

import com.datascraper.location.dto.CityResponse;
import com.datascraper.location.dto.CountryResponse;
import com.datascraper.location.exception.LocationNotFoundException;
import com.datascraper.location.repository.LocationRepository;
import com.datascraper.location.service.LocationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    public LocationServiceImpl(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Override
    public List<CountryResponse> listCountries() {
        return locationRepository.findAllCountries().stream()
                .map(country -> new CountryResponse(country.code(), country.name()))
                .toList();
    }

    @Override
    public List<CityResponse> listCities(String countryCode, String search) {
        if (locationRepository.findCountryByCode(countryCode).isEmpty()) {
            throw new LocationNotFoundException("Unknown country code: " + countryCode);
        }
        return locationRepository.findCitiesByCountry(countryCode, search).stream()
                .map(city -> new CityResponse(city.id(), city.name(), city.countryCode()))
                .toList();
    }
}
