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
import com.vaadin.flow.router.RouteConfiguration;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.scheduler.view.ContextTemplateManagementView;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.instance.model.SolrContextInstanceSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ContextInstanceGridWidget extends Div {

    private ContextInstanceFilteringGrid contextTemplateFilteringGrid;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private IkasanAuthentication authentication;
    private ContextTemplate contextTemplate;

    /**
     * Constructor
     */
    public ContextInstanceGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ContextTemplate contextTemplate, SchedulerJobInstanceService schedulerJobInstanceService) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.contextTemplate = contextTemplate;
        this.createGrid(dynamicImagePath, moduleMetaDataService
            , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, contextTemplate, schedulerJobInstanceService);

        Div div = new Div();
        div.setSizeFull();


        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");


        div.add(this.contextTemplateFilteringGrid);

        this.contextTemplateFilteringGrid.init();

        this.add(div);
        this.setSizeFull();
    }

    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                            ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                            MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                            LogStreamingService logStreamingService, ContextTemplate contextTemplate, SchedulerJobInstanceService schedulerJobInstanceService) {
        // Create a modulesGrid bound to the list
        ContextInstanceSearchFilter contextInstanceSearchFilter = new SolrContextInstanceSearchFilterImpl();
        contextInstanceSearchFilter.setContextSearchFilter(contextTemplate.getName());
        contextTemplateFilteringGrid = new ContextInstanceFilteringGrid(this.scheduledContextInstanceService, contextInstanceSearchFilter);
        contextTemplateFilteringGrid.removeAllColumns();
        contextTemplateFilteringGrid.setVisible(true);
        contextTemplateFilteringGrid.setWidthFull();
        contextTemplateFilteringGrid.setHeight("1000px");
        contextTemplateFilteringGrid.setContextName(contextTemplate.getName());


        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextInstanceRecord.getContextInstanceId());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader("Context Instance Id")
            .setResizable(true)
            .setSortable(true)
            .setKey("componentName")
            .setFlexGrow(2);

        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon view = VaadinIcon.EYE.create();
            view.setSize("14pt");
            view.getStyle().set("cursor", "pointer");
            view.getElement().setAttribute("title", getTranslation("tooltip.view-job", UI.getCurrent().getLocale()));
            ComponentSecurityVisibility.applySecurity(this.authentication, view, SecurityConstants.SCHEDULER_READ, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            view.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ContextInstanceDialog contextInstanceDialog = new ContextInstanceDialog(this.scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
                    , schedulerJobService, logStreamingService, scheduledContextInstanceRecord.getContextInstance(), this.contextTemplate, schedulerJobInstanceService);

                contextInstanceDialog.open();
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

            Icon newWindow = VaadinIcon.EXTERNAL_LINK.create();
            newWindow.setSize("14pt");
            newWindow.getStyle().set("cursor", "pointer");
            newWindow.getElement().setAttribute("title", "Open in New Window");
            newWindow.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextInstanceView.class, scheduledContextInstanceRecord.getId());

                getUI().ifPresent(ui -> ui.getPage().open(route));
            });

            layout.add(newWindow);

            return layout;
        }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
            .setFlexGrow(2);

        this.contextTemplateFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextInstanceRecord>of(
            "<div>[[item.start-date-time]]</div>")
            .withProperty("start-date-time",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getContextInstance().getStartTime())))
            .setHeader(getTranslation("table-header.start-date-time", UI.getCurrent().getLocale()))
            .setKey("startTime")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(3);

        this.contextTemplateFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextInstanceRecord>of(
            "<div>[[item.end-date-time]]</div>")
            .withProperty("end-date-time",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getContextInstance().getEndTime())))
            .setHeader(getTranslation("table-header.end-date-time", UI.getCurrent().getLocale()))
            .setKey("endTime")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(3);

        this.contextTemplateFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextInstanceRecord>of(
            "<div>[[item.created-date-time]]</div>")
            .withProperty("created-date-time",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(3);

        this.contextTemplateFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextInstanceRecord>of(
            "<div>[[item.modified-date-time]]</div>")
            .withProperty("modified-date-time",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getModifiedTimestamp())))
            .setHeader(getTranslation("table-header.modified-date-time", UI.getCurrent().getLocale()))
            .setKey("modifiedTimestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(3);

        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextInstanceRecord.getStatus());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.status", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("status")
            .setFlexGrow(2);

        // Add filtering to the relevant columns.
        HeaderRow hr = contextTemplateFilteringGrid.appendHeaderRow();
        this.contextTemplateFilteringGrid.addGridFiltering(hr, contextInstanceSearchFilter::setContextInstanceId, "componentName");
        this.contextTemplateFilteringGrid.addDateGridFiltering(hr, contextInstanceSearchFilter::setCreatedTimestamp, "timestamp");
        this.contextTemplateFilteringGrid.addDateGridFiltering(hr, contextInstanceSearchFilter::setModifiedTimestamp, "modifiedTimestamp");
        this.contextTemplateFilteringGrid.addSelectGridFiltering(hr, contextInstanceSearchFilter::setStatus
            , Arrays.asList(InstanceStatus.values()).stream().map(instanceStatus -> instanceStatus.name()).collect(Collectors.toList()), "status");
    }
}
