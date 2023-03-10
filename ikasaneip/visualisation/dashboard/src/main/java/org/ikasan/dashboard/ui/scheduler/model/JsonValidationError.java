package org.ikasan.dashboard.ui.scheduler.model;

public class JsonValidationError {
    private String errorMessage;
    private int lineNumber;
    private int columnNumber;

    public JsonValidationError(String errorMessage, int lineNumber, int columnNumber) {
        this.errorMessage = errorMessage;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }
}
