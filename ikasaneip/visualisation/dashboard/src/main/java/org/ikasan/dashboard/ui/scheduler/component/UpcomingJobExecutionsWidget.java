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
public class UpcomingJobExecutionsWidget extends Div {

    private UpcomingJobExecutionFilteringGrid upcomingJobExecutionFilteringGrid;
    private UpcomingJobExecutionFilter upcomingJobExecutionFilter;

    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private TextField textField = new TextField("Search");

    public UpcomingJobExecutionsWidget(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService) {
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
        H4 modules = new H4("Upcoming Job Executions");

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

        div.add(layout);
        div.add(this.upcomingJobExecutionFilteringGrid);

        this.add(div);
    }

    private void createGrid() {
        this.upcomingJobExecutionFilter = new UpcomingJobExecutionFilter();

        this.upcomingJobExecutionFilteringGrid = new UpcomingJobExecutionFilteringGrid(new JobExecutionService(), this.upcomingJobExecutionFilter);
        this.upcomingJobExecutionFilteringGrid.setHeight("300px");

        this.upcomingJobExecutionFilteringGrid.addGridFiltering(textField, this.upcomingJobExecutionFilter::setFilter);
    }

}
