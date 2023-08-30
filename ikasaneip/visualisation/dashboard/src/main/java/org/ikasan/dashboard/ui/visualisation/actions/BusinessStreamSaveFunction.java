package org.ikasan.dashboard.ui.visualisation.actions;

import com.vaadin.flow.component.notification.Notification;
import org.ikasan.business.stream.metadata.model.BusinessStreamMetaDataImpl;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.function.SaveFunction;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessStreamSaveFunction implements SaveFunction {

    Logger logger = LoggerFactory.getLogger(BusinessStreamSaveFunction.class);

    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    public BusinessStreamSaveFunction(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService) {
        this.businessStreamMetaDataService = businessStreamMetaDataService;
    }

    @Override
    public void save(String id, String name, String description, String payload) {
        try {
            BusinessStreamMetaData businessStreamMetaData = new BusinessStreamMetaDataImpl();
            businessStreamMetaData.setName(name);
            businessStreamMetaData.setDescription(description);
            businessStreamMetaData.setId(id);
            businessStreamMetaData.setJson(payload);

            this.businessStreamMetaDataService.save(businessStreamMetaData);
        }
        catch (Exception e) {
            logger.error(String.format("An error has occurred saving business stream[%s]",name), e);
            Notification.show("Error saving business stream! Please contact Ikasan Support.", 3, Notification.Position.MIDDLE);
            return;
        }

        Notification.show("Business stream saved", 3, Notification.Position.MIDDLE);
    }
}
