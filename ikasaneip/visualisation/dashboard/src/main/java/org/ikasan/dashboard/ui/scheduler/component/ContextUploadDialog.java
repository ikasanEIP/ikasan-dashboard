package org.ikasan.dashboard.ui.scheduler.component;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.scheduled.context.model.SolrScheduledContextRecordImpl;
import org.ikasan.scheduler.context.cache.ContextMachineCache;
import org.ikasan.scheduler.core.machine.ContextMachine;
import org.ikasan.scheduler.core.model.context.ContextImpl;
import org.ikasan.scheduler.core.model.context.ContextTemplateImpl;
import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class ContextUploadDialog extends AbstractCloseableResizableDialog
{
    private byte[] contextFile;

    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerService schedulerService;
    private ScheduledContextService scheduledContextService;

    /**
     * Constructor
     *
     */
    public ContextUploadDialog(ScheduledContextInstanceService scheduledContextInstanceService, SchedulerService schedulerService,
                               ScheduledContextService scheduledContextService)
    {
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.schedulerService = schedulerService;
        this.scheduledContextService = scheduledContextService;
        this.init();
    }

    private void init()
    {
        this.setModal(true);

        VerticalLayout verticalLayout = new VerticalLayout();

        Image mrSquidImage = new Image("/frontend/images/mr-squid-head.png", "");
        mrSquidImage.setHeight("35px");

        Label uploadContextHeader = new Label("Upload Context");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setHeight("40px");
        header.add(mrSquidImage, uploadContextHeader);
        header.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, mrSquidImage, uploadContextHeader);

        verticalLayout.add(header);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, header);

        TextField contextNameTextfield = new TextField("Context Instance Name");
        contextNameTextfield.setWidthFull();


        MemoryBuffer fileBuffer = new MemoryBuffer();
        Upload upload = new Upload(fileBuffer);
        upload.setMaxFiles(1);
        upload.addFinishedListener(event -> {
            InputStream inputStream =
                fileBuffer.getInputStream();

            try
            {
                contextFile = new byte[inputStream.available()];
                inputStream.read(contextFile);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });

        Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            ContextService contextService = new ContextService();
            try {
                ContextTemplateImpl contextTemplate = contextService.getContext(new String(contextFile));
                ScheduledContextRecord scheduledContextRecord = new SolrScheduledContextRecordImpl();
                scheduledContextRecord.setContextName(contextTemplate.getName());
                scheduledContextRecord.setContext(contextTemplate);
                scheduledContextRecord.setTimestamp(System.currentTimeMillis());
                this.scheduledContextService.save(scheduledContextRecord);

                ContextInstanceImpl contextInstance = contextService.getContextInstance(new String(contextFile));
                ContextImpl context = contextService.getContext(new String(contextFile));
                contextInstance.setId(UUID.randomUUID().toString());
                ContextMachine contextMachine = new ContextMachine(context, contextInstance, scheduledContextInstanceService);
                contextMachine.init();
                contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
                    // todo work out how to get agent url
                    schedulerService.raiseSchedulerJobInitiationEvent("http://localhost:8080/scheduler-agent", event);
                });

//                DryRunParameters dryRunParameters = new DryRunParametersImpl();
//                contextMachine.setDryRunParameters(dryRunParameters);

                ContextMachineCache.instance().put(contextMachine);
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            this.close();
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(saveButton, cancelButton);

        verticalLayout.add(contextNameTextfield, upload, buttonLayout);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, upload, buttonLayout);
        this.content.add(verticalLayout);
        super.setWidth("600px");
        super.setHeight("400px");
    }
}
