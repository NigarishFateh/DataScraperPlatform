package com.datascraper.discovery.controller;

import com.datascraper.discovery.dto.LeadershipLookupResponse;
import com.datascraper.discovery.service.NlRestaurantLeadershipService;
import com.datascraper.discovery.support.NlRestaurantBrandSeed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Isolated leadership API for priority NL restaurant brands.
 * Does not alter category discovery / job enrichment flows.
 */
@RestController
@RequestMapping("/api/discovery/leadership")
public class LeadershipController {

    private final NlRestaurantLeadershipService leadershipService;

    public LeadershipController(NlRestaurantLeadershipService leadershipService) {
        this.leadershipService = leadershipService;
    }

    /**
     * Lookup CEO / director / manager signals for seeded NL restaurant brands.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code GET /api/discovery/leadership/nl-restaurants}</li>
     *   <li>{@code GET /api/discovery/leadership/nl-restaurants?companyNames=FEBO,Smullers}</li>
     * </ul>
     */
    @GetMapping("/nl-restaurants")
    public LeadershipLookupResponse lookupGet(
            @RequestParam(required = false) List<String> companyNames
    ) {
        return leadershipService.lookup(companyNames);
    }

    /**
     * Same lookup via POST body: {@code {"companyNames":["FEBO","KFC"]}}.
     * Omit body / empty list to use the full seed list.
     */
    @PostMapping("/nl-restaurants")
    public LeadershipLookupResponse lookupPost(
            @RequestBody(required = false) Map<String, Object> body
    ) {
        List<String> names = extractCompanyNames(body);
        return leadershipService.lookup(names);
    }

    @GetMapping("/nl-restaurants/brands")
    public Map<String, Object> brands() {
        return Map.of(
                "count", NlRestaurantBrandSeed.BRANDS.size(),
                "brands", NlRestaurantBrandSeed.BRANDS,
                "websites", NlRestaurantBrandSeed.brandWebsiteMap(),
                "tickers", NlRestaurantBrandSeed.brandTickerMap()
        );
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractCompanyNames(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return List.of();
        }
        Object value = body.get("companyNames");
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item != null && !item.toString().isBlank())
                    .map(Object::toString)
                    .toList();
        }
        if (value instanceof String text && !text.isBlank()) {
            return List.of(text);
        }
        return List.of();
    }
}
