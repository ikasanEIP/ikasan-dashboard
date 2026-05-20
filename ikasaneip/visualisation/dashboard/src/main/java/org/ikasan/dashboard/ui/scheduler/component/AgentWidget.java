package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.LitRenderer;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.component.filter.ModuleSearchFilter;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.DownloadLogFileService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.general.SchedulerService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;

import java.util.Map;

public class AgentWidget extends DashboardWidget {

    private ScheduledAgentsFilteringGrid scheduledAgentsFilteringGrid;
    private ModuleMetaDataService moduleMetadataService;
    private TextField filterTextField;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private SchedulerService schedulerService;
    private SchedulerJobService schedulerJobService;

    private Map<String, String> schedulerJobExecutionEnvironmentLabel;
    private DownloadLogFileService downloadLogFileService;
    private ScheduledContextService scheduledContextService;
    private JobProvisionService jobProvisionService;


    public AgentWidget(ModuleMetaDataService moduleMetadataService,
                       ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                       SystemEventLogger systemEventLogger, SchedulerService schedulerService, JobProvisionService jobProvisionService, SchedulerJobService schedulerJobService,
                       DownloadLogFileService downloadLogFileService, ScheduledContextService scheduledContextService) {
        this.moduleMetadataService = moduleMetadataService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerService = schedulerService;
        this.schedulerJobService = schedulerJobService;
        this.jobProvisionService = jobProvisionService;
        this.downloadLogFileService = downloadLogFileService;
        this.scheduledContextService = scheduledContextService;
        this.filterTextField = new TextField();
        this.createGrid();

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        filterTextField.setPrefixComponent(icon);
        filterTextField.setId("filterTextField");
        HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(true);
        layout.setWidth("100%");
        H4 modules = new H4(getTranslation("header.scheduler-agents", UI.getCurrent().getLocale()));
        layout.add(modules, filterTextField);

        filterTextField.getElement().getStyle().set("margin-left", "auto");

        this.scheduledAgentsFilteringGrid.init();

        this.setHeaderContent(layout);
        this.setContent(this.scheduledAgentsFilteringGrid);
    }

    private void createGrid() {
        // Create a modulesGrid bound to the list
        ModuleSearchFilter moduleSearchFilter = new ModuleSearchFilter();
        this.scheduledAgentsFilteringGrid = new ScheduledAgentsFilteringGrid(this.moduleMetadataService, moduleSearchFilter);
        this.scheduledAgentsFilteringGrid.setId("scheduledAgentsFilteringGrid");
        this.scheduledAgentsFilteringGrid.removeAllColumns();
        this.scheduledAgentsFilteringGrid.setVisible(true);
        this.scheduledAgentsFilteringGrid.setWidthFull();
        this.scheduledAgentsFilteringGrid.setHeight(100, Unit.PERCENTAGE);

        scheduledAgentsFilteringGrid.addColumn(ModuleMetaData::getName)
            .setHeader(getTranslation("table-header.module-name", UI.getCurrent().getLocale())).setKey("name")
            .setFlexGrow(16);
        scheduledAgentsFilteringGrid.addColumn(LitRenderer.<ModuleMetaData>of("<div style='white-space:normal'>${item.description}</div>")
            .withProperty("description", ModuleMetaData::getDescription))
            .setHeader(getTranslation("table-header.module-description", UI.getCurrent().getLocale()))
            .setKey("description")
            .setFlexGrow(32);;

        this.scheduledAgentsFilteringGrid.addGridFiltering(filterTextField, moduleSearchFilter::setModuleNameFilter);
        this.scheduledAgentsFilteringGrid.addItemDoubleClickListener((ComponentEventListener<ItemDoubleClickEvent<ModuleMetaData>>) moduleMetaDataItemDoubleClickEvent -> {
            SchedulerAgentManagementDialog schedulerAgentManagementDialog
                = new SchedulerAgentManagementDialog(moduleMetaDataItemDoubleClickEvent.getItem(), downloadLogFileService
                , this.scheduledContextService, this.jobProvisionService, this.schedulerJobService);

            schedulerAgentManagementDialog.open();
        });
    }
}
