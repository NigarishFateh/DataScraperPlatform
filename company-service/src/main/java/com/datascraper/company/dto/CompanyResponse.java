/**
 * Carries response data for a single company.
 */
package com.datascraper.company.dto;

import java.util.List;

public record CompanyResponse(
        String id,
        String name,
        String website,
        String industry,
        String cityId,
        String countryCode,
        List<String> categoryIds
) {
}
