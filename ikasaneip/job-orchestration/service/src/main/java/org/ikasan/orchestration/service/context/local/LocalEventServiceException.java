package org.ikasan.orchestration.service.context.local;

public class LocalEventServiceException extends RuntimeException {

    public LocalEventServiceException() {
    }

    public LocalEventServiceException(String message) {
        super(message);
    }

    public LocalEventServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public LocalEventServiceException(Throwable cause) {
        super(cause);
    }

    public LocalEventServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
