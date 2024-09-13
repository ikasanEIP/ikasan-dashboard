package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.UI;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.*;
import org.ikasan.dashboard.ui.scheduler.listener.ContextSelectedListener;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextInstanceDraw2dAdapter;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.PositionedDialog;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.event.CanvasItemSingleClickEvent;
import org.ikasan.designer.event.JobMouseOverEvent;
import org.ikasan.designer.event.JobMouseOverListener;
import org.ikasan.designer.model.UserData;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JobSchedulerInstanceVisualisation extends SchedulerInstanceVisualisation implements JobMouseOverListener {

    private static Logger logger = LoggerFactory.getLogger(ContextInstanceWidget.class);

    private List<ContextSelectedListener> contextSelectedListeners;

    private ScheduledContextInstanceService scheduledContextInstanceService;
    private double jobVisualisationVerticalSpacing;
    private double jobVisualisationHorizontalSpacing;
    private double contextVisualisationLevelDistance;
    private double contextVisualisationNodeDistance;

    /**
     * Constructor
     *
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param logStreamingService
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param jobUtilsService
     * @param scheduledContextService
     */
    public JobSchedulerInstanceVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService
        , ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService, SystemEventLogger systemEventLogger
        , LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService, JobUtilsService jobUtilsService
        , ScheduledContextService scheduledContextService, GlobalEventService globalEventService, ScheduledContextInstanceService scheduledContextInstanceService, double jobVisualisationVerticalSpacing
        , double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {
        super(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService
            , systemEventLogger, logStreamingService, schedulerJobInstanceService, jobInitiationService, jobUtilsService, scheduledContextService, globalEventService, jobVisualisationVerticalSpacing
            , jobVisualisationHorizontalSpacing, contextVisualisationLevelDistance, contextVisualisationNodeDistance);

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.contextSelectedListeners = new ArrayList<>();
        this.jobVisualisationVerticalSpacing = jobVisualisationVerticalSpacing;
        this.jobVisualisationHorizontalSpacing = jobVisualisationHorizontalSpacing;
        this.contextVisualisationLevelDistance = contextVisualisationLevelDistance;
        this.contextVisualisationNodeDistance = contextVisualisationNodeDistance;
    }

    /**
     * Initialise the component.
     *
     * @throws IOException
     */
    protected void init() throws IOException {
        if(!initialised && contextInstance != null) {

            if (this.designerCanvas != null) {
                this.designerCanvas.clear();
                this.removeAll();
            }

            this.designerCanvas = new DesignerCanvas("canvas-viewport-"+ UUID.randomUUID(), this.dynamicImagePath
                , true, UI.getCurrent(), true);
            this.designerCanvas.clear();

            if (contextInstance.getScheduledJobs() != null && !contextInstance.getScheduledJobs().isEmpty()) {
                SearchResults<SchedulerJobInstanceRecord> jobs = this.schedulerJobInstanceService.getSchedulerJobInstancesByContextInstanceId(this.parentContextInstance.getId()
                    , -1, -1, null, null);

                Map<String, SchedulerJob> schedulerJobs = jobs.getResultList().stream()
                    .map(record -> record.getSchedulerJobInstance())
                    .collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity(), (a1, a2) -> a1));

                ContextInstanceDraw2dAdapter adapter = new ContextInstanceDraw2dAdapter(jobVisualisationVerticalSpacing,
                    jobVisualisationHorizontalSpacing, contextVisualisationLevelDistance, contextVisualisationNodeDistance);

                this.designerCanvas.setCanvasJson(adapter.adaptJobs(this.parentContextInstance, this.contextInstance, schedulerJobs
                    , this.getSchedulerJobsForContextInstance(this.parentContextInstance.getId())));
            }

            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);
            this.designerCanvas.addCanvasItemSingleClickEventListener(this);
            this.designerCanvas.addCanvasInitialisedListener(this);
            this.designerCanvas.addJobMouseOverEventListener(this);

            this.designerCanvas.manageClickableItems();

            this.add(initCanvasActions(), designerCanvas);

            this.initialised = true;
        }
    }

    /**
     * Helper method to get all command execution jobs associated with an context instance.
     *
     * @param contextInstanceId the id of the context instance that we want the jobs for.
     *
     * @return Map<String, InternalEventDrivenJobInstance> containing the command execution jobs
     * keyed on their identifier.
     */
    private Map<String, InternalEventDrivenJob> getCommandExecutionJobsForContextInstance(String contextInstanceId) {
        Map<String, InternalEventDrivenJob> result = new HashMap<>();
        this.schedulerJobInstanceService
            .getCommandExecutionJobsForContextInstance(contextInstanceId)
            .entrySet()
            .forEach(entry -> result.put(entry.getKey(), entry.getValue()));

        return result;
    }

    private Map<String, SchedulerJob> getSchedulerJobsForContextInstance(String contextInstanceId) {
        Map<String, SchedulerJob> result = new HashMap<>();
        this.schedulerJobInstanceService
            .getSchedulerJobInstancesByContextInstanceId(contextInstanceId, -1, -1, null, null)
            .getResultList()
            .forEach(entry -> result.put(entry.getSchedulerJobInstance().getIdentifier(), entry.getSchedulerJobInstance()));

        return result;
    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {
        if(canvasItemDoubleClickEvent.getFigure() != null &&
            canvasItemDoubleClickEvent.getFigure().getUserData() != null &&
            canvasItemDoubleClickEvent.getFigure().getUserData().getItemType() != null)
            if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(UserData.CONTEXT)) {
                this.contextSelectedListeners.forEach(listener
                    -> listener.contextSelected(canvasItemDoubleClickEvent.getFigure().getUserData().getContextName()));
            }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(UserData.FILE_EVENT_DRIVEN_JOB)) {
            this.openFileWatcherJob(canvasItemDoubleClickEvent.getFigure().getUserData().getIdentifier(),
                canvasItemDoubleClickEvent.getFigure().getUserData().getJobName());
        }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(UserData.INTERNAL_EVENT_DRIVEN_JOB)) {
            this.openCommandExecutionJob(canvasItemDoubleClickEvent.getFigure().getUserData().getIdentifier(),
                canvasItemDoubleClickEvent.getFigure().getUserData().getJobName());
        }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(UserData.QUARTZ_EVENT_DRIVEN_JOB)) {
            this.openQuartzScheduledJob(canvasItemDoubleClickEvent.getFigure().getUserData().getIdentifier(),
                canvasItemDoubleClickEvent.getFigure().getUserData().getJobName());
        }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(UserData.GLOBAL_EVENT_DRIVEN_JOB)) {
            this.openGlobalEventJob(canvasItemDoubleClickEvent.getFigure().getUserData().getIdentifier(),
                canvasItemDoubleClickEvent.getFigure().getUserData().getJobName());
        }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(UserData.LOCAL_EVENT_JOB)) {
            this.openLocalEventJob(canvasItemDoubleClickEvent.getFigure().getUserData().getIdentifier(),
                canvasItemDoubleClickEvent.getFigure().getUserData().getJobName());
        }

        super.doubleClickEvent(canvasItemDoubleClickEvent);
    }

    @Override
    public void singleClickEvent(CanvasItemSingleClickEvent canvasItemDoubleClickEvent) {
        if(canvasItemDoubleClickEvent.getFigure() != null && canvasItemDoubleClickEvent.getFigure().getIdentifier() != null) {
            String identifier = ContextHelper.getIdentifier(canvasItemDoubleClickEvent.getFigure().getIdentifier());
            logger.debug("Click event - " + identifier);

            this.nodeConnectionIndicators.forEach(nodeConnectionIndicator
                -> this.designerCanvas.removeFigure(nodeConnectionIndicator));

            this.nodeConnectionIndicators.clear();

            if(canvasItemDoubleClickEvent.getFigure().getUserData() != null &&
                canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(UserData.CONTEXT)) {
                canvasItemDoubleClickEvent.getFigure().getUserData().getSubsequentJobIdentifiers().forEach(id -> {
                    String nodeConnectorIndicator = UUID.randomUUID().toString();
                    this.nodeConnectionIndicators.add(nodeConnectorIndicator);
                    this.designerCanvas.addImageToFigure(id, nodeConnectorIndicator,
                        "frontend/images/mr-squid-head.png", 49.6, 37.8);
                });
            }

            SchedulerJobInstance job = this.contextInstance.getScheduledJobsMap()
                .get(identifier);

            if(job != null) {

                List<String> residingContexts = ContextHelper.getContextsWhereJobFilterMatchResides
                    (this.parentContextInstance, job.getJobName());

                residingContexts.forEach(context -> {
                    Map<String, SchedulerJob> lastJobs = ContextHelper.getJobsOutsideLogicalGrouping(this.contextInstance);

                    if(lastJobs.containsKey(job.getIdentifier())) {
                        String nodeConnectorIndicator = UUID.randomUUID().toString();
                        this.nodeConnectionIndicators.add(nodeConnectorIndicator);
                        this.designerCanvas.addImageToFigure(context + "_out", nodeConnectorIndicator,
                            "frontend/images/mr-squid-head.png", 49.6, 37.8);
                        this.designerCanvas.addImageToFigure(context, nodeConnectorIndicator,
                            "frontend/images/mr-squid-head.png", 49.6, 37.8);
                    }
                });

                LinkedList<List<SchedulerJob>> jobs
                    = ContextHelper.traceJobThroughContext(this.parentContextInstance, job.getJobName()
                    , this.contextInstance.getName());

                if (!jobs.isEmpty()) {
                    jobs.get(0).forEach(downstreamJob -> {
                        String nodeConnectorIndicator = UUID.randomUUID().toString();
                        this.nodeConnectionIndicators.add(nodeConnectorIndicator);
                        this.designerCanvas.addImageToFigure(downstreamJob.getIdentifier(), nodeConnectorIndicator,
                            "frontend/images/mr-squid-head.png", 49.6, 37.8);
                    });
                }
            }
        }
        super.singleClickEvent(canvasItemDoubleClickEvent);
    }

    @Override
    public void onJobMouseOverEvent(JobMouseOverEvent event) {
        if(event.getFigure().getUserData().getItemType().equals("INTERNAL_EVENT_DRIVEN_JOB")) {
            SchedulerJobInstanceRecord jobInstanceRecord = this.loadJob(event.getFigure().getUserData().getIdentifier(), event.getFigure().getUserData().getJobName(),
                JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
            CommandExecutionJobPositionedDialog positionedDialog = new CommandExecutionJobPositionedDialog(jobInstanceRecord, this.logStreamingService, this.moduleMetaDataService,
                this.schedulerJobInstanceService, this.scheduledContextInstanceService, this.parentContextInstance);
            PositionedDialog.Position position = new PositionedDialog.Position(event.getFigure().getY(), event.getFigure().getX());
            positionedDialog.setPosition(position);
            positionedDialog.open();
        }
    }

    /**
     * Helper method to load the job.
     *
     * @param jobName
     * @param jobType
     * @return
     */
    private SchedulerJobInstanceRecord loadJob(String identifier, String jobName, String jobType) {
        SchedulerJobInstance schedulerJob = this.contextInstance.getScheduledJobsMap()
            .get(identifier);
        if (schedulerJob == null) {
            logger.warn("Could not retrieve job type [{}], job identifier [{}] from job plan instance [{}] " +
                    "with job plan instance id[{}] and child job plan name[{}]!",
                jobType, jobName, parentContextInstance.getName(),
                parentContextInstance.getId(), contextInstance.getName());

            NotificationHelper.showErrorNotification(getTranslation("error.could-not-load-job"));
            return null;
        }
        SchedulerJobInstanceRecord schedulerJobRecord = this.schedulerJobInstanceService.findById(jobName +
            "_" + this.parentContextInstance.getId() + "_" + this.contextInstance.getName() + "_" + jobType);

        if (schedulerJobRecord == null) {
            logger.warn("Could not retrieve retrieve job type [{}], job identifoer [{}] from the database, job plan instance [{}] " +
                    ",job plan instance id[{}] and child job plan name[{}]!",
                jobType, schedulerJob.getJobName(), parentContextInstance.getName(), parentContextInstance.getId(),
                contextInstance.getName());

            NotificationHelper.showErrorNotification(getTranslation("error.could-not-load-job"));
            return null;
        }

        return schedulerJobRecord;
    }

    /**
     * Open the file watcher job dialog.
     *
     * @param jobName of the job
     */
    private void openFileWatcherJob(String identifier, String jobName) {
        SchedulerJobInstanceRecord schedulerJobRecord = this.loadJob(identifier, jobName, JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE);

        FileEventJobInstanceDialog fileEventJobDialog = new FileEventJobInstanceDialog
            (moduleMetaDataService.findById(schedulerJobRecord.getSchedulerJobInstance().getAgentName()),
                this.jobInitiationService, this.systemEventLogger, this.schedulerJobInstanceService, this.parentContextInstance);
        fileEventJobDialog.setJob(schedulerJobRecord);

        fileEventJobDialog.open();
    }

    /**
     * Open the command execution job dialog.
     *
     * @param identifier of the job
     */
    private void openCommandExecutionJob(String identifier, String jobName) {
        SchedulerJobInstanceRecord schedulerJobRecord = this.loadJob(identifier, jobName, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);

        InternalEventDrivenJobInstanceDialog internalEventDrivenJobInstanceDialog = new InternalEventDrivenJobInstanceDialog
            (moduleMetaDataService.findById(schedulerJobRecord.getSchedulerJobInstance().getAgentName()),
                scheduledProcessManagementService, configurationRestService, moduleControlRestService,
                metaDataRestService, systemEventLogger, this.schedulerJobInstanceService, this.parentContextInstance,
                this.jobInitiationService, moduleMetaDataService, this.logStreamingService, this.jobUtilsService,
                this.scheduledContextInstanceService);

        internalEventDrivenJobInstanceDialog.setJob(schedulerJobRecord);
        internalEventDrivenJobInstanceDialog.open();
    }

    /**
     * Open the quartz scheduled job dialog.
     *
     * @param identifier of the job
     */
    private void openQuartzScheduledJob(String identifier, String jobName) {
        SchedulerJobInstanceRecord schedulerJobRecord = this.loadJob(identifier, jobName, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);

        QuartzDrivenScheduledJobInstanceDialog quartzDrivenScheduledJobInstanceDialog = new QuartzDrivenScheduledJobInstanceDialog
            (moduleMetaDataService.findById(schedulerJobRecord.getSchedulerJobInstance().getAgentName()),
                this.jobInitiationService, systemEventLogger, this.schedulerJobInstanceService, this.parentContextInstance);

        quartzDrivenScheduledJobInstanceDialog.setJob(schedulerJobRecord);
        quartzDrivenScheduledJobInstanceDialog.open();
    }

    private void openGlobalEventJob(String identifier, String jobName) {
        SchedulerJobInstanceRecord schedulerJobRecord = this.loadJob(identifier, jobName, JobConstants.GLOBAL_EVENT_JOB_INSTANCE);

        GlobalEventJobInstanceDialog globalEventJobInstanceDialog = new GlobalEventJobInstanceDialog(systemEventLogger, schedulerJobInstanceService, this.globalEventService
            , this.parentContextInstance);
        globalEventJobInstanceDialog.setJob(schedulerJobRecord);

        globalEventJobInstanceDialog.open();
    }

    private void openLocalEventJob(String identifier, String jobName) {
        SchedulerJobInstanceRecord schedulerJobRecord = this.loadJob(identifier, jobName, JobConstants.LOCAL_EVENT_JOB_INSTANCE);

        LocalEventJobInstanceDialog localEventJobInstanceDialog = new LocalEventJobInstanceDialog(systemEventLogger
            , schedulerJobInstanceService, this.parentContextInstance);
        localEventJobInstanceDialog.setJob(schedulerJobRecord);

        localEventJobInstanceDialog.open();
    }

    public void addContextSelectedListener(ContextSelectedListener listener) {
        this.contextSelectedListeners.add(listener);
    }
}
