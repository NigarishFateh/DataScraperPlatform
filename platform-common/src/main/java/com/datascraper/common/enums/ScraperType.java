package com.datascraper.common.enums;

/**
 * @deprecated Prefer {@link ProviderType}. Kept for scraper HTTP compatibility.
 */
@Deprecated
public enum ScraperType {
    COMPANY_WEBSITE,
    CONTACT;

    public ProviderType toProviderType() {
        return switch (this) {
            case COMPANY_WEBSITE -> ProviderType.WEBSITE;
            case CONTACT -> ProviderType.CONTACT;
        };
    }

    public static ScraperType fromProviderType(ProviderType type) {
        return switch (type) {
            case WEBSITE -> COMPANY_WEBSITE;
            case CONTACT -> CONTACT;
        };
    }
}
