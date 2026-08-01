/**
 * Maps between enriched company DTOs and profile persistence entities.
 */
package com.datascraper.company.mapper;

import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.company.entity.CompanyContactEntity;
import com.datascraper.company.entity.CompanyLocationEntity;
import com.datascraper.company.entity.CompanyProfileEntity;
import com.datascraper.company.entity.CompanySocialEntity;
import com.datascraper.company.entity.CompanySourceEntity;
import com.datascraper.company.entity.CompanyTechnologyEntity;
import com.datascraper.company.util.ProfileNormalizationUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class CompanyProfileMapper {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public CompanyProfileMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void applyEnrichedCompany(
            CompanyProfileEntity entity,
            EnrichedCompany company,
            UUID jobId,
            String normalizedKey
    ) {
        entity.setJobId(jobId);
        entity.setName(trimToNull(company.name()));
        entity.setCategory(trimToNull(company.category()));
        entity.setIndustry(trimToNull(company.industry()));
        entity.setCountryCode(normalizeCountryCode(company.countryCode()));
        entity.setCountryName(trimToNull(company.countryName()));
        entity.setState(trimToNull(company.state()));
        entity.setCity(trimToNull(company.city()));
        entity.setWebsite(trimToNull(company.website()));
        entity.setDescription(trimToNull(company.description()));
        entity.setServices(trimToNull(company.services()));
        entity.setProducts(trimToNull(company.products()));
        entity.setFounder(trimToNull(company.founder()));
        entity.setCeo(trimToNull(company.ceo()));
        entity.setFoundedYear(company.foundedYear());
        entity.setEmployeeCount(trimToNull(company.employeeCount()));
        entity.setAddress(trimToNull(company.address()));
        entity.setContactPage(trimToNull(company.contactPage()));
        entity.setSourceUrl(trimToNull(company.sourceUrl()));
        entity.setConfidenceScore(company.confidenceScore());
        entity.setProviderName(trimToNull(company.providerName()));
        entity.setNotes(trimToNull(company.notes()));
        entity.setScrapedAt(company.scrapedAt());
        entity.setNormalizedKey(normalizedKey);
        entity.setCategoryIds(company.categoryIds() == null ? List.of() : company.categoryIds());

        entity.setContacts(buildContacts(entity, company));
        entity.setLocations(buildLocations(entity, company));
        entity.setTechnologies(buildTechnologies(entity, company.technologyStack()));
        entity.setSocials(buildSocials(entity, company));
        entity.setSources(buildSources(entity, company));
    }

    public EnrichedCompany toEnrichedCompany(CompanyProfileEntity entity) {
        Map<String, String> socials = mapSocials(entity.getSocials());
        CompanyContactEntity primaryContact = entity.getContacts().isEmpty()
                ? null
                : entity.getContacts().get(0);

        return new EnrichedCompany(
                entity.getId(),
                entity.getName(),
                entity.getCategory(),
                entity.getIndustry(),
                entity.getCountryCode(),
                entity.getCountryName(),
                entity.getState(),
                entity.getCity(),
                entity.getWebsite(),
                primaryContact == null ? null : primaryContact.getEmail(),
                primaryContact == null ? null : primaryContact.getPhone(),
                entity.getFounder(),
                entity.getCeo(),
                entity.getDescription(),
                entity.getServices(),
                entity.getProducts(),
                entity.getTechnologies().stream().map(CompanyTechnologyEntity::getName).toList(),
                socials.get("linkedin"),
                socials.get("github"),
                socials.get("facebook"),
                socials.get("twitter"),
                socials.get("instagram"),
                socials.get("youtube"),
                entity.getFoundedYear(),
                entity.getEmployeeCount(),
                entity.getAddress(),
                entity.getContactPage(),
                entity.getSourceUrl(),
                entity.getScrapedAt(),
                entity.getConfidenceScore() == null ? 0.0 : entity.getConfidenceScore(),
                entity.getProviderName(),
                entity.getNotes(),
                List.copyOf(entity.getCategoryIds()),
                readRawAttributes(entity)
        );
    }

    public String computeNormalizedKey(EnrichedCompany company) {
        return ProfileNormalizationUtil.normalizedKey(company.name(), company.website());
    }

    private List<CompanyContactEntity> buildContacts(CompanyProfileEntity profile, EnrichedCompany company) {
        if (isBlank(company.email()) && isBlank(company.phone()) && isBlank(company.contactPage())) {
            return List.of();
        }
        CompanyContactEntity contact = new CompanyContactEntity();
        contact.setId(nextChildId("ct"));
        contact.setEmail(trimToNull(company.email()));
        contact.setPhone(trimToNull(company.phone()));
        contact.setContactPage(trimToNull(company.contactPage()));
        contact.setProfile(profile);
        return List.of(contact);
    }

    private List<CompanyLocationEntity> buildLocations(CompanyProfileEntity profile, EnrichedCompany company) {
        if (isBlank(company.countryCode())
                && isBlank(company.state())
                && isBlank(company.city())
                && isBlank(company.address())) {
            return List.of();
        }
        CompanyLocationEntity location = new CompanyLocationEntity();
        location.setId(nextChildId("lo"));
        location.setCountryCode(normalizeCountryCode(company.countryCode()));
        location.setState(trimToNull(company.state()));
        location.setCity(trimToNull(company.city()));
        location.setAddress(trimToNull(company.address()));
        location.setProfile(profile);
        return List.of(location);
    }

    private List<CompanyTechnologyEntity> buildTechnologies(CompanyProfileEntity profile, List<String> stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        List<CompanyTechnologyEntity> technologies = new ArrayList<>();
        for (String name : stack) {
            if (isBlank(name)) {
                continue;
            }
            CompanyTechnologyEntity technology = new CompanyTechnologyEntity();
            technology.setId(nextChildId("te"));
            technology.setName(name.trim());
            technology.setProfile(profile);
            technologies.add(technology);
        }
        return technologies;
    }

    private List<CompanySocialEntity> buildSocials(CompanyProfileEntity profile, EnrichedCompany company) {
        List<CompanySocialEntity> socials = new ArrayList<>();
        addSocial(socials, profile, "linkedin", company.linkedIn());
        addSocial(socials, profile, "github", company.github());
        addSocial(socials, profile, "facebook", company.facebook());
        addSocial(socials, profile, "twitter", company.twitter());
        addSocial(socials, profile, "instagram", company.instagram());
        addSocial(socials, profile, "youtube", company.youtube());
        return socials;
    }

    private void addSocial(
            List<CompanySocialEntity> socials,
            CompanyProfileEntity profile,
            String platform,
            String url
    ) {
        if (isBlank(url)) {
            return;
        }
        CompanySocialEntity social = new CompanySocialEntity();
        social.setId(nextChildId("so"));
        social.setPlatform(platform);
        social.setUrl(url.trim());
        social.setProfile(profile);
        socials.add(social);
    }

    private List<CompanySourceEntity> buildSources(CompanyProfileEntity profile, EnrichedCompany company) {
        if (isBlank(company.sourceUrl())
                && isBlank(company.providerName())
                && (company.rawAttributes() == null || company.rawAttributes().isEmpty())) {
            return List.of();
        }
        CompanySourceEntity source = new CompanySourceEntity();
        source.setId(nextChildId("sr"));
        source.setProviderName(trimToNull(company.providerName()));
        source.setSourceUrl(trimToNull(company.sourceUrl()));
        source.setScrapedAt(company.scrapedAt());
        source.setRawJson(writeRawJson(company.rawAttributes()));
        source.setProfile(profile);
        return List.of(source);
    }

    private Map<String, String> mapSocials(List<CompanySocialEntity> socials) {
        Map<String, String> mapped = new LinkedHashMap<>();
        for (CompanySocialEntity social : socials) {
            mapped.put(social.getPlatform().toLowerCase(Locale.ROOT), social.getUrl());
        }
        return mapped;
    }

    private Map<String, Object> readRawAttributes(CompanyProfileEntity entity) {
        if (entity.getSources().isEmpty()) {
            return Map.of();
        }
        CompanySourceEntity latest = entity.getSources().get(entity.getSources().size() - 1);
        if (isBlank(latest.getRawJson())) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(latest.getRawJson(), MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of("raw", latest.getRawJson());
        }
    }

    private String writeRawJson(Map<String, Object> rawAttributes) {
        if (rawAttributes == null || rawAttributes.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(rawAttributes);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String normalizeCountryCode(String countryCode) {
        if (isBlank(countryCode)) {
            return null;
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nextChildId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
