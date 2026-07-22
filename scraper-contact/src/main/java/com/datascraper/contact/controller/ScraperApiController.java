package com.datascraper.contact.controller;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.contact.service.ContactScraperService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScraperApiController {

    private final ContactScraperService contactScraperService;

    public ScraperApiController(ContactScraperService contactScraperService) {
        this.contactScraperService = contactScraperService;
    }

    @PostMapping("/scrape")
    public ScraperResult scrape(@RequestBody ScraperContext context) {
        return contactScraperService.scrape(context);
    }
}
