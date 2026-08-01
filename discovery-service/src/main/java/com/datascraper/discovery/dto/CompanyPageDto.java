package com.datascraper.discovery.dto;

import java.util.List;

public record CompanyPageDto(
        List<CompanyDto> items,
        int page,
        int pageSize,
        long total,
        boolean hasMore
) {
}
