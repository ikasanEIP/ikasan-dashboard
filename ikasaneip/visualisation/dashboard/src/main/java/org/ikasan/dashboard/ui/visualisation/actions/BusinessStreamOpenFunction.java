package org.ikasan.dashboard.ui.visualisation.actions;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.GeneratedVaadinDialog;
import com.vaadin.flow.component.html.Image;
import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamOpenDialog;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.function.OpenFunction;
import org.ikasan.designer.json.DesignerDynamicImageManager;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;

import java.io.IOException;
import java.util.ArrayList;

public class BusinessStreamOpenFunction implements OpenFunction {
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private BusinessStreamMetaData businessStreamMetaData;
    private DesignerDynamicImageManager designerDynamicImageManager;

    public BusinessStreamOpenFunction(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService,
                                      ArrayList<Image> images) {
        this.businessStreamMetaDataService = businessStreamMetaDataService;

        this.designerDynamicImageManager = new DesignerDynamicImageManager(images);
    }

    @Override
    public String getName() {
        return businessStreamMetaData!=null?businessStreamMetaData.getName():null;
    }

    @Override
    public String getId() {
        return businessStreamMetaData!=null?businessStreamMetaData.getId():null;
    }

    @Override
    public String getDescription() {
        return businessStreamMetaData!=null?businessStreamMetaData.getDescription():null;
    }

    @Override
    public String getJson() {
        return businessStreamMetaData!=null?businessStreamMetaData.getJson():null;
    }

    @Override
    public void open(DesignerCanvas designerCanvas) {
        BusinessStreamOpenDialog businessStreamOpenDialog
            = new BusinessStreamOpenDialog(this.businessStreamMetaDataService);
        businessStreamOpenDialog.open();

        businessStreamOpenDialog.addOpenedChangeListener((ComponentEventListener<GeneratedVaadinDialog.OpenedChangeEvent<Dialog>>) dialogOpenedChangeEvent -> {
            if(!businessStreamOpenDialog.isOpened() && businessStreamOpenDialog.getBusinessStreamMetaData() != null) {
                this.businessStreamMetaData = businessStreamOpenDialog.getBusinessStreamMetaData();

                try {
                    String json = this.designerDynamicImageManager
                        .parse(businessStreamOpenDialog.getBusinessStreamMetaData().getJson());

                    designerCanvas.setCanvasJson(json);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                designerCanvas.clear();
                designerCanvas.importJson();
            }
        });
    }
}
