package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerJobLogFileViewerDialog;
import org.ikasan.designer.PositionedDialog;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorAcknowledgedPositionedDialog extends PositionedDialog {
    private Logger logger = LoggerFactory.getLogger(ErrorAcknowledgedPositionedDialog.class);

    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;
    private ContextInstance contextInstance;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ModuleMetaDataService moduleMetaDataService;
    private LogStreamingService logStreamingService;
    private SchedulerJobInstanceService schedulerJobInstanceService;

    /**
     * Represents a dialog used to acknowledge errors with position information.
     *
     * @param schedulerJobInstanceRecord The SchedulerJobInstanceRecord object associated with the error.
     * @param contextInstance The ContextInstance object associated with the error.
     * @param scheduledContextInstanceService The ScheduledContextInstanceService object.
     * @param moduleMetaDataService The ModuleMetaDataService object.
     * @param logStreamingService The LogStreamingService object.
     * @param schedulerJobInstanceService The SchedulerJobInstanceService object.
     */
    public ErrorAcknowledgedPositionedDialog(SchedulerJobInstanceRecord schedulerJobInstanceRecord,
                                             ContextInstance contextInstance,
                                             ScheduledContextInstanceService scheduledContextInstanceService,
                                             ModuleMetaDataService moduleMetaDataService,
                                             LogStreamingService logStreamingService,
                                             SchedulerJobInstanceService schedulerJobInstanceService) {
        super(600, 500);
        this.schedulerJobInstanceRecord = schedulerJobInstanceRecord;
        this.contextInstance = contextInstance;
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.logStreamingService = logStreamingService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;

        this.init();
    }

    /**
     * Initializes the ErrorAcknowledgedPositionedDialog.
     */
    private void init() {
        HorizontalLayout buttonLayout = new HorizontalLayout();

        Icon logFile = IconDecorator.decorate(new Icon(VaadinIcon.FILE_PROCESS), getTranslation("tooltip.view-log-file"
            , UI.getCurrent().getLocale()), "14pt", IkasanColours.IKASAN_ORANGE);
        logFile.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.streamLog(schedulerJobInstanceRecord, false);
        });

        Icon errorLogFile = IconDecorator.decorate(new Icon(VaadinIcon.FILE_REMOVE), getTranslation("tooltip.view-error-log-file"
            , UI.getCurrent().getLocale()), "14pt", IkasanColours.IKASAN_ORANGE);
        errorLogFile.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.streamLog(schedulerJobInstanceRecord, true);
        });

        Icon logFileHistory  = IconDecorator.decorate(new Icon(VaadinIcon.CLIPBOARD_HEART), getTranslation("tooltip.view-job-execution-history"
            , UI.getCurrent().getLocale()), "14pt", IkasanColours.IKASAN_ORANGE);
        logFileHistory.addClickListener(event -> {
            LogFileHistoryDialog logFileHistoryDialog = new LogFileHistoryDialog(scheduledContextInstanceService
                , this.contextInstance, schedulerJobInstanceRecord.getSchedulerJobInstance()
                , this.moduleMetaDataService, this.logStreamingService);
            logFileHistoryDialog.open();
        });

        Icon event = IconDecorator.decorate(new Icon(VaadinIcon.CALENDAR_CLOCK), getTranslation("tooltip.view-scheduled-process-event"
            , UI.getCurrent().getLocale()), "14pt", IkasanColours.IKASAN_ORANGE);
        event.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            SchedulerJobInstanceRecord updated = this.schedulerJobInstanceService.findById(schedulerJobInstanceRecord.getId());
            if(updated.getSchedulerJobInstance().getScheduledProcessEvent() == null) {
                NotificationHelper.showUserNotification(getTranslation("message.there-is-no-process-event-associated-with-this-job", UI.getCurrent().getLocale()));
                return;
            }
            JsonViewerDialog dialog = new JsonViewerDialog(updated.getSchedulerJobInstance().getScheduledProcessEvent()
                , getTranslation("header.catalyst-scheduled-process-event", UI.getCurrent().getLocale()));
            dialog.open();
        });

        Icon executionDetails = IconDecorator.decorate(new Icon(VaadinIcon.COG), getTranslation("tooltip.view-process-execution-details"
            , UI.getCurrent().getLocale()), "14pt", IkasanColours.IKASAN_ORANGE);
        executionDetails.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            SchedulerJobInstanceRecord updated = this.schedulerJobInstanceService.findById(schedulerJobInstanceRecord.getId());
            if(updated.getSchedulerJobInstance().getScheduledProcessEvent() == null) {
                NotificationHelper.showUserNotification(getTranslation("message.there-is-are-no-process-execution-details-associated-with-this-job", UI.getCurrent().getLocale()));
                return;
            }
            TextViewerDialog dialog = new TextViewerDialog(updated.getSchedulerJobInstance().getScheduledProcessEvent().getExecutionDetails()
                , getTranslation("header.process-execution-details", UI.getCurrent().getLocale()));
            dialog.open();
        });

        buttonLayout.add(logFile, errorLogFile, logFileHistory, event, executionDetails);
        buttonLayout.getStyle().set("position", "absolute");
        buttonLayout.getStyle().set("right", "20px");

        InternalEventDrivenJobInstance instance = (InternalEventDrivenJobInstance)schedulerJobInstanceRecord.getSchedulerJobInstance();
        FormLayout formLayout = new FormLayout();
        TextField acknowledgementTicketId = new TextField(getTranslation("label.ticket-id", UI.getCurrent().getLocale()));
        acknowledgementTicketId.getElement().getThemeList().add("always-float-label");
        acknowledgementTicketId.setValue(instance.getErrorAcknowledgmentTicketId());
        acknowledgementTicketId.setEnabled(false);
        formLayout.add(acknowledgementTicketId);
        TextField acknowledgementBy = new TextField("Acknowledged by");
        acknowledgementBy.getElement().getThemeList().add("always-float-label");
        acknowledgementBy.setValue(instance.getErrorAcknowledgeUser());
        acknowledgementBy.setEnabled(false);
        formLayout.add(acknowledgementBy);
        TextField acknowledgementDateTime = new TextField("Acknowledged date/time");
        acknowledgementDateTime.getElement().getThemeList().add("always-float-label");
        acknowledgementDateTime.setValue(DateFormatter.instance().getFormattedDate(instance.getErrorAcknowledgeTimestamp()));
        acknowledgementDateTime.setEnabled(false);
        formLayout.add(acknowledgementDateTime);
        TextArea acknowledgementReason = new TextArea(getTranslation("label.reason", UI.getCurrent().getLocale()));
        acknowledgementReason.getElement().getThemeList().add("always-float-label");
        acknowledgementReason.setValue(instance.getErrorAcknowledgedMessage());
        acknowledgementReason.setEnabled(false);
        acknowledgementReason.setMinHeight("200px");
        formLayout.add(acknowledgementReason);


        formLayout.getStyle().set("padding-top", "30px");
        VerticalLayout verticalLayout = new VerticalLayout(buttonLayout, formLayout);
        verticalLayout.setSizeFull();
        super.add(verticalLayout);
    }

    /**
     * Helper method to stream job log files.
     *
     * @param schedulerJobInstanceRecord
     * @param getErrorLog
     */
    protected void streamLog(SchedulerJobInstanceRecord schedulerJobInstanceRecord, boolean getErrorLog) {
        boolean displayLog = false;
        String host = null;
        String endPoint = null;
        String outputLog = null;

        ModuleMetaData agent = moduleMetaDataService.findById(schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName());
        ScheduledProcessEvent scheduledProcessEvent = schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent();

        if (scheduledProcessEvent != null && agent != null) {
            host = agent.getUrl();
            endPoint = "/rest/logs";
            outputLog = getErrorLog ? scheduledProcessEvent.getResultError() : scheduledProcessEvent.getResultOutput();
            logger.info(String.format("Streaming log for host %s, endPoint %s, log %s", host, endPoint, outputLog));
            if (outputLog != null && host != null) {
                displayLog = true;
            }
        }

        if (displayLog) {
            SchedulerJobLogFileViewerDialog schedulerJobLogFileViewerDialog = new SchedulerJobLogFileViewerDialog(this.logStreamingService, host, endPoint, outputLog);
            schedulerJobLogFileViewerDialog.open();
        } else {
            String message = "There is no " + (getErrorLog ? "error" : "output") + " log for the job";
            NotificationHelper.showUserNotification(message);
        }
    }
}
