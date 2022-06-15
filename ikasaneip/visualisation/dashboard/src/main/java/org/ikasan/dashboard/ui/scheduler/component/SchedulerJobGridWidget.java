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
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;

public class SchedulerJobGridWidget extends Div {

    private SchedulerJobFilteringGrid schedulerJobFilteringGrid;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private IkasanAuthentication authentication;
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private SystemEventLogger systemEventLogger;
    private ModuleMetaDataService moduleMetaDataService;
    private JobInitiationService jobInitiationService;
    private ContextTemplate contextTemplate;

    /**
     * Constructor
     */
    public SchedulerJobGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                  ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                  MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                  LogStreamingService logStreamingService, ContextTemplate contextTemplate, JobInitiationService jobInitiationService) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.systemEventLogger = systemEventLogger;
        this.moduleMetaDataService = moduleMetaDataService;
        this.jobInitiationService = jobInitiationService;
        this.contextTemplate = contextTemplate;
        this.createGrid(dynamicImagePath, moduleMetaDataService
            , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, contextTemplate);

        Div div = new Div();
        div.setSizeFull();


        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");


        div.add(this.schedulerJobFilteringGrid);

        this.schedulerJobFilteringGrid.init();

        this.add(div);
        this.setSizeFull();
    }

    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                            ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                            MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                            LogStreamingService logStreamingService, ContextTemplate contextTemplate) {
        // Create a modulesGrid bound to the list
        SolrSchedulerJobSearchFilterImpl schedulerJobSearchFilter = new SolrSchedulerJobSearchFilterImpl();
        schedulerJobFilteringGrid = new SchedulerJobFilteringGrid(schedulerJobService, schedulerJobSearchFilter);
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

        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon edit = IconDecorator.decorate(new Icon(VaadinIcon.EDIT), getTranslation("tooltip.edit-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            edit.setId("editScheduledJob");
            ComponentSecurityVisibility.applySecurity(this.authentication,  edit, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            edit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(schedulerJobRecord.getType().equals(JobConstants.FILE_EVENT_DRIVEN_JOB)) {
                    FileEventJobDialog fileEventJobDialog = new FileEventJobDialog(moduleMetaDataService.findById(schedulerJobRecord.getAgentName())
                        , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                    fileEventJobDialog.setJob(schedulerJobRecord, EditMode.EDIT);

                    fileEventJobDialog.open();

                    fileEventJobDialog.addOpenedChangeListener(event -> {
                        if(!event.isOpened()) {
                            this.schedulerJobFilteringGrid.refreshItem(schedulerJobRecord);
                        }
                    });
                }
                else if(schedulerJobRecord.getType().equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB)) {
                    QuartzDrivenScheduledJobDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobDialog(moduleMetaDataService.findById(schedulerJobRecord.getAgentName())
                        , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                    quartzDrivenScheduledJobDialog.setJob(schedulerJobRecord, EditMode.EDIT);

                    quartzDrivenScheduledJobDialog.open();

                    quartzDrivenScheduledJobDialog.addOpenedChangeListener(event -> {
                        if(!event.isOpened()) {
                            this.schedulerJobFilteringGrid.refreshItem(schedulerJobRecord);
                        }
                    });
                }
                else if(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB)) {
                    InternalEventDrivenJobDialog internalEventDrivenJobDialog = new InternalEventDrivenJobDialog(moduleMetaDataService.findById(schedulerJobRecord.getAgentName())
                        , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                    internalEventDrivenJobDialog.setJob(schedulerJobRecord, EditMode.EDIT);

                    internalEventDrivenJobDialog.open();

                    internalEventDrivenJobDialog.addOpenedChangeListener(event -> {
                        if(!event.isOpened()) {
                            this.schedulerJobFilteringGrid.refreshItem(schedulerJobRecord);
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

            Icon submit = IconDecorator.decorate(new Icon(VaadinIcon.PAPERPLANE), getTranslation("tooltip.submit-job", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            submit.setVisible(schedulerJobRecord.getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB));
            submit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                try {
                    InternalEventDrivenJobInstance jobInstance = this.objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                        , InternalEventDrivenJobInstance.class);
                    ContextInstance contextInstance = this.objectMapper.readValue(objectMapper.writeValueAsBytes(contextTemplate)
                        , ContextInstance.class);

                    InternalEventDrivenJobSubmissionDialog internalEventDrivenJobSubmissionDialog = new InternalEventDrivenJobSubmissionDialog(this.systemEventLogger,
                        this.moduleMetaDataService, contextInstance, this.jobInitiationService, jobInstance);

                    internalEventDrivenJobSubmissionDialog.open();
                }
                catch (Exception e) {
                    // add notification
                }
            });

            layout.add(submit);

            Icon export = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT), getTranslation("label.download-job", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
            StreamResource streamResource = new StreamResource(schedulerJobRecord.getJobName()+".json"
                , () -> {
                try {
                    return new ByteArrayInputStream(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(schedulerJobRecord.getJob()));
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

        HeaderRow hr = schedulerJobFilteringGrid.appendHeaderRow();
        this.schedulerJobFilteringGrid.addGridFiltering(hr, schedulerJobSearchFilter::setJobNameFilter, "flowName");
        this.schedulerJobFilteringGrid.addSelectGridFiltering(hr, schedulerJobSearchFilter::setJobTypeFilter
            , SolrSchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS.entrySet(), "type");

    }
}
