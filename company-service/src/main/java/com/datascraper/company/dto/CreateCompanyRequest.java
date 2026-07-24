/**
 * Carries request data for creating a company.
 */
package com.datascraper.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateCompanyRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Pattern(regexp = "^https?://.+", message = "Website must start with http:// or https://")
        String website,
        @NotBlank @Size(max = 100) String industry,
        @NotBlank String cityId,
        @NotBlank @Size(min = 2, max = 2) String countryCode,
        @NotEmpty List<@NotBlank String> categoryIds
) {
}
