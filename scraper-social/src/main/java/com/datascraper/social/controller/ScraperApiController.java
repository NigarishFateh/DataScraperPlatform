/**
 * Exposes the HTTP API endpoint that starts a company social scrape.
 */
package com.datascraper.social.controller;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.social.service.SocialScraperService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScraperApiController {

    private final SocialScraperService socialScraperService;

    public ScraperApiController(SocialScraperService socialScraperService) {
        this.socialScraperService = socialScraperService;
    }

    @PostMapping("/scrape")
    public ScraperResult scrape(@RequestBody ScraperContext context) {
        return socialScraperService.scrape(context);
    }
}
