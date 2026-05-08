package org.ikasan.esb.service.configuration.metadata;

import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.model.ConfigurationMetaData;
import org.ikasan.spec.metadata.service.ConfigurationMetaDataService;
import org.ikasan.spec.persistence.BatchInsert;

import java.util.List;

public class ConfigurationMetadataServiceImpl implements BatchInsert<ConfigurationMetaData>, ConfigurationMetaDataService
{
    private ComponentConfigurationMetadataDao componentConfigurationMetadataDao;

    /**
     * Constructs a new instance of ConfigurationMetadataServiceImpl.
     *
     * @param componentConfigurationMetadataDao the data access object used for interacting
     *                                           with the component configuration metadata storage.
     *                                           Must not be null.
     * @throws IllegalArgumentException if componentConfigurationMetadataDao is null.
     */
    public ConfigurationMetadataServiceImpl(ComponentConfigurationMetadataDao componentConfigurationMetadataDao) {
        this.componentConfigurationMetadataDao = componentConfigurationMetadataDao;
        if(this.componentConfigurationMetadataDao == null) {
            throw new IllegalArgumentException("componentConfigurationMetadataDao cannot be null!");
        }
    }

    @Override
    public void insert(List<ConfigurationMetaData> entities) {
        componentConfigurationMetadataDao.save(entities);
    }

    @Override
    public ConfigurationMetaData findById(String id) {
        return componentConfigurationMetadataDao.findById(id);
    }

    @Override
    public List<ConfigurationMetaData> findAll() {
        return componentConfigurationMetadataDao.findAll();
    }

    @Override
    public List<ConfigurationMetaData> findByIdList(List<String> configurationIds) {
        return componentConfigurationMetadataDao.findInIdList(configurationIds);
    }
}
