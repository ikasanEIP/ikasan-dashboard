package org.ikasan.orchestration.service.status;

public class ContextStatusServiceException extends RuntimeException {

    public ContextStatusServiceException(String message) {
        super(message);
    }

    public ContextStatusServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
