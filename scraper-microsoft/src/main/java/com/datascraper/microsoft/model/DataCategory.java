package com.datascraper.microsoft.model;

public enum DataCategory {

    JOBS,
    PRODUCTS,
    SERVICES,
    COMPANY_INFO,
    CONTACTS,
    NEWS;

    public static DataCategory fromPath(String value) {
        return DataCategory.valueOf(value.trim().toUpperCase().replace('-', '_'));
    }

}
