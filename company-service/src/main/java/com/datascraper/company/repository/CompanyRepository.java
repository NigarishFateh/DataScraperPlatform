package com.datascraper.company.repository;

import com.datascraper.company.domain.Company;
import com.datascraper.company.entity.CompanyEntity;
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

    public List<Company> search(List<String> cityIds, String search) {
        if (cityIds == null || cityIds.isEmpty()) {
            return List.of();
        }
        List<String> normalizedCityIds = cityIds.stream().map(String::trim).toList();
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        return companyJpaRepository.search(normalizedCityIds, q).stream()
                .map(this::toDomain)
                .toList();
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
