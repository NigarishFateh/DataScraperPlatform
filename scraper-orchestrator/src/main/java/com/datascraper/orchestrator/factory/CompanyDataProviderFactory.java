package com.datascraper.orchestrator.factory;

import com.datascraper.common.dto.provider.ProviderContext;
import com.datascraper.common.enums.ProviderType;
import com.datascraper.common.provider.CompanyDataProvider;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class CompanyDataProviderFactory {

    private final CompanyDataProviderRegistry registry;

    public CompanyDataProviderFactory(CompanyDataProviderRegistry registry) {
        this.registry = registry;
    }

    public List<CompanyDataProvider> resolve(ProviderContext context, List<String> requestedProviders) {
        List<CompanyDataProvider> enabled = registry.all().stream()
                .filter(CompanyDataProvider::enabled)
                .toList();

        if (requestedProviders == null || requestedProviders.isEmpty()) {
            return enabled;
        }

        boolean anyEnrichmentMatch = requestedProviders.stream()
                .anyMatch(name -> resolveProviderType(name).isPresent());

        if (!anyEnrichmentMatch) {
            return enabled;
        }

        return enabled.stream()
                .filter(provider -> matchesRequested(provider.type(), requestedProviders))
                .toList();
    }

    private boolean matchesRequested(ProviderType type, List<String> requestedProviders) {
        for (String requested : requestedProviders) {
            if (requested == null || requested.isBlank()) {
                continue;
            }
            var resolved = resolveProviderType(requested);
            if (resolved.isPresent() && resolved.get() == type) {
                return true;
            }
        }
        return false;
    }

    private java.util.Optional<ProviderType> resolveProviderType(String name) {
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(ProviderType.values())
                .filter(type -> type.name().equals(normalized)
                        || aliases(type).contains(normalized))
                .findFirst();
    }

    private List<String> aliases(ProviderType type) {
        return switch (type) {
            case WEBSITE -> List.of("COMPANY_WEBSITE", "WEB");
            case CONTACT -> List.of("CONTACT_INFO");
        };
    }
}
