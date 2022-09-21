package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.job.model.SolrSchedulerJobSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

public class SchedulerJobSelectGridWidget extends Div {

    private SchedulerJobFilteringGrid schedulerJobFilteringGrid;
    private IkasanAuthentication authentication;
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private SystemEventLogger systemEventLogger;
    private ModuleMetaDataService moduleMetaDataService;
    private JobInitiationService jobInitiationService;
    private ContextTemplate contextTemplate;
    private ModuleControlService moduleControlRestService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private MetaDataService metaDataRestService;
    private SchedulerJobService schedulerJobService;
    private JobProvisionService jobProvisionService;
    private List<SchedulerJobSelectedListener> schedulerJobSelectedListeners = new ArrayList<>();
    private Dialog parent;

    /**
     * Constructor
     */
    public SchedulerJobSelectGridWidget(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                        ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                        MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                        LogStreamingService logStreamingService, ContextTemplate contextTemplate, JobInitiationService jobInitiationService,
                                        JobProvisionService jobProvisionService, Dialog parent) {

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.systemEventLogger = systemEventLogger;
        this.moduleMetaDataService = moduleMetaDataService;
        this.jobInitiationService = jobInitiationService;
        this.contextTemplate = contextTemplate;
        this.moduleControlRestService = moduleControlRestService;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.metaDataRestService = metaDataRestService;
        this.schedulerJobService =  schedulerJobService;
        this.jobProvisionService =  jobProvisionService;
        this.parent = parent;

        this.createGrid(dynamicImagePath, moduleMetaDataService
            , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, contextTemplate);

        this.schedulerJobFilteringGrid.init();

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(false);
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.add(this.createButtonLayout(), this.schedulerJobFilteringGrid);

        this.add(layout);
        this.setSizeFull();
    }

    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                            ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                            MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                            LogStreamingService logStreamingService, ContextTemplate contextTemplate) {
        // Create a modulesGrid bound to the list
        SolrSchedulerJobSearchFilterImpl schedulerJobSearchFilter = new SolrSchedulerJobSearchFilterImpl();
        schedulerJobFilteringGrid = new SchedulerJobFilteringGrid(schedulerJobService, schedulerJobSearchFilter);
        schedulerJobFilteringGrid.getElement().getStyle().set("margin-top", "40px");
        schedulerJobFilteringGrid.removeAllColumns();
        schedulerJobFilteringGrid.setVisible(true);
        schedulerJobFilteringGrid.setWidthFull();
        schedulerJobFilteringGrid.setHeight("75vh");
        schedulerJobFilteringGrid.setContextName(contextTemplate.getName());


        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobRecord.getJobName());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("flowName")
            .setFlexGrow(3);

        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(SolrSchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS_INVERTED.get(schedulerJobRecord.getType()));

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-type", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("type")
            .setFlexGrow(2);

        this.schedulerJobFilteringGrid.addColumn(TemplateRenderer.<SchedulerJobRecord>of(
            "<div>[[item.date]]</div>")
            .withProperty("date",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.schedulerJobFilteringGrid.addColumn(TemplateRenderer.<SchedulerJobRecord>of(
            "<div>[[item.modified]]</div>")
            .withProperty("modified",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getModifiedTimestamp())))
            .setHeader(getTranslation("table-header.modified-date-time", UI.getCurrent().getLocale()))
            .setKey("modifiedTimestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobRecord.getModifiedBy());

            horizontalLayout.add(text);
            return horizontalLayout;
        }))
        .setResizable(true)
        .setHeader(getTranslation("table-header.modified-by", UI.getCurrent().getLocale()))
        .setSortable(true)
        .setFlexGrow(1);

        this.schedulerJobFilteringGrid.addItemDoubleClickListener(event -> {
            this.schedulerJobSelectedListeners.forEach(listener -> listener.jobSelected(event.getItem().getJob()));
            if(parent != null) {
                parent.close();
            }
        });

        HeaderRow hr = schedulerJobFilteringGrid.appendHeaderRow();
        this.schedulerJobFilteringGrid.addGridFiltering(hr, schedulerJobSearchFilter::setJobNameFilter, "flowName");
        this.schedulerJobFilteringGrid.addSelectGridFiltering(hr, schedulerJobSearchFilter::setJobTypeFilter
            , SolrSchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS.entrySet(), "type");

    }

    private HorizontalLayout createButtonLayout() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(false);
        buttonLayout.setPadding(false);
        buttonLayout.getElement().getStyle().set("position", "absolute");
        buttonLayout.getElement().getStyle().set("right", "30px");
        buttonLayout.getElement().getStyle().set("margin-top", "0px");
        Button refreshButton = this.createRefreshButton();
        buttonLayout.add(refreshButton);

        return buttonLayout;
    }

    private Button createRefreshButton() {
        Button refreshJobsButton = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()), VaadinIcon.REFRESH.create());
        refreshJobsButton.setIconAfterText(true);

        refreshJobsButton.addClickListener(event -> this.schedulerJobFilteringGrid.init());

        return refreshJobsButton;
    }

    public void addSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.schedulerJobSelectedListeners.add(listener);
    }

    public void removeSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.schedulerJobSelectedListeners.remove(listener);
    }
}
