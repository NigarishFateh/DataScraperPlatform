/**
 * Enum of data categories that can be scraped (jobs, news, contacts, and more).
 */
package com.datascraper.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DataCategory {

    JOBS,
    PRODUCTS,
    SERVICES,
    COMPANY_INFO,
    CONTACTS,
    NEWS;

    @JsonCreator
    public static DataCategory fromValue(String value) {
        return DataCategory.valueOf(value.trim().toUpperCase().replace('-', '_'));
    }

    public static DataCategory fromPath(String value) {
        return fromValue(value);
    }

}
