package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.*;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.general.SchedulerService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.systemevent.SystemEventSearchService;

@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
public class SchedulerAgentDashboardView extends HorizontalLayout implements BeforeEnterObserver
{
    private ModuleMetaDataService moduleMetadataService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SchedulerService schedulerService;
    private SchedulerJobService schedulerJobService;
    private SystemEventLogger systemEventLogger;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private String dynamicImagePath;
    private ModuleMetaDataService moduleMetaDataService;
    private LogStreamingService logStreamingService;
    private JobInitiationService jobInitiationService;
    private ContextProfileService contextProfileService;
    private JobUtilsService jobUtilsService;
    private ScheduledContextService scheduledContextService;
    private GlobalEventService globalEventService;
    private ContextInstanceRegistrationService contextInstanceRegistrationService;
    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;
    private DownloadLogFileService downloadLogFileService;
    private JobProvisionService jobProvisionService;
    private SystemEventSearchService systemEventSearchService;

    private double jobVisualisationVerticalSpacing;
    private double jobVisualisationHorizontalSpacing;
    private double contextVisualisationLevelDistance;
    private double contextVisualisationNodeDistance;

    private Board board;

    private boolean initialised = false;


    /**
     * Constructs an instance of the SchedulerAgentDashboardView class, initializing its dependencies and layout configuration.
     *
     * @param moduleMetadataService Provides services related to module metadata operations.
     * @param configurationRestService Handles configuration management through RESTful services.
     * @param moduleControlRestService Manages module-level control operations through RESTful services.
     * @param metaDataRestService Provides metadata-related services for the application.
     * @param systemEventLogger Facilitates logging of system events.
     * @param schedulerService Manages scheduling-related operations.
     * @param schedulerJobService Handles job operations within the scheduler.
     * @param schedulerJobInstanceService Provides services for managing job instances.
     * @param scheduledContextInstanceService Handles scheduled context instance operations.
     * @param dynamicImagePath The file path for dynamically generated images.
     * @param moduleMetaDataService Facilitates additional module metadata-related operations.
     * @param logStreamingService Provides services for streaming logs in real-time.
     * @param jobInitiationService Handles job initiation and scheduling activities.
     * @param contextProfileService Manages operations related to context profiles.
     * @param jobUtilsService Provides utilities for job-related operations.
     * @param scheduledContextService Handles scheduled context operations.
     * @param globalEventService Manages global events across the application.
     * @param contextInstanceRegistrationService Provides registration services for context instances.
     * @param downloadLogFileService Handles services related to downloading log files.
     * @param contextInstanceSchedulerService Manages scheduling for context instances.
     * @param jobProvisionService Facilitates job provisioning and preparation.
     * @param systemEventSearchService Provides search and query capabilities for system events.
     * @param jobVisualisationVerticalSpacing The vertical spacing for job visualization on the dashboard.
     * @param jobVisualisationHorizontalSpacing The horizontal spacing for job visualization on the dashboard.
     * @param contextVisualisationLevelDistance The distance between visualization levels for contexts.
     * @param contextVisualisationNodeDistance The distance between visualization nodes within the same context level.
     */
    public SchedulerAgentDashboardView(ModuleMetaDataService moduleMetadataService, ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                       MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerService schedulerService, SchedulerJobService schedulerJobService,
                                       SchedulerJobInstanceService schedulerJobInstanceService, ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath,
                                       ModuleMetaDataService moduleMetaDataService, LogStreamingService logStreamingService,
                                       JobInitiationService jobInitiationService, ContextProfileService contextProfileService,
                                       JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService,
                                       GlobalEventService globalEventService, ContextInstanceRegistrationService contextInstanceRegistrationService,
                                       DownloadLogFileService downloadLogFileService, ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService, JobProvisionService jobProvisionService,
                                       SystemEventSearchService systemEventSearchService, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance,
                                       double contextVisualisationNodeDistance) {
        this.moduleMetadataService = moduleMetadataService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerService = schedulerService;
        this.schedulerJobService = schedulerJobService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.dynamicImagePath = dynamicImagePath;
        this.moduleMetaDataService = moduleMetaDataService;
        this.logStreamingService = logStreamingService;
        this.jobInitiationService = jobInitiationService;
        this.contextProfileService = contextProfileService;
        this.jobUtilsService = jobUtilsService;
        this.scheduledContextService = scheduledContextService;
        this.globalEventService = globalEventService;
        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        this.downloadLogFileService = downloadLogFileService;
        this.contextInstanceSchedulerService = contextInstanceSchedulerService;
        this.jobProvisionService = jobProvisionService;
        this.jobVisualisationVerticalSpacing = jobVisualisationVerticalSpacing;
        this.jobVisualisationHorizontalSpacing = jobVisualisationHorizontalSpacing;
        this.contextVisualisationLevelDistance = contextVisualisationLevelDistance;
        this.contextVisualisationNodeDistance = contextVisualisationNodeDistance;
        this.systemEventSearchService = systemEventSearchService;

        board = new Board();
        board.addClassName("styled");
        board.setSizeFull();

        this.add(board);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!initialised) {
            board.addRow(new AgentWidget(this.moduleMetadataService, this.configurationRestService, this.moduleControlRestService
                , this.metaDataRestService, this.systemEventLogger, this.schedulerService, this.jobProvisionService
                , this.schedulerJobService, this.downloadLogFileService, this.scheduledContextService)
                , new SchedulerStatusWidget(this.moduleMetadataService, UI.getCurrent()));

            board.addRow(new ContextInstanceDashboardWidget(this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger
                , this.schedulerService, this.schedulerJobService, this.schedulerJobInstanceService, this.scheduledContextInstanceService,
                this.dynamicImagePath, this.moduleMetaDataService, this.logStreamingService, this.jobInitiationService, this.contextProfileService,
                this.jobUtilsService, this.scheduledContextService, false, this.globalEventService, this.contextInstanceRegistrationService,
                this.contextInstanceSchedulerService, this.systemEventSearchService, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance,
                this.contextVisualisationNodeDistance));

            initialised = true;
        }
    }
}

