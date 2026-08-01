package com.datascraper.common.enums;

/**
 * @deprecated Prefer {@link ProviderType}. Kept for scraper HTTP compatibility.
 */
@Deprecated
public enum ScraperType {
    COMPANY_WEBSITE,
    TECHNOLOGY_STACK,
    NEWS,
    GITHUB,
    CONTACT,
    SOCIAL;

    public ProviderType toProviderType() {
        return switch (this) {
            case COMPANY_WEBSITE -> ProviderType.WEBSITE;
            case TECHNOLOGY_STACK -> ProviderType.TECHNOLOGY;
            case NEWS -> ProviderType.NEWS;
            case GITHUB -> ProviderType.GITHUB;
            case CONTACT -> ProviderType.CONTACT;
            case SOCIAL -> ProviderType.SOCIAL;
        };
    }

    public static ScraperType fromProviderType(ProviderType type) {
        return switch (type) {
            case WEBSITE -> COMPANY_WEBSITE;
            case TECHNOLOGY -> TECHNOLOGY_STACK;
            case NEWS -> NEWS;
            case GITHUB -> GITHUB;
            case CONTACT -> CONTACT;
            case SOCIAL -> SOCIAL;
        };
    }
}
