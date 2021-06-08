package org.ikasan.designer.function;

public interface SaveFunction {

    /**
     * Save a business stream.
     *
     * @param id
     * @param name
     * @param description
     * @param payload
     */
    void save(String id, String name, String description, String payload);
}
