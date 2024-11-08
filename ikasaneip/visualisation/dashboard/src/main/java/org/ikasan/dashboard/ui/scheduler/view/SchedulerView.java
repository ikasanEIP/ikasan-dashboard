package org.ikasan.dashboard.ui.scheduler.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.scheduler.component.ContextTemplateWidget;
import org.ikasan.dashboard.ui.scheduler.component.SchedulerAgentDashboardView;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DashboardContextNavigator;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.*;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.general.SchedulerService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.*;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.util.HashMap;
import java.util.Map;

@Route(value = "scheduler", layout = IkasanAppLayout.class)
@UIScope
@Component
@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@PermitAll
@PreserveOnRefresh
public class SchedulerView extends VerticalLayout implements BeforeEnterObserver
{
    Logger logger = LoggerFactory.getLogger(SchedulerView.class);

    @Autowired
    private ModuleMetaDataService moduleMetadataService;

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
    private JobInitiationService jobInitiationService;

    @Resource
    private ScheduledContextInstanceService scheduledContextInstanceService;

    @Resource
    private ScheduledContextService scheduledContextService;

    @Resource
    private JobLockCacheService jobLockCacheService;

    @Resource
    private SchedulerJobService schedulerJobService;

    @Resource
    private SchedulerService schedulerService;

    @Resource(name = "moduleMetadataService")
    private ModuleMetaDataService moduleMetaDataService;

    @Value("${job.plan.export.remove.trailing.plan.name.context.after.underscore:true}")
    private boolean removeTrailingPlanNameContextAfterUnderscore;

    @Resource
    private LogStreamingService logStreamingService;

    @Resource
    private DownloadLogFileService downloadLogFileService;

    @Resource
    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Resource
    private ContextProfileService contextProfileService;

    @Resource
    private EmailNotificationDetailsService emailNotificationDetailsService;

    @Resource
    private EmailNotificationContextService emailNotificationContextService;

    @Value("${ikasan.dashboard.zip.working.directory:.}")
    private String zipWorkingDirectory;

    @Value("#{${scheduler.job.execution.environment.label}}")
    private Map<String, String> schedulerJobExecutionEnvironmentLabel;

    @Value("${job.plan.interval.multiple:3}")
    private int jobPlanIntervalMultiple;

    @Resource
    private ContextProvisionService contextProvisionService;

    @Resource
    private JobProvisionService jobProvisionService;

    @Resource
    private UserService userService;

    @Resource
    private SecurityService securityService;

    @Resource
    private JobUtilsService jobUtilsService;

    @Resource
    private ContextInstanceRegistrationService contextInstanceRegistrationService;

    @Resource
    private SpringCloudConfigRefreshService springCloudConfigRefreshService;

    @Resource
    private GlobalEventService globalEventService;

    @Resource
    private ContextInstanceSchedulerService contextInstanceSchedulerService;

    @Resource
    private ContextParametersInstanceService contextParametersInstanceService;

    @Value("${scheduler.provision.jobs.on.upload:true}")
    private boolean uploadProvisionJobs;

    @Value("${job.visualisation.vertical.spacing:120}")
    protected double jobVisualisationVerticalSpacing;
    @Value("${job.visualisation.horizontal.spacing:400}")
    protected double jobVisualisationHorizontalSpacing;
    @Value("${context.visualisation.level.distance:150}")
    protected double contextVisualisationLevelDistance;
    @Value("${context.visualisation.node.distance:75}")
    protected double contextVisualisationNodeDistance;

    private SchedulerAgentDashboardView schedulerAgentDashboardView;

    private ContextTemplateWidget contextTemplateWidget;

    private Board contextDebugBoard;

    private Tab schedulerDashboardTab;
    private Tab contextTemplateTab;
    private Tabs tabs;

    private boolean initialised = false;

    /**
     * Constructor
     */
    public SchedulerView() {
        this.setSpacing(false);
        this.setMargin(false);
    }

