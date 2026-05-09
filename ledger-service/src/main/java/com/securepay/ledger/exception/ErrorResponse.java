package com.securepay.ledger.exception;

import java.time.Instant;
import java.util.UUID;

/**
 * Structured error response returned to clients on any exception.
 * Includes a unique traceId for debugging and correlating errors in logs.
 */
public class ErrorResponse {
    private String errorCode;
    private String message;
    private int statusCode;
    private String timestamp;
    private String path;
    private String traceId;

    public ErrorResponse(String errorCode, String message, int statusCode, String path) {
        this.errorCode = errorCode;
        this.message = message;
        this.statusCode = statusCode;
        this.timestamp = Instant.now().toString();
        this.path = path;
        this.traceId = UUID.randomUUID().toString();
    }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
