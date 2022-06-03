package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.server.StreamResource;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceSearchFilterImpl;
import org.ikasan.scheduled.job.model.JobConstants;
import org.ikasan.scheduled.job.model.SolrSchedulerJobSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SchedulerJobInstanceGridWidget extends Div {

    private SchedulerJobInstanceFilteringGrid schedulerJobInstanceFilteringGrid;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private IkasanAuthentication authentication;
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    /**
     * Constructor
     */
    public SchedulerJobInstanceGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                          MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                          LogStreamingService logStreamingService, ContextInstance contextInstance, SchedulerJobInstanceService schedulerJobInstanceService) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.createGrid(dynamicImagePath, moduleMetaDataService
            , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, contextInstance);

        Div div = new Div();
        div.setSizeFull();


        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");


        div.add(this.schedulerJobInstanceFilteringGrid);

        this.schedulerJobInstanceFilteringGrid.init();

        this.add(div);
        this.setSizeFull();
    }

    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                            ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                            MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                            LogStreamingService logStreamingService, ContextInstance contextInstance) {
        // Create a modulesGrid bound to the list
        SchedulerJobInstanceSearchFilter schedulerJobSearchFilter = new SolrSchedulerJobInstanceSearchFilterImpl();
        schedulerJobInstanceFilteringGrid = new SchedulerJobInstanceFilteringGrid(this.schedulerJobInstanceService, schedulerJobSearchFilter);
        schedulerJobInstanceFilteringGrid.removeAllColumns();
        schedulerJobInstanceFilteringGrid.setVisible(true);
        schedulerJobInstanceFilteringGrid.setWidthFull();
        schedulerJobInstanceFilteringGrid.setHeight("75vh");
        schedulerJobInstanceFilteringGrid.setContextInstanceId(contextInstance.getId());


        schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobInstanceRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobInstanceRecord.getJobName());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("moduleName")
            .setFlexGrow(3);

        schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobInstanceRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(SolrSchedulerJobInstanceSearchFilterImpl.JOB_TYPE_MAPPINGS_INVERTED.get(schedulerJobInstanceRecord.getType()));

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-type", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("type")
            .setFlexGrow(2);

        schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobInstanceRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobInstanceRecord.getChildContextName());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.child-context-name", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("childContextName")
            .setFlexGrow(2);

        schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobInstanceRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon edit = IconDecorator.decorate(new Icon(VaadinIcon.EDIT), getTranslation("tooltip.edit-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            edit.setId("editScheduledJob");
            ComponentSecurityVisibility.applySecurity(this.authentication,  edit, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            edit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(schedulerJobInstanceRecord.getType().equals(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE)) {
                    FileEventJobInstanceDialog fileEventJobDialog = new FileEventJobInstanceDialog(moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName())
                        , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobInstanceService);
                    fileEventJobDialog.setJob(schedulerJobInstanceRecord, EditMode.READONLY);

                    fileEventJobDialog.open();

                    fileEventJobDialog.addOpenedChangeListener(event -> {
                        if(!event.isOpened()) {
                            this.schedulerJobInstanceFilteringGrid.refreshItem(schedulerJobInstanceRecord);
                        }
                    });
                }
                else if(schedulerJobInstanceRecord.getType().equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE)) {
                    QuartzDrivenScheduledJobInstanceDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobInstanceDialog(moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName())
                        , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, this.schedulerJobInstanceService);
                    quartzDrivenScheduledJobDialog.setJob(schedulerJobInstanceRecord, EditMode.READONLY);

                    quartzDrivenScheduledJobDialog.open();

                    quartzDrivenScheduledJobDialog.addOpenedChangeListener(event -> {
                        if(!event.isOpened()) {
                            this.schedulerJobInstanceFilteringGrid.refreshItem(schedulerJobInstanceRecord);
                        }
                    });
                }
                else if(schedulerJobInstanceRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
                    InternalEventDrivenJobInstanceDialog internalEventDrivenJobDialog = new InternalEventDrivenJobInstanceDialog(moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName())
                        , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, this.schedulerJobInstanceService);
                    internalEventDrivenJobDialog.setJob(schedulerJobInstanceRecord, EditMode.READONLY);

                    internalEventDrivenJobDialog.open();

                    internalEventDrivenJobDialog.addOpenedChangeListener(event -> {
                        if(!event.isOpened()) {
                            this.schedulerJobInstanceFilteringGrid.refreshItem(schedulerJobInstanceRecord);
                        }
                    });

                }
            });

            layout.add(edit);

            Icon view = IconDecorator.decorate(new Icon(VaadinIcon.EYE), getTranslation("tooltip.view-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, view, SecurityConstants.SCHEDULER_READ, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            view.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

            });

            layout.add(view);

            Icon chart = IconDecorator.decorate(new Icon(VaadinIcon.CHART), getTranslation("tooltip.job-statistics", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

            });

            layout.add(chart);

            Icon export = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT), getTranslation("label.download-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            StreamResource streamResource = new StreamResource(schedulerJobInstanceRecord.getJobName()+".json"
                , () -> {
                try {
                    return new ByteArrayInputStream(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(schedulerJobInstanceRecord.getSchedulerJobInstance()));
                }
                catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return null;
                }
            });

            FileDownloadWrapper exportWrapper = new FileDownloadWrapper(streamResource);
            exportWrapper.wrapComponent(export);
            layout.add(exportWrapper);

            return layout;
        }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
            .setFlexGrow(1);

        this.schedulerJobInstanceFilteringGrid.addColumn(TemplateRenderer.<SchedulerJobInstanceRecord>of(
            "<div>[[item.date]]</div>")
            .withProperty("date",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.schedulerJobInstanceFilteringGrid.addColumn(TemplateRenderer.<SchedulerJobInstanceRecord>of(
            "<div>[[item.modified]]</div>")
            .withProperty("modified",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getModifiedTimestamp())))
            .setHeader(getTranslation("table-header.modified-date-time", UI.getCurrent().getLocale()))
            .setKey("modifiedTimestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobRecord.getModifiedBy());

            horizontalLayout.add(text);
            return horizontalLayout;
        }))
        .setResizable(true)
        .setHeader(getTranslation("table-header.modified-by", UI.getCurrent().getLocale()))
        .setSortable(true)
        .setFlexGrow(1);

        schedulerJobInstanceFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobInstanceRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            SchedulerStatusDiv schedulerStatusDiv = new SchedulerStatusDiv();
            schedulerStatusDiv.getElement().getStyle().set("font-size", "10pt");
            schedulerStatusDiv.getElement().getStyle().set("margin-top", "1px");
            schedulerStatusDiv.getElement().getStyle().set("margin-bottom", "1px");
            schedulerStatusDiv.setWidth("100%");
            schedulerStatusDiv.setStatus(schedulerJobInstanceRecord.getStatus());

            horizontalLayout.add(schedulerStatusDiv);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.status", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("status")
            .setFlexGrow(1);

        HeaderRow hr = schedulerJobInstanceFilteringGrid.appendHeaderRow();
        this.schedulerJobInstanceFilteringGrid.addGridFiltering(hr, schedulerJobSearchFilter::setJobName, "moduleName");
        this.schedulerJobInstanceFilteringGrid.addGridFiltering(hr, schedulerJobSearchFilter::setChildContextName, "childContextName");
        this.schedulerJobInstanceFilteringGrid.addSelectGridFiltering(hr, schedulerJobSearchFilter::setJobType
            , SolrSchedulerJobInstanceSearchFilterImpl.JOB_TYPE_MAPPINGS.entrySet(), "type");
        this.schedulerJobInstanceFilteringGrid.addSelectGridFiltering(hr, schedulerJobSearchFilter::setStatus
            , Arrays.asList(InstanceStatus.values()).stream().map(instanceStatus -> instanceStatus.name()).collect(Collectors.toList()), "status");

    }
}
