package org.ikasan.dashboard.ui.visualisation.scheduler.component;

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
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JobSchedulerInstanceVisualisation extends SchedulerInstanceVisualisation {

    private List<ContextSelectedListener> contextSelectedListeners;

    public JobSchedulerInstanceVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService
        , ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService, SystemEventLogger systemEventLogger
        , LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService, JobUtilsService jobUtilsService
        , ScheduledContextService scheduledContextService) {
        super(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService
            , systemEventLogger, logStreamingService, schedulerJobInstanceService, jobInitiationService, jobUtilsService, scheduledContextService);

        this.contextSelectedListeners = new ArrayList<>();
    }

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
            .forEach(entry -> result.put(entry.getKey(), entry.getValue()));

        return result;
    }

    @Override
    public void doubleClickEvent(CanvasItemDoubleClickEvent canvasItemDoubleClickEvent) {
        if(canvasItemDoubleClickEvent.getFigure() != null &&
            canvasItemDoubleClickEvent.getFigure().getUserData() != null &&
            canvasItemDoubleClickEvent.getFigure().getUserData().getItemType() != null &&
            canvasItemDoubleClickEvent.getFigure().getUserData().getItemType().equals(UserData.CONTEXT)) {
            this.contextSelectedListeners.forEach(listener
                -> listener.contextSelected(canvasItemDoubleClickEvent.getFigure().getUserData().getContextName()));
        }

        super.doubleClickEvent(canvasItemDoubleClickEvent);
    }

    public void addContextSelectedListener(ContextSelectedListener listener) {
        this.contextSelectedListeners.add(listener);
    }
}
