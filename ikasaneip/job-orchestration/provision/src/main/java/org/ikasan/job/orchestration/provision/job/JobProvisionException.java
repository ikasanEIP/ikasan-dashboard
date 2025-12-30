package org.ikasan.job.orchestration.provision.job;

public class JobProvisionException extends RuntimeException {

    /**
     * Exception thrown to indicate an error during job provision.
     */
    public JobProvisionException() {
    }

    /**
     * Constructs a new JobProvisionException with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public JobProvisionException(String message) {
        super(message);
    }

    /**
     * Constructs a new JobProvisionException with the specified detail message and cause.
     * This exception is thrown to indicate an error during job provision.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public JobProvisionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new JobProvisionException with the specified cause.
     * This exception is thrown to indicate an error during job provision.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public JobProvisionException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new JobProvisionException with the specified detail message, cause, suppression enabled or disabled,
     * and writable stack trace enabled or disabled.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     * @param enableSuppression whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not the stack trace should be writable
     */
    public JobProvisionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
