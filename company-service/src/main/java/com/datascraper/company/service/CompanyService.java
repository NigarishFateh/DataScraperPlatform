package com.datascraper.company.service;

import com.datascraper.company.dto.CompanyPageResponse;
import com.datascraper.company.dto.CompanyResponse;
import com.datascraper.company.dto.CreateCompanyRequest;
import com.datascraper.company.dto.UpdateCompanyRequest;

import java.util.List;

public interface CompanyService {

    CompanyPageResponse search(
            List<String> cityIds,
            String search,
            List<String> categoryIds,
            int page,
            int pageSize
    );

    CompanyResponse getById(String id);

    CompanyResponse create(CreateCompanyRequest request);

    CompanyResponse update(String id, UpdateCompanyRequest request);

    void delete(String id);
}
