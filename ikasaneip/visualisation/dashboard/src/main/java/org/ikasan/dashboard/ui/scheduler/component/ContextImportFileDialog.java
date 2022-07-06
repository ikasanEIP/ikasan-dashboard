package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.job.orchestration.util.ContextImportZipUtils;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

// TODO write a vaadin test if we keep this dialogue
public class ContextImportFileDialog extends AbstractCloseableResizableDialog {
    private static final Logger LOG = LoggerFactory.getLogger(ContextImportFileDialog.class);

    private byte[] contextZipFile;

    private ContextProvisionService contextProvisionService;

    public ContextImportFileDialog(ContextProvisionService contextProvisionService) {
        this.contextProvisionService = contextProvisionService;
        if (this.contextProvisionService == null) {
            throw new IllegalArgumentException("contextUploadInitialisationService cannot be null!");
        }
        this.init();
    }

    private void init() {
        this.setModal(true);

        VerticalLayout verticalLayout = new VerticalLayout();

        Image mrSquidImage = new Image("/frontend/images/mr-squid-head.png", "");
        mrSquidImage.setHeight("35px");

        Label uploadContextHeader = new Label("Upload Context And Jobs");

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setWidthFull();
        horizontalLayout.setHeight("40px");
        horizontalLayout.add(mrSquidImage, uploadContextHeader);
        horizontalLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, mrSquidImage, uploadContextHeader);

        verticalLayout.add(horizontalLayout);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, horizontalLayout);

        MemoryBuffer fileBuffer = new MemoryBuffer();
        Upload upload = new Upload(fileBuffer);
        upload.setMaxFiles(1);
        upload.addFinishedListener(event -> {
            InputStream inputStream = fileBuffer.getInputStream();
            try {
                contextZipFile = new byte[inputStream.available()];
                inputStream.read(contextZipFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            try {
                ContextBundle contextBundle
                    = ContextImportZipUtils.extractZipFile(new ByteArrayInputStream(contextZipFile));

                this.contextProvisionService.provisionContext(contextBundle);

            } catch (Exception e) {
                LOG.warn("Could not upload context and jobs error " + e.getMessage());
                NotificationHelper.showErrorNotification("Could not import zip file");
            }
            this.close();
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(saveButton, cancelButton);

        verticalLayout.add(upload, buttonLayout);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, upload, buttonLayout);
        this.content.add(verticalLayout);
        super.setWidth("600px");
        super.setHeight("400px");
    }
}