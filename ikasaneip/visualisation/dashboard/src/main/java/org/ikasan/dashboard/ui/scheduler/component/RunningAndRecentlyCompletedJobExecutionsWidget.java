package org.ikasan.dashboard.ui.scheduler.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
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
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.service.SolrScheduledProcessServiceImpl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;


@CssImport("./styles/dashboard-view.css")
public class RunningAndRecentlyCompletedJobExecutionsWidget extends Div {

    private RunningAndRecentlyCompletedJobExecutionFilteringGrid runningAndRecentlyCompletedJobExecutionFilteringGrid;
    private ScheduledProcessFilter scheduledProcessFilter;

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private DateFormatter dateFormatter;
    private TextField textField = new TextField("Search");

    private DatePicker date;
    private TimePicker startTime;
    private TimePicker endTime;

    public RunningAndRecentlyCompletedJobExecutionsWidget(ScheduledProcessManagementService scheduledProcessManagementService,
                                                          DateFormatter dateFormatter) {
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.dateFormatter = dateFormatter;
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("380px");

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        this.textField.setSuffixComponent(icon);
        this.textField.setWidth("300px");

        HorizontalLayout layout = new HorizontalLayout();
        H4 modules = new H4("Running & Recently Completed Job Executions");

        this.date = new DatePicker("Execution date");
        this.date.setValue(LocalDate.now());

        this.startTime = new TimePicker("From");
        this.startTime.setStep(Duration.ofMinutes(15));
        this.startTime.setValue(LocalTime.now().minusHours(1));

        this.endTime = new TimePicker("To");
        this.endTime.setStep(Duration.ofMinutes(15));
        this.endTime.setValue(LocalTime.now());


        Button refreshButton = new Button();
        refreshButton.addClickListener(buttonClickEvent -> {
            this.runningAndRecentlyCompletedJobExecutionFilteringGrid.refresh();
        });
        refreshButton.getElement().appendChild(IronIcons.REFRESH.create().getElement());

        HorizontalLayout timeComponents = new HorizontalLayout();
        timeComponents.add(date, startTime, endTime, textField, refreshButton);
        timeComponents.getElement().getStyle().set("margin-left", "auto");

        layout.add(modules, timeComponents);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, modules);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, timeComponents);

        createGrid();

        div.add(layout);
        div.add(this.runningAndRecentlyCompletedJobExecutionFilteringGrid);

        this.add(div);
    }

    private void createGrid() {
        this.scheduledProcessFilter = new ScheduledProcessFilter();
        long epochMilli = this.date.getValue().atStartOfDay(DateTimeUtil.getZoneId()).toEpochSecond() * 1000;
        if((epochMilli + (this.startTime.getValue().toSecondOfDay()*1000)) > System.currentTimeMillis()) {
            this.scheduledProcessFilter.setStartTime(System.currentTimeMillis());
        }
        else {
            this.scheduledProcessFilter.setStartTime(epochMilli + (this.startTime.getValue().toSecondOfDay()*1000));
        }
        this.scheduledProcessFilter.setEndTime(epochMilli + (this.endTime.getValue().toSecondOfDay()*1000));

        this.runningAndRecentlyCompletedJobExecutionFilteringGrid = new RunningAndRecentlyCompletedJobExecutionFilteringGrid(scheduledProcessManagementService
            , this.scheduledProcessFilter, this.dateFormatter);
        this.runningAndRecentlyCompletedJobExecutionFilteringGrid.setHeight("300px");

        this.runningAndRecentlyCompletedJobExecutionFilteringGrid.addGridFiltering(textField, this.scheduledProcessFilter::setFilter);
        this.runningAndRecentlyCompletedJobExecutionFilteringGrid.addGridFiltering(date, startTime, endTime,
            this.scheduledProcessFilter::setStartTime, this.scheduledProcessFilter::setEndTime);
    }

}
