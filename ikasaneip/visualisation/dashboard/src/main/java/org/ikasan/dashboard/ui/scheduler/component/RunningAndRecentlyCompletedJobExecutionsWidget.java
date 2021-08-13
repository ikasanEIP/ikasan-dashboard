package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
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
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.vaadin.miki.shared.dates.DatePatterns;
import org.vaadin.miki.superfields.dates.SuperDatePicker;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;


@CssImport("./styles/dashboard-view.css")
public class RunningAndRecentlyCompletedJobExecutionsWidget extends Div {

    private RunningAndRecentlyCompletedJobExecutionFilteringGrid runningAndRecentlyCompletedJobExecutionFilteringGrid;
    private ScheduledProcessFilter scheduledProcessFilter;

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private DateFormatter dateFormatter;
    private TextField textField = new TextField(getTranslation("label.search", UI.getCurrent().getLocale()));

    private SuperDatePicker date;
    private TimePicker startTime;
    private TimePicker endTime;

    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private ModuleMetaDataService moduleMetaDataService;

    private SystemEventLogger systemEventLogger;

    /**
     * Constructor
     *
     * @param scheduledProcessManagementService
     * @param dateFormatter
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param moduleMetaDataService
     * @param isDeepLink
     * @param systemEventLogger
     */
    public RunningAndRecentlyCompletedJobExecutionsWidget(ScheduledProcessManagementService scheduledProcessManagementService,
                                                          DateFormatter dateFormatter, ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                          MetaDataService metaDataRestService, ModuleMetaDataService moduleMetaDataService, boolean isDeepLink, SystemEventLogger systemEventLogger) {
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.dateFormatter = dateFormatter;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.systemEventLogger = systemEventLogger;

        Div div = new Div();
        div.addClassNames("card-counter");
        if(isDeepLink) {
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
        H4 modules = new H4(getTranslation("header.running-and-recently-completed-job-executions", UI.getCurrent().getLocale()));

        this.date = new SuperDatePicker(getTranslation("label.execution-date", UI.getCurrent().getLocale()));
        this.date.setDatePattern(DatePatterns.D_MMMM_YYYY);
        this.date.setValue(LocalDate.now());

        this.startTime = new TimePicker(getTranslation("label.from", UI.getCurrent().getLocale()));
        this.startTime.setStep(Duration.ofMinutes(15));
        this.startTime.setValue(LocalTime.of(0, 0, 0));

        this.endTime = new TimePicker(getTranslation("label.to", UI.getCurrent().getLocale()));
        this.endTime.setStep(Duration.ofMinutes(15));
        this.endTime.setValue(LocalTime.of(23, 59, 59));



        Button refreshButton = new Button();
        refreshButton.addClickListener(buttonClickEvent -> {
            this.runningAndRecentlyCompletedJobExecutionFilteringGrid.refresh();
        });
        refreshButton.getElement().appendChild(VaadinIcon.REFRESH.create().getElement());

        Button newWindowButton = new Button();
        newWindowButton.addClickListener(buttonClickEvent -> {
            RouterLink link = new RouterLink(null, RunningAndRecentlyCompletedJobExecutionDeepLinkView.class);
            getUI().ifPresent(ui -> ui.getPage().open(link.getHref()));
        });
        newWindowButton.getElement().appendChild(VaadinIcon.EXTERNAL_LINK.create().getElement());
        newWindowButton.setVisible(!isDeepLink);



        HorizontalLayout timeComponents = new HorizontalLayout();
        timeComponents.add(date, startTime, endTime, textField, refreshButton, newWindowButton);
        timeComponents.getElement().getStyle().set("margin-left", "auto");

        layout.add(modules, timeComponents);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, modules);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, timeComponents);

        createGrid();

        div.add(layout);
        div.add(this.runningAndRecentlyCompletedJobExecutionFilteringGrid);

        if(isDeepLink) {
            this.runningAndRecentlyCompletedJobExecutionFilteringGrid.setHeight("87vh");
        }
        else {
            this.runningAndRecentlyCompletedJobExecutionFilteringGrid.setHeight("300px");
        }

        this.setSizeFull();
        this.add(div);
    }

    /**
     * Helper method to create the grid.
     */
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
            , this.scheduledProcessFilter, this.dateFormatter, this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.moduleMetaDataService
            , this.systemEventLogger);

        this.runningAndRecentlyCompletedJobExecutionFilteringGrid.addGridFiltering(textField, this.scheduledProcessFilter::setFilter);
        this.runningAndRecentlyCompletedJobExecutionFilteringGrid.addGridFiltering(date, startTime, endTime,
            this.scheduledProcessFilter::setStartTime, this.scheduledProcessFilter::setEndTime);
    }

}
