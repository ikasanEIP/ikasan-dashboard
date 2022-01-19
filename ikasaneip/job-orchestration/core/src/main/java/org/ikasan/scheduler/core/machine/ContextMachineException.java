package org.ikasan.scheduler.core.machine;

public class ContextMachineException extends RuntimeException {
    public ContextMachineException() {
    }

    public ContextMachineException(String message) {
        super(message);
    }

    public ContextMachineException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContextMachineException(Throwable cause) {
        super(cause);
    }

    public ContextMachineException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
