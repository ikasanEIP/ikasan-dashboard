package org.ikasan.dashboard.ui.scheduler.view;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.button.Button;
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
import org.ikasan.dashboard.ui.scheduler.component.*;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.solr.SolrGeneralService;
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
@CssImport(value="./styles/hospital-events.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
public class SchedulerCalendarView extends VerticalLayout implements BeforeEnterObserver
{
    Logger logger = LoggerFactory.getLogger(SchedulerCalendarView.class);

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

    private SchedulerCalendar schedulerCalendar;

    private SchedulerAgentDashboardView schedulerAgentDashboardView;

    private Board scheduleJobsTab;
    private Board scheduleJStatsTab;


    private boolean initialised = false;

    public SchedulerCalendarView()
    {
        this.setSpacing(false);
        this.setMargin(false);
    }

    private void init() {
        this.schedulerAgentDashboardView = new SchedulerAgentDashboardView(this.moduleMetadataService
            , this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService, this.metaDataRestService);
        this.schedulerAgentDashboardView.addClassName("styled");
        this.schedulerAgentDashboardView.setSizeFull();
        this.schedulerAgentDashboardView.setVisible(true);

        this.schedulerCalendar = new SchedulerCalendar(this.scheduledProcessManagementService, this.moduleMetadataService);
        this.schedulerCalendar.setVisible(false);

        scheduleJobsTab = new Board();
        scheduleJobsTab.addClassName("styled");
        scheduleJobsTab.setSizeFull();
        scheduleJobsTab.setVisible(false);

        scheduleJStatsTab = new Board();
        scheduleJStatsTab.addClassName("styled");
        scheduleJStatsTab.setSizeFull();
        scheduleJStatsTab.setVisible(false);


        Tab schedulerDashboardTab = new Tab("Scheduler Dashboard");
        Tab schedulerJobTab = new Tab("Scheduled Jobs");
        Tab calendarTab = new Tab("Scheduled Jobs Calendar - BETA");
        Tabs tabs = new Tabs(schedulerDashboardTab, schedulerJobTab, calendarTab);

        Map<Tab, com.vaadin.flow.component.Component> tabsToPages = new HashMap<>();
        tabsToPages.put(schedulerDashboardTab, this.schedulerAgentDashboardView);
        tabsToPages.put(schedulerJobTab, scheduleJobsTab);
//        tabsToPages.put(schedulerStatusTab, scheduleJStatsTab);
        tabsToPages.put(calendarTab, this.schedulerCalendar);
//        tabsToPages.put(schedulerJobManagementTab, jobManagementTab);

        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            com.vaadin.flow.component.Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
            selectedPage.setVisible(true);
        });

        Button addButton = new Button();
        addButton.getStyle().set("position", "absolute");
        addButton.getStyle().set("top", "70px");
        addButton.getStyle().set("right", "30px");

        addButton.addClickListener(buttonClickEvent -> {
            ScheduledJobDialog scheduledJobDialog = new ScheduledJobDialog(null, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService);
            scheduledJobDialog.open();
        });

        IronIcon addIcon = IronIcons.ADD.create();
        addIcon.setSize("16pt");

        addButton.getElement().appendChild(addIcon.getElement());

        this.add(tabs, addButton, this.schedulerAgentDashboardView, scheduleJobsTab, scheduleJStatsTab, this.schedulerCalendar);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!initialised) {
            this.init();
            this.schedulerAgentDashboardView.beforeEnter(beforeEnterEvent);
            scheduleJobsTab.addRow(new UpcomingJobExecutionsWidget(this.scheduledProcessManagementService, this.dateFormatter, this.configurationRestService,
                this.moduleControlRestService, this.metaDataRestService, this.moduleMetadataService, false));
            scheduleJobsTab.addRow(new RunningAndRecentlyCompletedJobExecutionsWidget(this.scheduledProcessManagementService, this.dateFormatter,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.moduleMetadataService, false));

            this.scheduleJStatsTab.addRow(new DurationWidget());
            this.scheduleJStatsTab.addRow(new StartAndEndTimeWidget());
            initialised = true;
        }

        this.schedulerCalendar.beforeEnter(beforeEnterEvent);
    }
}

