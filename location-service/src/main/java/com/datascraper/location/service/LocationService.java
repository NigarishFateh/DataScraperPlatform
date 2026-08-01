/**
 * Service contract for listing countries and cities.
 */
package com.datascraper.location.service;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.location.dto.CityResponse;
import com.datascraper.location.dto.CountryResponse;

import java.util.List;

public interface LocationService {

    PageResponse<CountryResponse> searchCountries(String search, int page, int pageSize);

    List<CityResponse> listCities(String countryCode, String search);
}
