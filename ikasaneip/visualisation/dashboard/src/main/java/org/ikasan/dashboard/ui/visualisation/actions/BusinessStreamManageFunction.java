package org.ikasan.dashboard.ui.visualisation.actions;

import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamManageDialog;
import org.ikasan.designer.function.ManageFunction;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataService;

public class BusinessStreamManageFunction implements ManageFunction {

    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private ModuleMetaDataService moduleMetaDataService;

    public BusinessStreamManageFunction(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService,
                                        ModuleMetaDataService moduleMetaDataService) {
        this.businessStreamMetaDataService = businessStreamMetaDataService;
        this.moduleMetaDataService = moduleMetaDataService;
    }

    @Override
    public void open() {
        BusinessStreamManageDialog dialog = new BusinessStreamManageDialog(this.businessStreamMetaDataService, moduleMetaDataService);
        dialog.open();
    }
}
