package org.ikasan.dashboard.ui.visualisation.actions;

import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamManageDialog;
import org.ikasan.designer.function.ManageFunction;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;

public class BusinessStreamManageFunction implements ManageFunction {

    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    public BusinessStreamManageFunction(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService) {
        this.businessStreamMetaDataService = businessStreamMetaDataService;
    }

    @Override
    public void open() {
        BusinessStreamManageDialog dialog = new BusinessStreamManageDialog(this.businessStreamMetaDataService);
        dialog.open();
    }
}
