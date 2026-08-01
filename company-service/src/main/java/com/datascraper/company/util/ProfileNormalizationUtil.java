/**
 * Builds stable dedupe keys for enriched company profiles within a job.
 */
package com.datascraper.company.util;

import java.util.Locale;

public final class ProfileNormalizationUtil {

    private ProfileNormalizationUtil() {
    }

    public static String normalizedKey(String name, String website) {
        String normalizedWebsite = normalizeWebsite(website);
        if (!normalizedWebsite.isBlank()) {
            return normalizedWebsite;
        }
        return normalizeName(name);
    }

    static String normalizeWebsite(String website) {
        if (website == null || website.isBlank()) {
            return "";
        }
        String value = website.trim().toLowerCase(Locale.ROOT);
        value = value.replaceFirst("^https?://", "");
        value = value.replaceFirst("^www\\.", "");
        value = value.replaceAll("/+$", "");
        return value;
    }

    static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }
}
