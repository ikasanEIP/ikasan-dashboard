package org.ikasan.mongo.persistence.setup.model;

import org.ikasan.spec.persistence.model.DashboardSetupItem;

/**
 * MongoDB implementation of DashboardSetupItem.
 */
public class MongoDashboardSetupItemImpl implements DashboardSetupItem {

    private String name;

    private String status;

    private long executionTimestamp;

    /**
     * Default constructor for Jackson deserialization
     */
    public MongoDashboardSetupItemImpl() {
    }

    /**
     * Constructor with all fields
     *
     * @param name the name of the setup item
     * @param status the status of the setup item
     * @param executionTimestamp the execution timestamp
     */
    public MongoDashboardSetupItemImpl(String name, String status, long executionTimestamp) {
        this.name = name;
        this.status = status;
        this.executionTimestamp = executionTimestamp;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setExecutionTimestamp(long timestamp) {
        this.executionTimestamp = timestamp;
    }

    @Override
    public long getExecutionTimestamp() {
        return executionTimestamp;
    }

    @Override
    public String toString() {
        return "MongoDashboardSetupItemImpl{" +
            "name='" + name + '\'' +
            ", status='" + status + '\'' +
            ", executionTimestamp=" + executionTimestamp +
            '}';
    }
}
