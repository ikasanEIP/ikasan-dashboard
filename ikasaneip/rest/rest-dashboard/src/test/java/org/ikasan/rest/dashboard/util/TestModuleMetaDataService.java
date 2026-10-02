package org.ikasan.rest.dashboard.util;

import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.search.SearchResults;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestModuleMetaDataService implements ModuleMetaDataService {

    private Map<String, ModuleMetaData> moduleMetaDataMap = new HashMap<>();

    public void save(ModuleMetaData moduleMetaData) {
        moduleMetaDataMap.put(moduleMetaData.getName(), moduleMetaData);
    }

    @Override
    public ModuleMetadataSearchResults find(List<String> list, Integer integer, Integer integer1) {
        return null;
    }

    @Override
    public ModuleMetadataSearchResults find(List<String> list) {
        return null;
    }

    @Override
    public ModuleMetadataSearchResults find(List<String> list, ModuleType moduleType, Integer integer, Integer integer1) {
        return null;
    }

    @Override
    public void deleteById(String id) {
        moduleMetaDataMap.remove(id);
    }

    @Override
    public ModuleMetaData findById(String moduleName) {
        return moduleMetaDataMap.get(moduleName);
    }

    @Override
    public List<ModuleMetaData> findAll() {
        return List.copyOf(moduleMetaDataMap.values());
    }



    public void clear() {
        moduleMetaDataMap.clear();
    }
}
