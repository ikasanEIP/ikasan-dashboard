package org.ikasan.dashboard.ui.scheduler.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import org.ikasan.dashboard.ui.scheduler.model.ScheduledProcessFilter;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.scheduled.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.model.UpcomingScheduledProcess;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;


@CssImport("./styles/dashboard-view.css")
public class UpcomingJobExecutionsWidget extends Div {

    Logger logger = LoggerFactory.getLogger(UpcomingJobExecutionsWidget.class);

    private UpcomingJobExecutionFilteringGrid upcomingJobExecutionFilteringGrid;
    private ScheduledProcessFilter scheduledProcessFilter;

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private DateFormatter dateFormatter;
    private TextField textField = new TextField("Search");
    private DatePicker date;
    private TimePicker startTime;
    private TimePicker endTime;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private ModuleMetaDataService moduleMetaDataService;

    public UpcomingJobExecutionsWidget(ScheduledProcessManagementService scheduledProcessManagementService, DateFormatter dateFormatter
        , ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService
        , ModuleMetaDataService moduleMetaDataService) {
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.dateFormatter = dateFormatter;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.moduleMetaDataService = moduleMetaDataService;

        this.scheduledProcessFilter = new ScheduledProcessFilter();
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("380px");

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        this.textField.setSuffixComponent(icon);
        this.textField.setWidth("300px");
        HorizontalLayout layout = new HorizontalLayout();
        H4 modules = new H4("Upcoming Job Executions");

        this.date = new DatePicker("Execution date");
        this.date.setValue(LocalDate.now());

        this.startTime = new TimePicker("From");
        this.startTime.setStep(Duration.ofMinutes(15));
        this.startTime.setValue(LocalTime.now());

        this.endTime = new TimePicker("To");
        this.endTime.setStep(Duration.ofMinutes(15));
        this.endTime.setValue(LocalTime.now().plusHours(1));

        Button refreshButton = new Button();
        refreshButton.addClickListener(buttonClickEvent -> {
           this.upcomingJobExecutionFilteringGrid.refresh();
        });
        refreshButton.getElement().appendChild(IronIcons.REFRESH.create().getElement());

        HorizontalLayout timeComponents = new HorizontalLayout();
        timeComponents.add(date, startTime, endTime, textField, refreshButton);
        timeComponents.getElement().getStyle().set("margin-left", "auto");

        layout.add(modules, timeComponents);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, modules);

        createGrid();

        div.add(layout);
        div.add(this.upcomingJobExecutionFilteringGrid);

        this.add(div);
    }

    private void createGrid() {
        this.scheduledProcessFilter = new ScheduledProcessFilter();
        long epochMilli = this.date.getValue().atStartOfDay(DateTimeUtil.getZoneId()).toEpochSecond() * 1000;
        if((epochMilli + (this.startTime.getValue().toSecondOfDay()*1000)) < System.currentTimeMillis()) {
            this.scheduledProcessFilter.setStartTime(System.currentTimeMillis());
        }
        else {
            this.scheduledProcessFilter.setStartTime(epochMilli + (this.startTime.getValue().toSecondOfDay()*1000));
        }
        this.scheduledProcessFilter.setEndTime(epochMilli + (this.endTime.getValue().toSecondOfDay()*1000));

        this.upcomingJobExecutionFilteringGrid = new UpcomingJobExecutionFilteringGrid(this.scheduledProcessManagementService
            , this.scheduledProcessFilter, this.dateFormatter, this.configurationRestService, this.moduleControlRestService,
            this.metaDataRestService, this.moduleMetaDataService);
        this.upcomingJobExecutionFilteringGrid.setHeight("300px");

        this.upcomingJobExecutionFilteringGrid.addGridFiltering(textField, this.scheduledProcessFilter::setFilter);
        this.upcomingJobExecutionFilteringGrid.addGridFiltering(date, startTime, endTime,
            this.scheduledProcessFilter::setStartTime, this.scheduledProcessFilter::setEndTime);

        this.upcomingJobExecutionFilteringGrid.addItemDoubleClickListener((ComponentEventListener<ItemDoubleClickEvent<UpcomingScheduledProcess>>)
            upcomingScheduledProcessItemDoubleClickEvent -> {
                ScheduledProcessAggregateConfiguration configuration = this.scheduledProcessManagementService.getScheduleProcessAggregateConfiguration(upcomingScheduledProcessItemDoubleClickEvent.getItem().getAgentName(),
                upcomingScheduledProcessItemDoubleClickEvent.getItem().getJobName());

                ModuleMetaData agent = this.moduleMetaDataService.findById(upcomingScheduledProcessItemDoubleClickEvent.getItem().getAgentName());

                NewSchedulerJobDialog newSchedulerJobDialog = new NewSchedulerJobDialog(agent,
                    this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService,
                    this.metaDataRestService);

                newSchedulerJobDialog.setScheduleProcessAggregateConfiguration(configuration, false);
                newSchedulerJobDialog.open();
        });
    }

}
