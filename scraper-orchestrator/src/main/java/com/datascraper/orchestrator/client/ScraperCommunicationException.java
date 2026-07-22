package com.datascraper.orchestrator.client;

public class ScraperCommunicationException extends RuntimeException {

    private final int statusCode;

    public ScraperCommunicationException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public ScraperCommunicationException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ScraperCommunicationException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
