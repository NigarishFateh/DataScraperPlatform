package com.datascraper.website.controller;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.website.service.WebsiteScraperService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScraperApiController {

    private final WebsiteScraperService websiteScraperService;

    public ScraperApiController(WebsiteScraperService websiteScraperService) {
        this.websiteScraperService = websiteScraperService;
    }

    @PostMapping("/scrape")
    public ScraperResult scrape(@RequestBody ScraperContext context) {
        return websiteScraperService.scrape(context);
    }
}
