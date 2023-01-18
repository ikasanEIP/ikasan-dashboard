package org.ikasan.orchestration.service.context.global;

public class GlobalEventServiceException extends RuntimeException {

    public GlobalEventServiceException() {
    }

    public GlobalEventServiceException(String message) {
        super(message);
    }

    public GlobalEventServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public GlobalEventServiceException(Throwable cause) {
        super(cause);
    }

    public GlobalEventServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