    /**
     * Initialise the internals of the object.
     */
    private void init() {
        this.schedulerAgentDashboardView = new SchedulerAgentDashboardView(this.moduleMetadataService
            , this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService, this.metaDataRestService
            , this.systemEventLogger, this.schedulerService, this.schedulerJobService, this.schedulerJobInstanceService, this.scheduledContextInstanceService,
            "", this.moduleMetaDataService, this.logStreamingService, this.jobInitiationService, this.contextProfileService, this.jobUtilsService,
            this.scheduledContextService, this.globalEventService, this.contextInstanceRegistrationService, this.downloadLogFileService, this.contextInstanceSchedulerService,
            this.jobProvisionService, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationNodeDistance, this.contextVisualisationNodeDistance);

        this.schedulerAgentDashboardView.addClassName("styled");
        this.schedulerAgentDashboardView.setSizeFull();
        this.schedulerAgentDashboardView.setVisible(true);

        this.contextDebugBoard = new Board();
        this.contextDebugBoard.addClassName("styled");
        this.contextDebugBoard.setSizeFull();
        this.contextDebugBoard.setVisible(false);
        this.contextDebugBoard.setId("contextDebugBoard");

        this.contextTemplateWidget = new ContextTemplateWidget(this.scheduledContextService, ".", this.moduleMetaDataService, this.scheduledProcessManagementService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService,
            this.scheduledContextInstanceService, this.schedulerJobInstanceService, this.jobInitiationService, this.zipWorkingDirectory, this.contextProvisionService,
            this.contextProfileService, this.jobProvisionService, userService, securityService, this.jobUtilsService, this.uploadProvisionJobs, this.contextInstanceRegistrationService,
            this.emailNotificationDetailsService, this.emailNotificationContextService, this.schedulerJobExecutionEnvironmentLabel, this.springCloudConfigRefreshService, this.globalEventService,
            this.contextInstanceSchedulerService, this.contextParametersInstanceService, removeTrailingPlanNameContextAfterUnderscore, this.jobPlanIntervalMultiple, this.jobVisualisationVerticalSpacing,
            this.jobVisualisationHorizontalSpacing, this.contextVisualisationNodeDistance, this.contextVisualisationLevelDistance);
        this.contextTemplateWidget.setVisible(false);


        this.schedulerDashboardTab = new Tab(getTranslation("tab.label.scheduler-dashboard", UI.getCurrent().getLocale()));
        this.schedulerDashboardTab.setId("schedulerDashboardTab");
        this.contextTemplateTab = new Tab(getTranslation("tab.label.job-plans", UI.getCurrent().getLocale()));
        this.contextTemplateTab.setId("contextTemplateTab");

        this.tabs = new Tabs(schedulerDashboardTab, this.contextTemplateTab);
        this.tabs.setId("schedulerViewTabs");

        Map<Tab, com.vaadin.flow.component.Component> tabsToPages = new HashMap<>();
        tabsToPages.put(this.schedulerDashboardTab, this.schedulerAgentDashboardView);
        tabsToPages.put(this.contextTemplateTab, this.contextTemplateWidget);

        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            com.vaadin.flow.component.Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
            selectedPage.setVisible(true);
        });

        Icon addIcon = VaadinIcon.PLUS.create();
        addIcon.setSize("16pt");

        HorizontalLayout tabsLayout = new HorizontalLayout();
        tabsLayout.setMargin(false);
        tabsLayout.add(tabs);
        tabsLayout.setWidth("100%");
        this.add(tabsLayout, this.schedulerAgentDashboardView, this.contextTemplateWidget, contextDebugBoard);
        this.setSizeFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {

        if(!ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.SCHEDULER_WRITE
            , SecurityConstants.SCHEDULER_READ
            , SecurityConstants.SCHEDULER_ADMIN
            , SecurityConstants.SCHEDULER_ALL_READ
            , SecurityConstants.SCHEDULER_ALL_WRITE
            , SecurityConstants.SCHEDULER_ALL_ADMIN
            , SecurityConstants.SCHEDULER_DEV_READ
            , SecurityConstants.SCHEDULER_DEV_WRITE
            , SecurityConstants.SCHEDULER_DEV_ADMIN
            , SecurityConstants.ALL_AUTHORITY)) {
            DashboardContextNavigator.navigateToLandingPage(beforeEnterEvent);
        }

        if(!initialised) {
            this.init();
            this.schedulerAgentDashboardView.beforeEnter(beforeEnterEvent);
            initialised = true;
        }
    }


}

