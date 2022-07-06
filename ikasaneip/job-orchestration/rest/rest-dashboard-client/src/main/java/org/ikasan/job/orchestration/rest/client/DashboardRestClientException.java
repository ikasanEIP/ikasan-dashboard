package org.ikasan.job.orchestration.rest.client;

public class DashboardRestClientException extends RuntimeException {
    public DashboardRestClientException() {
    }

    public DashboardRestClientException(String message) {
        super(message);
    }

    public DashboardRestClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public DashboardRestClientException(Throwable cause) {
        super(cause);
    }

    public DashboardRestClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
