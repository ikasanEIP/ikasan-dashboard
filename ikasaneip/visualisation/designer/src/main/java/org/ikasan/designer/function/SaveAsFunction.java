package org.ikasan.designer.function;

public interface SaveAsFunction {

    String getName();
    String getId();
    String getDescription();
    /**
     * Save a business stream as.
     *
     * @param payload
     */
    void saveAs(String payload);
}
