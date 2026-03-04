package com.bakerymanager.smartbill.invoicing.infrastructure;

public class SmartBillApiException extends RuntimeException {

    private final boolean retryable;
    private final int httpStatusCode;

    public SmartBillApiException(String message, boolean retryable, int httpStatusCode) {
        super(message);
        this.retryable = retryable;
        this.httpStatusCode = httpStatusCode;
    }

    public SmartBillApiException(String message, boolean retryable, int httpStatusCode, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
        this.httpStatusCode = httpStatusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
