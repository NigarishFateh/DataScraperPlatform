/**
 * Lists the Google scrape categories such as jobs, products, and news.
 */
package com.datascraper.google.model;

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
