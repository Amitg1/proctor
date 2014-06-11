package com.indeed.proctor.service.var;

/**
 * Thrown when a converter has some error during conversion that is unrecoverable.
 */
public class ConversionException extends Exception {
    public ConversionException(final String message) {
        super(message);
    }
}
