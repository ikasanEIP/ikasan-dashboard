package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.RouterLink;
import org.ikasan.dashboard.ui.scheduler.model.ScheduledProcessFilter;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.miki.shared.dates.DatePatterns;
import org.vaadin.miki.superfields.dates.SuperDatePicker;

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
    private SuperDatePicker date;
    private TimePicker startTime;
    private TimePicker endTime;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private ModuleMetaDataService moduleMetaDataService;

    public UpcomingJobExecutionsWidget(ScheduledProcessManagementService scheduledProcessManagementService, DateFormatter dateFormatter
        , ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService
        , ModuleMetaDataService moduleMetaDataService, boolean isDeeplink) {
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.dateFormatter = dateFormatter;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.moduleMetaDataService = moduleMetaDataService;

        this.scheduledProcessFilter = new ScheduledProcessFilter();
        Div div = new Div();
        div.addClassNames("card-counter");
        if(isDeeplink) {
            div.setHeight("100%");
        }
        else {
            div.setHeight("380px");
        }

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        this.textField.setSuffixComponent(icon);
        this.textField.setWidth("300px");
        HorizontalLayout layout = new HorizontalLayout();
        H4 modules = new H4("Upcoming Job Executions");

        this.date = new SuperDatePicker("Execution date");
        this.date.setDatePattern(DatePatterns.D_MMMM_YYYY);
        this.date.setValue(LocalDate.now());

        this.startTime = new TimePicker("From");
        this.startTime.setStep(Duration.ofMinutes(15));
        this.startTime.setValue(LocalTime.now());

        this.endTime = new TimePicker("To");
        this.endTime.setStep(Duration.ofMinutes(15));
        this.endTime.setValue(LocalTime.of(23, 59, 59));

        Button refreshButton = new Button();
        refreshButton.addClickListener(buttonClickEvent -> {
           this.upcomingJobExecutionFilteringGrid.refresh();
        });
        refreshButton.getElement().appendChild(VaadinIcon.REFRESH.create().getElement());

        Button newWindowButton = new Button();
        newWindowButton.addClickListener(buttonClickEvent -> {
            RouterLink link = new RouterLink(null, UpcomingJobExecutionDeepLinkView.class);
            getUI().ifPresent(ui -> ui.getPage().open(link.getHref()));
        });
        newWindowButton.getElement().appendChild(VaadinIcon.EXTERNAL_LINK.create().getElement());
        newWindowButton.setVisible(!isDeeplink);

        HorizontalLayout timeComponents = new HorizontalLayout();
        timeComponents.add(date, startTime, endTime, textField, refreshButton, newWindowButton);
        timeComponents.getElement().getStyle().set("margin-left", "auto");

        layout.add(modules, timeComponents);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, modules);

        createGrid();

        div.add(layout);
        div.add(this.upcomingJobExecutionFilteringGrid);

        if(isDeeplink) {
            this.upcomingJobExecutionFilteringGrid.setHeight("87vh");
        }
        else {
            this.upcomingJobExecutionFilteringGrid.setHeight("300px");
        }

        this.setSizeFull();
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

        this.upcomingJobExecutionFilteringGrid.addGridFiltering(textField, this.scheduledProcessFilter::setFilter);
        this.upcomingJobExecutionFilteringGrid.addGridFiltering(date, startTime, endTime,
            this.scheduledProcessFilter::setStartTime, this.scheduledProcessFilter::setEndTime);
    }

}
