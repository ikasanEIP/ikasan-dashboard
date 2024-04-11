package org.ikasan.dashboard.ui.scheduler.component;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.DryRunParametersImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceSearchFilterImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.scheduled.context.model.SolrScheduledContextRecordImpl;
import org.ikasan.scheduled.instance.service.SolrSchedulerJobInstancesInitialisationParametersImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.*;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContextUploadDialog extends AbstractCloseableResizableDialog {
    Logger logger = LoggerFactory.getLogger(ContextUploadDialog.class);

    private byte[] contextFile;

    private ScheduledContextInstanceService scheduledContextInstanceService;
    private JobInitiationService jobInitiationService;
    private ScheduledContextService scheduledContextService;
    private InternalEventDrivenJobService internalEventDrivenJobService;
    private String queueDir;
    private ModuleMetaDataService moduleMetaDataService;
    private JobLockCacheService jobLockCacheService;
    private ContextParametersInstanceService contextParametersInstanceService;
    private SchedulerJobInstanceService schedulerJobInstanceService;

    private JobLockCacheInitialisationService jobLockCacheInitialisationService;

    private ContextInstancePublicationService<ContextInstance> contextInstancePublicationService;

    /**
     * Constructor
     * TODO if this class stays around, it should leverage the base functionality in ContextInstanceServiceBase
     */
    public ContextUploadDialog(ScheduledContextInstanceService scheduledContextInstanceService, JobInitiationService jobInitiationService,
                               ScheduledContextService scheduledContextService, InternalEventDrivenJobService internalEventDrivenJobService,
                               String queueDir, ModuleMetaDataService moduleMetaDataService, JobLockCacheService jobLockCacheService,
                               ContextParametersInstanceService contextParametersInstanceService, SchedulerJobInstanceService schedulerJobInstanceService,
                               JobLockCacheInitialisationService jobLockCacheInitialisationService)
    {
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.jobInitiationService = jobInitiationService;
        this.scheduledContextService = scheduledContextService;
        this.internalEventDrivenJobService = internalEventDrivenJobService;
        this.queueDir = queueDir;
        this.moduleMetaDataService = moduleMetaDataService;
        this.jobLockCacheService = jobLockCacheService;
        this.contextParametersInstanceService = contextParametersInstanceService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.jobLockCacheInitialisationService = jobLockCacheInitialisationService;
        this.init();
    }

    private void init()
    {
        this.setModal(true);

        VerticalLayout verticalLayout = new VerticalLayout();

        H3 uploadContextHeader = new H3(getTranslation("label.context-upload", UI.getCurrent().getLocale()));

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setHeight("40px");
        header.add(uploadContextHeader);
        header.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, uploadContextHeader);
        header.getElement().getStyle().set("padding-bottom", "40px");

        verticalLayout.add(header);
        verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, header);


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

                ScheduledContextRecord scheduledContextRecord = new SolrScheduledContextRecordImpl();
                scheduledContextRecord.setContextName(contextTemplate.getName());
                scheduledContextRecord.setContext(contextTemplate);
                scheduledContextRecord.setTimestamp(System.currentTimeMillis());
                this.scheduledContextService.save(scheduledContextRecord);

                ContextInstance contextInstance = contextService.getContextInstance(new String(contextFile));
                contextInstance.setId(UUID.randomUUID().toString());

                // initialise all the scheduler job instances.
                SchedulerJobInstancesInitialisationParameters schedulerJobInstancesInitialisationParameters
                    = new SolrSchedulerJobInstancesInitialisationParametersImpl(false);
                this.schedulerJobInstanceService.initialiseSchedulerJobInstancesForContext(contextInstance
                    , schedulerJobInstancesInitialisationParameters);

                Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobMap = this.getInternalJobs(contextInstance.getId());

                HashMap<String, ModuleMetaData> agents = new HashMap<>();
                internalEventDrivenJobMap.values().forEach(job -> {
                    if(!agents.containsKey(job.getAgentName())) {
                        agents.put(job.getAgentName(), moduleMetaDataService.findById(job.getAgentName()));
                    }
                });

                Map<String, GlobalEventJobInstance> globalEventJobMap = this.getGlobalEventJobs(contextInstance.getId());
                globalEventJobMap.values().forEach(job -> {
                    if(!agents.containsKey(job.getAgentName())) {
                        agents.put(job.getAgentName(), moduleMetaDataService.findById(job.getAgentName()));
                    }
                });

                Map<String, QuartzScheduleDrivenJobInstance> quartzScheduleDrivenJobInstanceMap = this.getQuartzBasedJobs(contextInstance.getId());
                quartzScheduleDrivenJobInstanceMap.values().forEach(job -> {
                    if(!agents.containsKey(job.getAgentName())) {
                        agents.put(job.getAgentName(), moduleMetaDataService.findById(job.getAgentName()));
                    }
                });

                // if we are uploading a new context we add the all the locks
                JobLockCache jobLockCache = JobLockCacheImpl.instance();
                jobLockCache.setJobLockCacheService(jobLockCacheService);
                jobLockCache.addLocks(contextTemplate.getAllNestedJobLocks());

                ContextMachine contextMachine = new ContextMachine(contextTemplate, contextInstance, scheduledContextInstanceService, globalEventJobMap
                    , quartzScheduleDrivenJobInstanceMap, internalEventDrivenJobMap, this.queueDir, agents, this.moduleMetaDataService, jobLockCache, this.contextParametersInstanceService, this.scheduledContextService
                    , this.schedulerJobInstanceService, this.jobLockCacheInitialisationService, this.contextInstancePublicationService);
                contextMachine.init();

                // We add the listener to write initiation events to the agents.
                contextMachine.setSchedulerJobInitiationEventRaisedListener(event
                    -> jobInitiationService.raiseSchedulerJobInitiationEvent(event.getAgentUrl(), event));

                // We add a listener to broadcast any context state changes to interested parties.
                contextMachine.addContextInstanceStateChangeEventListener(event
                    -> ContextInstanceStateChangeEventBroadcaster.broadcast(event));

                // We add a listener to broadcast any job state changes to interested parties.
                contextMachine.addSchedulerJobStateChangeEventListener(event
                    -> SchedulerJobStateChangeEventBroadcaster.broadcast(event));

                // We add a listener to update scheduler job instances when a state change occurs.
                contextMachine.addSchedulerJobStateChangeEventListener( event
                    -> this.schedulerJobInstanceService.update(event.getSchedulerJobInstance()));

                DryRunParameters dryRunParameters = new DryRunParametersImpl();
                contextMachine.setDryRunParameters(dryRunParameters);

                ContextMachineCache.instance().put(contextMachine);
            }
            catch (Exception e) {
                NotificationHelper.showErrorNotification(getTranslation("error.context-upload", UI.getCurrent().getLocale()));
                e.printStackTrace();
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
        super.setHeight("300px");
    }

    private Map<String, InternalEventDrivenJobInstance> getInternalJobs(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults
            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
            .map(internalEventDrivenJobRecord -> (InternalEventDrivenJobInstance)internalEventDrivenJobRecord.getSchedulerJobInstance())
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity()));
        return internalEventDrivenJobMap;
    }

    private Map<String, GlobalEventJobInstance> getGlobalEventJobs(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType(JobConstants.GLOBAL_EVENT_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults
            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, GlobalEventJobInstance> globalEventJobMap = globalEventJobRecordSearchResults.getResultList().stream()
            .map(globalEventJobRecord -> (GlobalEventJobInstance) globalEventJobRecord.getSchedulerJobInstance())
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity()));
        return globalEventJobMap;
    }

    private Map<String, QuartzScheduleDrivenJobInstance> getQuartzBasedJobs(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults
            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, QuartzScheduleDrivenJobInstance> quartzScheduleDrivenJobInstanceMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
            .map(internalEventDrivenJobRecord -> (QuartzScheduleDrivenJobInstance)internalEventDrivenJobRecord.getSchedulerJobInstance())
            .collect(Collectors.toMap(key -> key.getIdentifier(), Function.identity()));

        filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setJobType(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE);
        internalEventDrivenJobRecordSearchResults
            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, QuartzScheduleDrivenJobInstance> fileEventDrivenJobs = internalEventDrivenJobRecordSearchResults.getResultList().stream()
            .map(internalEventDrivenJobRecord -> (QuartzScheduleDrivenJobInstance)internalEventDrivenJobRecord.getSchedulerJobInstance())
            .collect(Collectors.toMap(key -> key.getIdentifier(), Function.identity(), (a1, a2) -> a1));

        quartzScheduleDrivenJobInstanceMap.putAll(fileEventDrivenJobs);

        return quartzScheduleDrivenJobInstanceMap;
    }
}
