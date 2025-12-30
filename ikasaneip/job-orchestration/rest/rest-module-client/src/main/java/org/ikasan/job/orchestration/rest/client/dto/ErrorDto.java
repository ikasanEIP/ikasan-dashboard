package org.ikasan.job.orchestration.rest.client.dto;

import java.io.Serializable;

public class ErrorDto implements Serializable {
    private String errorCode;
    private String errorMessage;

    /**
     * Retrieves the error code associated with this ErrorDto.
     *
     * @return The error code as a String.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the error code for this ErrorDto instance.
     *
     * @param errorCode The error code to be set.
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Retrieves the error message associated with this ErrorDto instance.
     *
     * @return The error message as a String.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message for this ErrorDto instance.
     *
     * @param errorMessage The error message to be set.
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
