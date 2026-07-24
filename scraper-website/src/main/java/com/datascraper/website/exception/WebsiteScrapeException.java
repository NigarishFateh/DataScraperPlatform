/**
 * Custom error thrown when a website scrape fails.
 */
package com.datascraper.website.exception;

public class WebsiteScrapeException extends RuntimeException {

    public WebsiteScrapeException(String message) {
        super(message);
    }

    public WebsiteScrapeException(String message, Throwable cause) {
        super(message, cause);
    }
}
