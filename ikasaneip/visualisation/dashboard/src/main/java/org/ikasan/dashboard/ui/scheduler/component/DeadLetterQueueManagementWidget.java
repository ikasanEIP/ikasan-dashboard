package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.spec.scheduled.event.service.ContextInstanceDlqEventLocalBroadcastListener;
import org.ikasan.job.orchestration.broadcast.ContextInstanceDlqEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.rest.dashboard.model.scheduled.ScheduledProcessEventImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DeadLetterQueueManagementWidget extends VerticalLayout implements ContextInstanceDlqEventLocalBroadcastListener {
    private Logger logger = LoggerFactory.getLogger(DeadLetterQueueManagementWidget.class);

    protected Grid<BigQueueMessage> bigQueueMessageGrid = new Grid<>();
    private ConfigurableFilterDataProvider<BigQueueMessage, Void, BigQueueFilter> filteredDataProvider;
    private final ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private final ContextInstance contextInstance;
    private final SystemEventLogger systemEventLogger;
    private final BigQueueFilter bigQueueFilter = new BigQueueFilter();

    /**
     * Constructor for DeadLetterQueueManagementWidget class.
     *
     * @param contextInstance the context instance for the widget
     * @param systemEventLogger the system event logger
     */
    public DeadLetterQueueManagementWidget(ContextInstance contextInstance, SystemEventLogger systemEventLogger) {
        this.contextInstance = contextInstance;
        if(this.contextInstance == null) {
            throw new IllegalArgumentException("contextInstance cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }

        if(ContextMachineCache.instance().getByContextInstanceId(contextInstance.getId()) != null) {
            this.initialiseButtons();
            this.initialiseGrid();
            this.initDataProvider();
        }
        else {
            VerticalLayout noDLQManagementLayout = new VerticalLayout();
            noDLQManagementLayout.setWidthFull();
            Span message = new Span(getTranslation("label,no-dlq-management"));
            noDLQManagementLayout.add(message);
            noDLQManagementLayout.setHorizontalComponentAlignment(Alignment.CENTER, message);
            this.add(noDLQManagementLayout);
        }
        this.setSizeFull();
    }

    /**
     * Method to initialise buttons for resubmitting, deleting, and refreshing messages in the Dead Letter Queue.
     * Each button is configured with security visibility based on the user's granted authorities.
     * Resubmit All button prompts the user to confirm resubmitting all DLQ messages. Upon confirmation,
     * it starts a new thread to handle the resubmission logic.
     * Delete All button prompts the user to confirm deleting all DLQ messages. Upon confirmation,
     * it starts a new thread to handle the deletion logic.
     * Refresh Grid button simply refreshes the display of messages in the grid.
     */
    private void initialiseButtons() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        Button resubmitAllButton = new Button(getTranslation("button.resubmit-all"));
        resubmitAllButton.getElement().setAttribute("title"
            , getTranslation("tooltip.resubmit-all-from-dlq", UI.getCurrent().getLocale()));

        ComponentSecurityVisibility.applySecurity(resubmitAllButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        resubmitAllButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.resubmit-all-dlq-messages-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.resubmit-all-dlq-messages-text", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);

                progressIndicatorDialog.open(getTranslation("message.resubmitting-all-dlq-messages-header")
                    , getTranslation("message.resubmitting-all-dlq-messages-test"));

                Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("DeadLetterQueueManagement"));
                executor.execute(() -> {
                    boolean error = false;
                    try {
                        int counter = 0;
                        ContextMachine contextMachine = ContextMachineCache.instance()
                            .getByContextInstanceId(contextInstance.getId());
                        for (BigQueueMessage bigQueueMessage : contextMachine.getDlqMessages()) {
                            contextMachine.resubmitMessageFromDeadLetterQueue(bigQueueMessage.getMessageId());
                            counter++;
                        }
                        ContextInstanceDlqEventBroadcaster.broadcast(this.contextInstance);
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_ALL_DLQ_MESSAGES_RESUBMITTED
                            , String.format("Resubmitted [%s] DLQ messages for job plan [%s] with instance id [%s]"
                                , counter, this.contextInstance.getName(), this.contextInstance.getId())
                            , authentication.getName());
                    } catch (Exception e) {
                        error = true;
                        if(this.getUI().isPresent()) {
                            this.getUI().get().access(() -> {
                                this.bigQueueMessageGrid.getDataProvider().refreshAll();
                                progressIndicatorDialog.close();
                                NotificationHelper.showErrorNotification((String.format(getTranslation
                                    ("error.unable-to-resubmit-all-dlq-messages"), e.getMessage())));
                            });
                        }
                    }

                    if (!error) {
                        if(this.getUI().isPresent()) {
                            this.getUI().get().access(() -> {
                                this.bigQueueMessageGrid.getDataProvider().refreshAll();
                                progressIndicatorDialog.close();
                                NotificationHelper.showUserNotification(String.format(getTranslation
                                    ("message.successfully-resubmitted-all-dlq-messages")));
                            });
                        }
                    }
                });
            });
        });

        Button deleteAllButton = new Button(getTranslation("button.delete-all"));
        deleteAllButton.getElement().setAttribute("title"
            , getTranslation("tooltip.delete-all-from-dlq", UI.getCurrent().getLocale()));

        ComponentSecurityVisibility.applySecurity(deleteAllButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        deleteAllButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.delete-all-dlq-messages-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.delete-all-dlq-messages-text", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);

                progressIndicatorDialog.open(getTranslation("message.deleting-all-dlq-messages-header")
                    , getTranslation("message.deleting-all-dlq-messages-test"));

                Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("DeadLetterQueueManagement"));
                executor.execute(() -> {
                    boolean error = false;
                    try {
                        ContextMachine contextMachine = ContextMachineCache.instance()
                            .getByContextInstanceId(contextInstance.getId());
                        contextMachine.deleteAllDlqMessages();
                        ContextInstanceDlqEventBroadcaster.broadcast(this.contextInstance);
                        this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_ALL_DLQ_MESSAGES_DELETED
                            , String.format("Deleted all DLQ messages for job plan [%s] with instance id [%s]"
                                , this.contextInstance.getName(), this.contextInstance.getId())
                            , authentication.getName());
                    } catch (Exception e) {
                        error = true;
                        if(this.getUI().isPresent()) {
                            this.getUI().get().access(() -> {
                                this.bigQueueMessageGrid.getDataProvider().refreshAll();
                                progressIndicatorDialog.close();
                                NotificationHelper.showErrorNotification((String.format(getTranslation
                                    ("error.unable-to-delete-all-dlq-messages", e.getMessage()))));
                            });
                        }
                    }

                    if (!error) {
                        if(this.getUI().isPresent()) {
                            this.getUI().get().access(() -> {
                                this.bigQueueMessageGrid.getDataProvider().refreshAll();
                                progressIndicatorDialog.close();
                                NotificationHelper.showUserNotification(String.format(getTranslation
                                    ("message.successfully-deleted-all-dlq-messages")));
                            });
                        }
                    }
                });
            });
        });

        Button refreshGridButton = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()), VaadinIcon.REFRESH.create());
        refreshGridButton.setIconAfterText(true);

        refreshGridButton.addClickListener(event -> this.bigQueueMessageGrid.getDataProvider().refreshAll());

        buttonLayout.add(resubmitAllButton, deleteAllButton, refreshGridButton);

        this.add(buttonLayout);
        this.setHorizontalComponentAlignment(Alignment.END, buttonLayout);
    }

    /**
     * Method to initialise the grid for displaying big queue messages.
     * The grid consists of columns for message id, message payload, timestamp, and action buttons for resubmit and delete.
     * A header row is added to the grid for filtering purposes.
     */
    private void initialiseGrid() {
        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.bigQueueMessageGrid.setSizeFull();
        this.bigQueueMessageGrid.addColumn(new ComponentRenderer<>(bigQueueMessage -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                Div label = new Div();
                label.getElement().getStyle().set("word-wrap", "normal");
                label.getElement().getStyle().set("white-space", "normal");

                verticalLayout.add(label);

                label.setText(bigQueueMessage.getMessageId());

                return verticalLayout;
            })).setFlexGrow(3)
            .setKey("messageId")
            .setHeader(getTranslation("table-header.message-id"))
            .setSortable(true)
            .setResizable(true);
        this.bigQueueMessageGrid.addColumn(new ComponentRenderer<>(bigQueueMessage -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setSizeFull();

                AceEditor messagePayload = new AceEditor();
                messagePayload.setTheme(AceTheme.dracula);
                messagePayload.setMode(AceMode.json);
                messagePayload.setFontSize(11);
                messagePayload.setTabSize(4);
                messagePayload.setReadOnly(true);
                messagePayload.setWidthFull();
                messagePayload.setHeight("700px");

                try {
                    messagePayload.setValue(objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(objectMapper.readValue(bigQueueMessage.getMessage().toString()
                            , ScheduledProcessEventImpl.class)));
                }
                catch (JsonProcessingException e) {
                    logger.info("Could not resolve bigQueueMessage - ", e.getMessage());
                }


                Details details = new Details(getTranslation("label.expand-to-view-message-payload"), messagePayload);
                details.add(messagePayload);
                details.setWidthFull();
                details.setOpened(false);

                verticalLayout.add(details);

                return verticalLayout;
            })).setFlexGrow(10)
            .setKey("messagePayload")
            .setHeader(getTranslation("table-header.message-payload"))
            .setResizable(true);
        this.bigQueueMessageGrid.addColumn(new ComponentRenderer<>(bigQueueMessage -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                Div label = new Div();
                label.getElement().getStyle().set("word-wrap", "normal");
                label.getElement().getStyle().set("white-space", "normal");

                verticalLayout.add(label);

                try {
                    ScheduledProcessEvent scheduledProcessEvent
                        = objectMapper.readValue(bigQueueMessage.getMessage().toString(), ScheduledProcessEventImpl.class);
                    label.setText(DateFormatter.instance()
                        .getFormattedDate(scheduledProcessEvent.getFireTime()));
                }
                catch (JsonProcessingException e) {
                    logger.info("Could not resolve bigQueueMessage - ", e.getMessage());
                }

                return verticalLayout;
            })).setFlexGrow(2)
            .setHeader(getTranslation("table-header.timestamp"))
            .setSortable(true)
            .setKey("timestamp")
            .setResizable(true);
        this.bigQueueMessageGrid.addColumn(new ComponentRenderer<>(bigQueueMessage -> {
            HorizontalLayout buttonLayout = new HorizontalLayout();

            Button resubmitButton = new Button(getTranslation("button.resubmit"));
            resubmitButton.getElement().setAttribute("title", getTranslation("tooltip.resubmit-from-dlq", UI.getCurrent().getLocale()));

            ComponentSecurityVisibility.applySecurity(authentication, resubmitButton, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

            resubmitButton.addClickListener(event -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.resubmit-dlq-messages-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.resubmit-dlq-messages-text", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);

                    progressIndicatorDialog.open(getTranslation("message.resubmitting-dlq-messages-header")
                        , getTranslation("message.resubmitting-dlq-messages-test"));

                    Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("DeadLetterQueueManagement"));
                    executor.execute(() -> {
                        boolean error = false;
                        try {
                            ContextMachine contextMachine = ContextMachineCache.instance()
                                .getByContextInstanceId(contextInstance.getId());
                            contextMachine.resubmitMessageFromDeadLetterQueue(bigQueueMessage.getMessageId());
                            ContextInstanceDlqEventBroadcaster.broadcast(this.contextInstance);
                            this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_DLQ_MESSAGE_RESUBMITTED
                                , String.format("Resubmitted DLQ message[%s], with content[%s] for job plan [%s] with instance id [%s]"
                                    , bigQueueMessage.getMessageId(), bigQueueMessage.getMessage(), this.contextInstance.getName()
                                    , this.contextInstance.getId())
                                , authentication.getName());
                        } catch (Exception e) {
                            error = true;
                            if(this.getUI().isPresent()) {
                                this.getUI().get().access(() -> {
                                    this.bigQueueMessageGrid.getDataProvider().refreshAll();
                                    progressIndicatorDialog.close();
                                    NotificationHelper.showErrorNotification((String.format(getTranslation
                                        ("error.unable-to-resubmit-dlq-messages", e.getMessage()))));
                                });
                            }
                        }

                        if (!error) {
                            if(this.getUI().isPresent()) {
                                this.getUI().get().access(() -> {
                                    this.bigQueueMessageGrid.getDataProvider().refreshAll();
                                    progressIndicatorDialog.close();
                                    NotificationHelper.showUserNotification(String.format(getTranslation
                                        ("message.successfully-resubmitted-dlq-messages")));
                                });
                            }
                        }
                    });
                });
            });

            Button deleteButton = new Button(getTranslation("button.delete"));
            deleteButton.getElement().setAttribute("title", getTranslation("tooltip.delete-from-dlq", UI.getCurrent().getLocale()));

            ComponentSecurityVisibility.applySecurity(authentication, deleteButton, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

            deleteButton.addClickListener(event -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.delete-dlq-messages-header", UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.delete-dlq-messages-text", UI.getCurrent().getLocale()));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);
                confirmDialog.open();

                confirmDialog.addConfirmListener(confirmEvent -> {
                    ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);

                    progressIndicatorDialog.open(getTranslation("message.deleting-dlq-messages-header")
                        , getTranslation("message.deleting-dlq-messages-test"));

                    Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("DeadLetterQueueManagement"));
                    executor.execute(() -> {
                        boolean error = false;
                        try {
                            ContextMachine contextMachine = ContextMachineCache.instance()
                                .getByContextInstanceId(contextInstance.getId());
                            contextMachine.deleteDlqMessage(bigQueueMessage.getMessageId());
                            ContextInstanceDlqEventBroadcaster.broadcast(this.contextInstance);
                            this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_INSTANCE_DLQ_MESSAGE_DELETED
                                , String.format("Deleted DLQ message[%s], with content[%s] for job plan [%s] with instance id [%s]"
                                    , bigQueueMessage.getMessageId(), bigQueueMessage.getMessage(), this.contextInstance.getName()
                                    , this.contextInstance.getId())
                                , authentication.getName());
                        } catch (Exception e) {
                            error = true;
                            if(this.getUI().isPresent()) {
                                this.getUI().get().access(() -> {
                                    this.bigQueueMessageGrid.getDataProvider().refreshAll();
                                    progressIndicatorDialog.close();
                                    NotificationHelper.showErrorNotification((String.format(getTranslation
                                        ("error.unable-to-delete-dlq-messages", e.getMessage()))));
                                });
                            }
                        }

                        if (!error) {
                            if(this.getUI().isPresent()) {
                                this.getUI().get().access(() -> {
                                    this.bigQueueMessageGrid.getDataProvider().refreshAll();
                                    progressIndicatorDialog.close();
                                    NotificationHelper.showUserNotification(String.format(getTranslation
                                        ("message.successfully-deleted-dlq-messages")));
                                });
                            }
                        }
                    });
                });
            });
            buttonLayout.add(resubmitButton, deleteButton);

            return buttonLayout;
        })).setFlexGrow(1)
        .setKey("buttons")
        .setResizable(true);

        HeaderRow hr = this.bigQueueMessageGrid.appendHeaderRow();
        this.bigQueueMessageGrid.setSizeFull();
        this.addGridFiltering(hr, this.bigQueueFilter::setMessageId, "messageId");
        this.addGridFiltering(hr, this.bigQueueFilter::setMessageContent, "messagePayload");
        this.add(this.bigQueueMessageGrid);
    }

    /**
     * Initializes the data provider for the DeadLetterQueueManagementWidget.
     * The data provider is created based on filtering callbacks for loading and counting messages.
     * It retrieves messages based on the filter, offset, limit, and sorting parameters provided in the query.
     * The method sets up the filter and assigns the data provider to the bigQueueMessageGrid.
     */
    public void initDataProvider() {
        DataProvider<BigQueueMessage, BigQueueFilter> dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<BigQueueFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            List<BigQueueMessage> results;

            if(query.getSortOrders().size() > 0) {
                results = this.getResults(filter.get(), offset, limit, query.getSortOrders().get(0).getSorted(),
                    query.getSortOrders().get(0).getDirection().name());
            }
            else {
                results = this.getResults(filter.get(), offset, limit, null, null);
            }

            return results.stream();
        }, query -> {
            Optional<BigQueueFilter> filter = query.getFilter();

            List<BigQueueMessage> results = this.getResults(filter.get(), -1, -1, null, null);

            return results.size();
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.bigQueueFilter);

        this.bigQueueMessageGrid.setDataProvider(filteredDataProvider);
    }

    private List<BigQueueMessage> getResults(BigQueueFilter filter, int offset, int limit, String sortField, String sortDirection) {
        try {
            ContextMachine contextMachine = ContextMachineCache.instance()
                .getByContextInstanceId(contextInstance.getId());
            List<BigQueueMessage> results = contextMachine.getDlqMessages();

            if(filter.getMessageId() != null && !filter.getMessageId().isEmpty()) {
                results = results.stream()
                    .filter(bigQueueMessage -> bigQueueMessage.getMessageId().contains(filter.getMessageId()))
                    .collect(Collectors.toList());
            }

            if(filter.getMessageContent() != null && !filter.getMessageContent().isEmpty()) {
                results = results.stream()
                    .filter(bigQueueMessage -> ((String)bigQueueMessage.getMessage())
                        .contains(filter.getMessageContent()))
                    .collect(Collectors.toList());
            }

            if (limit == -1 && offset == -1) {
                return results;
            }
            else if (offset + limit < results.size()) {
                return results.subList(offset, offset+limit);
            }
            else {
                return results.subList(offset, results.size());
            }
        }
        catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Adds grid filtering functionality to the header row of a grid column.
     *
     * @param hr The header row where the filtering component will be added.
     * @param setFilter A Consumer function that accepts a String filter value.
     * @param columnKey The key of the column to which the filtering will be applied.
     */
    public void addGridFiltering(HeaderRow hr, Consumer<String> setFilter, String columnKey)
    {
        TextField textField = new TextField();
        textField.setWidthFull();

        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            if(filteredDataProvider != null) {
                filteredDataProvider.refreshAll();
            }
        });

        hr.getCell(this.bigQueueMessageGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * A private class representing a filter for big queue messages in the context of DeadLetterQueueManagementWidget.
     */
    private class BigQueueFilter {
        private String messageId;
        private String messageContent;

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getMessageContent() {
            return messageContent;
        }

        public void setMessageContent(String messageContent) {
            this.messageContent = messageContent;
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        ContextInstanceDlqEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        ContextInstanceDlqEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveBroadcast(ContextInstance contextInstance) {
        if(this.contextInstance != null && contextInstance != null
            && this.contextInstance.getId().equals(contextInstance.getId())) {
            if (this.getUI().isPresent() && this.getUI().get().isAttached()) {
                this.getUI().get().access(() -> {
                    this.bigQueueMessageGrid.getDataProvider().refreshAll();
                });
            }
        }
    }
}
