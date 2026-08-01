/**
 * Tests that company repository search paginates and filters correctly.
 */
package com.datascraper.company.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CompanyRepositorySearchTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void paginatesSearchResultsInDatabase() {
        CompanySearchPage page0 = companyRepository.search(
                List.of("DE-berlin"), "", List.of(), 0, 5);
        CompanySearchPage page1 = companyRepository.search(
                List.of("DE-berlin"), "", List.of(), 1, 5);

        assertThat(page0.total()).isGreaterThan(5);
        assertThat(page0.items()).hasSize(5);
        assertThat(page1.items()).isNotEmpty();
        assertThat(page0.items().get(0).getId()).isNotEqualTo(page1.items().get(0).getId());
    }

    @Test
    void filtersBySearchTerm() {
        CompanySearchPage baseline = companyRepository.search(
                List.of("DE-berlin"), "", List.of(), 0, 1);
        assertThat(baseline.items()).isNotEmpty();

        String term = baseline.items().get(0).getName().substring(0, 4).toLowerCase(Locale.ROOT);
        CompanySearchPage result = companyRepository.search(
                List.of("DE-berlin"), term, List.of(), 0, 20);

        assertThat(result.items()).isNotEmpty();
        assertThat(result.items()).allSatisfy(company ->
                assertThat(company.getName().toLowerCase(Locale.ROOT)).contains(term));
    }

    @Test
    void searchesByCategoryWithoutCityFilter() {
        CompanySearchPage result = companyRepository.search(
                List.of(), "", List.of("cat-software"), 0, 20);

        assertThat(result.total()).isGreaterThan(0);
        assertThat(result.items()).isNotEmpty();
        assertThat(result.items()).allSatisfy(company ->
                assertThat(company.getCategoryIds()).contains("cat-software"));
    }

    @Test
    void searchesAllWhenNoFiltersProvided() {
        CompanySearchPage result = companyRepository.search(List.of(), "", List.of(), 0, 5);

        assertThat(result.total()).isGreaterThan(5);
        assertThat(result.items()).hasSize(5);
    }
}
