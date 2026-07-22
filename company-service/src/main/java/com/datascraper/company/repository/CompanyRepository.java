package com.datascraper.company.repository;

import com.datascraper.company.domain.Company;
import com.datascraper.company.entity.CompanyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * PostgreSQL-backed company catalog (Phase 13).
 */
@Repository
public class CompanyRepository {

    private final CompanyJpaRepository companyJpaRepository;

    public CompanyRepository(CompanyJpaRepository companyJpaRepository) {
        this.companyJpaRepository = companyJpaRepository;
    }

    public List<Company> findAll() {
        return companyJpaRepository.findAll(Sort.by("name")).stream()
                .map(this::toDomain)
                .toList();
    }

    public Optional<Company> findById(String id) {
        return companyJpaRepository.findById(id).map(this::toDomain);
    }

    public Company save(Company company) {
        CompanyEntity entity = companyJpaRepository.findById(company.getId())
                .orElseGet(CompanyEntity::new);
        applyDomain(entity, company);
        return toDomain(companyJpaRepository.save(entity));
    }

    public void deleteById(String id) {
        companyJpaRepository.deleteById(id);
    }

    public long count() {
        return companyJpaRepository.count();
    }

    public CompanySearchPage search(
            List<String> cityIds,
            String search,
            List<String> categoryIds,
            int page,
            int pageSize
    ) {
        if (cityIds == null || cityIds.isEmpty()) {
            return new CompanySearchPage(List.of(), 0);
        }

        List<String> normalizedCityIds = cityIds.stream().map(String::trim).toList();
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<String> normalizedCategoryIds = categoryIds == null
                ? List.of()
                : categoryIds.stream().map(String::trim).filter(id -> !id.isBlank()).distinct().toList();
        boolean categoryFilter = !normalizedCategoryIds.isEmpty();

        Page<CompanyEntity> result = companyJpaRepository.search(
                normalizedCityIds,
                q,
                categoryFilter,
                categoryFilter ? normalizedCategoryIds : List.of(""),
                PageRequest.of(page, pageSize, Sort.by(Sort.Order.asc("name").ignoreCase()))
        );

        List<Company> items = result.getContent().stream().map(this::toDomain).toList();
        return new CompanySearchPage(items, result.getTotalElements());
    }

    private void applyDomain(CompanyEntity entity, Company company) {
        entity.setId(company.getId());
        entity.setName(company.getName());
        entity.setWebsite(company.getWebsite());
        entity.setIndustry(company.getIndustry());
        entity.setCityId(company.getCityId());
        entity.setCountryCode(company.getCountryCode());
        entity.setCategoryIds(company.getCategoryIds());
    }

    private Company toDomain(CompanyEntity entity) {
        return new Company(
                entity.getId(),
                entity.getName(),
                entity.getWebsite(),
                entity.getIndustry(),
                entity.getCityId(),
                entity.getCountryCode(),
                List.copyOf(entity.getCategoryIds())
        );
    }
}
