package org.ikasan.scheduler.context.builder;

public class ContextBuilderException extends RuntimeException {
    public ContextBuilderException() {
    }

    public ContextBuilderException(String message) {
        super(message);
    }

    public ContextBuilderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContextBuilderException(Throwable cause) {
        super(cause);
    }

    public ContextBuilderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
