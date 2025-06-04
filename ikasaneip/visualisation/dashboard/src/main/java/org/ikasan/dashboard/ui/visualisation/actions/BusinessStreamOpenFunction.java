package org.ikasan.dashboard.ui.visualisation.actions;

import com.vaadin.flow.component.html.Image;
import org.ikasan.dashboard.ui.visualisation.adapter.service.BusinessStreamHighLevelViewAdapter;
import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamOpenDialog;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.function.OpenFunction;
import org.ikasan.designer.json.DesignerDynamicImageManager;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

public class BusinessStreamOpenFunction implements OpenFunction {

    Logger logger = LoggerFactory.getLogger(BusinessStreamOpenFunction.class);

    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private BusinessStreamMetaData businessStreamMetaData;
    private DesignerDynamicImageManager designerDynamicImageManager;
    private ModuleMetaDataService moduleMetaDataService;

    public BusinessStreamOpenFunction(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService, ModuleMetaDataService moduleMetaDataService,
                                      ArrayList<Image> images) {
        this.businessStreamMetaDataService = businessStreamMetaDataService;
        this.moduleMetaDataService = moduleMetaDataService;

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
            = new BusinessStreamOpenDialog(this.businessStreamMetaDataService, moduleMetaDataService);
        businessStreamOpenDialog.open();

        businessStreamOpenDialog.addOpenedChangeListener(dialogOpenedChangeEvent -> {
            if(!businessStreamOpenDialog.isOpened() && businessStreamOpenDialog.getBusinessStreamMetaData() != null) {
                this.businessStreamMetaData = businessStreamOpenDialog.getBusinessStreamMetaData();

                try {
                    String json = this.designerDynamicImageManager
                        .parse(businessStreamOpenDialog.getBusinessStreamMetaData().getJson());

//                    BusinessStreamHighLevelViewAdapter adapter = new BusinessStreamHighLevelViewAdapter();

                    designerCanvas.setCanvasJson(json);

//                    String highLevelJson = adapter.adaptView(json);
//
//                    logger.info(highLevelJson);
//
//                    designerCanvas.setCanvasJson(highLevelJson);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                designerCanvas.clear();
                designerCanvas.importJson(false);
            }
        });
    }
}
