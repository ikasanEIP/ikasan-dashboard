package org.ikasan.dashboard.ui.scheduler.view;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
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
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.solr.SolrGeneralService;
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
public class SchedulerView extends VerticalLayout implements BeforeEnterObserver
{
    @Resource
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    @Autowired
    private ModuleMetaDataService moduleMetadataService;

    @Resource
    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;

    private Board scheduleJobsTab;
    private Board scheduleJStatsTab;
//    private Board jobManagementTab;

    private boolean initialised = false;

    public SchedulerView()
    {
        scheduleJobsTab = new Board();
        scheduleJobsTab.addClassName("styled");
        scheduleJobsTab.setSizeFull();

        scheduleJStatsTab = new Board();
        scheduleJStatsTab.addClassName("styled");
        scheduleJStatsTab.setSizeFull();
        scheduleJStatsTab.setVisible(false);

//        jobManagementTab = new Board();
//        jobManagementTab.addClassName("styled");
//        jobManagementTab.setSizeFull();
//        jobManagementTab.setVisible(false);

        Tab schedulerJobTab = new Tab("Scheduler Jobs");
        Tab schedulerStatusTab = new Tab("Scheduler Statistics");
//        Tab schedulerJobManagementTab = new Tab("Scheduler Status");
        Tabs tabs = new Tabs(schedulerJobTab, schedulerStatusTab);

        Map<Tab, com.vaadin.flow.component.Component> tabsToPages = new HashMap<>();
        tabsToPages.put(schedulerJobTab, scheduleJobsTab);
        tabsToPages.put(schedulerStatusTab, scheduleJStatsTab);
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
            NewSchedulerJobDialog newSchedulerJobDialog = new NewSchedulerJobDialog();
            newSchedulerJobDialog.open();
        });

        IronIcon addIcon = IronIcons.ADD.create();
        addIcon.setSize("16pt");

        addButton.getElement().appendChild(addIcon.getElement());

        this.add(tabs, addButton, scheduleJobsTab, scheduleJStatsTab);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!initialised) {
            scheduleJobsTab.addRow(new UpcomingJobExecutionsWidget(this.businessStreamMetaDataService));
            scheduleJobsTab.addRow(new RunningAndRecentlyCompletedJobExecutionsWidget(this.businessStreamMetaDataService));

            this.scheduleJStatsTab.addRow(new DurationWidget());
            this.scheduleJStatsTab.addRow(new StartAndEndTimeWidget());
            initialised = true;
        }
    }

}

