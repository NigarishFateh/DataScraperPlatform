/**
 * Holds one page of company search results and the total count.
 */
package com.datascraper.company.repository;

import com.datascraper.company.domain.Company;

import java.util.List;

public record CompanySearchPage(List<Company> items, long total) {
}
