package org.ikasan.dashboard.ui.scheduler.component;

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
import org.ikasan.dashboard.ui.scheduler.model.UpcomingJobExecutionFilter;
import org.ikasan.dashboard.ui.scheduler.service.JobExecutionService;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;

import java.time.Duration;


@CssImport("./styles/dashboard-view.css")
public class RunningAndRecentlyCompletedJobExecutionsWidget extends Div {

    private RunningAndRecentlyCompletedJobExecutionFilteringGrid runningAndRecentlyCompletedJobExecutionFilteringGrid;
    private UpcomingJobExecutionFilter upcomingJobExecutionFilter;

    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private TextField textField = new TextField("Search");

    public RunningAndRecentlyCompletedJobExecutionsWidget(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService) {
        this.businessStreamMetaDataService = businessStreamMetaDataService;
        createGrid();
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("380px");

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setSuffixComponent(icon);
        textField.setWidth("300px");

        HorizontalLayout layout = new HorizontalLayout();
        H4 modules = new H4("Running & Recently Completed Job Executions");

        DatePicker date = new DatePicker("Execution date");
        date.addValueChangeListener(
            event -> System.out.println(event));
        TimePicker startTime = new TimePicker("From");
        startTime.setStep(Duration.ofMinutes(15));
        startTime.addValueChangeListener(
            event -> System.out.println(event));
        TimePicker endTime = new TimePicker("To");
        endTime.setStep(Duration.ofMinutes(15));
        startTime.addValueChangeListener(
            event -> System.out.println(event));

        HorizontalLayout timeComponents = new HorizontalLayout();
        timeComponents.add(date, startTime, endTime, textField);
        timeComponents.getElement().getStyle().set("margin-left", "auto");

        layout.add(modules, timeComponents);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, modules);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, timeComponents);

        div.add(layout);
        div.add(this.runningAndRecentlyCompletedJobExecutionFilteringGrid);

        this.add(div);
    }


    private void createGrid() {
        this.upcomingJobExecutionFilter = new UpcomingJobExecutionFilter();

        this.runningAndRecentlyCompletedJobExecutionFilteringGrid = new RunningAndRecentlyCompletedJobExecutionFilteringGrid(new JobExecutionService(), this.upcomingJobExecutionFilter);
        this.runningAndRecentlyCompletedJobExecutionFilteringGrid.setHeight("300px");

        this.runningAndRecentlyCompletedJobExecutionFilteringGrid.addGridFiltering(textField, this.upcomingJobExecutionFilter::setFilter);
    }

}
