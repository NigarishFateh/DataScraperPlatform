package com.datascraper.github.controller;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.github.service.GitHubScraperService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScraperApiController {

    private final GitHubScraperService gitHubScraperService;

    public ScraperApiController(GitHubScraperService gitHubScraperService) {
        this.gitHubScraperService = gitHubScraperService;
    }

    @PostMapping("/scrape")
    public ScraperResult scrape(@RequestBody ScraperContext context) {
        return gitHubScraperService.scrape(context);
    }
}
