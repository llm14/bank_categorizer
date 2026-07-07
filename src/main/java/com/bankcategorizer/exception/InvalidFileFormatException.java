package com.bankcategorizer.exception;

/**
 * Thrown when an uploaded file cannot be processed: unsupported file type,
 * unreadable content, or a missing required column.
 */
public class InvalidFileFormatException extends RuntimeException {

    public InvalidFileFormatException(String message) {
        super(message);
    }

    public InvalidFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
