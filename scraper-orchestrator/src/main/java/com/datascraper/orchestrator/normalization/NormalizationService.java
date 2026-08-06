package com.datascraper.orchestrator.normalization;

import com.datascraper.orchestrator.model.CompanyDraft;
import com.datascraper.common.support.CompanyEmailSupport;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NormalizationService {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NON_DIGIT = Pattern.compile("\\D+");
    private static final Pattern COUNTRY_CODE = Pattern.compile("^[A-Z]{2}$");
    private static final Map<String, String> TECH_CANONICAL = Map.ofEntries(
            Map.entry("postgresql", "PostgreSQL"),
            Map.entry("postgre", "PostgreSQL"),
            Map.entry("nodejs", "Node.js"),
            Map.entry("node.js", "Node.js"),
            Map.entry("reactjs", "React"),
            Map.entry("spring boot", "Spring Boot"),
            Map.entry("springboot", "Spring Boot")
    );

    public CompanyDraft normalize(CompanyDraft draft) {
        draft.setName(normalizeCompanyName(draft.getName()));
        draft.setCountryCode(normalizeCountryCode(draft.getCountryCode()));
        draft.setCity(normalizeCity(draft.getCity()));
        draft.setWebsite(normalizeWebsite(draft.getWebsite()));
        draft.setEmail(normalizeEmail(draft.getEmail()));
        draft.setPhone(normalizePhone(draft.getPhone()));
        draft.setLinkedIn(canonicalizeSocialUrl(draft.getLinkedIn(), "linkedin.com"));
        draft.setGithub(canonicalizeSocialUrl(draft.getGithub(), "github.com"));
        draft.setFacebook(canonicalizeSocialUrl(draft.getFacebook(), "facebook.com"));
        draft.setTwitter(canonicalizeSocialUrl(draft.getTwitter(), "twitter.com", "x.com"));
        draft.setInstagram(canonicalizeSocialUrl(draft.getInstagram(), "instagram.com"));
        draft.setYoutube(canonicalizeSocialUrl(draft.getYoutube(), "youtube.com"));
        draft.setContactPage(normalizeWebsite(draft.getContactPage()));
        normalizeTechnologyStackInPlace(draft.getTechnologyStack());
        draft.setDuplicateKey(buildDuplicateKey(draft));
        draft.setConfidenceScore(calculateConfidence(draft));
        return draft;
    }

    public String normalizeCompanyName(String name) {
        if (name == null) {
            return null;
        }
        return WHITESPACE.matcher(name.trim()).replaceAll(" ");
    }

    public String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        String normalized = countryCode.trim().toUpperCase(Locale.ROOT);
        return COUNTRY_CODE.matcher(normalized).matches() ? normalized : null;
    }

    public String normalizeCity(String city) {
        if (city == null || city.isBlank()) {
            return null;
        }
        String trimmed = WHITESPACE.matcher(city.trim()).replaceAll(" ");
        return toTitleCaseWords(trimmed);
    }

    public String normalizeWebsite(String website) {
        if (website == null || website.isBlank()) {
            return null;
        }
        String value = website.trim();
        if (!value.matches("(?i)^https?://.*")) {
            value = "https://" + value;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public String normalizeEmail(String email) {
        return CompanyEmailSupport.clean(email);
    }

    public String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = NON_DIGIT.matcher(phone).replaceAll("");
        if (digits.isEmpty()) {
            return null;
        }
        return "+" + digits;
    }

    public String canonicalizeSocialUrl(String url, String... allowedHosts) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String normalized = normalizeWebsite(url);
        if (normalized == null) {
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "";
            for (String allowed : allowedHosts) {
                if (host.contains(allowed)) {
                    return normalized;
                }
            }
        } catch (IllegalArgumentException ignored) {
            return normalized;
        }
        return normalized;
    }

    public void normalizeTechnologyStackInPlace(List<String> stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        List<String> normalized = normalizeTechnologyStack(stack);
        stack.clear();
        stack.addAll(normalized);
    }

    public List<String> normalizeTechnologyStack(List<String> stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String tech : stack) {
            if (tech == null || tech.isBlank()) {
                continue;
            }
            String trimmed = WHITESPACE.matcher(tech.trim()).replaceAll(" ");
            String canonical = TECH_CANONICAL.get(trimmed.toLowerCase(Locale.ROOT));
            normalized.add(canonical != null ? canonical : toTitleCaseWords(trimmed));
        }
        return new ArrayList<>(normalized);
    }

    public String buildDuplicateKey(CompanyDraft draft) {
        if (draft.getWebsite() != null && !draft.getWebsite().isBlank()) {
            return "website:" + draft.getWebsite();
        }
        if (draft.getName() != null && draft.getCountryCode() != null) {
            return "name-country:" + draft.getName().toLowerCase(Locale.ROOT) + "|" + draft.getCountryCode();
        }
        return null;
    }

    public double calculateConfidence(CompanyDraft draft) {
        int filled = 0;
        filled += isFilled(draft.getName()) ? 1 : 0;
        filled += isFilled(draft.getWebsite()) ? 1 : 0;
        filled += isFilled(draft.getCountryCode()) ? 1 : 0;
        filled += isFilled(draft.getCity()) ? 1 : 0;
        filled += isFilled(draft.getEmail()) ? 1 : 0;
        filled += isFilled(draft.getPhone()) ? 1 : 0;
        filled += isFilled(draft.getDescription()) ? 1 : 0;
        filled += draft.getTechnologyStack().isEmpty() ? 0 : 1;
        filled += isFilled(draft.getLinkedIn()) ? 1 : 0;

        double fieldScore = filled / 10.0;
        double providerScore = Math.min(0.35, draft.getSuccessfulProviderCount() * 0.07);
        return Math.min(0.99, Math.max(0.05, fieldScore * 0.65 + providerScore));
    }

    private boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }

    private String toTitleCaseWords(String value) {
        return WHITESPACE.splitAsStream(value)
                .map(word -> {
                    if (word.isEmpty()) {
                        return word;
                    }
                    if (word.length() == 1) {
                        return word.toUpperCase(Locale.ROOT);
                    }
                    return word.substring(0, 1).toUpperCase(Locale.ROOT)
                            + word.substring(1).toLowerCase(Locale.ROOT);
                })
                .collect(Collectors.joining(" "));
    }
}
