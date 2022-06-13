package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceSearchFilterImpl;
import org.ikasan.scheduled.job.model.JobConstants;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SchedulerJobInstanceGridWidget extends Div {

    private Registration schedulerJobStateChangeRegistration;

    private SchedulerJobInstanceFilteringGrid schedulerJobInstanceFilteringGrid;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private IkasanAuthentication authentication;
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private ContextInstance contextInstance;
    private SystemEventLogger systemEventLogger;

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
        this.contextInstance = contextInstance;
        this.systemEventLogger = systemEventLogger;
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

            Icon skip = IconDecorator.decorate(new Icon(VaadinIcon.BAN), getTranslation("tooltip.skip-job", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            skip.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.skip-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.skip-job", UI.getCurrent().getLocale()));

                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.skipJob(schedulerJobInstanceRecord));
            });

            if(!(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED))) {
                skip.setVisible(true);
            }
            else {
                skip.setVisible(false);
            }

            layout.add(skip);

            Icon enable = IconDecorator.decorate(new Icon(VaadinIcon.PLAY), getTranslation("tooltip.enable-job", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            enable.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.enable-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.enable-job", UI.getCurrent().getLocale()));

                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.enableJob(schedulerJobInstanceRecord));
            });

            if(!schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED)) {
                enable.setVisible(false);
            }

            layout.add(enable);

            Icon hold = IconDecorator.decorate(new Icon(VaadinIcon.HAND), getTranslation("tooltip.hold-job", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            hold.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.hold-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.hold-job", UI.getCurrent().getLocale()));

                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.holdJob(schedulerJobInstanceRecord));
            });

            if(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD) ||
                schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.SKIPPED)) {
                hold.setVisible(false);
            }

            layout.add(hold);

            Icon release = IconDecorator.decorate(new Icon(VaadinIcon.HANDS_UP), getTranslation("tooltip.release-job", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            release.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog-header.release-job", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog-text.release-job", UI.getCurrent().getLocale()));

                confirmDialog.setCancelable(true);

                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent ->
                    this.releaseJob(schedulerJobInstanceRecord));
            });

            if(!schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ON_HOLD)) {
                release.setVisible(false);
            }

            layout.add(release);

            Icon chart = IconDecorator.decorate(new Icon(VaadinIcon.CHART), getTranslation("tooltip.job-statistics", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                UnderConstructionDialog underConstructionDialog = new UnderConstructionDialog();
                underConstructionDialog.open();
            });

            layout.add(chart);

            Icon export = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT), getTranslation("label.download-job", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
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

        this.schedulerJobInstanceFilteringGrid.addItemDoubleClickListener(event -> {
            if(event.getItem().getType().equals(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE)) {
                FileEventJobInstanceDialog fileEventJobDialog = new FileEventJobInstanceDialog(moduleMetaDataService.findById(event.getItem().getSchedulerJobInstance().getAgentName())
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobInstanceService);
                fileEventJobDialog.setJob(event.getItem(), EditMode.READONLY);

                fileEventJobDialog.open();

                fileEventJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobInstanceFilteringGrid.refreshItem(event.getItem());
                    }
                });
            }
            else if(event.getItem().getType().equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE)) {
                QuartzDrivenScheduledJobInstanceDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobInstanceDialog(moduleMetaDataService.findById(event.getItem().getSchedulerJobInstance().getAgentName())
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, this.schedulerJobInstanceService);
                quartzDrivenScheduledJobDialog.setJob(event.getItem(), EditMode.READONLY);

                quartzDrivenScheduledJobDialog.open();

                quartzDrivenScheduledJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobInstanceFilteringGrid.refreshItem(event.getItem());
                    }
                });
            }
            else if(event.getItem().getType().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
                InternalEventDrivenJobInstanceDialog internalEventDrivenJobDialog = new InternalEventDrivenJobInstanceDialog(moduleMetaDataService.findById(event.getItem().getSchedulerJobInstance().getAgentName())
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, this.schedulerJobInstanceService, this.contextInstance);
                internalEventDrivenJobDialog.setJob(event.getItem(), EditMode.READONLY);

                internalEventDrivenJobDialog.open();

                internalEventDrivenJobDialog.addOpenedChangeListener(openedChangeEvent -> {
                    if(!openedChangeEvent.isOpened()) {
                        this.schedulerJobInstanceFilteringGrid.refreshItem(event.getItem());
                    }
                });

            }
        });

    }

    /**
     * Helper method to skip the job.
     *
     * @return
     */
    private boolean skipJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-skipped", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.skipJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier()
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), true);
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.SKIPPED);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Skipped[%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), true)
                , this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.skipped-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to enable the job.
     *
     * @return
     */
    private boolean enableJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-enabled", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.skipJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName(), false);
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Skipped[%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), false)
                , this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.enabled-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to hold the job.
     *
     * @return
     */
    private boolean holdJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-held", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.holdJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName());
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.ON_HOLD);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_HELD, String.format("Agent Name[%s], Scheduled Job Name[%s], Held[%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), true)
                , this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.held-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to release the job.
     *
     * @return
     */
    private boolean releaseJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-released", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.releaseJob(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(), schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName());
            this.updateJobState(schedulerJobInstanceRecord, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_RELEASED, String.format("Agent Name[%s], Scheduled Job Name[%s], Released[%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName(), true)
                , this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.released-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    private void updateJobState(SchedulerJobInstanceRecord schedulerJobInstanceRecord, InstanceStatus newStatus) {
        InstanceStatus previousStatus = schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus();
        SchedulerJobInstance schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();
        schedulerJobInstance.setStatus(newStatus);
        schedulerJobInstanceRecord.setSchedulerJobInstance(schedulerJobInstance);
        updateScheduledJob(schedulerJobInstanceRecord, this.authentication);

        SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
            = new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance,
            this.contextInstance, previousStatus, newStatus);

        SchedulerJobStateChangeEventBroadcaster.broadcast(schedulerJobInstanceStateChangeEvent);
    }

    public void updateScheduledJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord, IkasanAuthentication authentication) {

        schedulerJobInstanceRecord.setModifiedTimestamp(System.currentTimeMillis());
        schedulerJobInstanceRecord.setModifiedBy(authentication.getName());
        schedulerJobInstanceRecord.setStatus(schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().name());

        this.schedulerJobInstanceService.save(schedulerJobInstanceRecord);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        schedulerJobStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(jobInstanceStateChangeEvent -> {
            if (jobInstanceStateChangeEvent.getSchedulerJobInstance() != null) {
                SchedulerJobInstanceSearchFilter filter = new SolrSchedulerJobInstanceSearchFilterImpl();
                filter.setContextInstanceId(jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextInstanceId());
                filter.setJobName(jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName());
                filter.setChildContextName(jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName());

                SearchResults<SchedulerJobInstanceRecord> searchResults = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter
                    (filter, 1, 0, null, null);

                if(searchResults.getResultList().size() == 1) {
                    ui.access(() -> this.schedulerJobInstanceFilteringGrid.refreshItem(searchResults.getResultList().get(0)));
                }
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.schedulerJobStateChangeRegistration.remove();
        this.schedulerJobStateChangeRegistration = null;
    }
}
