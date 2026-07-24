/**
 * Runtime exception used when a scrape coordination step fails.
 */
package com.datascraper.orchestrator.exception;

public class ScraperException extends RuntimeException {

    public ScraperException(String message) {
        super(message);
    }

    public ScraperException(String message, Throwable cause) {
        super(message, cause);
    }

}
