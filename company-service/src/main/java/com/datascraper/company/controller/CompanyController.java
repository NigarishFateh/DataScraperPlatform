/**
 * Exposes HTTP endpoints to search, create, update, and delete companies.
 */
package com.datascraper.company.controller;

import com.datascraper.common.dto.PageResponse;
import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.company.dto.CompanyPageResponse;
import com.datascraper.company.dto.CompanyResponse;
import com.datascraper.company.dto.CreateCompanyRequest;
import com.datascraper.company.dto.EnrichedCompanyUpsertResponse;
import com.datascraper.company.dto.UpdateCompanyRequest;
import com.datascraper.company.service.CompanyProfileService;
import com.datascraper.company.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyProfileService companyProfileService;

    public CompanyController(CompanyService companyService, CompanyProfileService companyProfileService) {
        this.companyService = companyService;
        this.companyProfileService = companyProfileService;
    }

    @GetMapping("/search")
    public CompanyPageResponse search(
            @RequestParam(required = false) List<String> cityIds,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) List<String> categoryIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int pageSize
    ) {
        return companyService.search(cityIds, search, categoryIds, page, pageSize);
    }

    @PostMapping("/enriched")
    @ResponseStatus(HttpStatus.OK)
    public EnrichedCompanyUpsertResponse upsertEnriched(
            @RequestHeader("X-Job-Id") UUID jobId,
            @RequestBody EnrichedCompany company
    ) {
        return companyProfileService.upsertEnriched(jobId, company);
    }

    @GetMapping("/by-job/{jobId}")
    public PageResponse<EnrichedCompany> findByJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int pageSize
    ) {
        return companyProfileService.findByJob(jobId, page, pageSize);
    }

    @GetMapping("/profiles/{id}")
    public EnrichedCompany getProfileById(@PathVariable String id) {
        return companyProfileService.getProfileById(id);
    }

    @GetMapping("/{id}")
    public CompanyResponse getById(@PathVariable String id) {
        return companyService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(@Valid @RequestBody CreateCompanyRequest request) {
        return companyService.create(request);
    }

    @PutMapping("/{id}")
    public CompanyResponse update(
            @PathVariable String id,
            @Valid @RequestBody UpdateCompanyRequest request
    ) {
        return companyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        companyService.delete(id);
    }
}
