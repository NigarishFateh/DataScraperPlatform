/**
 * Service contract for listing countries and cities.
 */
package com.datascraper.location.service;

import com.datascraper.location.dto.CityResponse;
import com.datascraper.location.dto.CountryResponse;

import java.util.List;

public interface LocationService {

    List<CountryResponse> listCountries();

    List<CityResponse> listCities(String countryCode, String search);
}
