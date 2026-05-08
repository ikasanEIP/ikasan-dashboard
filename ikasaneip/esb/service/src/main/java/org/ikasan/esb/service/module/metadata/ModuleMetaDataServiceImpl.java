package org.ikasan.esb.service.module.metadata;

import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.persistence.BatchInsert;

import java.util.List;

public class ModuleMetaDataServiceImpl implements BatchInsert<ModuleMetaData>, ModuleMetaDataService
{
    private ModuleMetadataDao dao;

    /**
     * Constructor
     *
     * @param dao
     */
    public ModuleMetaDataServiceImpl(ModuleMetadataDao dao)
    {
        this.dao = dao;
        if(this.dao == null)
        {
            throw new IllegalArgumentException("Dao cannot be null!");
        }
    }

    @Override
    public void insert(List<ModuleMetaData> entities) {
        dao.save(entities);
    }

    @Override
    public ModuleMetaData findById(String id) {
        return this.dao.findById(id);
    }

    @Override
    public List<ModuleMetaData> findAll() {
        ModuleMetadataSearchResults moduleMetadataSearchResults = this.find(null, 0, 0);

        int numResults = Integer.MAX_VALUE;
        if(moduleMetadataSearchResults.getTotalNumberOfResults() < Integer.MAX_VALUE)
        {
            numResults = (int) moduleMetadataSearchResults.getTotalNumberOfResults();
        }


        return this.dao.findAll(0, numResults);
    }

    @Override
    public ModuleMetadataSearchResults find(List<String> modulesNames) {
        if(modulesNames ==  null || modulesNames.isEmpty()) {
            return new ModuleMetadataSearchResults(List.of(), 0, 0);
        }

        ModuleMetadataSearchResults moduleMetadataSearchResults = this.find(modulesNames, 0, 0);

        int numResults = Integer.MAX_VALUE;
        if(moduleMetadataSearchResults.getTotalNumberOfResults() < Integer.MAX_VALUE)
        {
            numResults = (int) moduleMetadataSearchResults.getTotalNumberOfResults();
        }


        return this.dao.find(modulesNames,0, numResults);
    }

    @Override
    public ModuleMetadataSearchResults find(List<String> modulesNames, Integer startOffset, Integer resultSize) {
        return this.dao.find(modulesNames, startOffset, resultSize);
    }

    @Override
    public ModuleMetadataSearchResults find(List<String> modulesNames, ModuleType moduleType, Integer startOffset, Integer resultSize) {
        return this.dao.find(modulesNames, moduleType, startOffset, resultSize);
    }

    @Override
    public void deleteById(String name) {
        this.dao.deleteById(name);
    }
}
