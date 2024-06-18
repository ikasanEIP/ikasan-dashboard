package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import org.apache.commons.lang3.StringUtils;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobInstanceVisualisationDialog;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.JobLockCacheEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class JobLockCacheDialog extends AbstractCloseableResizableDialog implements JobLockCacheEventBroadcastListener {
    Logger logger = LoggerFactory.getLogger(JobLockCacheDialog.class);
    private ContextInstance contextInstance;
    private ModuleMetaDataService moduleMetaDataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private LogStreamingService logStreamingService;
    private JobInitiationService jobInitiationService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private ScheduledContextService scheduledContextService;
    private JobUtilsService jobUtilsService;
    private GlobalEventService globalEventService;

    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ContextProfileService contextProfileService;
    private Grid<JobLockHolder> grid;
    TextField filterTf = new TextField();
    private UI ui;

    private double jobVisualisationVerticalSpacing;
    private double jobVisualisationHorizontalSpacing;
    private double contextVisualisationLevelDistance;
    private double contextVisualisationNodeDistance;

    /**
     * Constructor
     *
     * @param contextInstance
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobInstanceService
     * @param logStreamingService
     * @param jobInitiationService
     * @param scheduledContextService
     * @param jobUtilsService
     * @param scheduledContextInstanceService
     * @param contextProfileService
     */
    public JobLockCacheDialog(ContextInstance contextInstance, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                              ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                              MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                              SchedulerJobInstanceService schedulerJobInstanceService, LogStreamingService logStreamingService,
                              JobInitiationService jobInitiationService, ScheduledContextService scheduledContextService,
                              JobUtilsService jobUtilsService, ScheduledContextInstanceService scheduledContextInstanceService, ContextProfileService contextProfileService,
                              GlobalEventService globalEventService, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing,
                              double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {
        this.contextInstance = contextInstance;
        if(this.contextInstance == null) {
            throw new IllegalArgumentException("contextInstance cannot be null!");
        }

        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }

        this.scheduledProcessManagementService = scheduledProcessManagementService;
        if(this.scheduledProcessManagementService == null) {
            throw new IllegalArgumentException("scheduledProcessManagementService cannot be null!");
        }

        this.configurationRestService = configurationRestService;
        if(this.configurationRestService == null) {
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }

        this.moduleControlRestService = moduleControlRestService;
        if(this.moduleControlRestService == null) {
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }

        this.metaDataRestService = metaDataRestService;
        if(this.metaDataRestService == null) {
            throw new IllegalArgumentException("metaDataRestService cannot be null!");
        }

        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }

        this.logStreamingService = logStreamingService;
        if(this.logStreamingService == null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }

        this.jobInitiationService = jobInitiationService;
        if(this.jobInitiationService == null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }

        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if(this.schedulerJobInstanceService == null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }

        this.jobUtilsService = jobUtilsService;
        if(this.jobUtilsService == null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if(this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }

        this.contextProfileService = contextProfileService;
        if(this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }

        this.globalEventService = globalEventService;
        if(this.globalEventService == null) {
            throw new IllegalArgumentException("globalEventService cannot be null!");
        }

        this.jobVisualisationVerticalSpacing = jobVisualisationVerticalSpacing;
        this.jobVisualisationHorizontalSpacing = jobVisualisationHorizontalSpacing;
        this.contextVisualisationLevelDistance = contextVisualisationLevelDistance;
        this.contextVisualisationNodeDistance = contextVisualisationNodeDistance;

        this.init();
    }

    /**
     * Initialise the component.
     */
    private void init() {
        this.setWidth("90vw");
        this.setHeight("80vh");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        grid = new Grid<>();
        grid.addColumn(JobLockHolder::getLockName)
            .setHeader(getTranslation("table-header.lock-name", UI.getCurrent().getLocale()))
            .setKey("lockName")
            .setFlexGrow(4);
        grid.addColumn(new ComponentRenderer<>(
            jobLockHolder -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidth("100%");
                verticalLayout.setSpacing(false);
                verticalLayout.setPadding(false);

                if(jobLockHolder.isExclusiveJobLock()) {
                    verticalLayout.add(new Text(getTranslation("label.exclusive", UI.getCurrent().getLocale())));
                }
                else {
                    verticalLayout.add(new Text(Long.toString(jobLockHolder.getLockCount())));
                }

                return verticalLayout;
            }))
            .setHeader(getTranslation("table-header.lock-count", UI.getCurrent().getLocale()))
            .setKey("lockCount")
            .setFlexGrow(1);
        grid.addColumn(new ComponentRenderer<>(
                jobLockHolder -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    Set<String> jobLockHolders = new HashSet<>();

                    if(jobLockHolder.isExclusiveJobLock()) {
                        if(JobLockCacheImpl.instance().getJobLockCacheData()
                            .getExclusiveLockHolder() != null) {
                            jobLockHolders = JobLockCacheImpl.instance().getJobLockCacheData()
                                .getExclusiveLockHolder().getLockHolders();
                        }
                    }
                    else {
                        jobLockHolders = jobLockHolder.getLockHolders();
                    }

                    if(jobLockHolders == null) {
                        logger.info("Lock Holders is Null! Job Lock Name[{}], Context Name[{}], Context Instance Id[{}]", jobLockHolder.getLockName()
                            , this.contextInstance.getName(), this.contextInstance.getId());
                    }

                    if(jobLockHolders != null && !jobLockHolders.isEmpty()) {
                        jobLockHolders.forEach(lockHolder -> {
                            String contextName = lockHolder.substring(lockHolder.indexOf(JobLockCacheImpl.CONTEXT_ID)+JobLockCacheImpl.CONTEXT_ID.length());
                            String jobIdentifier = lockHolder.substring(0
                                , lockHolder.indexOf(JobLockCacheImpl.CONTEXT_ID));

                            if(jobLockHolder.getSchedulerJobs() == null) {
                                logger.info("Lock Holder Scheduler Jobs are Null! Job Lock Name[{}], Context Name[{}], Context Instance Id[{}]", jobLockHolder.getLockName()
                                    , this.contextInstance.getName(), this.contextInstance.getId());
                                return;
                            }

                            List<SchedulerJobLockParticipant> schedulerJobs = jobLockHolder.getSchedulerJobs().get(contextName);

                            if(schedulerJobs == null && jobLockHolder.isExclusiveJobLock()) {
                                verticalLayout.add(new Text(getTranslation("label.exclusive-lock-held", UI.getCurrent().getLocale())));
                                return;
                            }
                            else if(schedulerJobs == null) {
                                String keys = StringUtils.join(jobLockHolder.getSchedulerJobs().values(), ',');
                                logger.info("Could not obtain scheduler jobs from lock holder scheduler jobs using key[{}]. Job Lock Name[{}], Context Name[{}], Context Instance Id[{}]. " +
                                    "The keys contained in the scheduler job map are[{}]", contextName, jobLockHolder.getLockName(), this.contextInstance.getName(), this.contextInstance.getId(), keys);
                                return;
                            }

                            SchedulerJob job = schedulerJobs.stream()
                                .filter(schedulerJob -> jobIdentifier.equals(schedulerJob.getIdentifier()))
                                .findAny()
                                .orElse(null);

                            Button lockHolderButton = new Button(job.getJobName());
                            lockHolderButton.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_RUNNING);
                            lockHolderButton.getElement().getStyle().set("color", IkasanColours.WHITE);
                            lockHolderButton.getElement().getStyle().set("margin-bottom", "5px");
                            lockHolderButton.getElement().setAttribute("title", contextName);
                            lockHolderButton.setIcon(VaadinIcon.SITEMAP.create());
                            verticalLayout.add(lockHolderButton);

                            lockHolderButton.addClickListener(event -> {
                                try {
                                    JobInstanceVisualisationDialog jobTemplateVisualisationDialog = new JobInstanceVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                                        this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService,
                                        this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService, this.scheduledContextInstanceService, this.contextProfileService,
                                        this.globalEventService, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance);
                                    jobTemplateVisualisationDialog.createSchedulerVisualisation(contextInstance, ContextHelper.getChildContextInstance(contextName, contextInstance));
                                    jobTemplateVisualisationDialog.open();
                                }
                                catch (IOException e) {
                                    e.printStackTrace();
                                    NotificationHelper.showErrorNotification(getTranslation("error.cannot-open-visualisation", UI.getCurrent().getLocale()));
                                }
                            });
                        });
                    }
                    else {
                        verticalLayout.add(new Text(getTranslation("label.no-current-lock-holders", UI.getCurrent().getLocale())));
                    }

                    return verticalLayout;
                }
            ))
            .setHeader(getTranslation("table-header.current-lock-holders", UI.getCurrent().getLocale()))
            .setFlexGrow(8);
        grid.addColumn(new ComponentRenderer<>(
                jobLockHolder -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    Queue<ContextualisedSchedulerJobInitiationEvent> contextualisedSchedulerJobInitiationEventQueue
                        = new LinkedList<>();

                    if(jobLockHolder.isExclusiveJobLock()) {
                        if(JobLockCacheImpl.instance().getJobLockCacheData()
                            .getExclusiveLockSchedulerJobInitiationEventWaitQueue() != null) {
                            contextualisedSchedulerJobInitiationEventQueue = JobLockCacheImpl.instance().getJobLockCacheData()
                                .getExclusiveLockSchedulerJobInitiationEventWaitQueue();
                        }
                    }
                    else {
                        contextualisedSchedulerJobInitiationEventQueue
                            = jobLockHolder.getSchedulerJobInitiationEventWaitQueue();
                    }

                    if(contextualisedSchedulerJobInitiationEventQueue != null
                        && !contextualisedSchedulerJobInitiationEventQueue.isEmpty()) {
                        AtomicBoolean initialIteration = new AtomicBoolean(true);

                        contextualisedSchedulerJobInitiationEventQueue.forEach(contextualisedSchedulerJobInitiationEvent -> {
                            if(!jobLockHolder.isExclusiveJobLock() || (jobLockHolder.isExclusiveJobLock() && jobLockHolder.getSchedulerJobs()
                                    .containsKey(contextualisedSchedulerJobInitiationEvent.getContextName()) &&
                                jobLockHolder.getSchedulerJobs()
                                    .get(contextualisedSchedulerJobInitiationEvent.getContextName()).stream()
                                    .filter(schedulerJob -> schedulerJob.getJobName().equals(contextualisedSchedulerJobInitiationEvent.getSchedulerJobInitiationEvent().getJobName()))
                                    .findFirst().isPresent())) {
                                Button queuedJobButton = new Button(contextualisedSchedulerJobInitiationEvent.getSchedulerJobInitiationEvent()
                                    .getInternalEventDrivenJob().getJobName());
                                queuedJobButton.setIcon(VaadinIcon.SITEMAP.create());
                                queuedJobButton.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_LOCK_QUEUED);
                                queuedJobButton.getElement().getStyle().set("color", IkasanColours.WHITE);
                                queuedJobButton.getElement().getStyle().set("margin-bottom", "5px");
                                queuedJobButton.getElement().setAttribute("title", contextualisedSchedulerJobInitiationEvent.getSchedulerJobInitiationEvent()
                                    .getInternalEventDrivenJob().getChildContextName());
                                verticalLayout.add(queuedJobButton);

                                queuedJobButton.addClickListener(event -> {
                                    try {
                                        JobInstanceVisualisationDialog jobTemplateVisualisationDialog = new JobInstanceVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                                            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService,
                                            this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService, this.scheduledContextInstanceService,
                                            this.contextProfileService, this.globalEventService, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance,
                                            this.contextVisualisationNodeDistance);
                                        jobTemplateVisualisationDialog.createSchedulerVisualisation(contextInstance, ContextHelper.getChildContextInstance(contextualisedSchedulerJobInitiationEvent.getSchedulerJobInitiationEvent()
                                            .getInternalEventDrivenJob().getChildContextName(), contextInstance));
                                        jobTemplateVisualisationDialog.open();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        NotificationHelper.showErrorNotification(getTranslation("error.cannot-open-visualisation", UI.getCurrent().getLocale()));
                                    }
                                });
                            }
                            else if(jobLockHolder.isExclusiveJobLock() && initialIteration.get()) {
                                verticalLayout.add(new Text(getTranslation("label.no-exclusive-jobs-queued", UI.getCurrent().getLocale())));
                            }

                            initialIteration.set(false);
                        });
                    }
                    else {
                        verticalLayout.add(new Text(getTranslation("label.no-queued-jobs", UI.getCurrent().getLocale())));
                    }

                    return verticalLayout;
                }
            ))
            .setHeader(getTranslation("table-header.queued-jobs-waiting-for-lock", UI.getCurrent().getLocale()))
            .setFlexGrow(8);

        grid.setWidthFull();
        layout.add(grid);

        this.populateGrid(null);

        HeaderRow hr = grid.appendHeaderRow();
        this.addGridFiltering(hr, "lockName");


        super.content.add(layout);
        super.title.setText(getTranslation("label.job-locks", UI.getCurrent().getLocale()));

        super.showResize(false);
        super.setResizable(false);
    }

    /**
     * Add filtering to the grid.
     *
     * @param hr
     * @param columnKey
     */
    public void addGridFiltering(HeaderRow hr, String columnKey) {
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        filterTf.setSuffixComponent(filterIcon);
        filterTf.setWidthFull();

        filterTf.addValueChangeListener(ev->{
            this.populateGrid(filterTf.getValue());
        });

        hr.getCell(grid.getColumnByKey(columnKey)).setComponent(filterTf);
    }


    /**
     * Populate the grid.
     *
     * @param filter
     */
    private void populateGrid(String filter) {
        List<JobLockHolder> jobLocks = JobLockCacheImpl.instance().getJobLockCacheData()
            .getJobLocksByLockName().values().stream()
            .filter(jobLockHolder ->
                jobLockHolder.getSchedulerJobs().entrySet().stream()
                    .filter(entry -> entry.getValue().stream()
                        .filter(job -> job.getContextName().equals(this.contextInstance.getName()))
                        .collect(Collectors.toList()).size() > 0)
                    .collect(Collectors.toList()).size() > 0
            ).collect(Collectors.toList());

        if(filter != null && !filter.isEmpty()) {
            jobLocks = jobLocks.stream()
                .filter(lock -> lock.getLockName().toLowerCase().contains(filter.toLowerCase()))
                .collect(Collectors.toList());
        }
        this.grid.setItems(jobLocks);
    }


    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();
        JobLockCacheEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.ui = null;
        JobLockCacheEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveBroadcast(JobLockCacheEvent event) {
        if(this.ui.isAttached()) {
            this.ui.access(() -> {
                populateGrid(this.filterTf.getValue());
            });
        }
    }
}
