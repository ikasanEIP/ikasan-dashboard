package org.ikasan.job.orchestration.builder.context;

public class ContextBuilderException extends RuntimeException {
    /**
     * Represents an exception that occurs during the building of a context.
     */
    public ContextBuilderException() {
    }

    /**
     * Constructs a new ContextBuilderException with the specified detail message.
     *
     * @param message the detail message
     */
    public ContextBuilderException(String message) {
        super(message);
    }

    /**
     * Constructs a new ContextBuilderException with the specified detail message and cause.
     *
     * @param message the detail message of the exception
     * @param cause   the cause of the exception
     */
    public ContextBuilderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new ContextBuilderException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public ContextBuilderException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new ContextBuilderException with the specified detail message, cause, suppression enabled or disabled,
     * and writable stack trace.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     * @param enableSuppression whether or not suppression is enabled
     * @param writableStackTrace whether or not the stack trace should be writable
     */
    public ContextBuilderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
