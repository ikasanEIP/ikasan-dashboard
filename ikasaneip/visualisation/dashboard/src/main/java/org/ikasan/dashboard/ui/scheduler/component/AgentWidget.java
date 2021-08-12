package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.component.filter.ModuleSearchFilter;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.SchedulerService;

public class AgentWidget extends Div {

    private ScheduledAgentsFilteringGrid scheduledAgentsFilteringGrid;
    private ModuleMetaDataService moduleMetadataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private TextField textField;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private SchedulerService schedulerService;

    /**
     * Constructor
     *
     * @param moduleMetadataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerService
     */
    public AgentWidget(ModuleMetaDataService moduleMetadataService, ScheduledProcessManagementService scheduledProcessManagementService,
                       ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                       SystemEventLogger systemEventLogger, SchedulerService schedulerService) {
        this.moduleMetadataService = moduleMetadataService;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerService = schedulerService;
        this.textField = new TextField();
        this.createGrid();

        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("500px");


        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);
        HorizontalLayout layout = new HorizontalLayout();
        H4 modules = new H4(getTranslation("header.scheduler-agents", UI.getCurrent().getLocale()));
        layout.add(modules, textField);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, modules);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, textField);

        textField.getElement().getStyle().set("margin-left", "auto");

        div.add(layout);
        div.add(this.scheduledAgentsFilteringGrid);

        this.scheduledAgentsFilteringGrid.init();

        this.add(div);
    }

    private void createGrid() {
        // Create a modulesGrid bound to the list
        ModuleSearchFilter moduleSearchFilter = new ModuleSearchFilter();
        scheduledAgentsFilteringGrid = new ScheduledAgentsFilteringGrid(this.moduleMetadataService, moduleSearchFilter);
        scheduledAgentsFilteringGrid.removeAllColumns();
        scheduledAgentsFilteringGrid.setVisible(true);
        scheduledAgentsFilteringGrid.setWidthFull();
        scheduledAgentsFilteringGrid.setHeight("80%");

        scheduledAgentsFilteringGrid.addColumn(ModuleMetaData::getName)
            .setHeader(getTranslation("table-header.module-name", UI.getCurrent().getLocale())).setKey("name")
            .setFlexGrow(16);
        scheduledAgentsFilteringGrid.addColumn(TemplateRenderer.<ModuleMetaData>of("<div style='white-space:normal'>[[item.description]]</div>")
            .withProperty("description", ModuleMetaData::getDescription))
            .setHeader(getTranslation("table-header.module-description", UI.getCurrent().getLocale()))
            .setKey("description")
            .setFlexGrow(32);
        scheduledAgentsFilteringGrid.addColumn(new ComponentRenderer<>(moduleMetaData -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            String route = RouteConfiguration.forSessionScope()
                .getUrl(GraphVisualisationDeepLinkView.class, VisualisationType.MODULE.name() + ":" + moduleMetaData.getName());
            Anchor link = new Anchor(route, "view");
            link.setTarget("_blank");
            add(link);
            horizontalLayout.add(link);
            link.getStyle().set("color", "blue");

            return horizontalLayout;
        })).setWidth("60px");

        this.scheduledAgentsFilteringGrid.addGridFiltering(textField, moduleSearchFilter::setModuleNameFilter);

        this.scheduledAgentsFilteringGrid.addItemDoubleClickListener((ComponentEventListener<ItemDoubleClickEvent<ModuleMetaData>>) moduleMetaDataItemDoubleClickEvent -> {
            SchedulerAgentManagementDialog schedulerAgentManagementDialog
                = new SchedulerAgentManagementDialog(moduleMetaDataItemDoubleClickEvent.getItem()
                    , this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService, this.metaDataRestService
                    , this.moduleMetadataService, systemEventLogger, schedulerService);

            schedulerAgentManagementDialog.open();
        });
    }
}
