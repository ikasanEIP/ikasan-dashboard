package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.*;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.designer.PositionedDialog;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceSearchFilterImpl;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class CommandExecutionJobPositionedDialog extends PositionedDialog {
    private final Logger logger = LoggerFactory.getLogger(CommandExecutionJobPositionedDialog.class);
    private final SchedulerJobInstanceRecord schedulerJobInstanceRecord;
    private final LogStreamingService logStreamingService;
    private final ModuleMetaDataService moduleMetaDataService;
    private final SchedulerJobInstanceService schedulerJobInstanceService;
    private final ScheduledContextInstanceService scheduledContextInstanceService;
    private final ContextInstance contextInstance;
    private final ConfigurationService configurationRestService;
    private final ModuleControlService moduleControlRestService;
    private final MetaDataService metaDataRestService;
    private final SystemEventLogger systemEventLogger;
    private final JobInitiationService jobInitiationService;
    private final JobUtilsService jobUtilsService;


    public CommandExecutionJobPositionedDialog(SchedulerJobInstanceRecord schedulerJobInstanceRecord, LogStreamingService logStreamingService,
                                               ModuleMetaDataService moduleMetaDataService,SchedulerJobInstanceService schedulerJobInstanceService,
                                               ScheduledContextInstanceService scheduledContextInstanceService, ContextInstance contextInstance,
                                               ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                                               SystemEventLogger systemEventLogger, JobInitiationService jobInitiationService, JobUtilsService jobUtilsService) {
        super(450, 400);
        this.schedulerJobInstanceRecord = schedulerJobInstanceRecord;
        this.logStreamingService = logStreamingService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.contextInstance = contextInstance;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;
        this.jobInitiationService = jobInitiationService;
        this.jobUtilsService = jobUtilsService;
        this.init();
    }

    private void init() {
        HorizontalLayout buttonLayout = new HorizontalLayout();

        Icon openJob = IconDecorator.decorate(new Icon(VaadinIcon.MODAL), getTranslation("tooltip.open-job"
            , UI.getCurrent().getLocale()), "14pt", IkasanColours.IKASAN_ORANGE);
        openJob.addClickListener((ComponentEventListener<ClickEvent<Icon>>) event -> {
            this.openCommandExecutionJob(this.schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier(),
                schedulerJobInstanceRecord.getJobName());
        });


        Icon logFile = IconDecorator.decorate(new Icon(VaadinIcon.FILE_PROCESS), getTranslation("tooltip.view-log-file"
            , UI.getCurrent().getLocale()), "14pt", IkasanColours.IKASAN_ORANGE);
        logFile.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.streamLog(this.schedulerJobInstanceRecord, false);
        });

        Icon errorLogFile = IconDecorator.decorate(new Icon(VaadinIcon.FILE_REMOVE), getTranslation("tooltip.view-error-log-file"
            , UI.getCurrent().getLocale()), "14pt", IkasanColours.IKASAN_ORANGE);
        errorLogFile.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.streamLog(this.schedulerJobInstanceRecord, true);
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

        Icon acknowledgedIcon = IconDecorator.decorate(new Icon(VaadinIcon.THUMBS_UP), getTranslation("tooltip.view-acknowledgement-details"
            , UI.getCurrent().getLocale()), "14pt", IkasanColours.IKASAN_ORANGE);

        if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance &&
            schedulerJobInstanceRecord.getSchedulerJobInstance().getStatus().equals(InstanceStatus.ERROR) &&
            schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged() != null &&
            schedulerJobInstanceRecord.getSchedulerJobInstance().isErrorAcknowledged()) {
            acknowledgedIcon.setVisible(true);
        }
        else {
            acknowledgedIcon.setVisible(false);
        }

        acknowledgedIcon.addClickListener(ackEvent -> {
            SchedulerJobInstanceRecord dbRecord = this.schedulerJobInstanceService.findById(schedulerJobInstanceRecord.getId());
            ErrorAcknowledgedPositionedDialog errorAcknowledgedPositionedDialog = new ErrorAcknowledgedPositionedDialog(dbRecord,
                this.contextInstance, this.scheduledContextInstanceService, this.moduleMetaDataService, this.logStreamingService,
                this.schedulerJobInstanceService);
            PositionedDialog.Position position = new PositionedDialog.Position(ackEvent.getScreenY(), ackEvent.getScreenX());
            errorAcknowledgedPositionedDialog.setPosition(position);
            errorAcknowledgedPositionedDialog.open();
        });

        buttonLayout.add(openJob, logFile, errorLogFile, logFileHistory, event, executionDetails, acknowledgedIcon);
        buttonLayout.getStyle().set("position", "absolute");
        buttonLayout.getStyle().set("right", "20px");

        FormLayout formLayout = new FormLayout();
        TextField jobName = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));

        if(this.schedulerJobInstanceRecord != null) {
            jobName.setValue(this.schedulerJobInstanceRecord.getJobName());
        }
        jobName.setEnabled(false);
        formLayout.add(jobName);

        TextField startTime = new TextField(getTranslation("label.start-time", UI.getCurrent().getLocale()));

        if(this.schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent() != null) {
            startTime.setValue(this.schedulerJobInstanceRecord.getSchedulerJobInstance()
                .getScheduledProcessEvent().getFireTime() == 0 ? getTranslation("label.not-available", UI.getCurrent().getLocale())
                : DateFormatter.instance().getFormattedDate(this.schedulerJobInstanceRecord.getSchedulerJobInstance()
                .getScheduledProcessEvent().getFireTime()));
        }
        else {
            startTime.setValue(getTranslation("label.not-available", UI.getCurrent().getLocale()));
        }

        startTime.setEnabled(false);
        formLayout.add(startTime);
        TextField endTime = new TextField(getTranslation("label.end-time", UI.getCurrent().getLocale()));
        if(this.schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent() != null) {
            endTime.setValue(this.schedulerJobInstanceRecord.getSchedulerJobInstance()
                .getScheduledProcessEvent().getCompletionTime() == 0 ? getTranslation("label.not-available", UI.getCurrent().getLocale())
                : DateFormatter.instance().getFormattedDate(this.schedulerJobInstanceRecord.getSchedulerJobInstance()
                .getScheduledProcessEvent().getCompletionTime()));
        }
        else {
            endTime.setValue(getTranslation("label.not-available", UI.getCurrent().getLocale()));
        }
        endTime.setEnabled(false);
        formLayout.add(endTime);

        TextField duration = new TextField(getTranslation("label.duration", UI.getCurrent().getLocale()));
        if(this.schedulerJobInstanceRecord.getSchedulerJobInstance().getScheduledProcessEvent() != null) {
            duration.setValue(this.schedulerJobInstanceRecord.getSchedulerJobInstance()
                .getScheduledProcessEvent().getFireTime() == 0 || this.schedulerJobInstanceRecord.getSchedulerJobInstance()
                .getScheduledProcessEvent().getCompletionTime() == 0 ? getTranslation("label.not-available", UI.getCurrent().getLocale())
                : DurationFormatUtils.formatDuration(this.schedulerJobInstanceRecord.getSchedulerJobInstance()
                .getScheduledProcessEvent().getCompletionTime() - this.schedulerJobInstanceRecord.getSchedulerJobInstance()
                .getScheduledProcessEvent().getFireTime(), "HH'h':mm'm':ss.SSS's'", true));

        }
        else {
            duration.setValue(getTranslation("label.not-available", UI.getCurrent().getLocale()));
        }

        duration.setEnabled(false);
        formLayout.add(duration);

        TextArea last5ExecutionDurations = new TextArea(getTranslation("label.last-5-execution-durations", UI.getCurrent().getLocale()));

        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setJobName(this.schedulerJobInstanceRecord.getJobName());
        filter.setContextName(this.contextInstance.getName());
        last5ExecutionDurations.setValue(
            this.schedulerJobInstanceService.getScheduledContextInstancesByFilter
                    (filter, 20, 0, EntityFields.CREATED_DATE_TIME, "DESCENDING")
            .getResultList().stream()
            .map(record -> {
                    if (record.getSchedulerJobInstance() != null && record.getStatus().equals(InstanceStatus.COMPLETE.toString())) {
                        return DurationFormatUtils.formatDuration(record.getSchedulerJobInstance()
                            .getScheduledProcessEvent().getCompletionTime() - record.getSchedulerJobInstance()
                            .getScheduledProcessEvent().getFireTime(), "HH'h':mm'm':ss.SSS's'", true);
                    } else {
                        return "";
                    }
                }
            )
            .collect(Collectors.toList()).stream().filter(s -> !s.isEmpty()).limit(5).collect(Collectors.joining(", ")));

        if(last5ExecutionDurations.getValue().isEmpty()) {
            last5ExecutionDurations.setValue(getTranslation("label.no-previous-execution-durations-available", UI.getCurrent().getLocale()));
        }

        last5ExecutionDurations.setEnabled(false);
        formLayout.add(last5ExecutionDurations);

        formLayout.getStyle().set("padding-top", "30px");
        buttonLayout.getStyle().set("z-index", "1000");
        super.add(buttonLayout, formLayout);
        this.setModality(ModalityMode.STRICT);
    }

    /**
     * Open the command execution job dialog.
     *
     * @param identifier of the job
     */
    private void openCommandExecutionJob(String identifier, String jobName) {
        InternalEventDrivenJobInstanceDialog internalEventDrivenJobInstanceDialog = new InternalEventDrivenJobInstanceDialog
            (moduleMetaDataService.findById(this.schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName()),
                configurationRestService, moduleControlRestService,
                metaDataRestService, systemEventLogger, this.schedulerJobInstanceService, this.contextInstance,
                this.jobInitiationService, moduleMetaDataService, this.logStreamingService, this.jobUtilsService,
                this.scheduledContextInstanceService);

        internalEventDrivenJobInstanceDialog.setJob(schedulerJobInstanceRecord);
        internalEventDrivenJobInstanceDialog.open();
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
