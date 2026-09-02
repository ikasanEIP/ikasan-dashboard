package org.ikasan.persistence.initialisation.core;

public class DataJobException extends Exception {

    /**
     * Constructs a new DataJobException with no detail message or cause.
     * This constructor provides a default initialization for instances of
     * DataJobException where no additional context or message is required.
     */
    public DataJobException() {
    }

    /**
     * Constructs a new DataJobException with the specified detail message.
     *
     * @param message the detail message, which can provide additional context
     *                about the cause of the exception.
     */
    public DataJobException(String message) {
        super(message);
    }

    /**
     * Constructs a new DataJobException with the specified detail message and cause.
     *
     * @param message the detail message, providing a specific description of the exception.
     * @param cause the cause of the exception, which allows for chaining exceptions to represent a root cause.
     */
    public DataJobException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new DataJobException with the specified cause.
     * This constructor allows the creation of an exception wrapping another throwable.
     *
     * @param cause the underlying cause of this exception. It can be used to propagate the root exception.
     */
    public DataJobException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new DataJobException with the specified detail message, cause,
     * suppression enabled or disabled, and writable stack trace enabled or disabled.
     *
     * @param message the detail message to provide more specific information about the exception.
     * @param cause the cause of the exception. A {@code null} value is permitted,
     *              and indicates that the cause is nonexistent or unknown.
     * @param enableSuppression a boolean indicating whether or not suppression is enabled
     *                          or disabled for the exception.
     * @param writableStackTrace a boolean indicating whether or not the stack trace should
     *                           be writable for the exception.
     */
    public DataJobException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
