package org.ikasan.job.orchestration.context.validation;

public class InvalidContextTemplateException extends Exception {

    public InvalidContextTemplateException(String message) {
        super(message);
    }

    public InvalidContextTemplateException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidContextTemplateException(Throwable cause) {
        super(cause);
    }

    public InvalidContextTemplateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
