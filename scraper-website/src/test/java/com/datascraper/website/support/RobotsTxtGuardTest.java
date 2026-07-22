package com.datascraper.website.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RobotsTxtGuardTest {

    @Test
    void detectsDisallowedPathForStarAgent() {
        String robots = """
                User-agent: *
                Disallow: /private/
                Allow: /
                """;

        assertThat(RobotsTxtGuard.isPathDisallowed(robots, "/private/data")).isTrue();
        assertThat(RobotsTxtGuard.isPathDisallowed(robots, "/public")).isFalse();
    }
}
