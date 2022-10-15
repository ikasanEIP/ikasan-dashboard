package org.ikasan.job.orchestration.integration.inbound.exception;

public class InvalidContextInstanceIdException extends RuntimeException {

    public InvalidContextInstanceIdException() {
    }

    public InvalidContextInstanceIdException(String message) {
        super(message);
    }

    public InvalidContextInstanceIdException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidContextInstanceIdException(Throwable cause) {
        super(cause);
    }

    public InvalidContextInstanceIdException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
