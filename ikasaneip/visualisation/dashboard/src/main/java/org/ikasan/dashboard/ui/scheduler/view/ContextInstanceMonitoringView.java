package org.ikasan.dashboard.ui.scheduler.view;

import com.vaadin.flow.component.dashboard.Dashboard;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.scheduler.component.ContextInstanceDashboardWidget;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
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
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.security.PermitAll;

@Route(value = "contextInstanceMonitoring", layout = IkasanAppLayout.class)
@UIScope
@Component
@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/tree-view.css", themeFor = "vaadin-grid")
@PermitAll
@PreserveOnRefresh
public class ContextInstanceMonitoringView extends VerticalLayout implements BeforeEnterObserver
{
    Logger logger = LoggerFactory.getLogger(ContextInstanceMonitoringView.class);

    @Autowired
    private ConfigurationService configurationRestService;

    @Autowired
    private ModuleControlService moduleControlRestService;

    @Autowired
    private MetaDataService metaDataRestService;

    @Autowired
    private SystemEventLogger systemEventLogger;

    @Autowired
    private ScheduledContextInstanceService scheduledContextInstanceService;

    @Autowired
    private ScheduledContextService scheduledContextService;

    @Autowired
    private SchedulerJobService schedulerJobService;

    @Autowired
    @Qualifier("moduleMetadataService")
    private ModuleMetaDataService moduleMetaDataService;

    @Autowired
    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Autowired
    private LogStreamingService logStreamingService;

    @Autowired
    private JobInitiationService jobInitiationService;

    @Autowired
    private ContextProfileService contextProfileService;

    @Autowired
    private JobUtilsService jobUtilsService;

    @Autowired
    private GlobalEventService globalEventService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private ContextInstanceRegistrationService contextInstanceRegistrationService;
    @Autowired
    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;
    @Autowired
    private SystemEventSearchService systemEventSearchService;

    @Value("${job.visualisation.vertical.spacing:120}")
    protected double jobVisualisationVerticalSpacing;
    @Value("${job.visualisation.horizontal.spacing:400}")
    protected double jobVisualisationHorizontalSpacing;
    @Value("${context.visualisation.level.distance:150}")
    protected double contextVisualisationLevelDistance;
    @Value("${context.visualisation.node.distance:75}")
    protected double contextVisualisationNodeDistance;

    private ContextInstanceDashboardWidget contextInstanceDashboardWidget;

    private Dashboard board;

    private boolean initialised = false;

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
        this.contextInstanceDashboardWidget = new ContextInstanceDashboardWidget(this.configurationRestService,
            this.moduleControlRestService, this.metaDataRestService,
            this.systemEventLogger, this.schedulerService, this.schedulerJobService,
            this.schedulerJobInstanceService, this.scheduledContextInstanceService, "",
            this.moduleMetaDataService, this.logStreamingService, this.jobInitiationService, this.contextProfileService,
            this.jobUtilsService, this.scheduledContextService, true, this.globalEventService,
            this.contextInstanceRegistrationService, this.contextInstanceSchedulerService, this.systemEventSearchService, this.jobVisualisationVerticalSpacing,
            this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance);

        this.getElement().getStyle().set("padding-top", "0px");
        board = new Dashboard();
        board.setMinimumColumnWidth("100px");
        board.setDenseLayout(true);
        board.setSizeFull();
        board.setMaximumColumnCount(1);
        board.setMinimumRowHeight("100px");

        board.add(this.contextInstanceDashboardWidget);

        this.add(this.board);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!initialised) {
            init();
            initialised = true;
        }
    }
}

