package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.ContextInstanceWidget;
import org.ikasan.dashboard.ui.scheduler.component.FileEventJobInstanceDialog;
import org.ikasan.dashboard.ui.scheduler.component.InternalEventDrivenJobInstanceDialog;
import org.ikasan.dashboard.ui.scheduler.component.QuartzDrivenScheduledJobInstanceDialog;
import org.ikasan.dashboard.ui.scheduler.listener.ContextSelectedListener;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextInstanceDraw2dAdapter;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.CanvasItemDoubleClickEvent;
import org.ikasan.designer.model.UserData;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JobSchedulerInstanceVisualisation extends SchedulerInstanceVisualisation {

    private static Logger logger = LoggerFactory.getLogger(ContextInstanceWidget.class);

    private List<ContextSelectedListener> contextSelectedListeners;

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
        , ScheduledContextService scheduledContextService) {
        super(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService
            , systemEventLogger, logStreamingService, schedulerJobInstanceService, jobInitiationService, jobUtilsService, scheduledContextService);

        this.contextSelectedListeners = new ArrayList<>();
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

            this.designerCanvas = new DesignerCanvas("canvas-viewport-"+ UUID.randomUUID(), this.dynamicImagePath, true);
            this.designerCanvas.clear();

            if (contextInstance.getScheduledJobs() != null && !contextInstance.getScheduledJobs().isEmpty()) {
                if(this.scheduledContextViewRecord == null) {
                    SearchResults<SchedulerJobInstanceRecord> jobs = this.schedulerJobInstanceService.getSchedulerJobInstancesByContextInstanceId(this.parentContextInstance.getId()
                        , -1, -1, null, null);

                    Map<String, SchedulerJob> schedulerJobs = jobs.getResultList().stream()
                        .map(record -> record.getSchedulerJobInstance())
                        .collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity(), (a1, a2) -> a1));

                    ContextInstanceDraw2dAdapter adapter = new ContextInstanceDraw2dAdapter();
                    this.designerCanvas.setCanvasJson(adapter.adaptJobs(this.parentContextInstance, this.contextInstance, schedulerJobs
                        , this.getCommandExecutionJobsForContextInstance(this.parentContextInstance.getId())));
                }
                else {
                    this.designerCanvas.setCanvasJson(adapter.adaptContextView(this.contextInstance, this.scheduledContextViewRecord.getContextView()));
                }
            }

            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);
            this.designerCanvas.addCanvasItemSingleClickEventListener(this);
            this.designerCanvas.addCanvasInitialisedListener(this);

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
            .forEach(entry -> result.put(entry.getKey(), (InternalEventDrivenJob) entry.getValue()));

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
            this.openFileWatcherJob(canvasItemDoubleClickEvent.getFigure().getUserData().getIdentifier());
        }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(UserData.INTERNAL_EVENT_DRIVEN_JOB)) {
            this.openCommandExecutionJob(canvasItemDoubleClickEvent.getFigure().getUserData().getIdentifier());
        }
        else if(canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(UserData.QUARTZ_EVENT_DRIVEN_JOB)) {
            this.openQuartzScheduledJob(canvasItemDoubleClickEvent.getFigure().getUserData().getIdentifier());
        }

        super.doubleClickEvent(canvasItemDoubleClickEvent);
    }

    /**
     * Helper method to load the job.
     *
     * @param identifier
     * @param jobType
     * @return
     */
    private SchedulerJobInstanceRecord loadJob(String identifier, String jobType) {
        SchedulerJobInstance schedulerJob = this.contextInstance.getScheduledJobsMap()
            .get(identifier);
        if (schedulerJob == null) {
            logger.warn("Could not retrieve job type [{}], job identifier [{}] from job plan instance [{}] " +
                    "with job plan instance id[{}] and child job plan name[{}]!",
                jobType, identifier, parentContextInstance.getName(),
                parentContextInstance.getId(), contextInstance.getName());

            NotificationHelper.showErrorNotification(getTranslation("error.could-not-load-file-watcher-job"));
            return null;
        }
        SchedulerJobInstanceRecord schedulerJobRecord = this.schedulerJobInstanceService.findByContextIdJobNameChildContextName
            (this.parentContextInstance.getId(), schedulerJob.getJobName(), this.contextInstance.getName());

        if (schedulerJobRecord == null) {
            logger.warn("Could not retrieve retrieve job type [{}], job identifoer [{}] from the database, job plan instance [{}] " +
                    ",job plan instance id[{}] and child job plan name[{}]!",
                jobType, schedulerJob.getJobName(), parentContextInstance.getName(), parentContextInstance.getId(),
                contextInstance.getName());

            NotificationHelper.showErrorNotification(getTranslation("error.could-not-load-command-execution-job"));
            return null;
        }

        return schedulerJobRecord;
    }

    /**
     * Open the file watcher job dialog.
     *
     * @param identifier of the job
     */
    private void openFileWatcherJob(String identifier) {
        SchedulerJobInstanceRecord schedulerJobRecord = this.loadJob(identifier, UserData.FILE_EVENT_DRIVEN_JOB);

        FileEventJobInstanceDialog fileEventJobDialog = new FileEventJobInstanceDialog
            (moduleMetaDataService.findById(schedulerJobRecord.getSchedulerJobInstance().getAgentName()),
                this.jobInitiationService, this.systemEventLogger, this.schedulerJobInstanceService);
        fileEventJobDialog.setJob(schedulerJobRecord);

        fileEventJobDialog.open();
    }

    /**
     * Open the command execution job dialog.
     *
     * @param identifier of the job
     */
    private void openCommandExecutionJob(String identifier) {
        SchedulerJobInstanceRecord schedulerJobRecord = this.loadJob(identifier, UserData.INTERNAL_EVENT_DRIVEN_JOB);

        InternalEventDrivenJobInstanceDialog internalEventDrivenJobInstanceDialog = new InternalEventDrivenJobInstanceDialog
            (moduleMetaDataService.findById(schedulerJobRecord.getSchedulerJobInstance().getAgentName()),
                scheduledProcessManagementService, configurationRestService, moduleControlRestService,
                metaDataRestService, systemEventLogger, this.schedulerJobInstanceService, this.parentContextInstance,
                this.jobInitiationService, moduleMetaDataService, this.logStreamingService, this.jobUtilsService);

        internalEventDrivenJobInstanceDialog.setJob(schedulerJobRecord);
        internalEventDrivenJobInstanceDialog.open();
    }

    /**
     * Open the quartz scheduled job dialog.
     *
     * @param identifier of the job
     */
    private void openQuartzScheduledJob(String identifier) {
        SchedulerJobInstanceRecord schedulerJobRecord = this.loadJob(identifier, UserData.QUARTZ_EVENT_DRIVEN_JOB);

        QuartzDrivenScheduledJobInstanceDialog quartzDrivenScheduledJobInstanceDialog = new QuartzDrivenScheduledJobInstanceDialog
            (moduleMetaDataService.findById(schedulerJobRecord.getSchedulerJobInstance().getAgentName()),
                this.jobInitiationService, systemEventLogger, this.schedulerJobInstanceService);

        quartzDrivenScheduledJobInstanceDialog.setJob(schedulerJobRecord);
        quartzDrivenScheduledJobInstanceDialog.open();
    }

    public void addContextSelectedListener(ContextSelectedListener listener) {
        this.contextSelectedListeners.add(listener);
    }
}
