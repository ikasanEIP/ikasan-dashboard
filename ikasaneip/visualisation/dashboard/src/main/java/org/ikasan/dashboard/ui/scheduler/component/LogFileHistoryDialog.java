package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerJobLogFileViewerDialog;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class LogFileHistoryDialog extends AbstractCloseableResizableDialog implements SchedulerJobStateChangeEventLocalBroadcastListener {
    private Logger logger = LoggerFactory.getLogger(LogFileHistoryDialog.class);
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ContextInstance contextInstance;
    private SchedulerJobInstance schedulerJobInstance;
    protected ModuleMetaDataService moduleMetaDataService;
    protected LogStreamingService logStreamingService;

    private Map<Long, ProcessEventHolder> processEventHolderMap;

    private Grid<Map.Entry<Long, ProcessEventHolder>> logFileGrid = new Grid<>();

    private ScheduledContextInstanceAuditAggregateSearchFilter filter
        = new ScheduledContextInstanceAuditAggregateSearchFilter();

    private UI ui;
    private Binder<SchedulerJobInstance> formBinder;

    private TextField jobNameTf;
    private TextField agentTf;

    public LogFileHistoryDialog(ScheduledContextInstanceService scheduledContextInstanceService
        , ContextInstance contextInstance, SchedulerJobInstance schedulerJobInstance
        , ModuleMetaDataService moduleMetaDataService, LogStreamingService logStreamingService) {
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.contextInstance = contextInstance;
        this.schedulerJobInstance = schedulerJobInstance;
        this.moduleMetaDataService = moduleMetaDataService;
        this.logStreamingService = logStreamingService;

        this.init();
        this.initialiseGrid();
    }

    private void init() {
        super.showResize(false);
        super.title.setText(getTranslation("label.log-file-history", UI.getCurrent().getLocale()));
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.setHeight("700px");
        this.setWidth("1400px");

        FormLayout formLayout = new FormLayout();
        this.formBinder = new Binder<>();

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setId("jobNameTf");
        this.jobNameTf.setEnabled(false);
        formBinder.forField(this.jobNameTf)
            .bind(SchedulerJobInstance::getJobName, SchedulerJobInstance::setJobName);
        formLayout.add(jobNameTf);

        this.agentTf = new TextField(getTranslation("label.agent", UI.getCurrent().getLocale()));
        this.agentTf.setId("agentCb");
        this.agentTf.setClearButtonVisible(true);
        this.agentTf.setEnabled(false);

        formBinder.forField(this.agentTf)
            .bind(SchedulerJobInstance::getAgentName, SchedulerJobInstance::setAgentName);
        formLayout.add(agentTf);

        this.formBinder.readBean(this.schedulerJobInstance);
        this.jobNameTf.setReadOnly(true);
        this.agentTf.setReadOnly(true);

        formLayout.getElement().getStyle().set("margin-bottom", "40px");

        this.content.add(formLayout);
    }

    @Override
    public void open() {
        // Attempt to get the list of log files. Only modules Ikasan 3.3+ is supported
        try {
            processEventHolderMap = this.getAuditRecords();
        } catch (Exception e) {
            logger.warn("There was an issue to get the list of logs files for the job and context instance: {}", e.getMessage());
        }

        if (processEventHolderMap == null || processEventHolderMap.isEmpty()) {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("header.no-job-execution-history", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("paragraph.no-job-execution-history", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(false);
            confirmDialog.open();
        } else{
            DataProvider<Map.Entry<Long, ProcessEventHolder>, ScheduledContextInstanceAuditAggregateSearchFilter> dataProvider =
                DataProvider.fromFilteringCallbacks(
                    // First callback fetches items based on a query
                    query -> {
                        this.processEventHolderMap = this.getAuditRecords();
                        // The index of the first item to load
                        int offset = query.getOffset();

                        // The number of items to load
                        int limit = query.getLimit();

                        if(query.getSortOrders().size() > 0) {
                            if(query.getSortOrders().get(0).getDirection().equals(SortDirection.DESCENDING)) {
                                Map<Long, ProcessEventHolder> sortedTreeMap = new TreeMap<>(this.processEventHolderMap);
                                return sortedTreeMap.entrySet().stream().skip(offset).limit(limit);
                            }
                            else if(query.getSortOrders().get(0).getDirection().equals(SortDirection.ASCENDING)) {
                                Map<Long, ProcessEventHolder> sortedTreeMap = new TreeMap<>(Comparator.reverseOrder());
                                sortedTreeMap.putAll(this.processEventHolderMap);
                                return sortedTreeMap.entrySet().stream().skip(offset).limit(limit);
                            }
                            else {
                                return this.processEventHolderMap.entrySet().stream().skip(offset).limit(limit);
                            }
                        }

                        Map<Long, ProcessEventHolder> sortedTreeMap = new TreeMap<>(Comparator.reverseOrder());
                        sortedTreeMap.putAll(this.processEventHolderMap);
                        return sortedTreeMap.entrySet().stream().skip(offset).limit(limit);
                    },
                    // Second callback fetches the total number of items currently in the Grid.
                    // The grid can then use it to properly adjust the scrollbars.
                    query -> {
                        this.processEventHolderMap = this.getAuditRecords();
                        return processEventHolderMap.entrySet().stream().collect(Collectors.toList()).size();
                    });

            dataProvider.withConfigurableFilter().setFilter(this.filter);

            this.logFileGrid.setDataProvider(dataProvider);
            this.logFileGrid.getDataProvider().refreshAll();
            super.open();
        }
    }

    private void initialiseGrid() {
        this.logFileGrid.addColumn(new ComponentRenderer<>(logFile -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                Div label = new Div();
                label.getElement().getStyle().set("word-wrap", "normal");
                label.getElement().getStyle().set("white-space", "normal");

                verticalLayout.add(label);

                label.setText(DateFormatter.instance().getFormattedDate(logFile.getKey()));

                return verticalLayout;
            })).setFlexGrow(3)
            .setKey("fire-time")
            .setHeader(getTranslation("table-header.fire-time", UI.getCurrent().getLocale()))
            .setSortable(true)
            .setResizable(true);
        this.logFileGrid.addColumn(new ComponentRenderer<>(logFile -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                Div label = new Div();
                label.getElement().getStyle().set("word-wrap", "normal");
                label.getElement().getStyle().set("white-space", "normal");

                verticalLayout.add(label);

                if(logFile.getValue().getEndEvent() != null) {
                    label.setText(DateFormatter.instance().getFormattedDate(logFile.getValue().getEndEvent().getCompletionTime()));
                }

                return verticalLayout;
            })).setFlexGrow(3)
            .setHeader(getTranslation("table-header.completion-time", UI.getCurrent().getLocale()))
            .setResizable(true);
        this.logFileGrid.addColumn(new ComponentRenderer<>(logFile -> {
            VerticalLayout verticalLayout = new VerticalLayout();
            verticalLayout.setWidthFull();
            verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
            SchedulerStatusDiv statusDiv = new SchedulerStatusDiv();
            statusDiv.setWidthFull();
            statusDiv.getElement().getStyle().set("font-size", "10pt");
            statusDiv.getElement().getStyle().set("margin-top", "20px");
            statusDiv.getElement().getStyle().set("margin-bottom", "1px");

            verticalLayout.add(statusDiv);
            verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, statusDiv);

            if(logFile.getValue().getEndEvent() != null && logFile.getValue().getEndEvent().isSuccessful()) {
                statusDiv.setStatus(InstanceStatus.COMPLETE);
            }
            else if(logFile.getValue().getEndEvent() != null && !logFile.getValue().getEndEvent().isSuccessful()) {
                statusDiv.setStatus(InstanceStatus.ERROR);
            }
            else if(logFile.getValue().getEndEvent() == null) {
                statusDiv.setStatus(InstanceStatus.RUNNING);
            }
            return verticalLayout;
        })).setHeader(getTranslation("table-header.status", UI.getCurrent().getLocale())).setFlexGrow(1).setResizable(true);
        this.logFileGrid.addColumn(new ComponentRenderer<>(logFile -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidth("100%");
                verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

                Button downloadLogFileButton = new Button(getTranslation("button.log-file"), VaadinIcon.FILE_PROCESS.create());

                verticalLayout.add(downloadLogFileButton);
                verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, downloadLogFileButton);

                downloadLogFileButton.addClickListener(event -> {
                    if(logFile.getValue().getStartEvent() != null) {
                        this.streamLog(logFile.getValue().getStartEvent().getResultOutput());
                    }
                    if(logFile.getValue().getEndEvent() != null) {
                        this.streamLog(logFile.getValue().getEndEvent().getResultOutput());
                    }
                });
                return verticalLayout;
            })).setFlexGrow(1).setKey("log-file")
            .setResizable(true);
        this.logFileGrid.addColumn(new ComponentRenderer<>(logFile -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidth("100%");
                verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

                Button downloadLogFileButton = new Button(getTranslation("button.error-log-file"), VaadinIcon.FILE_REMOVE.create());

                verticalLayout.add(downloadLogFileButton);
                verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, downloadLogFileButton);

                downloadLogFileButton.addClickListener(event -> {
                    if(logFile.getValue().getStartEvent() != null) {
                        this.streamLog(logFile.getValue().getStartEvent().getResultError());
                    }
                    if(logFile.getValue().getEndEvent() != null) {
                        this.streamLog(logFile.getValue().getEndEvent().getResultError());
                    }
                });

                return verticalLayout;
            })).setFlexGrow(1).setKey("error-log-file")
            .setResizable(true);

        this.logFileGrid.setSizeFull();

        super.content.add(this.logFileGrid);
    }
    private Map<Long, ProcessEventHolder> getAuditRecords() {
        ScheduledContextInstanceAuditAggregateSearchFilter filter
            = new ScheduledContextInstanceAuditAggregateSearchFilter();

        filter.setContextInstanceId(this.contextInstance.getId());
        filter.setScheduledProcessEventName(this.schedulerJobInstance.getJobName());

        Map<Long, ProcessEventHolder> results = new HashMap<>();
        this.scheduledContextInstanceService.findAllAuditRecordsByFilter
            (filter, -1, -1, null, null).getResultList().forEach(scheduledContextInstanceAuditAggregateRecord
            -> {
                // we do not display incomplete jobs
                if(scheduledContextInstanceAuditAggregateRecord.getScheduledContextInstanceAuditAggregate()
                    .getProcessEvent().getCompletionTime() == 0) return;

                // we are only interested in events associated with the job
                if(!scheduledContextInstanceAuditAggregateRecord.getScheduledContextInstanceAuditAggregate()
                    .getProcessEvent().getJobName().equals(this.schedulerJobInstance.getJobName())) {
                    return;
                }

                if(!results.containsKey(scheduledContextInstanceAuditAggregateRecord
                    .getScheduledContextInstanceAuditAggregate().getProcessEvent().getFireTime())) {
                    if(scheduledContextInstanceAuditAggregateRecord
                        .getScheduledContextInstanceAuditAggregate().getProcessEvent().isJobStarting()) {
                        ProcessEventHolder processEventHolder = new ProcessEventHolder();
                        processEventHolder.setStartEvent(scheduledContextInstanceAuditAggregateRecord
                            .getScheduledContextInstanceAuditAggregate().getProcessEvent());
                        results.put(scheduledContextInstanceAuditAggregateRecord
                            .getScheduledContextInstanceAuditAggregate().getProcessEvent().getFireTime(), processEventHolder);
                    }
                    else {
                        ProcessEventHolder processEventHolder = new ProcessEventHolder();
                        processEventHolder.setEndEvent(scheduledContextInstanceAuditAggregateRecord
                            .getScheduledContextInstanceAuditAggregate().getProcessEvent());
                        results.put(scheduledContextInstanceAuditAggregateRecord
                            .getScheduledContextInstanceAuditAggregate().getProcessEvent().getFireTime(), processEventHolder);
                    }
                }
                else {
                    ProcessEventHolder processEventHolder = results.get(scheduledContextInstanceAuditAggregateRecord
                        .getScheduledContextInstanceAuditAggregate().getProcessEvent().getFireTime());

                    if(scheduledContextInstanceAuditAggregateRecord
                        .getScheduledContextInstanceAuditAggregate().getProcessEvent().isJobStarting()) {
                        processEventHolder.setStartEvent(scheduledContextInstanceAuditAggregateRecord
                            .getScheduledContextInstanceAuditAggregate().getProcessEvent());
                    }
                    else {
                        processEventHolder.setEndEvent(scheduledContextInstanceAuditAggregateRecord
                            .getScheduledContextInstanceAuditAggregate().getProcessEvent());
                    }
                }

        });

        return results;
    }

    /**
     * Helper method to stream job log files.
     */
    protected void streamLog(String logFilePath) {
        boolean displayLog = false;
        String host = null;
        String endPoint = null;
        String outputLog = null;

        ModuleMetaData agent = moduleMetaDataService.findById(this.schedulerJobInstance.getAgentName());

        if (logFilePath != null && agent != null) {
            host = agent.getUrl();
            endPoint = "/rest/logs";
            outputLog = logFilePath;
            logger.info(String.format("Streaming log for host %s, endPoint %s, log %s", host, endPoint, outputLog));
            if (outputLog != null && host != null) {
                displayLog = true;
            }
        }

        if (displayLog) {
            SchedulerJobLogFileViewerDialog schedulerJobLogFileViewerDialog
                = new SchedulerJobLogFileViewerDialog(this.logStreamingService, host, endPoint, outputLog);
            schedulerJobLogFileViewerDialog.open();
        } else {
            String message = "Unable to open log file!";
            NotificationHelper.showUserNotification(message);
        }
    }

    private class ProcessEventHolder {
        private ScheduledProcessEvent startEvent;
        private ScheduledProcessEvent endEvent;

        public ScheduledProcessEvent getStartEvent() {
            return startEvent;
        }

        public void setStartEvent(ScheduledProcessEvent startEvent) {
            this.startEvent = startEvent;
        }

        public ScheduledProcessEvent getEndEvent() {
            return endEvent;
        }

        public void setEndEvent(ScheduledProcessEvent endEvent) {
            this.endEvent = endEvent;
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();

        SchedulerJobStateChangeEventBroadcaster.instance().register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        logger.debug("Detaching ContextInstanceTreeView");
        this.ui = null;

        SchedulerJobStateChangeEventBroadcaster.instance().unregister(this);

        logger.debug("Finished detaching ContextInstanceTreeView");
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent event) {
        if(event.getSchedulerJobInstance().getJobName().equals(this.schedulerJobInstance.getJobName()) &&
            event.getSchedulerJobInstance().getContextInstanceId().equals(this.schedulerJobInstance.getContextInstanceId())) {
            ui.access(() -> this.logFileGrid.getDataProvider().refreshAll());
        }
    }
}
