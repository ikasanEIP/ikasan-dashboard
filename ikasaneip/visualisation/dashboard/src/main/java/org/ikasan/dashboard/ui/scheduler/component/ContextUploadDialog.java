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
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.validation.ContextTemplateValidator;
import org.ikasan.job.orchestration.context.validation.InvalidContextTemplateException;
import org.ikasan.job.orchestration.model.event.DryRunParametersImpl;
import org.ikasan.scheduled.context.model.SolrScheduledContextRecordImpl;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContextUploadDialog extends AbstractCloseableResizableDialog
{
    private byte[] contextFile;

    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerService schedulerService;
    private ScheduledContextService scheduledContextService;
    private InternalEventDrivenJobService internalEventDrivenJobService;
    private String queueDir;
    private ModuleMetaDataService moduleMetaDataService;
    private JobLockCacheService jobLockCacheService;

    /**
     * Constructor
     *
     */
    public ContextUploadDialog(ScheduledContextInstanceService scheduledContextInstanceService, SchedulerService schedulerService,
                               ScheduledContextService scheduledContextService, InternalEventDrivenJobService internalEventDrivenJobService,
                               String queueDir, ModuleMetaDataService moduleMetaDataService, JobLockCacheService jobLockCacheService)
    {
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.schedulerService = schedulerService;
        this.scheduledContextService = scheduledContextService;
        this.internalEventDrivenJobService = internalEventDrivenJobService;
        this.queueDir = queueDir;
        this.moduleMetaDataService = moduleMetaDataService;
        this.jobLockCacheService = jobLockCacheService;
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
                ContextTemplate contextTemplate = contextService.getContextTemplate(new String(contextFile));

//                ContextTemplateValidator contextTemplateValidator = new ContextTemplateValidator();
//                contextTemplateValidator.validate(contextTemplate);

                ScheduledContextRecord scheduledContextRecord = new SolrScheduledContextRecordImpl();
                scheduledContextRecord.setContextName(contextTemplate.getName());
                scheduledContextRecord.setContext(contextTemplate);
                scheduledContextRecord.setTimestamp(System.currentTimeMillis());
                this.scheduledContextService.save(scheduledContextRecord);

                ContextInstance contextInstance = contextService.getContextInstance(new String(contextFile));
                contextInstance.setId(UUID.randomUUID().toString());

                SearchResults<InternalEventDrivenJobRecord> internalEventDrivenJobRecordSearchResults
                    = this.internalEventDrivenJobService.findByContext(scheduledContextRecord.getContextName(), -1, -1);

                Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
                    .map(internalEventDrivenJobRecord -> internalEventDrivenJobRecord.getInternalEventDrivenJob())
                    .collect(Collectors.toMap(InternalEventDrivenJob::getIdentifier, Function.identity()));

                HashMap<String, ModuleMetaData> agents = new HashMap<>();
                internalEventDrivenJobMap.values().forEach(job -> {
                    if(!agents.containsKey(job.getAgentName())) {
                        agents.put(job.getAgentName(), moduleMetaDataService.findById(job.getAgentName()));
                    }
                });

                ContextMachine contextMachine = new ContextMachine(contextTemplate, contextInstance, scheduledContextInstanceService
                    , internalEventDrivenJobMap, this.queueDir, agents, jobLockCacheService);
                contextMachine.init();
                contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
                    schedulerService.raiseSchedulerJobInitiationEvent(event.getAgentUrl(), event);
                });
                contextMachine.addContextInstanceStateChangeEventListener(event -> ContextInstanceStateChangeEventBroadcaster.broadcast(event));

                DryRunParameters dryRunParameters = new DryRunParametersImpl();
                contextMachine.setDryRunParameters(dryRunParameters);

                ContextMachineCache.instance().put(contextMachine);
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
//            catch (InvalidContextTemplateException e) {
//                e.printStackTrace();
//            }

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
