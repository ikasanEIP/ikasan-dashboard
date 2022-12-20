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
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class JobLockCacheDialog extends AbstractCloseableResizableDialog {
    Logger logger = LoggerFactory.getLogger(JobLockCacheDialog.class);
    private Registration registration;
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

    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ContextProfileService contextProfileService;
    private Grid<JobLockHolder> grid;
    TextField filterTf = new TextField();

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
                              JobUtilsService jobUtilsService, ScheduledContextInstanceService scheduledContextInstanceService, ContextProfileService contextProfileService) {
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
        grid.addColumn(JobLockHolder::getLockCount)
            .setHeader(getTranslation("table-header.lock-count", UI.getCurrent().getLocale()))
            .setKey("lockCount")
            .setFlexGrow(1);
        grid.addColumn(new ComponentRenderer<>(
                jobLockHolder -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    if(jobLockHolder.getLockHolders() != null
                        && !jobLockHolder.getLockHolders().isEmpty()) {
                        jobLockHolder.getLockHolders().forEach(lockHolder -> {
                            String contextName = lockHolder.substring(lockHolder.indexOf(JobLockCacheImpl.CONTEXT_ID)+JobLockCacheImpl.CONTEXT_ID.length());
                            String jobIdentifier = lockHolder.substring(0
                                , lockHolder.indexOf(JobLockCacheImpl.CONTEXT_ID));

                            List<SchedulerJob> schedulerJobs = jobLockHolder.getSchedulerJobs().get(contextName);

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
                                        this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService, this.scheduledContextInstanceService, this.contextProfileService);
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

                    if(jobLockHolder.getSchedulerJobInitiationEventWaitQueue() != null
                        && !jobLockHolder.getSchedulerJobInitiationEventWaitQueue().isEmpty()) {
                        jobLockHolder.getSchedulerJobInitiationEventWaitQueue().forEach(lockHolder -> {
                            Button queuedJobButton = new Button(lockHolder.getSchedulerJobInitiationEvent()
                                .getInternalEventDrivenJob().getJobName());
                            queuedJobButton.setIcon(VaadinIcon.SITEMAP.create());
                            queuedJobButton.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_LOCK_QUEUED);
                            queuedJobButton.getElement().getStyle().set("color", IkasanColours.WHITE);
                            queuedJobButton.getElement().getStyle().set("margin-bottom", "5px");
                            queuedJobButton.getElement().setAttribute("title", lockHolder.getSchedulerJobInitiationEvent()
                                .getInternalEventDrivenJob().getChildContextName());
                            verticalLayout.add(queuedJobButton);

                            queuedJobButton.addClickListener(event -> {
                                try {
                                    JobInstanceVisualisationDialog jobTemplateVisualisationDialog = new JobInstanceVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService,
                                        this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService,
                                        this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService, this.scheduledContextInstanceService, this.contextProfileService);
                                    jobTemplateVisualisationDialog.createSchedulerVisualisation(contextInstance, ContextHelper.getChildContextInstance(lockHolder.getSchedulerJobInitiationEvent()
                                        .getInternalEventDrivenJob().getChildContextName(), contextInstance));
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
        UI ui = attachEvent.getUI();
        this.registration = JobLockCacheEventBroadcaster.register(jobLockCacheEvent -> {
            if(ui.isAttached()) {
                ui.access(() -> {
                    populateGrid(this.filterTf.getValue());
                });
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.registration.remove();
        this.registration = null;
    }
}
