package com.homegenie.marketplaceservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to create a duplicate review for a booking.
 * Returns HTTP 409 Conflict status.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateReviewException extends RuntimeException {
    
    public DuplicateReviewException(String message) {
        super(message);
    }
    
    public DuplicateReviewException(String message, Throwable cause) {
        super(message, cause);
    }
}
