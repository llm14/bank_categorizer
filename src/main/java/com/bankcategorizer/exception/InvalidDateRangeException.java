package com.bankcategorizer.exception;

/**
 * Thrown when a spending query's {@code from}/{@code to} date range is missing or invalid
 * (e.g. {@code from} is after {@code to}).
 */
public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException(String message) {
        super(message);
    }
}
