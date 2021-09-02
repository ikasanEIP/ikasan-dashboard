package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.business.stream.metadata.model.BusinessStreamMetaDataImpl;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;

public class BusinessStreamSaveAsDialog extends AbstractCloseableResizableDialog {

    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    private String id;
    private String name;
    private String description;

    /**
     * Constructor
     *
     * @param businessStreamMetaDataService
     */
    public BusinessStreamSaveAsDialog(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService,
                                      String businessStreamJson)
    {
        this.businessStreamMetaDataService = businessStreamMetaDataService;
        if(this.businessStreamMetaDataService == null)
        {
            throw new IllegalArgumentException("businessStreamMetaDataService cannot be null!");
        }
        this.init(businessStreamJson);
    }

    private void init(String businessStreamJson)
    {
        this.setModal(true);

        VerticalLayout verticalLayout = new VerticalLayout();

        Image mrSquidImage = new Image("/frontend/images/mr-squid-head.png", "");
        mrSquidImage.setHeight("35px");

        Label businessStreamHeader = new Label(String.format(getTranslation("label.upload-business-stream", UI.getCurrent().getLocale())));

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setHeight("40px");
        header.add(mrSquidImage, businessStreamHeader);
        header.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, mrSquidImage, businessStreamHeader);

        verticalLayout.add(header);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, header);

        TextField businessStreamNameTextfield = new TextField(getTranslation("label.business-stream-name", UI.getCurrent().getLocale()));
        businessStreamNameTextfield.setWidthFull();

        TextArea businessStreamDescriptionTextfield = new TextArea(getTranslation("label.business-stream-description", UI.getCurrent().getLocale()));
        businessStreamDescriptionTextfield.setWidthFull();
        businessStreamDescriptionTextfield.setHeight("200px");


        Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
        {
            boolean isValid = true;
            if(businessStreamNameTextfield.getValue() == null || businessStreamNameTextfield.getValue().isEmpty())
            {
                businessStreamNameTextfield.setErrorMessage(getTranslation("error.business-stream-name-empty", UI.getCurrent().getLocale()));
                businessStreamNameTextfield.setInvalid(true);
                isValid = false;
            }

            if(businessStreamDescriptionTextfield.getValue() == null || businessStreamDescriptionTextfield.getValue().isEmpty())
            {
                businessStreamDescriptionTextfield.setErrorMessage(getTranslation("error.business-stream-description-empty", UI.getCurrent().getLocale()));
                businessStreamDescriptionTextfield.setInvalid(true);
                isValid = false;
            }


            if(!isValid)
            {
                return;
            }


            this.id = businessStreamNameTextfield.getValue();
            this.name = businessStreamNameTextfield.getValue();
            this.description = businessStreamDescriptionTextfield.getValue();
            BusinessStreamMetaData saveBusinessStreamMetaData = new BusinessStreamMetaDataImpl();
            saveBusinessStreamMetaData.setId(this.id);
            saveBusinessStreamMetaData.setName(this.name);
            saveBusinessStreamMetaData.setDescription(this.description);
            saveBusinessStreamMetaData.setJson(businessStreamJson);

            this.businessStreamMetaDataService.save(saveBusinessStreamMetaData);

            this.close();
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(saveButton, cancelButton);

        verticalLayout.add(businessStreamNameTextfield, businessStreamDescriptionTextfield, buttonLayout);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);
        this.content.add(verticalLayout);
        super.setWidth("600px");
        super.setHeight("600px");
    }


    public String getBusinessStreamId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
