package com.datascraper.discovery.support;

import java.util.Locale;
import java.util.Set;

/**
 * Detects non-company URLs (maps, directories, social) that should not be stored as websites.
 */
public final class WebsiteUrlSupport {

    private static final Set<String> BLOCKED_HOST_FRAGMENTS = Set.of(
            "google.com/maps",
            "maps.google.",
            "goo.gl/maps",
            "maps.app.goo.gl",
            "openstreetmap.org",
            "osm.org",
            "facebook.com",
            "instagram.com",
            "linkedin.com",
            "twitter.com",
            "x.com",
            "youtube.com",
            "crunchbase.com",
            "yelp.com",
            "tripadvisor."
    );

    private WebsiteUrlSupport() {
    }

    public static boolean isUsableCompanyWebsite(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        if (isMapOrDirectoryUrl(normalized)) {
            return false;
        }
        try {
            String host = java.net.URI.create(normalized).getHost();
            return host != null && host.contains(".");
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isMapOrDirectoryUrl(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        for (String fragment : BLOCKED_HOST_FRAGMENTS) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    public static String normalizeHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String value = url.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "https://" + value;
        }
        return value;
    }

    /**
     * Collapse store-locator / branch paths to the brand origin so custom scrape
     * does not fetch a unique URL (and robots.txt) per location.
     */
    public static String brandHomepageUrl(String url) {
        String normalized = normalizeHttpUrl(url);
        if (normalized.isBlank() || !isUsableCompanyWebsite(normalized)) {
            return normalized;
        }
        try {
            java.net.URI uri = java.net.URI.create(normalized);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return normalized;
            }
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            boolean storePath = path.contains("/vestigingen")
                    || path.contains("/locations")
                    || path.contains("/location/")
                    || path.contains("/stores")
                    || path.contains("/store/")
                    || path.contains("/filialen")
                    || path.contains("/restaurants")
                    || path.contains("/restaurant/")
                    || path.contains("/finder")
                    || path.contains("/winkel")
                    || path.contains("/shops");
            if (!storePath) {
                return normalized;
            }
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme();
            return scheme + "://" + host;
        } catch (Exception ex) {
            return normalized;
        }
    }
}
