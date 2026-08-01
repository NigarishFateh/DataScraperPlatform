/**
 * Exception thrown when a category cannot be found.
 */
package com.datascraper.category.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(String message) {
        super(message);
    }
}
