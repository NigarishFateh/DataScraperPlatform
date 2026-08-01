package com.datascraper.discovery.service;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CompanyDeduplicationService {

    public List<DiscoveredCompany> deduplicate(List<DiscoveredCompany> companies) {
        Set<String> seen = new LinkedHashSet<>();
        List<DiscoveredCompany> unique = new ArrayList<>();

        for (DiscoveredCompany company : companies) {
            String key = dedupeKey(company);
            if (seen.add(key)) {
                unique.add(company);
            }
        }

        return unique;
    }

    String dedupeKey(DiscoveredCompany company) {
        if (company.website() != null && !company.website().isBlank()) {
            return "w:" + normalizeWebsite(company.website());
        }
        String name = company.name() == null ? "" : company.name().trim().toLowerCase(Locale.ROOT);
        String country = company.countryCode() == null ? "" : company.countryCode().trim().toUpperCase(Locale.ROOT);
        return "n:" + name + "|" + country;
    }

    private String normalizeWebsite(String website) {
        String normalized = website.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceFirst("^https?://", "");
        normalized = normalized.replaceFirst("^www\\.", "");
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
