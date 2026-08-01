package com.datascraper.discovery.factory;

import com.datascraper.common.dto.discovery.DiscoveredCompany;
import com.datascraper.common.dto.discovery.DiscoveryRequest;
import com.datascraper.common.enums.DiscoveryProviderType;
import com.datascraper.common.provider.DiscoveryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryProviderFactoryTest {

    private DiscoveryProviderFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DiscoveryProviderFactory(new DiscoveryProviderRegistry(List.of(
                stubProvider(DiscoveryProviderType.CATALOG_SEED, "Catalog Seed", true),
                stubProvider(DiscoveryProviderType.OPEN_DATA, "Open Data Heuristics", true),
                stubProvider(DiscoveryProviderType.BUSINESS_DIRECTORY, "Business Directory", false)
        )));
    }

    @Test
    void returnsOnlyEnabledProvidersByDefault() {
        List<DiscoveryProvider> providers = factory.getEnabledProviders(null);

        assertThat(providers).hasSize(2);
        assertThat(providers).extracting(DiscoveryProvider::type)
                .containsExactlyInAnyOrder(
                        DiscoveryProviderType.CATALOG_SEED,
                        DiscoveryProviderType.OPEN_DATA
                );
    }

    @Test
    void filtersByRequestedProviderTypes() {
        List<DiscoveryProvider> providers = factory.getEnabledProviders(List.of("CATALOG_SEED"));

        assertThat(providers).hasSize(1);
        assertThat(providers.get(0).type()).isEqualTo(DiscoveryProviderType.CATALOG_SEED);
    }

    @Test
    void listProvidersIncludesDisabledFlag() {
        assertThat(factory.listProviders())
                .anyMatch(info -> info.type() == DiscoveryProviderType.BUSINESS_DIRECTORY && !info.enabled());
    }

    private static DiscoveryProvider stubProvider(
            DiscoveryProviderType type,
            String name,
            boolean enabled
    ) {
        return new DiscoveryProvider() {
            @Override
            public DiscoveryProviderType type() {
                return type;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public List<DiscoveredCompany> discover(DiscoveryRequest request) {
                return List.of(new DiscoveredCompany(
                        "ext-1",
                        "Acme",
                        "https://acme.example",
                        "DE",
                        "Berlin",
                        "DE-berlin",
                        List.of("cat-1"),
                        "https://acme.example",
                        name,
                        Map.of()
                ));
            }
        };
    }
}
