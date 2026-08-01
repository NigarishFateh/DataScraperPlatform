package com.datascraper.discovery.dto;

import com.datascraper.common.dto.discovery.DiscoveredCompany;

import java.util.List;

public record DiscoveryRunResponse(
        String jobId,
        int discoveredCount,
        int enqueuedCount,
        List<DiscoveredCompany> companies
) {
}
