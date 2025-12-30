package org.ikasan.job.orchestration.provision.job;

public class JobProvisionLockException extends RuntimeException {

    /**
     * This class represents an exception that is thrown when there is an issue related to locking in job provisioning.
     */
    public JobProvisionLockException() {
    }

    /**
     * Constructs a new JobProvisionLockException with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage() method)
     */
    public JobProvisionLockException(String message) {
        super(message);
    }

    /**
     * Constructs a new JobProvisionLockException with the specified detail message
     * and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage() method)
     * @param cause the cause (which is saved for later retrieval by the getCause() method)
     */
    public JobProvisionLockException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new JobProvisionLockException with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public JobProvisionLockException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new JobProvisionLockException with the specified detail message, cause, suppression enabled or disabled,
     * and writable stack trace enabled or disabled.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage() method)
     * @param cause the cause (which is saved for later retrieval by the getCause() method)
     * @param enableSuppression whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not the stack trace should be writable
     */
    public JobProvisionLockException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
