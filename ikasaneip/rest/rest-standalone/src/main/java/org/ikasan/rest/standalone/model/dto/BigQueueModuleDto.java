package org.ikasan.rest.standalone.model.dto;

import java.util.Map;

public class BigQueueModuleDto {

    String moduleName;
    String moduleUrl;
    Map<String, Long> queueSizeMap;
    boolean isSuccessful;

    public BigQueueModuleDto(String moduleName, String moduleUrl, Map<String, Long> queueSizeMap, boolean isSuccessful) {
        this.moduleName = moduleName;
        this.moduleUrl = moduleUrl;
        this.queueSizeMap = queueSizeMap;
        this.isSuccessful = isSuccessful;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleUrl() {
        return moduleUrl;
    }

    public void setModuleUrl(String moduleUrl) {
        this.moduleUrl = moduleUrl;
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
