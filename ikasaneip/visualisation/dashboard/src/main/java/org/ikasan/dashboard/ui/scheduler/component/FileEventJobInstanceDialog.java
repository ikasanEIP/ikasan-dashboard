package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.FileEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;

public class FileEventJobInstanceDialog extends AbstractCloseableResizableDialog implements SchedulerJobStateChangeEventBroadcastListener {

    Logger logger = LoggerFactory.getLogger(FileEventJobInstanceDialog.class);

    private TextField agentTf;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextArea jobDescriptionTa;
    private TextField filenameTf;
    private TextField archiveDirectoryTf;
    private TextField cronExpressionTf;
    private TextField slaCronExpressionTf;
    private TextField timezoneTf;
    private TextField minFileAgeSecondsTf;

    private ModuleMetaData agent;

    private FileEventDrivenJobInstance fileEventDrivenJobInstance;
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;

    private Binder<FileEventDrivenJobInstance> formBinder;

    private EditMode editMode = EditMode.NEW;

    private FormLayout formLayout;

    private SystemEventLogger systemEventLogger;

    private SchedulerJobInstanceService schedulerJobInstanceService;

    private SchedulerStatusDiv statusDiv;

    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private JobInitiationService jobInitiationService;

    private IkasanAuthentication authentication;

    private Button viewProcessEventButton;

    private ScheduledProcessEvent scheduledProcessEvent;
    private UI ui;
    private ContextInstance contextInstance;

    /**
     * Constructor
     *
     * @param agent
     * @param jobInitiationService
     * @param systemEventLogger
     * @param schedulerJobInstanceService
     */
    public FileEventJobInstanceDialog(ModuleMetaData agent, JobInitiationService jobInitiationService, SystemEventLogger systemEventLogger,
                                      SchedulerJobInstanceService schedulerJobInstanceService, ContextInstance contextInstance) {
        super.showResize(false);
        super.title.setText(getTranslation("label.file-watcher-job", UI.getCurrent().getLocale()));

        this.agent = agent;
        this.systemEventLogger = systemEventLogger;
        this.jobInitiationService = jobInitiationService;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.contextInstance = contextInstance;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }

    private void init() {
        this.formBinder
            = new Binder<>(FileEventDrivenJobInstance.class);

        this.setHeight("750px");
        this.setWidth("95vw");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.getStyle().set("padding-top", "0px");
        layout.getStyle().set("padding-bottom", "10px");
        layout.add(createConfigurationForm());
        super.content.getStyle().set("padding-top", "0px");
        super.content.add(layout);
    }

