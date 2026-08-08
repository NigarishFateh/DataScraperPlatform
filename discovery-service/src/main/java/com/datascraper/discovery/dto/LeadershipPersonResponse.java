package com.datascraper.discovery.dto;

/**
 * One leadership hit for an isolated brand leadership lookup.
 */
public record LeadershipPersonResponse(
        String companyName,
        String website,
        String leaderName,
        String leadershipTitle,
        String source,
        boolean found,
        String ticker,
        Long compensation
) {
    public LeadershipPersonResponse(
            String companyName,
            String website,
            String leaderName,
            String leadershipTitle,
            String source,
            boolean found
    ) {
        this(companyName, website, leaderName, leadershipTitle, source, found, null, null);
    }
}
