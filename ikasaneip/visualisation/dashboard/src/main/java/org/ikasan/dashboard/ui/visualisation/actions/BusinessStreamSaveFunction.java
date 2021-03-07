package org.ikasan.dashboard.ui.visualisation.actions;

import org.ikasan.business.stream.metadata.model.BusinessStreamMetaDataImpl;
import org.ikasan.designer.function.SaveFunction;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;

public class BusinessStreamSaveFunction implements SaveFunction {
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    public BusinessStreamSaveFunction(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService) {
        this.businessStreamMetaDataService = businessStreamMetaDataService;
    }

    @Override
    public void save(String id, String name, String description, String payload) {
        BusinessStreamMetaData businessStreamMetaData = new BusinessStreamMetaDataImpl();
        businessStreamMetaData.setName(name);
        businessStreamMetaData.setDescription(description);
        businessStreamMetaData.setId(id);
        businessStreamMetaData.setJson(payload);

        this.businessStreamMetaDataService.save(businessStreamMetaData);
    }
}