    /**
     * Initialise the form.
     *
     * @return
     */
    private FormLayout createConfigurationForm() {
        formLayout = new FormLayout();
        formLayout.getStyle().set("padding-top", "0px");
        this.statusDiv = new SchedulerStatusDiv();
        this.statusDiv.setHeight("45px");
        this.statusDiv.setWidth("100%");

        formLayout.add(this.statusDiv, 2);

        Button submitButton = new Button(getTranslation("button.submit"
            , UI.getCurrent().getLocale()), new Icon(VaadinIcon.PAPERPLANE));
        submitButton.setIconAfterText(true);
        submitButton.setVisible(!this.fileEventDrivenJobInstance.getStatus().equals(InstanceStatus.COMPLETE) &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        Button resetButton = new Button("Reset", new Icon(VaadinIcon.ARROW_BACKWARD));
        resetButton.setIconAfterText(true);
        resetButton.setVisible(this.fileEventDrivenJobInstance.getStatus().equals(InstanceStatus.COMPLETE)&&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        submitButton.addClickListener(event -> {
            if(!this.canPerformAction()) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-file-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.submit-file-job", UI.getCurrent().getLocale()));

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                try {
                    this.jobInitiationService.raiseFileEventSchedulerJob(
                        this.agent.getUrl(), this.agent.getName(), this.fileEventDrivenJobInstance, this.fileEventDrivenJobInstance.getContextInstanceId());

                    this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Job Plan Instance Id[%s]"
                        , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName()
                        , schedulerJobInstanceRecord.getSchedulerJobInstance().getContextName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getContextInstanceId()
                            )
                        , this.authentication.getName());

                    this.schedulerJobInstanceRecord.setManuallySubmittedBy(authentication.getName());
                    this.schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                    NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));

                    submitButton.setVisible(false);
                    resetButton.setVisible(true &&
                        ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                        SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                        SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                }
            });
        });

        resetButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.reset-job-header", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.reset-job-body", UI.getCurrent().getLocale()));

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.resetJob()) {
                    this.statusDiv.setStatus(InstanceStatus.WAITING);
                    submitButton.setVisible(true &&
                        ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
                    resetButton.setVisible(false);
                }
            });
        });

        this.viewProcessEventButton = new Button(getTranslation
            ("button.view-event", UI.getCurrent().getLocale()), VaadinIcon.CALENDAR_CLOCK.create());
        this.viewProcessEventButton.setVisible(this.scheduledProcessEvent != null &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        this.viewProcessEventButton.setIconAfterText(true);
        this.viewProcessEventButton.addClickListener(event -> {
            JsonViewerDialog dialog = new JsonViewerDialog(this.scheduledProcessEvent
                , getTranslation("header.scheduled-process-event", UI.getCurrent().getLocale()));
            dialog.open();
        });

        Button export = new Button(getTranslation("button.download", UI.getCurrent().getLocale()), VaadinIcon.DOWNLOAD_ALT.create());
        StreamResource streamResource = new StreamResource(schedulerJobInstanceRecord.getJobName()+".json"
            , () -> {
            try {
                return new ByteArrayInputStream(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(schedulerJobInstanceRecord.getSchedulerJobInstance()));
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        });

        FileDownloadWrapper exportWrapper = new FileDownloadWrapper(streamResource);
        exportWrapper.wrapComponent(export);

        ComponentSecurityVisibility.applySecurity(exportWrapper, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.add(submitButton, resetButton, this.viewProcessEventButton, exportWrapper);
        actionsLayout.setMargin(false);
        actionsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, submitButton);
        actionsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, exportWrapper);

        VerticalLayout actionsButtonLayout = new VerticalLayout();
        actionsButtonLayout.setWidth("100%");
        actionsButtonLayout.add(actionsLayout);
        actionsButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, actionsLayout);
        actionsButtonLayout.setMargin(false);

        // Fields to capture schedule job properties.
        H3 fileWatcherJobLabel = new H3(getTranslation("label.file-watcher-job", UI.getCurrent().getLocale()));
        formLayout.add(fileWatcherJobLabel, actionsButtonLayout);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setId("jobNameTf");
        this.jobNameTf.setRequired(true);
        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW);
        formBinder.forField(this.jobNameTf)
            .bind(FileEventDrivenJobInstance::getJobName, FileEventDrivenJobInstance::setJobName);
        formLayout.add(jobNameTf);

        this.agentTf = new TextField(getTranslation("label.agent", UI.getCurrent().getLocale()));
        this.agentTf.setId("agentCb");
        this.agentTf.setRequired(true);
        this.agentTf.setClearButtonVisible(true);
        formBinder.forField(this.agentTf)
            .bind(FileEventDrivenJobInstance::getAgentName, FileEventDrivenJobInstance::setAgentName);
        formLayout.add(agentTf);


        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setRequired(true);
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .bind(FileEventDrivenJobInstance::getJobDescription, FileEventDrivenJobInstance::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);

        this.filenameTf = new TextField("File path");
        this.filenameTf.setRequired(true);
        this.filenameTf.setId("filePathTf");
        filenameTf.setValue(this.fileEventDrivenJobInstance.getFilenames().get(0));
        formLayout.add(filenameTf, 2);

        archiveDirectoryTf = new TextField(getTranslation("label.archive-directory", UI.getCurrent().getLocale()));
        this.archiveDirectoryTf.setId("archiveDirectoryTf");
        formBinder.forField(this.archiveDirectoryTf)
            .bind(FileEventDrivenJobInstance::getMoveDirectory, FileEventDrivenJobInstance::setMoveDirectory);
        formLayout.add(this.archiveDirectoryTf, 2);

        this.cronExpressionTf = new TextField(getTranslation("label.polling-interval-cron-expression", UI.getCurrent().getLocale()));
        this.cronExpressionTf.setRequired(true);
        this.cronExpressionTf.setId("cronExpressionTf");
        formBinder.forField(this.cronExpressionTf)
            .bind(FileEventDrivenJobInstance::getCronExpression, FileEventDrivenJobInstance::setCronExpression);
        formLayout.add(cronExpressionTf);

        this.slaCronExpressionTf = new TextField(getTranslation("label.sla-interval-cron-expression", UI.getCurrent().getLocale()));
        this.slaCronExpressionTf.setRequired(false);
        this.slaCronExpressionTf.setId("slaCronExpressionTf");
        formBinder.forField(this.slaCronExpressionTf)
            .bind(FileEventDrivenJobInstance::getSlaCronExpression, FileEventDrivenJobInstance::setSlaCronExpression);
        formLayout.add(slaCronExpressionTf);

        this.minFileAgeSecondsTf = new TextField(getTranslation("label.min-file-age-seconds", UI.getCurrent().getLocale()));
        this.minFileAgeSecondsTf.setRequired(true);
        this.minFileAgeSecondsTf.setId("minFileAgeSecondsTf");
        formBinder.forField(this.minFileAgeSecondsTf)
            .withConverter(new StringToIntegerConverter(getTranslation("error.min-file-age-must-be-a-number", UI.getCurrent().getLocale())))
            .bind(FileEventDrivenJobInstance::getMinFileAgeSeconds, FileEventDrivenJobInstance::setMinFileAgeSeconds);
        formLayout.add(minFileAgeSecondsTf);

        this.timezoneTf = new TextField(getTranslation("label.timezone", UI.getCurrent().getLocale()));
        this.timezoneTf.setId("timezoneCb");
        formBinder.forField(this.timezoneTf)
            .bind(FileEventDrivenJobInstance::getTimeZone, FileEventDrivenJobInstance::setTimeZone);
        this.timezoneTf.setClearButtonVisible(true);
        this.timezoneTf.setPlaceholder(getTranslation("label.choose-a-timezone", UI.getCurrent().getLocale()));
        this.timezoneTf.setErrorMessage(getTranslation("error.timezone-required", UI.getCurrent().getLocale()));
        formLayout.add(timezoneTf);

        return formLayout;
    }

    /**
     * Helper method to confirm that actions can be performed on a job plan
     * @return
     */
    private boolean canPerformAction() {
        if(!ContextMachineCache.instance().containsInstanceIdentifier(this.contextInstance.getId())) {
            if(this.contextInstance.getStatus().equals(InstanceStatus.ENDED)) {
                NotificationHelper.showUserNotification(getTranslation("notification.cannot-perform-action-against-ended-plan"
                    , UI.getCurrent().getLocale()));
                return false;
            }
            else {
                NotificationHelper.showErrorNotification(getTranslation("error.cannot-locate-job-plan-instance-in-cache-and-is-not-ended"
                    , UI.getCurrent().getLocale()));
                return false;
            }
        }

        return true;
    }

    /**
     * Helper method to set controls on the form elements if the form is read only
     * or editable.
     */
    public void setEnabled() {
        this.filenameTf.setEnabled(false);
        this.archiveDirectoryTf.setEnabled(false);
        this.jobNameTf.setEnabled(false);
        this.jobDescriptionTa.setEnabled(false);
        this.cronExpressionTf.setEnabled(false);
        this.slaCronExpressionTf.setEnabled(false);
        this.timezoneTf.setEnabled(false);
        this.minFileAgeSecondsTf.setEnabled(false);
        this.agentTf.setEnabled(false);
    }

    /**
     * Set the underlying pojo for the form along with the edit mode.
     *
     * @param fileEventDrivenJob
     */
    private void setJob(FileEventDrivenJobInstance fileEventDrivenJob) {
        this.fileEventDrivenJobInstance = fileEventDrivenJob;
        this.scheduledProcessEvent = this.fileEventDrivenJobInstance.getScheduledProcessEvent();

        this.init();

        this.formBinder.readBean(this.fileEventDrivenJobInstance);
        this.setEnabled();
        this.statusDiv.setStatus(this.schedulerJobInstanceRecord.getStatus());
    }

    public void setJob(SchedulerJobInstanceRecord scheduledEventDrivenJobRecord) {
        this.schedulerJobInstanceRecord = scheduledEventDrivenJobRecord;
        this.setJob((FileEventDrivenJobInstance) this.schedulerJobInstanceService
            .findById(scheduledEventDrivenJobRecord.getId()).getSchedulerJobInstance());
    }

    private boolean resetJob() {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.job-reset-error-no-active-context", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.resetJob(this.fileEventDrivenJobInstance.getIdentifier(), this.fileEventDrivenJobInstance.getChildContextName());

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_RESET, String.format("Agent Name[%s], Scheduled Job Name[%s], Reset[%s]"
                , this.fileEventDrivenJobInstance.getAgentName(), fileEventDrivenJobInstance.getJobName(), true), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(String.format("An error has occurred resetting job[%s], context name[%s], context instance id[%s]"
                , fileEventDrivenJobInstance.getJobName(), fileEventDrivenJobInstance.getContextName()
                , fileEventDrivenJobInstance.getContextInstanceId()), e);
            NotificationHelper.showErrorNotification(getTranslation("error.reset-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        SchedulerJobStateChangeEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.ui = null;
        SchedulerJobStateChangeEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent event) {
        if (event.getSchedulerJobInstance() != null
            && event.getSchedulerJobInstance().getContextInstanceId().equals(this.fileEventDrivenJobInstance.getContextInstanceId())
            && event.getSchedulerJobInstance().getJobName().equals(this.fileEventDrivenJobInstance.getJobName())) {
            this.scheduledProcessEvent = event.getSchedulerJobInstance().getScheduledProcessEvent();
            if(this.ui.isAttached()) {
                this.ui.access(() -> {
                    this.fileEventDrivenJobInstance.setStatus(event.getNewStatus());
                    this.statusDiv.setStatus(event.getNewStatus());
                    this.viewProcessEventButton.setVisible(this.scheduledProcessEvent != null);
                });
            }
        }
    }
}
