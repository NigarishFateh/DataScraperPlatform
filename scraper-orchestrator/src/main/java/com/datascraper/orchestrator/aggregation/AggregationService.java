package com.datascraper.orchestrator.aggregation;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.provider.ProviderResult;
import com.datascraper.common.enums.ProviderExecutionStatus;
import com.datascraper.common.support.CompanyEmailSupport;
import com.datascraper.orchestrator.model.CompanyDraft;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AggregationService {

    public CompanyDraft aggregate(DiscoveredCompany seed, List<ProviderResult> providerResults) {
        CompanyDraft draft = new CompanyDraft();
        draft.setId(seed.externalId());
        draft.setName(seed.name());
        draft.setWebsite(seed.website());
        draft.setCountryCode(seed.countryCode());
        draft.setCity(seed.cityName());
        draft.setSourceUrl(seed.sourceUrl());
        draft.setProviderName(seed.providerName());
        draft.getCategoryIds().addAll(seed.categoryIds());
        draft.setScrapedAt(Instant.now());

        if (seed.metadata() != null && !seed.metadata().isEmpty()) {
            draft.getRawAttributes().putAll(seed.metadata());
        }

        int successCount = 0;
        for (ProviderResult result : providerResults) {
            if (result.status() == ProviderExecutionStatus.SUCCESS) {
                successCount++;
            }
            mergeProviderResult(draft, result);
        }
        draft.setSuccessfulProviderCount(successCount);
        return draft;
    }

    private void mergeProviderResult(CompanyDraft draft, ProviderResult result) {
        if (result.items() == null) {
            return;
        }
        for (Map<String, Object> item : result.items()) {
            mergeItem(draft, result, item);
        }
        if (result.metadata() != null) {
            draft.getRawAttributes().put(result.providerType().name(), result.metadata());
        }
    }

    private void mergeItem(CompanyDraft draft, ProviderResult result, Map<String, Object> item) {
        String field = stringValue(item.get("field"));
        String section = stringValue(item.get("section"));
        String value = firstNonBlank(
                stringValue(item.get("value")),
                stringValue(item.get("title")),
                stringValue(item.get("description")),
                stringValue(item.get("url")),
                stringValue(item.get("profileUrl"))
        );

        if (field == null && value == null) {
            return;
        }

        switch (result.providerType()) {
            case WEBSITE -> mergeWebsiteField(draft, field, value, item);
            case CONTACT -> mergeContactField(draft, field, value, item);
            case GITHUB -> mergeGitHubField(draft, item);
            case TECHNOLOGY -> mergeTechnologyField(draft, field, value);
            case NEWS -> mergeNewsField(draft, item);
            case SOCIAL -> mergeSocialField(draft, field, value, item);
            default -> draft.getRawAttributes().put(field != null ? field : section, value);
        }
    }

    private void mergeWebsiteField(CompanyDraft draft, String field, String value, Map<String, Object> item) {
        if (field == null) {
            return;
        }
        switch (field) {
            case "pageTitle", "ogTitle", "ogSiteName" -> prefer(draft::getName, draft::setName, value, 0.5);
            case "metaDescription", "ogDescription", "paragraph" -> preferLonger(draft::getDescription, draft::setDescription, value);
            case "email" -> preferEmail(draft, value);
            case "phone" -> prefer(draft::getPhone, draft::setPhone, value, 0.6);
            case "founder" -> prefer(draft::getFounder, draft::setFounder, value, 0.8);
            case "ceo" -> {
                prefer(draft::getCeo, draft::setCeo, value, 0.8);
                prefer(draft::getFounder, draft::setFounder, value, 0.55);
            }
            case "linkedin" -> prefer(draft::getLinkedIn, draft::setLinkedIn, stringValue(item.get("url")), 0.7);
            case "github" -> prefer(draft::getGithub, draft::setGithub, stringValue(item.get("url")), 0.7);
            case "twitter" -> prefer(draft::getTwitter, draft::setTwitter, stringValue(item.get("url")), 0.7);
            case "facebook" -> prefer(draft::getFacebook, draft::setFacebook, stringValue(item.get("url")), 0.7);
            case "youtube" -> prefer(draft::getYoutube, draft::setYoutube, stringValue(item.get("url")), 0.7);
            case "service-or-product-link" -> appendCsv(draft::getServices, draft::setServices, stringValue(item.get("title")));
            case "careers-link" -> draft.getRawAttributes().putIfAbsent("careersUrl", stringValue(item.get("url")));
            case "canonicalUrl" -> prefer(draft::getWebsite, draft::setWebsite, value, 0.8);
            default -> draft.getRawAttributes().put(field, value);
        }
    }

    private void mergeContactField(CompanyDraft draft, String field, String value, Map<String, Object> item) {
        if (field == null || value == null) {
            return;
        }
        switch (field) {
            case "email" -> preferEmail(draft, value);
            case "phone" -> prefer(draft::getPhone, draft::setPhone, value, 0.75);
            case "address" -> preferLonger(draft::getAddress, draft::setAddress, value);
            default -> draft.getRawAttributes().put(field, value);
        }
        String sourceUrl = stringValue(item.get("sourceUrl"));
        if (sourceUrl != null && sourceUrl.toLowerCase(Locale.ROOT).contains("contact")) {
            prefer(draft::getContactPage, draft::setContactPage, sourceUrl, 0.7);
        }
    }

    private void preferEmail(CompanyDraft draft, String candidate) {
        String chosen = CompanyEmailSupport.prefer(draft.getEmail(), candidate, draft.getWebsite());
        if (chosen != null) {
            draft.setEmail(chosen);
        }
    }

    private void mergeGitHubField(CompanyDraft draft, Map<String, Object> item) {
        String profileUrl = stringValue(item.get("profileUrl"));
        if (profileUrl != null) {
            prefer(draft::getGithub, draft::setGithub, profileUrl, 0.85);
        }
    }

    private void mergeTechnologyField(CompanyDraft draft, String field, String value) {
        String tech = value != null ? value : field;
        if (tech != null && !tech.isBlank()) {
            addUnique(draft.getTechnologyStack(), tech);
        }
    }

    private void mergeNewsField(CompanyDraft draft, Map<String, Object> item) {
        @SuppressWarnings("unchecked")
        List<String> headlines = (List<String>) draft.getRawAttributes()
                .computeIfAbsent("newsHeadlines", key -> new ArrayList<String>());
        String title = stringValue(item.get("title"));
        if (title != null) {
            headlines.add(title);
        }
    }

    private void mergeSocialField(CompanyDraft draft, String field, String value, Map<String, Object> item) {
        String url = firstNonBlank(value, stringValue(item.get("url")), stringValue(item.get("profileUrl")));
        if (field == null || url == null) {
            return;
        }
        switch (field.toLowerCase(Locale.ROOT)) {
            case "linkedin" -> prefer(draft::getLinkedIn, draft::setLinkedIn, url, 0.8);
            case "github" -> prefer(draft::getGithub, draft::setGithub, url, 0.8);
            case "twitter", "x" -> prefer(draft::getTwitter, draft::setTwitter, url, 0.8);
            case "facebook" -> prefer(draft::getFacebook, draft::setFacebook, url, 0.8);
            case "instagram" -> prefer(draft::getInstagram, draft::setInstagram, url, 0.8);
            case "youtube" -> prefer(draft::getYoutube, draft::setYoutube, url, 0.8);
            default -> draft.getRawAttributes().put(field, url);
        }
    }

    private interface Getter {
        String get();
    }

    private interface Setter {
        void set(String value);
    }

    private void prefer(Getter getter, Setter setter, String value, double ignoredConfidence) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (getter.get() == null || getter.get().isBlank()) {
            setter.set(value);
        }
    }

    private void preferLonger(Getter getter, Setter setter, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (getter.get() == null || value.length() > getter.get().length()) {
            setter.set(value);
        }
    }

    private void appendCsv(Getter getter, Setter setter, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (getter.get() == null || getter.get().isBlank()) {
            setter.set(value);
            return;
        }
        Set<String> parts = new LinkedHashSet<>(List.of(getter.get().split(",")));
        parts.add(value.trim());
        setter.set(parts.stream().filter(s -> !s.isBlank()).collect(Collectors.joining(", ")));
    }

    private void addUnique(List<String> target, String value) {
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return;
        }
        boolean exists = target.stream().anyMatch(existing -> existing.equalsIgnoreCase(normalized));
        if (!exists) {
            target.add(normalized);
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
