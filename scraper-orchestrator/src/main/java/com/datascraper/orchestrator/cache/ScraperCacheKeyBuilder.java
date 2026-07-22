package com.datascraper.orchestrator.cache;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.enums.ScraperType;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Builds stable Redis keys for scraper results.
 */
public final class ScraperCacheKeyBuilder {

    private ScraperCacheKeyBuilder() {
    }

    public static String build(String keyPrefix, ScraperType scraperType, ScraperContext context) {
        String companyId = context.companyId() != null ? context.companyId() : "unknown";
        String urlHash = hashUrl(context.websiteUrl());
        return "%s:%s:%s:%s".formatted(keyPrefix, scraperType.name(), companyId, urlHash);
    }

    static String normalizeUrl(String websiteUrl) {
        if (websiteUrl == null || websiteUrl.isBlank()) {
            return "none";
        }
        String trimmed = websiteUrl.trim();
        String withScheme = trimmed.contains("://") ? trimmed : "https://" + trimmed;
        try {
            URI uri = URI.create(withScheme);
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : trimmed.toLowerCase();
            String path = uri.getPath() != null ? uri.getPath() : "";
            if ("/".equals(path)) {
                path = "";
            } else if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return host + path;
        } catch (IllegalArgumentException ex) {
            return trimmed.toLowerCase();
        }
    }

    private static String hashUrl(String websiteUrl) {
        String normalized = normalizeUrl(websiteUrl);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(normalized.hashCode());
        }
    }
}
