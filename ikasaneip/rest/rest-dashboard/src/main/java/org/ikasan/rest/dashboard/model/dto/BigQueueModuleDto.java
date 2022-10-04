package org.ikasan.rest.dashboard.model.dto;

import java.util.Map;

public class BigQueueModuleDto {

    String moduleName;
    Map<String, Long> queueSizeMap;

    public BigQueueModuleDto(String moduleName, Map<String, Long> queueSizeMap) {
        this.moduleName = moduleName;
        this.queueSizeMap = queueSizeMap;
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
}
