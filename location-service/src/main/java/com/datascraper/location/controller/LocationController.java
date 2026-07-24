/**
 * Exposes HTTP endpoints to list countries and cities.
 */
package com.datascraper.location.controller;

import com.datascraper.location.dto.CityResponse;
import com.datascraper.location.dto.CountryResponse;
import com.datascraper.location.service.LocationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/countries")
    public List<CountryResponse> countries() {
        return locationService.listCountries();
    }

    @GetMapping("/cities")
    public List<CityResponse> cities(
            @RequestParam String countryCode,
            @RequestParam(required = false, defaultValue = "") String search
    ) {
        return locationService.listCities(countryCode, search);
    }
}
