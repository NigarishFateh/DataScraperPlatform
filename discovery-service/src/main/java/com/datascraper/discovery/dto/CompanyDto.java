package com.datascraper.discovery.dto;

import java.util.List;

public record CompanyDto(
        String id,
        String name,
        String website,
        String industry,
        String cityId,
        String countryCode,
        List<String> categoryIds
) {
}
