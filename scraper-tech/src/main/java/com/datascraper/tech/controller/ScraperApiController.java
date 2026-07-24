/**
 * Exposes the HTTP API endpoint that starts a company tech stack scrape.
 */
package com.datascraper.tech.controller;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.tech.service.TechScraperService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScraperApiController {

    private final TechScraperService techScraperService;

    public ScraperApiController(TechScraperService techScraperService) {
        this.techScraperService = techScraperService;
    }

    @PostMapping("/scrape")
    public ScraperResult scrape(@RequestBody ScraperContext context) {
        return techScraperService.scrape(context);
    }
}
