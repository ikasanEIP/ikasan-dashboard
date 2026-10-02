package org.ikasan.rest.dashboard.util;

import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.metadata.model.FlowElementMetaData;
import org.ikasan.spec.metadata.model.FlowMetaData;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.model.Transition;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.ModuleType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test implementation of ModuleMetaDataService for unit testing.
 */
public class TestModuleMetaDataService implements ModuleMetaDataService {

    private List<ModuleMetaData> modules = new ArrayList<>();

    @Override
    public ModuleMetaData findById(String id) {
        return modules.stream()
            .filter(m -> m.getName().equals(id))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<ModuleMetaData> findAll() {
        return new ArrayList<>(modules);
    }

    @Override
    public ModuleMetadataSearchResults find(List<String> modulesNames) {
        if (modulesNames == null || modulesNames.isEmpty()) {
            return new ModuleMetadataSearchResults(new ArrayList<>(modules), 0, modules.size());
        }

        List<ModuleMetaData> filtered = modules.stream()
            .filter(m -> modulesNames.contains(m.getName()))
            .collect(Collectors.toList());

        return new ModuleMetadataSearchResults(filtered, 0, filtered.size());
    }

    @Override
    public ModuleMetadataSearchResults find(List<String> modulesNames, Integer startOffset, Integer resultSize) {
        List<ModuleMetaData> filtered;

        if (modulesNames == null || modulesNames.isEmpty()) {
            filtered = new ArrayList<>(modules);
        } else {
            filtered = modules.stream()
                .filter(m -> modulesNames.contains(m.getName()))
                .collect(Collectors.toList());
        }

        int start = startOffset != null ? startOffset : 0;
        int size = resultSize != null ? resultSize : filtered.size();
        int end = Math.min(start + size, filtered.size());

        List<ModuleMetaData> results = start < filtered.size()
            ? filtered.subList(start, end)
            : new ArrayList<>();

        return new ModuleMetadataSearchResults(results, start, filtered.size());
    }

    @Override
    public ModuleMetadataSearchResults find(List<String> modulesNames, ModuleType moduleType, Integer startOffset, Integer resultSize) {
        return find(modulesNames, startOffset, resultSize);
    }

    @Override
    public void deleteById(String name) {
        modules.removeIf(m -> m.getName().equals(name));
    }

    // Test helper methods

    public void setModules(List<ModuleMetaData> modules) {
        this.modules = new ArrayList<>(modules);
    }

    public void addModule(ModuleMetaData module) {
        this.modules.add(module);
    }

    public void clear() {
        this.modules.clear();
    }

    /**
     * Helper class to create test ModuleMetaData instances
     */
    public static class TestModuleMetaData implements ModuleMetaData {
        private String name;
        private List<FlowMetaData> flows = new ArrayList<>();

        public TestModuleMetaData(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setDescription(String description) {

        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public void setVersion(String version) {

        }

        @Override
        public String getIkasanVersion() {
            return "";
        }

        @Override
        public void setIkasanVersion(String ikasanVersion) {

        }

        @Override
        public String getVersion() {
            return "";
        }

        @Override
        public void setType(ModuleType moduleType) {

        }

        @Override
        public ModuleType getType() {
            return null;
        }

        @Override
        public String getUrl() {
            return "";
        }

        @Override
        public void setUrl(String url) {

        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public List<FlowMetaData> getFlows() {
            return flows;
        }

        @Override
        public String getConfiguredResourceId() {
            return "";
        }

        @Override
        public void setConfiguredResourceId(String id) {

        }

        @Override
        public String getHost() {
            return "";
        }

        @Override
        public void setHost(String host) {

        }

        @Override
        public Integer getPort() {
            return 0;
        }

        @Override
        public void setPort(Integer port) {

        }

        @Override
        public String getContext() {
            return "";
        }

        @Override
        public void setContext(String context) {

        }

        @Override
        public String getProtocol() {
            return "";
        }

        @Override
        public void setProtocol(String protocol) {

        }

        public void setFlows(List<FlowMetaData> flows) {
            this.flows = flows;
        }

        public void addFlow(FlowMetaData flow) {
            this.flows.add(flow);
        }
    }

    /**
     * Helper class to create test FlowMetaData instances
     */
    public static class TestFlowMetaData implements FlowMetaData {
        private String name;

        public TestFlowMetaData(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setConsumer(FlowElementMetaData consumer) {

        }

        @Override
        public FlowElementMetaData getConsumer() {
            return null;
        }

        @Override
        public List<Transition> getTransitions() {
            return List.of();
        }

        @Override
        public void setTransitions(List<Transition> transitions) {

        }

        @Override
        public List<FlowElementMetaData> getFlowElements() {
            return List.of();
        }

        @Override
        public void setFlowElements(List<FlowElementMetaData> transitions) {

        }

        @Override
        public String getConfigurationId() {
            return "";
        }

        @Override
        public void setConfigurationId(String configurationId) {

        }

        @Override
        public String getFlowStartupType() {
            return "";
        }

        @Override
        public void setFlowStartupType(String flowStartupType) {

        }

        @Override
        public String getFlowStartupComment() {
            return "";
        }

        @Override
        public void setFlowStartupComment(String flowStartupComment) {

        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
