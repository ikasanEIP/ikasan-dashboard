package org.ikasan.job.orchestration.context.validation;

import java.util.List;

public class InvalidContextTemplateException extends Exception {

    private List<ContextError> contextErrors;

    public InvalidContextTemplateException(String message, List<ContextError> contextErrors) {
        super(message);
        this.contextErrors = contextErrors;
    }

    public InvalidContextTemplateException(String message, Throwable cause, List<ContextError> contextErrors) {
        super(message, cause);
        this.contextErrors = contextErrors;
    }

    public InvalidContextTemplateException(Throwable cause, List<ContextError> contextErrors) {
        super(cause);
        this.contextErrors = contextErrors;
    }

    public InvalidContextTemplateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace,
                                           List<ContextError> contextErrors) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.contextErrors = contextErrors;
    }

    public List<ContextError> getContextErrors() {
        return contextErrors;
    }
}
