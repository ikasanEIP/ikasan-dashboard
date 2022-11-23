package org.ikasan.dashboard.ui.scheduler.view;

import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.scheduler.component.ContextInstanceDashboardWidget;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
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
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Route(value = "contextInstanceMonitoring", layout = IkasanAppLayout.class)
@UIScope
@Component
@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/tree-view.css", themeFor = "vaadin-grid")
public class ContextInstanceMonitoringView extends VerticalLayout implements BeforeEnterObserver
{
    Logger logger = LoggerFactory.getLogger(ContextInstanceMonitoringView.class);

    @Resource
    private ConfigurationService configurationRestService;

    @Resource
    private ScheduledProcessManagementService scheduledProcessManagementService;

    @Resource
    private ModuleControlService moduleControlRestService;

    @Resource
    private MetaDataService metaDataRestService;

    @Resource
    private SystemEventLogger systemEventLogger;

    @Resource
    private ScheduledContextInstanceService scheduledContextInstanceService;

    @Resource
    private ScheduledContextService scheduledContextService;

    @Resource
    private SchedulerJobService schedulerJobService;

    @Resource(name = "moduleMetadataService")
    private ModuleMetaDataService moduleMetaDataService;

    @Resource
    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Value("${scheduled.job.context.queue.directory}")
    private String queueDirectory;

    @Resource
    private LogStreamingService logStreamingService;

    @Resource
    private JobInitiationService jobInitiationService;

    @Resource
    private ContextProfileService contextProfileService;

    @Resource
    private JobProvisionService jobProvisionService;

    @Resource
    private UserService userService;

    @Resource
    private JobUtilsService jobUtilsService;

    @Resource
    private SecurityService securityService;

    @Resource
    private ModuleMetaDataService moduleMetadataService;

    @Resource
    private SchedulerService schedulerService;

    private ContextInstanceDashboardWidget contextInstanceDashboardWidget;

    private Board board;

    /**
     * Constructor
     */
    public ContextInstanceMonitoringView() {
        this.setSpacing(false);
        this.setMargin(false);
    }

    /**
     * Initialise the internals of the object.
     */
    private void init() {
        this.contextInstanceDashboardWidget = new ContextInstanceDashboardWidget(this.scheduledProcessManagementService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService,
            this.systemEventLogger, this.schedulerService, this.schedulerJobService,
            this.schedulerJobInstanceService, this.scheduledContextInstanceService, "",
            this.moduleMetaDataService, this.logStreamingService,
            this.jobInitiationService, this.contextProfileService,
            this.jobUtilsService, this.scheduledContextService, true);

        this.getElement().getStyle().set("padding-top", "0px");
        board = new Board();
        board.addClassName("styled");
        board.setSizeFull();

        board.addRow(this.contextInstanceDashboardWidget);

        this.add(this.board);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        init();
    }
}

