/**
 * Exposes the HTTP API endpoint that starts a company news scrape.
 */
package com.datascraper.news.controller;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.news.service.NewsScraperService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScraperApiController {

    private final NewsScraperService newsScraperService;

    public ScraperApiController(NewsScraperService newsScraperService) {
        this.newsScraperService = newsScraperService;
    }

    @PostMapping("/scrape")
    public ScraperResult scrape(@RequestBody ScraperContext context) {
        return newsScraperService.scrape(context);
    }
}
