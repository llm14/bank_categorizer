package com.bankcategorizer.exception;

/**
 * Thrown when a requested resource (e.g. a category or transaction) cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
