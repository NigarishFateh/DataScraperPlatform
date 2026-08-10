package com.datascraper.orchestrator.aggregation;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.provider.ProviderResult;
import com.datascraper.common.enums.ProviderExecutionStatus;
import com.datascraper.common.support.CompanyEmailSupport;
import com.datascraper.orchestrator.model.CompanyDraft;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
        applySeedLeadership(draft, seed);
        return draft;
    }

    /**
     * Custom scrape attaches CEO/founder via discovery metadata; fill blanks after website scrape.
     */
    private void applySeedLeadership(CompanyDraft draft, DiscoveredCompany seed) {
        if (seed == null || seed.metadata() == null || seed.metadata().isEmpty()) {
            return;
        }
        String founder = stringMeta(seed.metadata(), "founder");
        String ceo = stringMeta(seed.metadata(), "ceo");
        String leadershipName = stringMeta(seed.metadata(), "leadershipName");
        String leadershipTitle = stringMeta(seed.metadata(), "leadershipTitle");
        if (founder == null && leadershipName != null) {
            founder = leadershipTitle == null || leadershipTitle.isBlank()
                    ? leadershipName
                    : leadershipName + " (" + leadershipTitle + ")";
        }
        if (ceo == null && leadershipName != null) {
            String titleLower = leadershipTitle == null ? "" : leadershipTitle.toLowerCase(Locale.ROOT);
            if (titleLower.contains("ceo")
                    || titleLower.contains("chief executive")
                    || titleLower.contains("managing director")
                    || titleLower.contains("directeur")
                    || titleLower.isBlank()) {
                ceo = founder != null ? founder : leadershipName;
            }
        }
        if (isBlank(draft.getFounder()) && !isBlank(founder)) {
            draft.setFounder(founder);
        }
        if (isBlank(draft.getCeo()) && !isBlank(ceo)) {
            draft.setCeo(ceo);
        } else if (isBlank(draft.getCeo()) && !isBlank(founder)) {
            draft.setCeo(founder);
        }
    }

    private static String stringMeta(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
