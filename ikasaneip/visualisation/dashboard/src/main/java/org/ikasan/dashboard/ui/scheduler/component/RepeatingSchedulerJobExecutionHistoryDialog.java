package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerJobLogFileViewerDialog;
import org.ikasan.job.orchestration.broadcast.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RepeatingSchedulerJobExecutionHistoryDialog extends AbstractCloseableResizableDialog
    implements SchedulerJobStateChangeEventBroadcastListener, ContextInstanceStateChangeEventBroadcastListener {
    private Logger logger = LoggerFactory.getLogger(RepeatingSchedulerJobExecutionHistoryDialog.class);
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ContextInstance contextInstance;
    protected ModuleMetaDataService moduleMetaDataService;
    protected LogStreamingService logStreamingService;

    private Map<Long, ProcessEventHolder> processEventHolderMap;

    private Grid<Map.Entry<Long, ProcessEventHolder>> logFileGrid = new Grid<>();

    private ScheduledContextInstanceAuditAggregateSearchFilter filter
        = new ScheduledContextInstanceAuditAggregateSearchFilter();

    private UI ui;

    private TextField jobNameFilterTf = new TextField();
    private Select<String> select = new Select<>();
    private InstanceStatus filterStatus = null;


    public RepeatingSchedulerJobExecutionHistoryDialog(ScheduledContextInstanceService scheduledContextInstanceService
        , ContextInstance contextInstance, ModuleMetaDataService moduleMetaDataService
        , LogStreamingService logStreamingService) {
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.contextInstance = contextInstance;
        this.moduleMetaDataService = moduleMetaDataService;
        this.logStreamingService = logStreamingService;

        this.init();
    }

    private void init() {
        super.showResize(false);
        super.title.setText(getTranslation("label.log-file-history", UI.getCurrent().getLocale()));

        this.setHeight("90%");
        this.setWidth("90%");
    }

    @Override
    public void open() {
        this.initialiseGrid();
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

    /**
     * Initialises the grid for displaying job statuses and log files.
     * Adds columns to the grid with corresponding renderers.
     * Configures header, key, sortable, resizable properties for each column.
     * Sets size of the grid.
     * Adds filter fields to the header row for specific columns.
     * Sets initial value for status filter if specified.
     */
    private void initialiseGrid() {
        this.logFileGrid.addColumn(new ComponentRenderer<>(logFile -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                Div label = new Div();
                label.getElement().getStyle().set("word-wrap", "normal");
                label.getElement().getStyle().set("white-space", "normal");

                label.setText(logFile.getValue().getEvent().getJobName());
                verticalLayout.add(label);

                return verticalLayout;
            })).setFlexGrow(3)
            .setKey("job-name")
            .setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setSortable(true)
            .setResizable(true);
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

                if(logFile.getValue().getEvent() != null) {
                    label.setText(DateFormatter.instance().getFormattedDate(logFile.getValue().getEvent().getCompletionTime()));
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

            if(logFile.getValue().getEvent() != null && logFile.getValue().getEvent().isSuccessful()) {
                statusDiv.setStatus(InstanceStatus.COMPLETE);
            }
            else if(logFile.getValue().getEvent() != null && !logFile.getValue().getEvent().isSuccessful()) {
                statusDiv.setStatus(InstanceStatus.ERROR);
            }
            else if(logFile.getValue().getEvent() == null) {
                statusDiv.setStatus(InstanceStatus.RUNNING);
            }
            return verticalLayout;
        })).setHeader(getTranslation("table-header.status", UI.getCurrent().getLocale()))
            .setFlexGrow(1)
            .setKey("status")
            .setResizable(true);
        this.logFileGrid.addColumn(new ComponentRenderer<>(logFile -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidth("100%");
                verticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

                Button downloadLogFileButton = new Button(getTranslation("button.log-file"), VaadinIcon.FILE_PROCESS.create());

                verticalLayout.add(downloadLogFileButton);
                verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, downloadLogFileButton);

                downloadLogFileButton.addClickListener(event ->
                    this.streamLog(logFile.getValue().getEvent().getResultOutput()
                        , logFile.getValue().getEvent().getAgentName()));

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

                downloadLogFileButton.addClickListener(event ->
                    this.streamLog(logFile.getValue().getEvent().getResultError()
                        , logFile.getValue().getEvent().getAgentName()));

                return verticalLayout;
            })).setFlexGrow(1).setKey("error-log-file")
            .setResizable(true);

        this.logFileGrid.setSizeFull();

        HeaderRow hr = this.logFileGrid.appendHeaderRow();

        this.addGridFiltering(hr, "job-name"
            , this.jobNameFilterTf, this.filter::setScheduledProcessEventName);
        this.addSelectGridFiltering(hr, this.filter::setStatus,
            List.of(InstanceStatus.COMPLETE.name(), InstanceStatus.ERROR.name()), "status");

        super.content.add(this.logFileGrid);

        if(this.filterStatus != null) {
            this.select.setValue(this.filterStatus.name());
        }
    }

    /**
     * Retrieves the audit records for the current context instance.
     *
     * @return a map of audit records, where the key is the fire time of the process event
     *         and the value is the corresponding process event holder.
     */
    private Map<Long, ProcessEventHolder> getAuditRecords() {

        filter.setContextInstanceId(this.contextInstance.getId());

        Map<Long, ProcessEventHolder> results = new HashMap<>();
        this.scheduledContextInstanceService.findAllAuditRecordsByFilter
            (filter, -1, -1, null, null).getResultList().forEach(scheduledContextInstanceAuditAggregateRecord
            -> {
                // we're only interested in completed events
                if(scheduledContextInstanceAuditAggregateRecord.getScheduledContextInstanceAuditAggregate().getProcessEvent().getCompletionTime() == 0) return;
                if(!results.containsKey(scheduledContextInstanceAuditAggregateRecord
                    .getScheduledContextInstanceAuditAggregate().getProcessEvent().getFireTime())) {
                    ProcessEventHolder processEventHolder = new ProcessEventHolder();
                    processEventHolder.setEvent(scheduledContextInstanceAuditAggregateRecord
                        .getScheduledContextInstanceAuditAggregate().getProcessEvent());
                    results.put(scheduledContextInstanceAuditAggregateRecord
                        .getScheduledContextInstanceAuditAggregate().getProcessEvent().getFireTime(), processEventHolder);
                }
                else {
                    ProcessEventHolder processEventHolder = results.get(scheduledContextInstanceAuditAggregateRecord
                        .getScheduledContextInstanceAuditAggregate().getProcessEvent().getFireTime());

                    processEventHolder.setEvent(scheduledContextInstanceAuditAggregateRecord
                        .getScheduledContextInstanceAuditAggregate().getProcessEvent());
                }

        });

        return results;
    }

    /**
     * Helper method to stream job log files.
     */
    protected void streamLog(String logFilePath, String agentId) {
        boolean displayLog = false;
        String host = null;
        String endPoint = null;
        String outputLog = null;

        ModuleMetaData agent = moduleMetaDataService.findById(agentId);

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

    /**
     * Adds grid filtering functionality to a header row cell.
     *
     * @param hr the header row
     * @param columnKey the key of the column to add filtering to
     * @param textField the text field used for filtering
     * @param setFilter the consumer function to apply the filter
     */
    private void addGridFiltering(HeaderRow hr, String columnKey, TextField textField, Consumer<String> setFilter) {
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setWidthFull();

        textField.addValueChangeListener(ev-> {
            setFilter.accept(textField.getValue());
            this.logFileGrid.getDataProvider().refreshAll();
        });

        hr.getCell(this.logFileGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * Adds grid filtering functionality with a select component to a header row cell.
     *
     * @param hr          the header row
     * @param setFilter   the consumer function to apply the filter
     * @param options     the options for the select component
     * @param columnKey   the key of the column to add filtering to
     */
    public void addSelectGridFiltering(HeaderRow hr, Consumer<String> setFilter, List<String> options, String columnKey) {
        this.select.setItems(options);
        this.select.setWidthFull();
        this.select.setEmptySelectionAllowed(true);
        this.select.setItemLabelGenerator(entry -> {
            if(entry == null) {
                return "";
            }

            return entry;
        });

        this.select.addValueChangeListener(ev-> {

            setFilter.accept(ev.getValue());

            this.logFileGrid.getDataProvider().refreshAll();
        });

        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");

        HorizontalLayout layout = new HorizontalLayout(this.select, filterIcon);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, filterIcon);

        hr.getCell(this.logFileGrid.getColumnByKey(columnKey)).setComponent(layout);
    }

    /**
     * A class representing a holder for a scheduled process event.
     */
    private class ProcessEventHolder {
        private ScheduledProcessEvent event;

        public ScheduledProcessEvent getEvent() {
            return event;
        }

        public void setEvent(ScheduledProcessEvent event) {
            this.event = event;
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();

        SchedulerJobStateChangeEventBroadcaster.register(this);
        ContextInstanceStateChangeEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.ui = null;

        SchedulerJobStateChangeEventBroadcaster.unregister(this);
        ContextInstanceStateChangeEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent event) {
        if(event.getSchedulerJobInstance().getContextInstanceId().equals(this.contextInstance.getId())) {
            ui.access(() -> this.logFileGrid.getDataProvider().refreshAll());
        }
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        if(event.getContextInstanceId().equals(this.contextInstance.getId())) {
            ui.access(() -> this.logFileGrid.getDataProvider().refreshAll());
        }
    }

    /**
     * Set the filter status for the scheduler job execution history dialog.
     *
     * @param filterStatus the filter status to set
     */
    public void setFilterStatus(InstanceStatus filterStatus) {
        this.filterStatus = filterStatus;
    }
}
