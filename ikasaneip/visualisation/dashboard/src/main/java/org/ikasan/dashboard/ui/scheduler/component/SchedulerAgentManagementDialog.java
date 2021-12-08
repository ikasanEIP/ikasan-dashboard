package org.ikasan.dashboard.ui.scheduler.component;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.IronIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.model.AgentJobFilter;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.SchedulerService;

public class SchedulerAgentManagementDialog extends AbstractCloseableResizableDialog {

    private ModuleMetaData agent;

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private ModuleMetaDataService moduleMetaDataService;
    private TextField filterTf;
    private SchedulerService schedulerService;

    private SystemEventLogger systemEventLogger;

    /**
     * Constructor
     *
     * @param agent
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param moduleMetaDataService
     * @param systemEventLogger
     * @param schedulerService
     */
    public SchedulerAgentManagementDialog(ModuleMetaData agent, ScheduledProcessManagementService scheduledProcessManagementService,
                                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                          MetaDataService metaDataRestService, ModuleMetaDataService moduleMetaDataService,
                                          SystemEventLogger systemEventLogger, SchedulerService schedulerService) {
        this.agent = agent;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerService = schedulerService;
        super.showResize(false);
        super.title.setText(getTranslation("header.scheduler-agent-management", UI.getCurrent().getLocale()));

        this.setHeight("850px");
        this.setWidth("95%");

        Button addButton = new Button();
        addButton.setId("newScheduledJobButton");
        addButton.getStyle().set("position", "absolute");
        addButton.getStyle().set("top", "70px");
        addButton.getStyle().set("right", "30px");

        addButton.addClickListener(buttonClickEvent -> {
            ScheduledJobDialog scheduledJobDialog = new ScheduledJobDialog(this.agent,
                this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService,
                this.metaDataRestService, this.systemEventLogger);

            scheduledJobDialog.open();
        });

        IronIcon addIcon = IronIcons.ADD.create();
        addIcon.setSize("16pt");
        addButton.getElement().appendChild(addIcon.getElement());

        Button addQuartzJobButton = new Button("New Quartz");
        addQuartzJobButton.setId("newScheduledJobButton");
        addQuartzJobButton.getStyle().set("position", "absolute");
        addQuartzJobButton.getStyle().set("top", "70px");
        addQuartzJobButton.getStyle().set("right", "70px");

        addQuartzJobButton.addClickListener(buttonClickEvent -> {
            QuartzDrivenScheduledJobDialog scheduledJobDialog = new QuartzDrivenScheduledJobDialog(this.agent,
                this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService,
                this.metaDataRestService, this.systemEventLogger);

            scheduledJobDialog.open();
        });

        Button addFileEventJobButton = new Button("New File");
        addFileEventJobButton.setId("addFileEventJobButton");
        addFileEventJobButton.getStyle().set("position", "absolute");
        addFileEventJobButton.getStyle().set("top", "70px");
        addFileEventJobButton.getStyle().set("right", "180px");

        addFileEventJobButton.addClickListener(buttonClickEvent -> {
            FileEventJobDialog fileEventJobDialog = new FileEventJobDialog(this.agent,
                this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService,
                this.metaDataRestService, this.systemEventLogger);

            fileEventJobDialog.open();
        });

        Button addInternalEventJobButton = new Button("New Internal");
        addInternalEventJobButton.setId("addInternalEventJobButton");
        addInternalEventJobButton.getStyle().set("position", "absolute");
        addInternalEventJobButton.getStyle().set("top", "70px");
        addInternalEventJobButton.getStyle().set("right", "270px");

        addInternalEventJobButton.addClickListener(buttonClickEvent -> {
            InternalEventDrivenJobDialog fileEventJobDialog = new InternalEventDrivenJobDialog(this.agent,
                this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService,
                this.metaDataRestService, this.systemEventLogger);

            fileEventJobDialog.open();
        });

        // todo translation for header.agent-details not in bundle.
        H4 agentDetails = new H4(getTranslation("header.agent-details", UI.getCurrent().getLocale()));


        FormLayout formLayout = new FormLayout();

        TextField agentName = new TextField(getTranslation("label.agent", UI.getCurrent().getLocale()));
        agentName.setValue(agent.getName());
        agentName.setEnabled(false);
        formLayout.add(agentName);

        Anchor link = new Anchor(agent.getUrl(), agent.getUrl());
        link.setTarget("_blank");
        link.getStyle().set("color", "blue");

        TextField agentUrlLf = new TextField(getTranslation("label.agent-url", UI.getCurrent().getLocale()));
        agentUrlLf.setPrefixComponent(link);
        agentUrlLf.setValue(" ");
        formLayout.add(agentUrlLf);


        H4 scheduledJobsLabel = new H4(getTranslation("header.scheduled-jobs", UI.getCurrent().getLocale()));

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        AgentJobFilter agentJobFiler = new AgentJobFilter();
        AgentJobFilteringGrid filteringGrid = new AgentJobFilteringGrid(this.agent, this.scheduledProcessManagementService
            , agentJobFiler, new DateFormatter(), this.configurationRestService, this.moduleControlRestService,
            this.metaDataRestService, this.moduleMetaDataService, this.systemEventLogger, this.schedulerService);
        filteringGrid.setSizeFull();

        this.filterTf = new TextField();
        this.filterTf.setPrefixComponent(icon);
        HorizontalLayout filterLayout = new HorizontalLayout();

        this.filterTf.getElement().getStyle().set("margin-left", "auto");

        Button refreshButton = new Button();
        refreshButton.addClickListener(buttonClickEvent -> {
            filteringGrid.refresh();
        });
        refreshButton.getElement().appendChild(VaadinIcon.REFRESH.create().getElement());
        refreshButton.getElement().getStyle().set("margin-left", "auto");

        filterLayout.add(this.filterTf, refreshButton);
        filterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, this.filterTf);
        filterLayout.getElement().getStyle().set("margin-left", "auto");

        filteringGrid.addGridFiltering(this.filterTf, agentJobFiler::setFilter);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(agentDetails, addInternalEventJobButton, addButton, addFileEventJobButton, addQuartzJobButton, formLayout, scheduledJobsLabel,     filterLayout, filteringGrid);
        super.content.add(layout);
    }
}
