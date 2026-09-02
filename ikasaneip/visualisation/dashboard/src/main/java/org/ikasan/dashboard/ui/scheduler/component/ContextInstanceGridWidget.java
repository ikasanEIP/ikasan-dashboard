package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

public class ContextInstanceGridWidget extends Div {

    private ContextInstanceFilteringGrid contextInstanceFilteringGrid;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private IkasanAuthentication authentication;
    private ContextTemplate contextTemplate;
    private JobInitiationService jobInitiationService;
    private ContextProfileService contextProfileService;
    private JobUtilsService jobUtilsService;
    private ScheduledContextService scheduledContextService;
    private GlobalEventService globalEventService;
    private ContextInstanceRegistrationService contextInstanceRegistrationService;
    private ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;
    private SystemEventSearchService systemEventSearchService;


    /**
     * Constructor
     *
     * @param scheduledContextInstanceService
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     * @param contextTemplate
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param contextProfileService
     * @param jobUtilsService
     * @param scheduledContextService
     */
    public ContextInstanceGridWidget(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                                     ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                     MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                     LogStreamingService logStreamingService, ContextTemplate contextTemplate, SchedulerJobInstanceService schedulerJobInstanceService,
                                     JobInitiationService jobInitiationService, ContextProfileService contextProfileService, JobUtilsService jobUtilsService,
                                     ScheduledContextService scheduledContextService, GlobalEventService globalEventService, ContextInstanceRegistrationService contextInstanceRegistrationService,
                                     ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService, SystemEventSearchService systemEventSearchService, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing,
                                     double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.contextTemplate = contextTemplate;
        if (this.contextTemplate == null) {
            throw new IllegalArgumentException("contextTemplate cannot be null!");
        }
        this.jobInitiationService = jobInitiationService;
        if (this.jobInitiationService == null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }
        this.contextProfileService = contextProfileService;
        if (this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }
        this.jobUtilsService = jobUtilsService;
        if (this.jobUtilsService == null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.globalEventService = globalEventService;
        if (this.globalEventService == null) {
            throw new IllegalArgumentException("globalEventService cannot be null!");
        }
        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        if (this.contextInstanceRegistrationService == null) {
            throw new IllegalArgumentException("contextInstanceRegistrationService cannot be null!");
        }
        this.contextInstanceSchedulerService = contextInstanceSchedulerService;
        if (this.contextInstanceSchedulerService == null) {
            throw new IllegalArgumentException("contextInstanceSchedulerService cannot be null!");
        }
        this.systemEventSearchService = systemEventSearchService;
        if (this.systemEventSearchService == null) {
            throw new IllegalArgumentException("systemEventSearchService cannot be null!");
        }

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        this.createGrid(dynamicImagePath, moduleMetaDataService
            , configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, contextTemplate, schedulerJobInstanceService, jobVisualisationVerticalSpacing, jobVisualisationHorizontalSpacing,
            contextVisualisationLevelDistance, contextVisualisationNodeDistance);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(false);
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.add(this.createButtonLayout(), this.contextInstanceFilteringGrid);

        this.contextInstanceFilteringGrid.init();

        this.add(layout);
        this.setSizeFull();
    }

    /**
     * Helper method to create the context instances grid.
     *
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     * @param contextTemplate
     * @param schedulerJobInstanceService
     */
    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                            ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                            MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                            LogStreamingService logStreamingService, ContextTemplate contextTemplate, SchedulerJobInstanceService schedulerJobInstanceService,
                            double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {
        // Create a modulesGrid bound to the list
        ContextInstanceSearchFilter contextInstanceSearchFilter = new ContextInstanceSearchFilterImpl();
        contextInstanceSearchFilter.setContextInstanceNames(Collections.singletonList(contextTemplate.getName()));
        contextInstanceFilteringGrid = new ContextInstanceFilteringGrid(this.scheduledContextInstanceService, contextInstanceSearchFilter);
        contextInstanceFilteringGrid.getElement().getStyle().set("margin-top", "40px");
        contextInstanceFilteringGrid.removeAllColumns();
        contextInstanceFilteringGrid.setVisible(true);
        contextInstanceFilteringGrid.setWidthFull();
        contextInstanceFilteringGrid.setHeight("1000px");
        contextInstanceFilteringGrid.setContextName(contextTemplate.getName());


        contextInstanceFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextInstanceRecord.getContextInstanceId());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader("Context Instance Id")
            .setResizable(true)
            .setSortable(true)
            .setKey("componentName")
            .setFlexGrow(2);


        contextInstanceFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon view = VaadinIcon.MODAL.create();
            view.setSize("14pt");
            view.getStyle().set("cursor", "pointer");
            view.getElement().setAttribute("title", getTranslation("tooltip.view-job", UI.getCurrent().getLocale()));
            ComponentSecurityVisibility.applySecurity(this.authentication, view, SecurityConstants.SCHEDULER_READ, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            view.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ContextInstanceDialog contextInstanceDialog = new ContextInstanceDialog(this.scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService
                    , configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
                    , schedulerJobService, logStreamingService, scheduledContextInstanceRecord.getContextInstance(), this.contextTemplate, schedulerJobInstanceService
                    , this.jobInitiationService, this.contextProfileService, this.jobUtilsService, this.scheduledContextService, this.globalEventService
                    , this.contextInstanceRegistrationService, this.contextInstanceSchedulerService, this.systemEventSearchService, jobVisualisationVerticalSpacing, jobVisualisationHorizontalSpacing,
                    contextVisualisationLevelDistance, contextVisualisationNodeDistance);

                contextInstanceDialog.open();
            });

