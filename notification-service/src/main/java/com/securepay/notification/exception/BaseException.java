package com.securepay.notification.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all custom exceptions in notification-service.
 * Provides a common structure with HTTP status and error code.
 */
public abstract class BaseException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected BaseException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
