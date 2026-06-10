package org.ikasan.dashboard.ui.visualisation.layout;

/**
 * Exception class representing errors encountered during the layout management process.
 * This class extends {@link RuntimeException} to denote non-checked exceptions that can
 * occur during operations involving layout computations or flow diagram management.
 *
 * Instances of this exception typically provide details about the error condition
 * via a descriptive message and an optional underlying cause, enabling better debugging
 * and error tracing in layout-related workflows.
 */
public class LayoutManagerException extends RuntimeException {


    /**
     * Constructs a new LayoutManagerException with the specified detail message
     * and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause the underlying cause of the exception, which can be used
     *              to retrieve detailed trace information
     */
    public LayoutManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
