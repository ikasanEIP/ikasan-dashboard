package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.general.SchedulerService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.springframework.beans.factory.annotation.Value;

@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
public class SchedulerAgentDashboardView extends HorizontalLayout implements BeforeEnterObserver
{
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

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

    private Board board;

    private boolean initialised = false;

    private int statusRefreshInterval;

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
                                       JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService, int statusRefreshInterval) {
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
        this.statusRefreshInterval = statusRefreshInterval;

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
                , this.schedulerService, this.schedulerJobService), new SchedulerStatusWidget(this.moduleMetadataService, UI.getCurrent()));

            board.addRow(new ContextInstanceDashboardWidget(this.moduleMetadataService, this.scheduledProcessManagementService
                , this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger
                , this.schedulerService, this.schedulerJobService, this.schedulerJobInstanceService, this.scheduledContextInstanceService,
                this.dynamicImagePath, this.moduleMetaDataService, this.logStreamingService, this.jobInitiationService, this.contextProfileService,
                this.jobUtilsService, this.scheduledContextService, false, statusRefreshInterval));

            initialised = true;
        }
    }
}

