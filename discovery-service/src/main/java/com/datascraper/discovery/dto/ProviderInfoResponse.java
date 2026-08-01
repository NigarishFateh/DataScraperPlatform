package com.datascraper.discovery.dto;

import com.datascraper.common.enums.DiscoveryProviderType;

public record ProviderInfoResponse(
        DiscoveryProviderType type,
        String name,
        boolean enabled
) {
}
