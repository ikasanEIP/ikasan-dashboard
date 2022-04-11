package org.ikasan.orchestration.service.context.reset;

public class ContextResetException extends RuntimeException {

    public ContextResetException(String message) {
        super(message);
    }

    public ContextResetException(String message, Throwable cause) {
        super(message, cause);
    }
}
