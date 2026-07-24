/**
 * Implements company search and CRUD using the company repository.
 */
package com.datascraper.company.service.impl;

import com.datascraper.company.domain.Company;
import com.datascraper.company.dto.CompanyPageResponse;
import com.datascraper.company.dto.CompanyResponse;
import com.datascraper.company.dto.CreateCompanyRequest;
import com.datascraper.company.dto.UpdateCompanyRequest;
import com.datascraper.company.exception.CompanyNotFoundException;
import com.datascraper.company.repository.CompanyRepository;
import com.datascraper.company.repository.CompanySearchPage;
import com.datascraper.company.service.CompanyService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public CompanyPageResponse search(
            List<String> cityIds,
            String search,
            List<String> categoryIds,
            int page,
            int pageSize
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);

        CompanySearchPage result = companyRepository.search(cityIds, search, categoryIds, safePage, safeSize);
        long total = result.total();
        int end = safePage * safeSize + result.items().size();

        List<CompanyResponse> items = result.items().stream().map(this::toResponse).toList();

        return new CompanyPageResponse(
                items,
                safePage,
                safeSize,
                total,
                end < total
        );
    }

    @Override
    public CompanyResponse getById(String id) {
        return companyRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new CompanyNotFoundException(id));
    }

    @Override
    public CompanyResponse create(CreateCompanyRequest request) {
        Company company = new Company(
                "co-" + UUID.randomUUID(),
                request.name().trim(),
                request.website().trim(),
                request.industry().trim(),
                request.cityId().trim(),
                request.countryCode().trim().toUpperCase(Locale.ROOT),
                request.categoryIds()
        );
        return toResponse(companyRepository.save(company));
    }

    @Override
    public CompanyResponse update(String id, UpdateCompanyRequest request) {
        Company existing = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));

        existing.setName(request.name().trim());
        existing.setWebsite(request.website().trim());
        existing.setIndustry(request.industry().trim());
        existing.setCityId(request.cityId().trim());
        existing.setCountryCode(request.countryCode().trim().toUpperCase(Locale.ROOT));
        existing.setCategoryIds(request.categoryIds());

        return toResponse(companyRepository.save(existing));
    }

    @Override
    public void delete(String id) {
        if (companyRepository.findById(id).isEmpty()) {
            throw new CompanyNotFoundException(id);
        }
        companyRepository.deleteById(id);
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getWebsite(),
                company.getIndustry(),
                company.getCityId(),
                company.getCountryCode(),
                List.copyOf(company.getCategoryIds())
        );
    }
}
