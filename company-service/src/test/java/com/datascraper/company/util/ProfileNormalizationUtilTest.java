/**
 * Unit tests for profile normalization key generation.
 */
package com.datascraper.company.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileNormalizationUtilTest {

    @Test
    void prefersNormalizedWebsiteForDedupeKey() {
        String key = ProfileNormalizationUtil.normalizedKey(
                "Acme Global Inc",
                "https://www.acme-global.com/"
        );
        assertThat(key).isEqualTo("acme-global.com");
    }

    @Test
    void fallsBackToNormalizedNameWhenWebsiteMissing() {
        String key = ProfileNormalizationUtil.normalizedKey("Acme Global Inc", null);
        assertThat(key).isEqualTo("acmeglobalinc");
    }
}
