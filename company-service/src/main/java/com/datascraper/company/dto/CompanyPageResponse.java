package com.datascraper.company.dto;

import java.util.List;

public record CompanyPageResponse(
        List<CompanyResponse> items,
        int page,
        int pageSize,
        long total,
        boolean hasMore
) {
}
