/**
 * Exception thrown when an enriched company profile id cannot be found.
 */
package com.datascraper.company.exception;

public class CompanyProfileNotFoundException extends RuntimeException {

    public CompanyProfileNotFoundException(String id) {
        super("Company profile not found: " + id);
    }
}