            ComponentSecurityVisibility.applySecurity(view, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

            layout.add(view);

            // todo will incorporate statistics into a future version of the scheduler
//            Icon chart = VaadinIcon.CHART.create();
//            chart.setSize("14pt");
//            chart.getStyle().set("cursor", "pointer");
//            chart.getElement().setAttribute("title", getTranslation("tooltip.job-statistics", UI.getCurrent().getLocale()));
//            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
//
//            });
//
//            ComponentSecurityVisibility.applySecurity(chart, SecurityConstants.ALL_AUTHORITY,
//                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
//                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);
//
//            layout.add(chart);

//            Icon export = VaadinIcon.DOWNLOAD_ALT.create();
//            export.setSize("14pt");
//            export.getStyle().set("cursor", "pointer");
//            export.getElement().setAttribute("title", "Export");
//            export.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
//
//            });
//
//            ComponentSecurityVisibility.applySecurity(export, SecurityConstants.ALL_AUTHORITY,
//                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
//                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);
//
//            layout.add(export);

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

            ComponentSecurityVisibility.applySecurity(newWindow, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

            return layout;
        }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
            .setFlexGrow(2);

        this.contextInstanceFilteringGrid.addColumn(LitRenderer.<ScheduledContextInstanceRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">${item.startDateTime}</div>")
            .withProperty("startDateTime",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getStartTime())))
            .setHeader(getTranslation("table-header.start-date-time", UI.getCurrent().getLocale()))
            .setKey("startTime")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(3);

        this.contextInstanceFilteringGrid.addColumn(LitRenderer.<ScheduledContextInstanceRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">${item.endDateTime}</div>")
            .withProperty("endDateTime",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getEndTime())))
            .setHeader(getTranslation("table-header.end-date-time", UI.getCurrent().getLocale()))
            .setKey("endTime")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(3);

        this.contextInstanceFilteringGrid.addColumn(LitRenderer.<ScheduledContextInstanceRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">${item.createdDateTime}</div>")
            .withProperty("createdDateTime",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(3);

        this.contextInstanceFilteringGrid.addColumn(LitRenderer.<ScheduledContextInstanceRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">${item.modifiedDateTime}</div>")
            .withProperty("modifiedDateTime",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getModifiedTimestamp())))
            .setHeader(getTranslation("table-header.modified-date-time", UI.getCurrent().getLocale()))
            .setKey("modifiedTimestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(3);

        contextInstanceFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceRecord -> {
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
        HeaderRow hr = contextInstanceFilteringGrid.appendHeaderRow();
        this.contextInstanceFilteringGrid.addGridFiltering(hr, contextInstanceSearchFilter::setContextInstanceId, "componentName");
        this.contextInstanceFilteringGrid.addDateGridFiltering(hr, contextInstanceSearchFilter::setCreatedTimestamp, "timestamp");
        this.contextInstanceFilteringGrid.addDateGridFiltering(hr, contextInstanceSearchFilter::setModifiedTimestamp, "modifiedTimestamp");
        this.contextInstanceFilteringGrid.addDateGridFiltering(hr, contextInstanceSearchFilter::setStartTime, "startTime");
        this.contextInstanceFilteringGrid.addDateGridFiltering(hr, contextInstanceSearchFilter::setEndTime, "endTime");
        this.contextInstanceFilteringGrid.addSelectGridFiltering(hr, contextInstanceSearchFilter::setStatus
            , Arrays.asList(InstanceStatus.values()).stream().map(instanceStatus -> instanceStatus.name()).collect(Collectors.toList()), "status");
        this.contextInstanceFilteringGrid.getElement().getStyle().set("margin-top", "0px");
    }

    private Component createButtonLayout() {
        VerticalLayout buttonWrapper = new VerticalLayout();
        buttonWrapper.setMargin(false);
        buttonWrapper.setPadding(false);
        buttonWrapper.setWidthFull();
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(false);
        buttonLayout.setPadding(false);
        Button refreshButton = this.createRefreshButton();
        buttonLayout.add(refreshButton);

        buttonWrapper.add(buttonLayout);
        buttonWrapper.setHorizontalComponentAlignment(FlexComponent.Alignment.END, buttonLayout);

        return buttonWrapper;
    }

    private Button createRefreshButton() {
        Button refreshJobsButton = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()), VaadinIcon.REFRESH.create());
        refreshJobsButton.setIconAfterText(true);

        refreshJobsButton.addClickListener(event -> this.contextInstanceFilteringGrid.init());

        return refreshJobsButton;
    }
}
