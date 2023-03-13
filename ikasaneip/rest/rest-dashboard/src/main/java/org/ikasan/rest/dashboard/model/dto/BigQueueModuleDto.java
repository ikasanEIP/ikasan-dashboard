package org.ikasan.rest.dashboard.model.dto;

import java.util.Map;

public class BigQueueModuleDto {

    String moduleName;
    Map<String, Long> queueSizeMap;
    boolean isSuccessful;

    public BigQueueModuleDto(String moduleName, Map<String, Long> queueSizeMap, boolean isSuccessful) {
        this.moduleName = moduleName;
        this.queueSizeMap = queueSizeMap;
        this.isSuccessful = isSuccessful;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public Map<String, Long> getQueueSizeMap() {
        return queueSizeMap;
    }

    public void setQueueSizeMap(Map<String, Long> queueSizeMap) {
        this.queueSizeMap = queueSizeMap;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }
}
