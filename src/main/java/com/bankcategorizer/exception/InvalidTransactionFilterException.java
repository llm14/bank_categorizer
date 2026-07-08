package com.bankcategorizer.exception;

/**
 * Thrown when listing transactions with an unsupported value for the {@code category}
 * query parameter.
 */
public class InvalidTransactionFilterException extends RuntimeException {

    public InvalidTransactionFilterException(String message) {
        super(message);
    }
}
