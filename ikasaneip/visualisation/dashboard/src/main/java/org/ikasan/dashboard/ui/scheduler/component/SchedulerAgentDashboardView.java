package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
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
    private ScheduledProcessManagementService scheduledProcessManagementService;
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
     * Constructor
     *
     * @param moduleMetadataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerService
     */
    public SchedulerAgentDashboardView(ModuleMetaDataService moduleMetadataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                       ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                                       SystemEventLogger systemEventLogger, SchedulerService schedulerService, SchedulerJobService schedulerJobService,
                                       SchedulerJobInstanceService schedulerJobInstanceService, ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath,
                                       ModuleMetaDataService moduleMetaDataService, LogStreamingService logStreamingService,
                                       JobInitiationService jobInitiationService, ContextProfileService contextProfileService,
                                       JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService,
                                       GlobalEventService globalEventService, ContextInstanceRegistrationService contextInstanceRegistrationService,
                                       DownloadLogFileService downloadLogFileService, ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService, JobProvisionService jobProvisionService,
                                       SystemEventSearchService systemEventSearchService, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance,
                                       double contextVisualisationNodeDistance) {
        this.moduleMetadataService = moduleMetadataService;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
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
            board.addRow(new AgentWidget(this.moduleMetadataService, this.scheduledProcessManagementService
                , this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger
                , this.schedulerService, this.jobProvisionService, this.schedulerJobService, this.downloadLogFileService, this.scheduledContextService), new SchedulerStatusWidget(this.moduleMetadataService, UI.getCurrent()));

            board.addRow(new ContextInstanceDashboardWidget(this.scheduledProcessManagementService
                , this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger
                , this.schedulerService, this.schedulerJobService, this.schedulerJobInstanceService, this.scheduledContextInstanceService,
                this.dynamicImagePath, this.moduleMetaDataService, this.logStreamingService, this.jobInitiationService, this.contextProfileService,
                this.jobUtilsService, this.scheduledContextService, false, this.globalEventService, this.contextInstanceRegistrationService,
                this.contextInstanceSchedulerService, this.systemEventSearchService, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance,
                this.contextVisualisationNodeDistance));

            initialised = true;
        }
    }
}

