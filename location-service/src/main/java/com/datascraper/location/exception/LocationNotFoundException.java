/**
 * Exception thrown when a location cannot be found.
 */
package com.datascraper.location.exception;

public class LocationNotFoundException extends RuntimeException {

    public LocationNotFoundException(String message) {
        super(message);
    }
}
