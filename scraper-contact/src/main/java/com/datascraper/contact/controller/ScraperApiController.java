package com.datascraper.contact.controller;

import com.datascraper.common.dto.ScraperContext;
import com.datascraper.common.dto.ScraperResult;
import com.datascraper.common.enums.ScraperType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScraperApiController {

    @PostMapping("/scrape")
    public ScraperResult scrape(@RequestBody ScraperContext context) {
        return ScraperResult.success(
                ScraperType.CONTACT,
                "Contact scraper stub ready (Phase 8 framework)",
                List.of(Map.of("websiteUrl", context.websiteUrl())),
                Map.of("phase", "8", "service", "scraper-contact")
        );
    }
}
