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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.scheduler.model.ScheduledProcessFilter;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.FlowMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.miki.shared.dates.DatePatterns;
import org.vaadin.miki.superfields.dates.SuperDatePicker;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@CssImport("./styles/dashboard-view.css")
public class UpcomingJobExecutionsWidget extends Div {

    Logger logger = LoggerFactory.getLogger(UpcomingJobExecutionsWidget.class);

    private UpcomingJobExecutionFilteringGrid upcomingJobExecutionFilteringGrid;
    private ScheduledProcessFilter scheduledProcessFilter;

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private DateFormatter dateFormatter;
    private Select<String> agentSelect = new Select();
    private Select<FlowMetaData> jobSelect = new Select();
    private SuperDatePicker date;
    private TimePicker startTime;
    private TimePicker endTime;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private ModuleMetaDataService moduleMetaDataService;
    private SystemEventLogger systemEventLogger;
    private IkasanAuthentication authentication;

    private boolean initialised = false;

    private String selectedAgent = null;
    private FlowMetaData selectedFlowMetaData = null;

    /**
     * Constructor
     *
     * @param scheduledProcessManagementService
     * @param dateFormatter
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param moduleMetaDataService
     * @param isDeeplink
     * @param systemEventLogger
     */
    public UpcomingJobExecutionsWidget(ScheduledProcessManagementService scheduledProcessManagementService, DateFormatter dateFormatter
        , ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService
        , ModuleMetaDataService moduleMetaDataService, boolean isDeeplink, SystemEventLogger systemEventLogger) {
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.dateFormatter = dateFormatter;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.systemEventLogger = systemEventLogger;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

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

        agentSelect.setLabel(getTranslation("label.agent", UI.getCurrent().getLocale()));
        jobSelect.setLabel(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        jobSelect.setItemLabelGenerator(FlowMetaData::getName);

        HorizontalLayout layout = new HorizontalLayout();
        H4 modules = new H4(getTranslation("header.upcoming-job-executions", UI.getCurrent().getLocale()));

        this.date = new SuperDatePicker(getTranslation("label.execution-date", UI.getCurrent().getLocale()));
        this.date.setDatePattern(DatePatterns.D_MMMM_YYYY);
        this.date.setValue(LocalDate.now());

        this.startTime = new TimePicker(getTranslation("label.from", UI.getCurrent().getLocale()));
        this.startTime.setStep(Duration.ofMinutes(15));
        this.startTime.setValue(LocalTime.now());

        this.endTime = new TimePicker(getTranslation("label.to", UI.getCurrent().getLocale()));
        this.endTime.setStep(Duration.ofMinutes(15));
        this.endTime.setValue(LocalTime.of(23, 59, 59));

        Button refreshButton = new Button();
        refreshButton.addClickListener(buttonClickEvent -> {
           this.upcomingJobExecutionFilteringGrid.refresh();
        });
        refreshButton.getElement().appendChild(VaadinIcon.REFRESH.create().getElement());

        Button newWindowButton = new Button();
        newWindowButton.addClickListener(buttonClickEvent -> {
            RouteParameters routeParameters = new RouteParameters(new RouteParam("agent", this.selectedAgent), new RouteParam("job", this.selectedFlowMetaData.getName()));
            RouterLink link = new RouterLink(null, UpcomingJobExecutionDeepLinkView.class, routeParameters);
            getUI().ifPresent(ui -> ui.getPage().open(link.getHref()));
        });
        newWindowButton.getElement().appendChild(VaadinIcon.EXTERNAL_LINK.create().getElement());
        newWindowButton.setVisible(!isDeeplink);

        HorizontalLayout timeComponents = new HorizontalLayout();
        timeComponents.add(date, startTime, endTime, agentSelect, jobSelect, refreshButton, newWindowButton);
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

    /**
     * Initialise the grid.
     */
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
            this.metaDataRestService, this.moduleMetaDataService, this.systemEventLogger);

        this.upcomingJobExecutionFilteringGrid.addGridFiltering(this.agentSelect, this.jobSelect, this.scheduledProcessFilter::setAgentName,
            this.scheduledProcessFilter::setJobName);
        this.upcomingJobExecutionFilteringGrid.addGridFiltering(date, startTime, endTime,
            this.scheduledProcessFilter::setStartTime, this.scheduledProcessFilter::setEndTime);
    }

