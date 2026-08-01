package com.datascraper.common.provider;

import com.datascraper.common.dto.provider.ProviderContext;
import com.datascraper.common.dto.provider.ProviderResult;
import com.datascraper.common.enums.ProviderType;

/**
 * Plugin contract for enriching an already-discovered company.
 * Providers execute independently and concurrently.
 */
public interface CompanyDataProvider {

    ProviderType type();

    String name();

    boolean enabled();

    ProviderResult enrich(ProviderContext context);
}
