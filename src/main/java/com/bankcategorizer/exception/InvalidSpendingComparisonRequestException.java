package com.bankcategorizer.exception;

/**
 * Thrown when a spending comparison query's {@code period} or {@code lookback} parameters
 * are missing or invalid (e.g. an unsupported period granularity, or a non-positive lookback).
 */
public class InvalidSpendingComparisonRequestException extends RuntimeException {

    public InvalidSpendingComparisonRequestException(String message) {
        super(message);
    }
}
