package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToLongConverter;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerJobLogFileViewerDialog;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.instance.model.SolrInternalEventDrivenJobInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClientException;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class InternalEventDrivenJobInstanceDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(InternalEventDrivenJobInstanceDialog.class);

    private Registration schedulerJobStateChangeRegistration;

    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private ComboBox<String> agentCb;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextArea jobDescriptionTa;


    // Fields to capture job execution properties.
    private AceEditor commandLineTa;
    private TextField workingDirectoryTf;
    private TextField minExecutionTimeTf;
    private TextField maxExecutionTimeTf;

    private Button skipButton;
    private Button enableButton;
    private Button holdButton;
    private Button releaseButton;
    private Button submitButton;
    private Button killButton;

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleMetaData agent;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;

    private InternalEventDrivenJobInstance internalEventDrivenJobInstance;
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;

    private Binder<InternalEventDrivenJobInstance> formBinder;

    private EditMode editMode = EditMode.NEW;

    private FormLayout formLayout;

    private boolean enabled = true;

    private SystemEventLogger systemEventLogger;

    private SchedulerJobInstanceService schedulerJobInstanceService;

    private SchedulerStatusDiv statusDiv;

    private IkasanAuthentication authentication;

    private ContextInstance contextInstance;

    private ModuleMetaDataService moduleMetaDataService;

    private JobInitiationService jobInitiationService;

    private JobUtilsService jobUtilsService;

    private SchedulerJobLogFileViewerDialog schedulerJobLogFileViewerDialog;

    private Button viewErrorLogButton;
    private Button viewOutputLogButton;

    private ScheduledProcessEvent scheduledProcessEvent;

    private LogStreamingService logStreamingService;

    /**
     * Constructor
     *
     * @param agent
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobInstanceService
     * @param contextInstance
     * @param jobInitiationService
     * @param moduleMetaDataService
     * @param logStreamingService
     */
    public InternalEventDrivenJobInstanceDialog(ModuleMetaData agent, ScheduledProcessManagementService scheduledProcessManagementService,
                                                ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                                SchedulerJobInstanceService schedulerJobInstanceService, ContextInstance contextInstance,
                                                JobInitiationService jobInitiationService, ModuleMetaDataService moduleMetaDataService,
                                                LogStreamingService logStreamingService, JobUtilsService jobUtilsService) {
        super.showResize(false);
        super.title.setText(getTranslation("label.command-execution-job-instance", UI.getCurrent().getLocale()));

        this.agent = agent;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.contextInstance = contextInstance;
        this.jobInitiationService = jobInitiationService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.logStreamingService = logStreamingService;
        this.jobUtilsService = jobUtilsService;

        this.internalEventDrivenJobInstance = new SolrInternalEventDrivenJobInstanceImpl();

        authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }

    private void init() {
        this.formBinder
            = new Binder<>(InternalEventDrivenJobInstance.class);

        this.setHeight("1100px");
        this.setWidth("1400px");


        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(false);
        layout.add(this.createJobForm());
        layout.getStyle().set("padding-top", "0px");
        layout.getStyle().set("padding-bottom", "10px");
        super.content.getStyle().set("padding-top", "0px");
        super.content.add(layout);
    }

    /**
     * Initialise the form.
     *
     * @return
     */
    private FormLayout createJobForm() {
        formLayout = new FormLayout();
        formLayout.getStyle().set("padding-top", "0px");
        this.statusDiv = new SchedulerStatusDiv();
        this.statusDiv.setHeight("45px");
        this.statusDiv.setWidth("100%");
        this.statusDiv.setStatus(this.schedulerJobInstanceRecord.getStatus());

        formLayout.add(this.statusDiv, 2);

        this.holdButton = new Button(getTranslation("button.hold", UI.getCurrent().getLocale()), new Icon(VaadinIcon.HAND));
        this.holdButton.setIconAfterText(true);

        if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.ON_HOLD) ||
            this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.SKIPPED)) {
            this.holdButton.setVisible(false);
        }

        this.holdButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.hold-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.hold-job", UI.getCurrent().getLocale()));

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.holdJob()) {
                    this.statusDiv.setStatus(InstanceStatus.ON_HOLD);
                    this.releaseButton.setVisible(true);
                    this.holdButton.setVisible(false);
                    this.skipButton.setVisible(false);
                }
            });
        });

        this.releaseButton = new Button(getTranslation("button.release", UI.getCurrent().getLocale()), new Icon(VaadinIcon.HANDS_UP));
        this.releaseButton.setIconAfterText(true);

        if(!this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.ON_HOLD)) {
            this.releaseButton.setVisible(false);
        }

        this.releaseButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.release-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.release-job", UI.getCurrent().getLocale()));

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.releaseJob()) {
                    this.statusDiv.setStatus(InstanceStatus.WAITING);
                    this.holdButton.setVisible(true);
                    this.skipButton.setVisible(true);
                    this.releaseButton.setVisible(false);
                }
            });
        });

        this.skipButton = new Button(getTranslation("button.skip", UI.getCurrent().getLocale()), new Icon(VaadinIcon.BAN));
        this.skipButton.setIconAfterText(true);

        if(!(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.ON_HOLD) ||
            this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.SKIPPED))) {
            this.skipButton.setVisible(true);
        }
        else {
            this.skipButton.setVisible(false);
        }

        this.skipButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.skip-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.skip-job", UI.getCurrent().getLocale()));

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.skipJob()) {
                    this.statusDiv.setStatus(InstanceStatus.SKIPPED);
                    this.releaseButton.setVisible(false);
                    this.holdButton.setVisible(false);

                    this.enableButton.setVisible(true);
                    this.skipButton.setVisible(false);
                }
            });
        });

        this.enableButton = new Button(getTranslation("button.enable", UI.getCurrent().getLocale()), new Icon(VaadinIcon.PLAY));
        this.enableButton.setIconAfterText(true);

        if(!this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.SKIPPED)) {
            this.enableButton.setVisible(false);
        }

        this.enableButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.enable-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.enable-job", UI.getCurrent().getLocale()));

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.enableJob()) {
                    this.statusDiv.setStatus(InstanceStatus.WAITING);
                    this.releaseButton.setVisible(false);
                    this.holdButton.setVisible(true);
                    this.enableButton.setVisible(false);
                    this.skipButton.setVisible(true);
                }
            });
        });

        this.submitButton = new Button(getTranslation("button.submit", UI.getCurrent().getLocale()), new Icon(VaadinIcon.PAPERPLANE));
        this.submitButton.setIconAfterText(true);

        this.submitButton.addClickListener(event -> {
            InternalEventDrivenJobSubmissionDialog internalEventDrivenJobSubmissionDialog = new InternalEventDrivenJobSubmissionDialog(this.systemEventLogger,
                this.moduleMetaDataService, this.contextInstance, this.jobInitiationService, this.internalEventDrivenJobInstance);

            internalEventDrivenJobSubmissionDialog.open();
        });

        this.killButton = new Button(getTranslation("button.kill-job", UI.getCurrent().getLocale()), new Icon(VaadinIcon.CLOSE_BIG));
        this.killButton.setIconAfterText(true);

        if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.RUNNING)) {
            this.killButton.setVisible(true);
        }
        else {
            this.killButton.setVisible(false);
        }

        this.killButton.addClickListener(event -> {
            try {
                this.jobUtilsService.killJob(agent.getUrl(), scheduledProcessEvent.getPid(), true);
            }
            catch (Exception e) {
                NotificationHelper.showErrorNotification("An error has occurred attempting to kill the job! Please contact Ikasan Support.");

                return;
            }

            NotificationHelper.showUserNotification("The job was successfully killed");
        });


        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.add(holdButton, releaseButton, skipButton, enableButton, submitButton, killButton);
        actionsLayout.setMargin(false);

        VerticalLayout actionsButtonLayout = new VerticalLayout();
        actionsButtonLayout.setWidth("100%");
        actionsButtonLayout.add(actionsLayout);
        actionsButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, actionsLayout);
        actionsButtonLayout.setMargin(false);

        H3 jobExecutionLabel = new H3(getTranslation("label.command-execution-job-instance", UI.getCurrent().getLocale()));
        formLayout.add(jobExecutionLabel, actionsButtonLayout);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setId("jobNameTf");
        this.jobNameTf.setRequired(true);
        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW);
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), getTranslation("error.missing-job-name", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJobInstance::getJobName, InternalEventDrivenJobInstance::setJobName);
        formLayout.add(jobNameTf);

        this.agentCb = new ComboBox<>(getTranslation("label.agent", UI.getCurrent().getLocale()));
        this.agentCb.setId("agentCb");
        this.agentCb.setRequired(true);
        this.agentCb.setClearButtonVisible(true);
        this.agentCb.setItems(this.scheduledProcessManagementService.getAllAgentNames());
        if(agent != null) {
            this.agentCb.setValue(agent.getName());
            this.agentCb.setEnabled(false);
        }
        formBinder.forField(this.agentCb)
            .withValidator(agentValue -> agentValue != null && !agentValue.isEmpty(), getTranslation("error.missing-agent", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJobInstance::getAgentName, InternalEventDrivenJobInstance::setAgentName);
        formLayout.add(agentCb);

        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setRequired(true);
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJobInstance::getJobDescription, InternalEventDrivenJobInstance::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);

        this.minExecutionTimeTf = new TextField(getTranslation("label.minimum-execution-time", UI.getCurrent().getLocale()));
        formBinder.forField(this.minExecutionTimeTf)
            .withNullRepresentation("")
            .withConverter(
                new StringToLongConverter("Please enter a number"))
            .bind(InternalEventDrivenJobInstance::getMinExecutionTime, InternalEventDrivenJobInstance::setMinExecutionTime);

        this.maxExecutionTimeTf = new TextField(getTranslation("label.maximum-execution-time", UI.getCurrent().getLocale()));
        formBinder.forField(this.maxExecutionTimeTf)
            .withNullRepresentation("")
            .withConverter(
                new StringToLongConverter("Please enter a number"))
            .bind(InternalEventDrivenJobInstance::getMaxExecutionTime, InternalEventDrivenJobInstance::setMaxExecutionTime);

        formLayout.add(minExecutionTimeTf, maxExecutionTimeTf);

        this.workingDirectoryTf = new TextField(getTranslation("label.working-directory", UI.getCurrent().getLocale()));
        formBinder.forField(this.workingDirectoryTf)
            .withNullRepresentation("")
            .bind(InternalEventDrivenJobInstance::getWorkingDirectory, InternalEventDrivenJobInstance::setWorkingDirectory);
        formLayout.add(workingDirectoryTf, 2);

        Button executionDaysButton = new Button(getTranslation("button.execution-days", UI.getCurrent().getLocale()), new Icon(VaadinIcon.CALENDAR));
        executionDaysButton.setIconAfterText(true);
        executionDaysButton.addClickListener(event -> {
            DayOfWeekJobDialog dayOfWeekJobDialog = new DayOfWeekJobDialog(this.internalEventDrivenJobInstance.getDaysOfWeekToRun() == null
                ? null : new ArrayList<>(this.internalEventDrivenJobInstance.getDaysOfWeekToRun()), false);
            dayOfWeekJobDialog.open();

            dayOfWeekJobDialog.addOpenedChangeListener(openedChangeEvent -> {
               if(!openedChangeEvent.isOpened() && dayOfWeekJobDialog.isSaveClose()) {
                   this.internalEventDrivenJobInstance.setDaysOfWeekToRun(dayOfWeekJobDialog.getDaysOfWeek());
               }
            });
        });

        Button parametersButton = new Button(getTranslation("button.parameters", UI.getCurrent().getLocale()), new Icon(VaadinIcon.SLIDERS));
        parametersButton.setIconAfterText(true);
        parametersButton.addClickListener(event -> {
            ContextParameterDialog contextParameterDialog = new ContextParameterDialog(false);
            contextParameterDialog.initParams(this.internalEventDrivenJobInstance.getContextParameters() == null ? new ArrayList<>() : this.internalEventDrivenJobInstance.getContextParameters());
            contextParameterDialog.open();

            contextParameterDialog.addOpenedChangeListener(changeEvent -> {
                if(!changeEvent.isOpened() && contextParameterDialog.isSaveClose()) {
                    this.internalEventDrivenJobInstance.setContextParameters(contextParameterDialog.getContextParameters());
                }
            });
        });

        Button successfulReturnCodesButton = new Button(getTranslation("button.return-codes", UI.getCurrent().getLocale()), new Icon(VaadinIcon.CHECK));
        successfulReturnCodesButton.setIconAfterText(true);
        successfulReturnCodesButton.addClickListener(event -> {
            SuccessfulReturnCodesDialog successfulReturnCodesDialog = new SuccessfulReturnCodesDialog(false);
            successfulReturnCodesDialog.initReturnCodes(this.internalEventDrivenJobInstance.getSuccessfulReturnCodes());
            successfulReturnCodesDialog.open();

            successfulReturnCodesDialog.addOpenedChangeListener(changeEvent -> {
                if(!changeEvent.isOpened() && successfulReturnCodesDialog.isSaveClose()) {
                    this.internalEventDrivenJobInstance.setSuccessfulReturnCodes(successfulReturnCodesDialog.getSuccessfulReturnCodes());
                }
            });
        });

        this.viewOutputLogButton = new Button("Output Log", VaadinIcon.FILE_PROCESS.create());
        this.viewOutputLogButton.setVisible(this.scheduledProcessEvent != null);
        this.viewOutputLogButton.setIconAfterText(true);
        this.viewOutputLogButton.addClickListener(event -> {
           this.streamLog(this.internalEventDrivenJobInstance, false);
        });

        this.viewErrorLogButton = new Button("Error Log", VaadinIcon.FILE_PROCESS.create());
        this.viewErrorLogButton.setVisible(this.scheduledProcessEvent != null);
        this.viewErrorLogButton.setIconAfterText(true);
        this.viewErrorLogButton.addClickListener(event -> {
            this.streamLog(this.internalEventDrivenJobInstance, true);
        });

        Button downloadButton = new Button(getTranslation("button.download", UI.getCurrent().getLocale()), new Icon(VaadinIcon.DOWNLOAD_ALT));
        downloadButton.setIconAfterText(true);
        StreamResource streamResource = new StreamResource(this.internalEventDrivenJobInstance.getJobName()+".json"
            , () -> {
            try {
                return new ByteArrayInputStream(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this.internalEventDrivenJobInstance));
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        });

        FileDownloadWrapper buttonWrapper = new FileDownloadWrapper(streamResource);
        buttonWrapper.wrapComponent(downloadButton);

        Button expandButton = new Button(getTranslation("button.expand", UI.getCurrent().getLocale()), new Icon(VaadinIcon.EXTERNAL_LINK));
        expandButton.setIconAfterText(true);

        HorizontalLayout jobActionsButtonLayout = new HorizontalLayout();
        jobActionsButtonLayout.add(executionDaysButton, parametersButton, successfulReturnCodesButton, this.viewOutputLogButton, this.viewErrorLogButton);
        jobActionsButtonLayout.setMargin(false);
        VerticalLayout wrapperLayout = new VerticalLayout();
        wrapperLayout.setWidth("100%");
        wrapperLayout.add(jobActionsButtonLayout);
        wrapperLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.START, jobActionsButtonLayout);
        wrapperLayout.setMargin(false);

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.add(buttonWrapper, expandButton);

        VerticalLayout newButtonLayout = new VerticalLayout();
        newButtonLayout.setWidth("100%");
        newButtonLayout.add(horizontalLayout);
        newButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, horizontalLayout);
        newButtonLayout.setMargin(false);

        formLayout.add(wrapperLayout, newButtonLayout);

        this.commandLineTa = new AceEditor();
        this.commandLineTa.setHeight("450px");
        this.commandLineTa.setMode(AceMode.batchfile);
        this.commandLineTa.setTheme(AceTheme.dracula);
        this.commandLineTa.setId("commandLineTa");

        formLayout.add(commandLineTa, 2);
        commandLineTa.getStyle().set("minHeight", "100px");

        this.setButtonVisibility();

        return formLayout;
    }

    private void streamLog(SchedulerJob schedulerJob, boolean getErrorLog) {
        // TODO remove all the log info when happy this is working correctly
        boolean displayLog = false;
        String host = null;
        String endPoint = null;
        String outputLog = null;

        SchedulerJobInstance schedulerJobInstance = this.contextInstance.getScheduledJobsMap().get(schedulerJob.getIdentifier());

        schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();

        logger.info("schedulerJobInstance is " + schedulerJobInstance + " for job identifier " + schedulerJob.getIdentifier());

        logger.info("agent is " + agent + " for name " + schedulerJob.getAgentName());
        if (this.scheduledProcessEvent != null && agent != null) {
            host = agent.getUrl();
            endPoint = "/rest/logs";
            outputLog = getErrorLog ? this.scheduledProcessEvent.getResultError() : this.scheduledProcessEvent.getResultOutput();
            logger.info(String.format("Streaming log for host %s, endPoint %s, log %s", host, endPoint, outputLog));
            if (outputLog != null && host != null) {
                displayLog = true;
            }
        }

        if (displayLog) {
            schedulerJobLogFileViewerDialog = new SchedulerJobLogFileViewerDialog(this.logStreamingService, host, endPoint, outputLog);
            schedulerJobLogFileViewerDialog.open();
        } else {
            String message = "There is no " + (getErrorLog ? "error" : "output") + " log for the job";
            NotificationHelper.showUserNotification(message);
        }
    }

    /**
     * Helper method to skip the job.
     *
     * @return
     */
    private boolean skipJob() {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-skipped", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.skipJob(this.internalEventDrivenJobInstance.getIdentifier(), this.internalEventDrivenJobInstance.getChildContextName(), true);
            this.updateJobState(this.internalEventDrivenJobInstance, InstanceStatus.SKIPPED);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Skipped[%s]"
                , this.internalEventDrivenJobInstance.getAgentName(), internalEventDrivenJobInstance.getJobName(), true), this.authentication.getName());
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
    private boolean enableJob() {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-enabled", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.skipJob(this.internalEventDrivenJobInstance.getIdentifier(), this.internalEventDrivenJobInstance.getChildContextName(), false);
            this.updateJobState(this.internalEventDrivenJobInstance, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Skipped[%s]"
                , this.internalEventDrivenJobInstance.getAgentName(), internalEventDrivenJobInstance.getJobName(), false), this.authentication.getName());
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
    private boolean holdJob() {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-held", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.holdJob(this.internalEventDrivenJobInstance.getIdentifier(), this.internalEventDrivenJobInstance.getChildContextName());
            this.updateJobState(this.internalEventDrivenJobInstance, InstanceStatus.ON_HOLD);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_HELD, String.format("Agent Name[%s], Scheduled Job Name[%s], Held[%s]"
                , this.internalEventDrivenJobInstance.getAgentName(), internalEventDrivenJobInstance.getJobName(), true), this.authentication.getName());
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
    private boolean releaseJob() {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-released", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.releaseJob(this.internalEventDrivenJobInstance.getIdentifier(), this.internalEventDrivenJobInstance.getChildContextName());
            this.updateJobState(this.internalEventDrivenJobInstance, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_RELEASED, String.format("Agent Name[%s], Scheduled Job Name[%s], Released[%s]"
                , this.internalEventDrivenJobInstance.getAgentName(), internalEventDrivenJobInstance.getJobName(), true), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.released-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }
    

    /**
     * Update and persist the associated job.
     *
     * @param internalEventDrivenJobInstance
     */
    public void updateScheduledJob(InternalEventDrivenJobInstance internalEventDrivenJobInstance, IkasanAuthentication authentication) {

        this.schedulerJobInstanceRecord.setModifiedTimestamp(System.currentTimeMillis());
        this.schedulerJobInstanceRecord.setSchedulerJobInstance(internalEventDrivenJobInstance);
        this.schedulerJobInstanceRecord.setModifiedBy(authentication.getName());
        this.schedulerJobInstanceRecord.setStatus(internalEventDrivenJobInstance.getStatus().name());

        this.schedulerJobInstanceService.save(this.schedulerJobInstanceRecord);
     }

    /**
     * Helper method to set controls on the form elements if the form is read only
     * or editable.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        this.agentCb.setEnabled(enabled);
        this.commandLineTa.setEnabled(enabled);
        this.commandLineTa.setReadOnly(!enabled);

        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW);
        this.jobDescriptionTa.setEnabled(enabled);

        this.commandLineTa.setEnabled(enabled);
        this.workingDirectoryTf.setEnabled(enabled);

        this.minExecutionTimeTf.setEnabled(enabled);
        this.maxExecutionTimeTf.setEnabled(enabled);
    }

    /**
     * Set the underlying pojo for the form along with the edit mode.
     *
     * @param internalEventDrivenJob
     */
    private void setJob(InternalEventDrivenJobInstance internalEventDrivenJob) {
        this.internalEventDrivenJobInstance = internalEventDrivenJob;
        this.scheduledProcessEvent = this.internalEventDrivenJobInstance.getScheduledProcessEvent();

        this.init();

        this.formBinder.readBean(this.internalEventDrivenJobInstance);
        this.commandLineTa.setValue(internalEventDrivenJob.getCommandLine());

        // make sure all value are bound before calling set enabled
        this.setEnabled(false);
    }

    /**
     * Helper method to update a jobs state and persist it before broadcasting the state change.
     *
     * @param internalEventDrivenJobInstance
     * @param newStatus
     */
    private void updateJobState(InternalEventDrivenJobInstance internalEventDrivenJobInstance, InstanceStatus newStatus) {
        InstanceStatus previousStatus = internalEventDrivenJobInstance.getStatus();
        internalEventDrivenJobInstance.setStatus(newStatus);
        this.updateScheduledJob(internalEventDrivenJobInstance, this.authentication);

        SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
            = new SchedulerJobInstanceStateChangeEventImpl(internalEventDrivenJobInstance,
            this.contextInstance, previousStatus, newStatus);

        SchedulerJobStateChangeEventBroadcaster.broadcast(schedulerJobInstanceStateChangeEvent);
    }

    /**
     * Set the job record on this object.
     *
     * @param internalEventDrivenJobRecord
     */
    public void setJob(SchedulerJobInstanceRecord internalEventDrivenJobRecord) {
        this.schedulerJobInstanceRecord = internalEventDrivenJobRecord;
        this.setJob((InternalEventDrivenJobInstance) this.schedulerJobInstanceService
            .findById(internalEventDrivenJobRecord.getId()).getSchedulerJobInstance());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        schedulerJobStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(jobInstanceStateChangeEvent -> {
            if (jobInstanceStateChangeEvent.getSchedulerJobInstance() != null
                && jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextInstanceId().equals(this.internalEventDrivenJobInstance.getContextInstanceId())
                && jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName().equals(this.internalEventDrivenJobInstance.getJobName())) {
                this.internalEventDrivenJobInstance.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                this.statusDiv.setStatus(jobInstanceStateChangeEvent.getNewStatus());

                this.scheduledProcessEvent = jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent();

                this.viewOutputLogButton.setVisible(this.scheduledProcessEvent != null);
                this.viewErrorLogButton.setVisible(this.scheduledProcessEvent != null);

                this.setButtonVisibility();
            }
        });
    }

    private void setButtonVisibility() {
        if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.RUNNING)) {
            this.killButton.setVisible(true);
            this.submitButton.setVisible(false);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
        }
        else if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.ERROR)) {
            this.killButton.setVisible(false);
            this.submitButton.setVisible(true);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
        }
        else if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.WAITING)) {
            this.killButton.setVisible(false);
            this.submitButton.setVisible(true);
            this.holdButton.setVisible(true);
            this.releaseButton.setVisible(false);
            this.skipButton.setVisible(true);
            this.enableButton.setVisible(false);
        }
        else if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.ON_HOLD)) {
            this.killButton.setVisible(false);
            this.submitButton.setVisible(true);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(true);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
        }
        else if(this.internalEventDrivenJobInstance.getStatus().equals(InstanceStatus.SKIPPED)) {
            this.killButton.setVisible(false);
            this.submitButton.setVisible(true);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(true);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.schedulerJobStateChangeRegistration.remove();
        this.schedulerJobStateChangeRegistration = null;
    }
}
