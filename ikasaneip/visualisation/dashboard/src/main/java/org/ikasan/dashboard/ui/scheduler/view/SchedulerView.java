package org.ikasan.dashboard.ui.scheduler.view;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.IronIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.scheduler.component.RunningAndRecentlyCompletedJobExecutionsWidget;
import org.ikasan.dashboard.ui.scheduler.component.SchedulerAgentDashboardView;
import org.ikasan.dashboard.ui.scheduler.component.SchedulerCalendar;
import org.ikasan.dashboard.ui.scheduler.component.UpcomingJobExecutionsWidget;
import org.ikasan.dashboard.ui.scheduler.model.CalendarConfiguration;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private DateFormatter dateFormatter;

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
    private SchedulerService schedulerService;

    private SchedulerCalendar schedulerCalendar;

    private SchedulerAgentDashboardView schedulerAgentDashboardView;

    private Board scheduleJobsTab;

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
            , this.systemEventLogger, this.schedulerService);

        this.schedulerAgentDashboardView.addClassName("styled");
        this.schedulerAgentDashboardView.setSizeFull();
        this.schedulerAgentDashboardView.setVisible(true);

        this.schedulerCalendar = new SchedulerCalendar(this.scheduledProcessManagementService, this.moduleMetadataService, new CalendarConfiguration());
        this.schedulerCalendar.setVisible(false);

        scheduleJobsTab = new Board();
        scheduleJobsTab.addClassName("styled");
        scheduleJobsTab.setSizeFull();
        scheduleJobsTab.setVisible(false);


        Tab schedulerDashboardTab = new Tab(getTranslation("tab.label.scheduler-dashboard", UI.getCurrent().getLocale()));
        Tab schedulerJobTab = new Tab(getTranslation("tab.label.scheduled-jobs", UI.getCurrent().getLocale()));
        Tab calendarTab = new Tab(getTranslation("tab.label.scheduled-jobs-calendar", UI.getCurrent().getLocale()));
        Tabs tabs = new Tabs(schedulerDashboardTab, schedulerJobTab, calendarTab);

        Map<Tab, com.vaadin.flow.component.Component> tabsToPages = new HashMap<>();
        tabsToPages.put(schedulerDashboardTab, this.schedulerAgentDashboardView);
        tabsToPages.put(schedulerJobTab, scheduleJobsTab);
        tabsToPages.put(calendarTab, this.schedulerCalendar);

        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            com.vaadin.flow.component.Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
            selectedPage.setVisible(true);

            if(selectedPage.equals(this.schedulerCalendar)) {
                this.schedulerCalendar.renderCalendar();
            }
        });


        IronIcon addIcon = IronIcons.ADD.create();
        addIcon.setSize("16pt");

        this.add(tabs, this.schedulerAgentDashboardView, scheduleJobsTab, this.schedulerCalendar);
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
            scheduleJobsTab.addRow(new UpcomingJobExecutionsWidget(this.scheduledProcessManagementService, this.dateFormatter, this.configurationRestService,
                this.moduleControlRestService, this.metaDataRestService, this.moduleMetadataService, false, this.systemEventLogger));
            scheduleJobsTab.addRow(new RunningAndRecentlyCompletedJobExecutionsWidget(this.scheduledProcessManagementService, this.dateFormatter,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.moduleMetadataService, false, this.systemEventLogger));

            initialised = true;
        }

        this.schedulerCalendar.beforeEnter(beforeEnterEvent);
    }

}