    public void initialise() {
        List<String> agentNames;

        if(this.authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY)
            || this.authentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ADMIN)) {

            agentNames = this.scheduledProcessManagementService.getAllAgentNames();
        }
        else {
            agentNames = new ArrayList<>(SecurityUtils.getAccessibleModules(this.authentication));
        }
        agentSelect.setItems(agentNames);

        if(agentNames.size() > 0) {
            List<FlowMetaData> flowMetaData;
            if(selectedAgent != null && agentNames.contains(selectedAgent)) {
                agentSelect.setValue(selectedAgent);
                this.scheduledProcessFilter.setAgentName(selectedAgent);
                flowMetaData = this.scheduledProcessManagementService.getFlowsForAgent(selectedAgent);
            }
            else {
                agentSelect.setValue(agentNames.get(0));
                this.scheduledProcessFilter.setAgentName(agentNames.get(0));
                flowMetaData = this.scheduledProcessManagementService.getFlowsForAgent(agentNames.get(0));
            }

            this.selectedAgent = this.agentSelect.getValue();


            if(flowMetaData.size() > 0) {
                flowMetaData = flowMetaData.stream()
                    .filter(flow -> flow.getConsumer().getComponentName().equals("Scheduled Consumer")
                        || flow.getConsumer().getComponentName().equals("File Consumer"))
                    .collect(Collectors.toList());

                jobSelect.setItems(flowMetaData);
                if(selectedFlowMetaData != null && flowMetaData.contains(selectedFlowMetaData)) {
                    jobSelect.setValue(flowMetaData.stream().filter(e -> e.equals(selectedFlowMetaData)).findFirst().get());
                    this.scheduledProcessFilter.setJobName(selectedFlowMetaData.getName());
                }
                else {
                    jobSelect.setValue(flowMetaData.get(0));
                    this.scheduledProcessFilter.setJobName(flowMetaData.get(0).getName());
                }

                this.selectedFlowMetaData = this.jobSelect.getValue();
            }
        }

        if(!initialised) {
            jobSelect.addValueChangeListener(event -> {
                if(event.getValue() != null) {
                    this.selectedFlowMetaData = event.getValue();
                }
            });

            agentSelect.addValueChangeListener(event -> {
                jobSelect.removeAll();

                if (event.getValue() != null) {
                    this.selectedAgent = event.getValue();
                    this.selectedAgent = this.agentSelect.getValue();
                    List<FlowMetaData> flowMetaData = this.scheduledProcessManagementService.getFlowsForAgent(event.getValue());

                    if (flowMetaData.size() > 0) {
                        flowMetaData = flowMetaData.stream()
                            .filter(flow -> flow.getConsumer().getComponentName().equals("Scheduled Consumer")
                                || flow.getConsumer().getComponentName().equals("File Consumer"))
                            .collect(Collectors.toList());
                        jobSelect.setItems(flowMetaData);
                        if (this.selectedFlowMetaData != null && flowMetaData.contains(this.selectedFlowMetaData)) {
                            jobSelect.setValue(flowMetaData.stream().filter(e -> e.equals(selectedFlowMetaData)).findFirst().get());
                            this.scheduledProcessFilter.setJobName(selectedFlowMetaData.getName());
                        } else {
                            jobSelect.setValue(flowMetaData.get(0));
                            this.scheduledProcessFilter.setJobName(flowMetaData.get(0).getName());
                        }
                        this.selectedFlowMetaData = this.jobSelect.getValue();
                    }
                }
            });
        }

        this.upcomingJobExecutionFilteringGrid.refresh();
        this.initialised = true;
    }


    public void setSelectedAgent(String selectedAgent) {
        this.selectedAgent = selectedAgent;
    }


    public void setSelectedFlowMetaData(FlowMetaData selectedFlowMetaData) {
        this.selectedFlowMetaData = selectedFlowMetaData;
    }
}
