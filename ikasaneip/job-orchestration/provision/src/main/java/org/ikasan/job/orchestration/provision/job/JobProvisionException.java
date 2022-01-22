package org.ikasan.job.orchestration.provision.job;

public class JobProvisionException extends RuntimeException {

    public JobProvisionException() {
    }

    public JobProvisionException(String message) {
        super(message);
    }

    public JobProvisionException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobProvisionException(Throwable cause) {
        super(cause);
    }

    public JobProvisionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
