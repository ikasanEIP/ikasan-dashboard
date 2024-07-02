package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.UI;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.CanvasItemSingleClickEvent;
import org.ikasan.designer.model.UserData;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.search.SearchResults;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JobSchedulerVisualisation extends SchedulerVisualisation {

    public JobSchedulerVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                     ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                                     SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService, LogStreamingService logStreamingService, JobInitiationService jobInitiationService,
                                     ContextProfileService contextProfileService, UserService userService, SecurityService securityService, JobProvisionService jobProvisionService,
                                     ScheduledContextService scheduledContextService, Map<String, String> schedulerJobExecutionEnvironmentLabel, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing,
                                     double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {
        super(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService
            , metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, jobInitiationService, contextProfileService
            , userService, securityService, jobProvisionService, scheduledContextService, schedulerJobExecutionEnvironmentLabel
            ,  jobVisualisationVerticalSpacing, jobVisualisationHorizontalSpacing, contextVisualisationLevelDistance, contextVisualisationNodeDistance);
    }

    protected void init(UI ui) throws IOException {
        if(!initialised && contextTemplate != null) {

            if (this.designerCanvas != null) {
                this.removeAll();
            }

            this.designerCanvas = new DesignerCanvas(this, null, "canvas-viewport-"+ UUID.randomUUID().toString(), this.dynamicImagePath, !this.edit, ui, true);
            this.designerCanvas.addCanvasInitialisedListener(this);

            if(contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
                if(this.scheduledContextViewRecord == null) {
                    SearchResults<SchedulerJobRecord> jobs = this.schedulerJobService.findByContext(parentContextTemplate.getName(), -1, -1);

                    Map<String, SchedulerJob> schedulerJobs = jobs.getResultList().stream()
                        .map(record -> record.getJob())
                        .collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity(), (key1, key2)-> key2));

                    this.designerCanvas.setCanvasJson(adapter.adaptJobs(this.parentContextTemplate, contextTemplate, schedulerJobs,
                        this.getSchedulerJobsForContextInstance(this.parentContextTemplate.getName())));
                }
                else {
                    this.designerCanvas.setCanvasJson(this.scheduledContextViewRecord.getContextView());
                }
            }

            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);
            this.designerCanvas.addConnectorEventListener(this);
            this.designerCanvas.addCanvasUpdatedListener(this);
            this.designerCanvas.addFigureDeleteEventListeners(this);
            this.designerCanvas.addFigureUndoDeleteEventListeners(this);
            this.designerCanvas.addCanvasItemSingleClickEventListener(this);

            this.add(initCanvasActions(), designerCanvas);

            this.initialised = true;
        }
    }

    @Override
    public void singleClickEvent(CanvasItemSingleClickEvent canvasItemDoubleClickEvent) {
        if(canvasItemDoubleClickEvent.getFigure() != null && canvasItemDoubleClickEvent.getFigure().getIdentifier() != null) {
            String identifier = ContextHelper.getIdentifier(canvasItemDoubleClickEvent.getFigure().getIdentifier());

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

            SchedulerJob job = this.contextTemplate.getScheduledJobsMap()
                .get(identifier);

            if(job != null) {

                List<String> residingContexts = ContextHelper.getContextsWhereJobFilterMatchResides
                    (this.parentContextTemplate, job.getJobName());

                residingContexts.forEach(context -> {
                    Map<String, SchedulerJob> lastJobs = ContextHelper.getJobsOutsideLogicalGrouping(this.contextTemplate);

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
                    = ContextHelper.traceJobThroughContext(this.parentContextTemplate, job.getJobName()
                    , this.contextTemplate.getName());

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

    /**
     * Helper method to get all command execution jobs associated with an context instance.
     *
     * @param contextInstanceId the id of the context instance that we want the jobs for.
     *
     * @return Map<String, InternalEventDrivenJobInstance> containing the command execution jobs
     * keyed on their identifier.
     */
    private Map<String, InternalEventDrivenJob> getCommandExecutionJobsForContextInstance(String contextInstanceId) {
        return this.schedulerJobService
            .getCommandExecutionJobsForContext(contextInstanceId);

    }

    private Map<String, SchedulerJob> getSchedulerJobsForContextInstance(String contextInstanceId) {
        // todo provide method to get all jobs
        return Map.of();

    }
}
