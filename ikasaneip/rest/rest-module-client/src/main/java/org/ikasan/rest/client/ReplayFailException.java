package org.ikasan.rest.client;

public class ReplayFailException extends RuntimeException {

    /**
     * An exception that is thrown to indicate a failure during replay of a certain operation.
     */
    public ReplayFailException(String message) {
        super(message);
    }

    /**
     * Constructs a new ReplayFailException with the specified detail message and cause.
     * <p>
     * This exception is used to indicate a failure during replay of a certain operation.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage() method).
     * @param cause   the cause (which is saved for later retrieval by the getCause() method).
     */
    public ReplayFailException(String message, Throwable cause) {
        super(message, cause);
    }
}
