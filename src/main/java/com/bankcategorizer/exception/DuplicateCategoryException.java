package com.bankcategorizer.exception;

/**
 * Thrown when attempting to create a category whose name already exists (case-insensitive).
 */
public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException(String message) {
        super(message);
    }
}
