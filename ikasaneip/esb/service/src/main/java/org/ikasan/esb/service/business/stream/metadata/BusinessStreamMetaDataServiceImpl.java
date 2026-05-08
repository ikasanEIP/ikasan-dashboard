package org.ikasan.esb.service.business.stream.metadata;


import org.ikasan.spec.metadata.BusinessStreamMetadataSearchResults;
import org.ikasan.spec.metadata.dao.BusinessStreamMetadataDao;
import org.ikasan.spec.metadata.model.BusinessStreamMetaData;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.BusinessStreamMetaDataService;

import java.util.List;

public class BusinessStreamMetaDataServiceImpl implements BusinessStreamMetaDataService<BusinessStreamMetaData>
{
    private BusinessStreamMetadataDao dao;

    /**
     * Constructs an instance of the BusinessStreamMetaDataServiceImpl.
     *
     * @param dao the data access object used to interact with the business stream metadata storage
     */
    public BusinessStreamMetaDataServiceImpl(BusinessStreamMetadataDao dao)
    {
        this.dao = dao;
    }

    @Override
    public BusinessStreamMetaData findById(String id)
    {
        return dao.findById(id);
    }

    @Override
    public List<BusinessStreamMetaData> findAll(Integer startOffset, Integer resultSize) {
        return this.dao.findAll(startOffset, resultSize);
    }

    @Override
    public BusinessStreamMetadataSearchResults find(List<String> businessStreamNames, Integer startOffset, Integer resultSize) {
        return this.dao.find(businessStreamNames, startOffset, resultSize);
    }

    @Override
    public void save(BusinessStreamMetaData metaData) {
        this.dao.save(metaData);
    }

    @Override
    public void delete(String id) {
        this.dao.delete(id);
    }

    @Override
    public List<BusinessStreamMetaData> findBusinessStreamsContainingFlow(String moduleName, String flowName, int offset, int limit) {
        return this.dao.findBusinessStreamsContainingFlow(moduleName, flowName, offset, limit);
    }

    @Override
    public BusinessStreamMetadataSearchResults findBusinessStreamsForModules(String filter, List<ModuleMetaData> modules, int offset, int limit) {
        return this.dao.findBusinessStreamsForModules(filter, modules, offset, limit);
    }
}
