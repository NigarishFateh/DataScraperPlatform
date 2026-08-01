package com.datascraper.job.exception;

public class InvalidJobStateException extends RuntimeException {

    public InvalidJobStateException(String message) {
        super(message);
    }
}
