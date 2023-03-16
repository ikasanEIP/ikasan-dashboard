package org.ikasan.dashboard.ui.scheduler.view;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.IronIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.scheduler.component.*;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.general.SchedulerService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.*;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
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
import java.util.HashMap;
import java.util.Map;

@Route(value = "scheduler", layout = IkasanAppLayout.class)
@UIScope
@Component
@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
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

    @Value("${scheduled.job.context.queue.directory}")
    private String queueDirectory;


    @Resource
    private LogStreamingService logStreamingService;

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

    @Value("${ikasan.dashboard.unzip.and.provision.jobs:true}")
    private boolean uploadProvisionJobs;

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
            this.scheduledContextService, this.globalEventService, this.contextInstanceRegistrationService);

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
            this.contextInstanceSchedulerService);
        this.contextTemplateWidget.setVisible(false);


        this.schedulerDashboardTab = new Tab(getTranslation("tab.label.scheduler-dashboard", UI.getCurrent().getLocale()));
        this.schedulerDashboardTab.setId("schedulerDashboardTab");
        this.contextTemplateTab = new Tab(getTranslation("tab.label.job-plans", UI.getCurrent().getLocale()));
        this.contextTemplateTab.setId("contextTemplateTab");

        this.tabs = new Tabs(schedulerDashboardTab, this.contextTemplateTab);

        Map<Tab, com.vaadin.flow.component.Component> tabsToPages = new HashMap<>();
        tabsToPages.put(this.schedulerDashboardTab, this.schedulerAgentDashboardView);
        tabsToPages.put(this.contextTemplateTab, this.contextTemplateWidget);

        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            com.vaadin.flow.component.Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
            selectedPage.setVisible(true);
        });

        IronIcon addIcon = IronIcons.ADD.create();
        addIcon.setSize("16pt");

        HorizontalLayout tabsLayout = new HorizontalLayout();
        tabsLayout.setMargin(false);
        tabsLayout.add(tabs);
        tabsLayout.setWidth("100%");
        this.add(tabsLayout, this.schedulerAgentDashboardView, this.contextTemplateWidget, contextDebugBoard);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {

        if(!ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_READ,
            SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.ALL_AUTHORITY)) {
            UI.getCurrent().navigate("");
        }

        if(!initialised) {
            this.init();
            this.schedulerAgentDashboardView.beforeEnter(beforeEnterEvent);
            initialised = true;
        }
    }
}

