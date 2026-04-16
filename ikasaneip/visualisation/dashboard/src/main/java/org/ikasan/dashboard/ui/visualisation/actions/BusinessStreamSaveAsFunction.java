package org.ikasan.dashboard.ui.visualisation.actions;

import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamSaveAsDialog;
import org.ikasan.designer.function.SaveAsFunction;
import org.ikasan.spec.metadata.model.BusinessStreamMetaData;
import org.ikasan.spec.metadata.service.BusinessStreamMetaDataService;

public class BusinessStreamSaveAsFunction implements SaveAsFunction {

    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private BusinessStreamSaveAsDialog businessStreamSaveAsDialog;

    public BusinessStreamSaveAsFunction(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService) {
        this.businessStreamMetaDataService = businessStreamMetaDataService;
    }

    @Override
    public void saveAs(String payload) {
        this.businessStreamSaveAsDialog = new BusinessStreamSaveAsDialog(businessStreamMetaDataService,
            payload);
        businessStreamSaveAsDialog.open();
    }

    @Override
    public String getName() {
        return this.businessStreamSaveAsDialog.getName();
    }

    @Override
    public String getId() {
        return this.businessStreamSaveAsDialog.getBusinessStreamId();
    }

    @Override
    public String getDescription() {
        return this.businessStreamSaveAsDialog.getDescription();
    }

}
