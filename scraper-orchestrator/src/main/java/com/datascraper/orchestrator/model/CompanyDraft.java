package com.datascraper.orchestrator.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CompanyDraft {

    private String id;
    private String name;
    private String category;
    private String industry;
    private String countryCode;
    private String countryName;
    private String state;
    private String city;
    private String website;
    private String email;
    private String phone;
    private String founder;
    private String ceo;
    private String description;
    private String services;
    private String products;
    private final List<String> technologyStack = new ArrayList<>();
    private String linkedIn;
    private String github;
    private String facebook;
    private String twitter;
    private String instagram;
    private String youtube;
    private Integer foundedYear;
    private String employeeCount;
    private String address;
    private String contactPage;
    private String sourceUrl;
    private Instant scrapedAt;
    private double confidenceScore;
    private String providerName;
    private final List<String> notes = new ArrayList<>();
    private final List<String> categoryIds = new ArrayList<>();
    private final Map<String, Object> rawAttributes = new LinkedHashMap<>();
    private String duplicateKey;
    private boolean incomplete;
    private boolean validationFailed;
    private int successfulProviderCount;
}
