package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.UI;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.SelectContextDialog;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextTemplateDraw2dAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.Draw2dCanvasJsonHelper;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.designer.event.CanvasItemRightClickEvent;
import org.ikasan.designer.event.CanvasItemSingleClickEvent;
import org.ikasan.designer.event.FigureDeleteEvent;
import org.ikasan.designer.model.Image;
import org.ikasan.designer.model.UserData;
import org.ikasan.job.orchestration.builder.context.ContextTemplateBuilder;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.*;
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

    private UI ui;

    public JobSchedulerVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                     ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                                     SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService, LogStreamingService logStreamingService, JobInitiationService jobInitiationService,
                                     ContextProfileService contextProfileService, UserService userService, SecurityService securityService, JobProvisionService jobProvisionService,
                                     ScheduledContextService scheduledContextService,
                                     Map<String, String> schedulerJobExecutionEnvironmentLabel, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing,
                                     double contextVisualisationLevelDistance, double contextVisualisationNodeDistance, boolean showPrettyFormattedDiagram) {
        super(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService
            , metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, jobInitiationService, contextProfileService
            , userService, securityService, jobProvisionService, scheduledContextService, schedulerJobExecutionEnvironmentLabel
            , jobVisualisationVerticalSpacing, jobVisualisationHorizontalSpacing, contextVisualisationLevelDistance, contextVisualisationNodeDistance
            , showPrettyFormattedDiagram);
    }

    protected void init(UI ui) throws IOException {
        this.ui = ui;
        if(!initialised && contextTemplate != null) {

            if (this.designerCanvas != null) {
                this.removeAll();
            }

            this.initialiseCanvas();

            if(contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
                if(this.scheduledContextViewRecord == null) {
                    this.setCanvasJson();
                }
                else {
                    this.designerCanvas.setCanvasJson(this.scheduledContextViewRecord.getContextView());
                }
            }

            this.add(initCanvasActions(), designerCanvas);

            this.initialised = true;
        }
    }

    private void initialiseCanvas() throws IOException {
        this.designerCanvas = new DesignerCanvas(this, null, "canvas-viewport-"+ UUID.randomUUID(), this.dynamicImagePath, !this.edit, ui, true);
        this.designerCanvas.addCanvasInitialisedListener(this);

        this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
        this.designerCanvas.addCanvasItemRightClickEventListener(this);
        this.designerCanvas.addConnectorEventListener(this);
        this.designerCanvas.addCanvasUpdatedListener(this);
        this.designerCanvas.addFigureDeleteEventListeners(this);
        this.designerCanvas.addFigureUndoDeleteEventListeners(this);
        this.designerCanvas.addCanvasItemSingleClickEventListener(this);
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

    @Override
    public void figureDeleted(FigureDeleteEvent figureDeleteEvent) {
        if(figureDeleteEvent.getFigure().getUserData().getItemType().equals(UserData.CONTEXT)) {
            // we are removing a link here
            ContextTemplate contextLinkToDelete = (ContextTemplate) ContextHelper.getChildContext
                (figureDeleteEvent.getFigure().getUserData().getContextName(), this.parentContextTemplate);

            Optional<ContextTerminalJob> contextTerminalJob = ContextHelper.getContextTerminalJobFromContext(this.contextTemplate);

            if(contextTerminalJob.isPresent()) {
                contextLinkToDelete.getScheduledJobs().remove(contextTerminalJob.get());
                Optional<JobDependency> jobDependencyToRemove = contextLinkToDelete.getJobDependencies().stream()
                    .filter(jobDependency -> jobDependency.getLogicalGrouping() != null
                        && jobDependency.getLogicalGrouping().getAnd() != null
                        && jobDependency.getLogicalGrouping().getAnd().stream()
                        .filter(and -> and.getIdentifier().equals(contextTerminalJob.get().getIdentifier())).findFirst().isPresent()).findFirst();

                if(jobDependencyToRemove.isPresent()) {
                    if(jobDependencyToRemove.get().getLogicalGrouping().getAnd().size() > 1) {
                        jobDependencyToRemove.get().getLogicalGrouping().getAnd()
                            .removeIf(a -> a.getIdentifier().equals(contextTerminalJob.get().getIdentifier()));
                    }
                    else {
                        contextLinkToDelete.getJobDependencies().remove(jobDependencyToRemove.get());
                    }
                }
            }
        }
    }

    public void linkToContext(ContextTerminalJob contextTerminalJob, ContextStartJob contextStartJob, ContextTemplate context) throws IOException {
        ContextTemplateBuilder contextTemplateBuilder = new ContextTemplateBuilder();
        context.getScheduledJobs().add(contextTerminalJob);
        if(context.getJobDependencies() == null)context.setJobDependencies(new ArrayList<>());

        Optional<JobDependency> optionalJobDependency = context.getJobDependencies().stream()
            .filter(jobDependency -> jobDependency.getJobIdentifier()
                .equals(contextStartJob.getIdentifier())).findFirst();

        if(optionalJobDependency.isPresent()) {
            if(optionalJobDependency.get().getLogicalGrouping().getAnd() == null) {
                optionalJobDependency.get().getLogicalGrouping().setAnd(new ArrayList<>());
            }

            optionalJobDependency.get().getLogicalGrouping().getAnd()
                .add(contextTemplateBuilder.getJobAndBuilder().withJobName(contextTerminalJob.getJobName())
                .withAgentName(contextTerminalJob.getAgentName()).build());
        }
        else {
            context.getJobDependencies().add(contextTemplateBuilder.getJobDependencyBuilder().withJobName(contextStartJob.getJobName())
                .withAgentName(contextStartJob.getAgentName())
                .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                    .addAnd(contextTemplateBuilder.getJobAndBuilder().withJobName(contextTerminalJob.getJobName())
                        .withAgentName(contextTerminalJob.getAgentName()).build()).build()).build());
        }
        ContextHelper.replaceChildContextTemplate(this.parentContextTemplate, context);
        this.setCanvasJson();
        this.designerCanvas.importJson();
    }

    @Override
    public void rightClickEvent(CanvasItemRightClickEvent canvasItemRightClickEvent) {
        if(canvasItemRightClickEvent.getFigure() != null && canvasItemRightClickEvent.getFigure().getIdentifier() != null) {
            String identifier = ContextHelper.getIdentifier(canvasItemRightClickEvent.getFigure().getIdentifier());

            SchedulerJob job = this.contextTemplate.getScheduledJobsMap()
                .get(identifier);


            if(job.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)) {
                if(this.saveRequired) {
                    NotificationHelper.showUserNotification("You must save the job plan before linking a terminal job!");
                    return;
                }
                SelectContextDialog selectContextDialog = new SelectContextDialog
                    (super.systemEventLogger, super.parentContextTemplate, this.contextTemplate, this);
                selectContextDialog.open();
            }
        }
        super.rightClickEvent(canvasItemRightClickEvent);
    }

    private void setCanvasJson() throws IOException {
        SearchResults<SchedulerJobRecord> jobs = this.schedulerJobService.findByContext(parentContextTemplate.getName(), -1, -1);

        Map<String, SchedulerJob> schedulerJobs = jobs.getResultList().stream()
            .map(record -> record.getJob())
            .collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity(), (key1, key2)-> key2));

        schedulerJobs.putAll(ContextHelper.getContextTerminalJobsFromContext(parentContextTemplate).stream()
            .collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity(), (key1, key2)-> key2)));

        schedulerJobs.putAll(ContextHelper.getContextStartJobsFromContext(parentContextTemplate).stream()
            .collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity(), (key1, key2)-> key2)));

        schedulerJobs.putAll(ContextHelper.getLocalEventJobsFromContext(parentContextTemplate).stream()
            .collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity(), (key1, key2)-> key2)));

        schedulerJobs.putAll(ContextHelper.getBridgingJobsFromContext(parentContextTemplate).stream()
            .collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity(), (key1, key2)-> key2)));

        ContextTemplateDraw2dAdapter contextTemplateDraw2dAdapter = new ContextTemplateDraw2dAdapter();

        Map<String, Image> schedulerJobImageMap = new HashMap<>();
        if(this.contextTemplate.getUserGeneratedLayout() != null
            && !this.contextTemplate.getUserGeneratedLayout().isEmpty()
            && !this.showPrettyFormattedDiagram) {
            schedulerJobImageMap = Draw2dCanvasJsonHelper.getSchedulerJobImagesFromCanvasJson
                (this.contextTemplate.getUserGeneratedLayout());
        }

        this.designerCanvas.setCanvasJson(contextTemplateDraw2dAdapter.adaptJobs(this.parentContextTemplate, contextTemplate, schedulerJobs,
            schedulerJobs.values().stream().collect(Collectors.toMap(SchedulerJob::getIdentifier, Function.identity(), (key1, key2)-> key2)),
            schedulerJobImageMap));
    }
}
