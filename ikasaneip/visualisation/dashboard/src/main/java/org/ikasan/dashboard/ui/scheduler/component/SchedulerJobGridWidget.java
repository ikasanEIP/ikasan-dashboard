package org.ikasan.dashboard.ui.scheduler.component;

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
import org.ikasan.dashboard.ui.scheduler.component.filter.SchedulerJobSearchFilterImpl;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.job.model.JobConstants;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.springframework.security.core.context.SecurityContextHolder;

public class SchedulerJobGridWidget extends Div {

    private SchedulerJobFilteringGrid schedulerJobFilteringGrid;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private IkasanAuthentication authentication;

    /**
     * Constructor
     */
    public SchedulerJobGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                  ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                  MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                  LogStreamingService logStreamingService, ContextTemplate contextTemplate) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
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
        SchedulerJobSearchFilterImpl schedulerJobSearchFilter = new SchedulerJobSearchFilterImpl();
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

            Text text = new Text(SchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS_INVERTED.get(schedulerJobRecord.getType()));

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-type", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("type")
            .setFlexGrow(2);

        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon edit = VaadinIcon.EDIT.create();
            edit.setId("editScheduledJob");
            edit.setSize("14pt");
            edit.getStyle().set("cursor", "pointer");
            edit.getElement().setAttribute("title", getTranslation("tooltip.edit-job", UI.getCurrent().getLocale()));
            ComponentSecurityVisibility.applySecurity(this.authentication,  edit, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            edit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                if(schedulerJobRecord.getType().equals(JobConstants.FILE_EVENT_DRIVEN_JOB)) {
                    FileEventJobDialog fileEventJobDialog = new FileEventJobDialog(moduleMetaDataService.findById(schedulerJobRecord.getAgentName())
                        , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                    fileEventJobDialog.setJob((FileEventDrivenJob)schedulerJobRecord.getJob(), EditMode.EDIT);

                    fileEventJobDialog.open();
                }
                else if(schedulerJobRecord.getType().equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB)) {
                    QuartzDrivenScheduledJobDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobDialog(moduleMetaDataService.findById(schedulerJobRecord.getAgentName())
                        , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                    quartzDrivenScheduledJobDialog.setJob((QuartzScheduleDrivenJob)schedulerJobRecord.getJob(), EditMode.EDIT);

                    quartzDrivenScheduledJobDialog.open();
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

            Icon view = VaadinIcon.EYE.create();
            view.setSize("14pt");
            view.getStyle().set("cursor", "pointer");
            view.getElement().setAttribute("title", getTranslation("tooltip.view-job", UI.getCurrent().getLocale()));
            ComponentSecurityVisibility.applySecurity(this.authentication, view, SecurityConstants.SCHEDULER_READ, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            view.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

            });

            layout.add(view);

            Icon chart = VaadinIcon.CHART.create();
            chart.setSize("14pt");
            chart.getStyle().set("cursor", "pointer");
            chart.getElement().setAttribute("title", getTranslation("tooltip.job-statistics", UI.getCurrent().getLocale()));
            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

            });

            layout.add(chart);

            Icon export = VaadinIcon.DOWNLOAD_ALT.create();
            export.setSize("14pt");
            export.getStyle().set("cursor", "pointer");
            export.getElement().setAttribute("title", "Export");
            export.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

            });

            layout.add(export);

            Icon newWindow = VaadinIcon.PLUS_SQUARE_O.create();
            newWindow.setSize("14pt");
            newWindow.getStyle().set("cursor", "pointer");
            newWindow.getElement().setAttribute("title", "Open in New Window");
            newWindow.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

            });

            layout.add(newWindow);

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
            , SchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS.entrySet(), "type");

    }
}
