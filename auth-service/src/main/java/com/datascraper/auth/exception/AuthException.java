/**
 * Exception thrown when authentication fails.
 */
package com.datascraper.auth.exception;

public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }
}
