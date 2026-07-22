package com.datascraper.company.exception;

public class CompanyNotFoundException extends RuntimeException {

    public CompanyNotFoundException(String id) {
        super("Company not found: " + id);
    }
}
