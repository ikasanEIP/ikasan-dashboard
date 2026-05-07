package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.orchestration.service.context.global.GlobalEventServiceImpl;
import org.ikasan.orchestration.service.context.local.LocalEventServiceImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.LocalEventJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.LocalEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

public class LocalEventJobInstanceDialog extends AbstractCloseableResizableDialog implements SchedulerJobStateChangeEventLocalBroadcastListener {

    Logger logger = LoggerFactory.getLogger(LocalEventJobInstanceDialog.class);

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextField jobNameAliasTf;
    private TextArea jobDescriptionTa;

    private TextField catalystJobNameTf;
    private TextField catalystContextNameTf;
    private TextField catalystContextIdentifierTf;
    private TextField catalystJobFireTimeTf;
    private TextField catalystJobCompletionTimeTf;
    private Icon viewRawJobButton;
    private Icon viewProcessExecutionButton;

    private Button holdButton;
    private Button releaseButton;
    private Button submitButton;
    private Button skipButton;
    private Button enableButton;

    private IkasanAuthentication authentication;
    private LocalEventJobInstance localEventJobInstance;
    private Binder<LocalEventJobInstance> formBinder;
    private EditMode editMode = EditMode.NEW;
    private FormLayout formLayout;
    private boolean enabled = true;
    private SystemEventLogger systemEventLogger;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;
    private SchedulerStatusDiv statusDiv;
    private LocalEventService localEventService;
    private ContextInstance contextInstance;
    private UI ui;

    /**
     * Constructor
     *
     * @param systemEventLogger
     * @param schedulerJobInstanceService
     * @param contextInstance
     */
    public LocalEventJobInstanceDialog(SystemEventLogger systemEventLogger, SchedulerJobInstanceService schedulerJobInstanceService,
                                       ContextInstance contextInstance) {
        super.showResize(false);
        super.title.setText(getTranslation("header.local-job", UI.getCurrent().getLocale()));

        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger ==  null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if(this.schedulerJobInstanceService ==  null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.contextInstance = contextInstance;
        if(this.contextInstance ==  null) {
            throw new IllegalArgumentException("contextInstance cannot be null!");
        }

        this.localEventService = new LocalEventServiceImpl();

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }

    private void init() {
        this.formBinder = new Binder<>(LocalEventJobInstance.class);

        this.setHeight("750px");
        this.setWidth("90vw");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(false);
        layout.add(this.createForm());
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
    private FormLayout createForm() {
        this.formLayout = new FormLayout();
        this.formLayout.getStyle().set("padding-top", "0px");

        this.statusDiv = new SchedulerStatusDiv();
        this.statusDiv.setHeight("45px");
        this.statusDiv.setWidth("100%");
        this.statusDiv.setStatus(this.schedulerJobInstanceRecord.getStatus());

        formLayout.add(this.statusDiv, 2);

        this.holdButton = new Button(getTranslation("button.hold", UI.getCurrent().getLocale()), new Icon(VaadinIcon.HAND));
        this.holdButton.setIconAfterText(true);

        this.holdButton.addClickListener(event -> {
            if(!canPerformAction()) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.hold-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.hold-job", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.holdJob()) {
                    this.statusDiv.setStatus(InstanceStatus.ON_HOLD);
                    this.localEventJobInstance.setStatus(InstanceStatus.ON_HOLD);
                    this.setButtonVisibility();
                }
            });
        });

        this.releaseButton = new Button(getTranslation("button.release", UI.getCurrent().getLocale()), new Icon(VaadinIcon.HANDS_UP));
        this.releaseButton.setIconAfterText(true);

        this.releaseButton.addClickListener(event -> {
            if(!canPerformAction()) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.release-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.release-job", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.releaseJob()) {
                    this.statusDiv.setStatus(InstanceStatus.WAITING);
                    this.localEventJobInstance.setStatus(InstanceStatus.WAITING);
                    this.setButtonVisibility();
                }
            });
        });

        this.skipButton = new Button(getTranslation("button.skip", UI.getCurrent().getLocale()), new Icon(VaadinIcon.BAN));
        this.skipButton.setIconAfterText(true);

        this.skipButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.skip-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.skip-job", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.skipJob()) {
                    this.statusDiv.setStatus(InstanceStatus.SKIPPED);
                    this.localEventJobInstance.setStatus(InstanceStatus.SKIPPED);
                    this.setButtonVisibility();
                }
            });
        });

        this.enableButton = new Button(getTranslation("button.enable", UI.getCurrent().getLocale()), new Icon(VaadinIcon.PLAY));
        this.enableButton.setIconAfterText(true);

        this.enableButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.enable-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.enable-job", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                if(this.enableJob()) {
                    this.statusDiv.setStatus(InstanceStatus.WAITING);
                    this.localEventJobInstance.setStatus(InstanceStatus.WAITING);
                    this.setButtonVisibility();
                }
            });
        });

        this.submitButton = new Button(getTranslation("button.submit", UI.getCurrent().getLocale())
            , new Icon(VaadinIcon.PAPERPLANE));
        this.submitButton.setIconAfterText(true);

        this.submitButton.addClickListener(event -> {
            if(!this.canPerformAction()) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-local-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.submit-local-job", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                try {
                    LocalEventJobInstance instance = (LocalEventJobInstance) schedulerJobInstanceRecord
                        .getSchedulerJobInstance();

                    this.localEventService.raiseLocalEventJob(instance,
                        this.contextInstance.getId(), SecurityContextHolder.getContext().getAuthentication().getName());

                    this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Job Plan Instance Id[%s]"
                            , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName()
                            , schedulerJobInstanceRecord.getSchedulerJobInstance().getContextName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getContextInstanceId())
                        , this.authentication.getName());

                    instance.setStatus(InstanceStatus.COMPLETE);
                    schedulerJobInstanceRecord.setSchedulerJobInstance(instance);
                    schedulerJobInstanceRecord.setStatus(InstanceStatus.COMPLETE.toString());
                    schedulerJobInstanceRecord.setManuallySubmittedBy(this.authentication.getName());
                    schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                    this.setButtonVisibility();

                    NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
                } catch (Exception e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                }
            });
        });

        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.add(this.holdButton, this.releaseButton, this.skipButton, this.enableButton, this.submitButton);
        actionsLayout.setMargin(false);

        VerticalLayout actionsButtonLayout = new VerticalLayout();
        actionsButtonLayout.setWidth("100%");
        actionsButtonLayout.add(actionsLayout);
        actionsButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, actionsLayout);
        actionsButtonLayout.setMargin(false);

        formLayout.add(actionsButtonLayout, 2);

        // Fields to capture schedule job properties.
        H3 globaljobLabel = new H3(getTranslation("header.local-job", UI.getCurrent().getLocale()));
        formLayout.add(globaljobLabel, 2);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setId("jobNameTf");
        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), getTranslation("error.missing-job-name", UI.getCurrent().getLocale()))
            .bind(LocalEventJobInstance::getJobName, LocalEventJobInstance::setJobName);
        formLayout.add(jobNameTf, 2);


        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(LocalEventJobInstance::getJobDescription, LocalEventJobInstance::setJobDescription);

        if(this.contextInstance.isUseDisplayName()) {
            this.jobNameAliasTf = new TextField(getTranslation("label.job-name-alias", UI.getCurrent().getLocale()));
            this.jobNameAliasTf.setId("jobNameAliasTf");
            this.jobNameAliasTf.setRequired(false);
            this.jobNameAliasTf.setEnabled(false);
            formBinder.forField(this.jobNameAliasTf)
                .bind(LocalEventJobInstance::getDisplayName, LocalEventJobInstance::setDisplayName);
            formLayout.add(jobNameAliasTf, jobDescriptionTa);
        }
        else {
            formLayout.add(jobDescriptionTa, 2);
        }

        H4 catalystJobLabel = new H4(getTranslation("header.catalyst-job", UI.getCurrent().getLocale()));
        formLayout.add(catalystJobLabel, 2);

        this.catalystJobNameTf = new TextField(getTranslation("label.catalyst-job-name", UI.getCurrent().getLocale()));
        this.catalystJobNameTf.getElement().getThemeList().add("always-float-label");
        formLayout.add(this.catalystJobNameTf, 1);

        this.viewRawJobButton = IconDecorator.decorate(VaadinIcon.CALENDAR_CLOCK.create(), getTranslation("tooltip.open-raw-catalyst-job-event", UI.getCurrent().getLocale())
            , "16pt", IkasanColours.IKASAN_ORANGE);
        viewRawJobButton.addClickListener(event -> {
            ContextualisedScheduledProcessEvent catalystEvent = (ContextualisedScheduledProcessEvent) ((ContextualisedScheduledProcessEvent)this.localEventJobInstance.getScheduledProcessEvent())
                .getCatalystEvent();

            JsonViewerDialog viewerDialog = new JsonViewerDialog(catalystEvent, getTranslation("header.catalyst-scheduled-process-event", UI.getCurrent().getLocale()));
            viewerDialog.open();
        });

        this.viewProcessExecutionButton = IconDecorator.decorate(VaadinIcon.COG.create(), getTranslation("tooltip.open-raw-catalyst-job-event", UI.getCurrent().getLocale())
            , "16pt", IkasanColours.IKASAN_ORANGE);
        viewProcessExecutionButton.addClickListener(event -> {
            ContextualisedScheduledProcessEvent catalystEvent = (ContextualisedScheduledProcessEvent) ((ContextualisedScheduledProcessEvent)this.localEventJobInstance.getScheduledProcessEvent())
                .getCatalystEvent();

            TextViewerDialog viewerDialog = new TextViewerDialog(catalystEvent.getExecutionDetails()
                , getTranslation("header.process-execution-details", UI.getCurrent().getLocale()));
            viewerDialog.open();
        });

        VerticalLayout buttonWrapper = new VerticalLayout();
        buttonWrapper.setWidthFull();
        buttonWrapper.setPadding(false);
        buttonWrapper.setMargin(false);
        buttonWrapper.setSpacing(false);

        HorizontalLayout buttonLayout = new HorizontalLayout(viewRawJobButton
            , viewProcessExecutionButton);
        buttonLayout.setPadding(false);
        buttonLayout.setMargin(false);
        buttonWrapper.add(buttonLayout);
        buttonWrapper.setHorizontalComponentAlignment(FlexComponent.Alignment.END, buttonLayout);

        formLayout.add(buttonWrapper, 1);

        this.catalystContextNameTf = new TextField(getTranslation("label.catalyst-context-name", UI.getCurrent().getLocale()));
        this.catalystContextNameTf.getElement().getThemeList().add("always-float-label");
        formLayout.add(this.catalystContextNameTf, 1);

        this.catalystContextIdentifierTf = new TextField(getTranslation("label.catalyst-context-id", UI.getCurrent().getLocale()));
        this.catalystContextIdentifierTf.getElement().getThemeList().add("always-float-label");
        formLayout.add(this.catalystContextIdentifierTf, 1);

        this.catalystJobFireTimeTf = new TextField(getTranslation("label.catalyst-job-fire-time", UI.getCurrent().getLocale()));
        this.catalystJobFireTimeTf.getElement().getThemeList().add("always-float-label");
        formLayout.add(this.catalystJobFireTimeTf, 1);

        this.catalystJobCompletionTimeTf = new TextField(getTranslation("label.catalyst-job-completion-time", UI.getCurrent().getLocale()));
        this.catalystJobCompletionTimeTf.getElement().getThemeList().add("always-float-label");
        formLayout.add(this.catalystJobCompletionTimeTf, 1);

        this.setButtonVisibility();

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
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        this.jobNameTf.setEnabled(enabled);
        this.jobDescriptionTa.setEnabled(enabled);
        this.catalystJobNameTf.setEnabled(enabled);
        this.catalystContextNameTf.setEnabled(enabled);
        this.catalystContextIdentifierTf.setEnabled(enabled);
        this.catalystJobFireTimeTf.setEnabled(false);
        this.catalystJobCompletionTimeTf.setEnabled(false);

        if(this.localEventJobInstance.getScheduledProcessEvent() == null
            || ((ContextualisedScheduledProcessEvent)this.localEventJobInstance.getScheduledProcessEvent()).getCatalystEvent() == null) {
            this.viewProcessExecutionButton.setVisible(false);
            this.viewRawJobButton.setVisible(false);
        }
        else {
            this.viewProcessExecutionButton.setVisible(true);
            if(((ContextualisedScheduledProcessEvent)((ContextualisedScheduledProcessEvent)this.localEventJobInstance
                .getScheduledProcessEvent()).getCatalystEvent()).getContextName().equals(GlobalEventServiceImpl.GLOBAL_EVENT_MANUALLY_RAISED)) {
                this.viewRawJobButton.setVisible(false);
            }
            else {
                this.viewRawJobButton.setVisible(true);
            }
        }
    }

    /**
     * Set the underlying pojo for the form along with the edit mode.
     *
     * @param localEventJob
     */
    public void setJob(LocalEventJobInstance localEventJob) {
        this.enabled = false;
        this.localEventJobInstance = localEventJob;

        this.init();

        this.formBinder.readBean(this.localEventJobInstance);

        if(this.localEventJobInstance.getScheduledProcessEvent() != null
            && ((ContextualisedScheduledProcessEvent)this.localEventJobInstance.getScheduledProcessEvent()).getCatalystEvent() != null) {
            ContextualisedScheduledProcessEvent catalystEvent = (ContextualisedScheduledProcessEvent) ((ContextualisedScheduledProcessEvent)this.localEventJobInstance.getScheduledProcessEvent())
                .getCatalystEvent();
            this.setCatalystJobs(catalystEvent);
        }

        // make sure all value are bound before calling set enabled
        this.setEnabled(this.enabled);
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
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-skipped"
                , UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.skipJob(this.localEventJobInstance.getIdentifier(), this.localEventJobInstance.getChildContextName(), true);
            this.updateJobState(this.localEventJobInstance, InstanceStatus.SKIPPED);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Job Plan Instance Id[%s], Skipped[%s]"
                , this.localEventJobInstance.getAgentName(), localEventJobInstance.getJobName(), localEventJobInstance.getContextName(), localEventJobInstance.getContextInstanceId(), true), this.authentication.getName());
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
            contextMachine.skipJob(this.localEventJobInstance.getIdentifier(), this.localEventJobInstance.getChildContextName(), false);
            this.updateJobState(this.localEventJobInstance, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Job Plan Instance Id[%s], Skipped[%s]"
                , this.localEventJobInstance.getAgentName(), localEventJobInstance.getJobName(), localEventJobInstance.getContextName(), localEventJobInstance.getContextInstanceId(), false), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.enabled-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Method to hold a job in the system.
     *
     * @return true if the job was successfully put on hold, false otherwise
     */
    private boolean holdJob() {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-held", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.holdJob(this.localEventJobInstance.getIdentifier(), this.localEventJobInstance.getChildContextName());
            this.updateJobState(this.localEventJobInstance, InstanceStatus.ON_HOLD);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_HELD, String.format("Agent Name[%s], Scheduled Job Name[%s], Held[%s], Job Plan Name[%s], Job Plan Id[%s]"
                , this.localEventJobInstance.getAgentName(), localEventJobInstance.getJobName(), true, localEventJobInstance.getContextName()
                , localEventJobInstance.getContextInstanceId()), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.held-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Release the job instance handled by this method.
     *
     * @return true if the job was successfully released, false otherwise
     */
    private boolean releaseJob() {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId
            (this.schedulerJobInstanceRecord.getContextInstanceId());

        if(contextMachine == null) {
            NotificationHelper.showErrorNotification(getTranslation("error.not-active-context-released", UI.getCurrent().getLocale()));
            return false;
        }

        try {
            contextMachine.releaseJob(this.localEventJobInstance.getIdentifier(), this.localEventJobInstance.getChildContextName());
            this.updateJobState(this.localEventJobInstance, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_RELEASED, String.format("Agent Name[%s], Scheduled Job Name[%s], Released[%s], Job Plan Name[%s], Job Plan Id[%s]"
                , this.localEventJobInstance.getAgentName(), this.localEventJobInstance.getJobName(), true, this.localEventJobInstance.getContextName()
                , this.localEventJobInstance.getContextInstanceId()), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.released-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to update a jobs state and persist it before broadcasting the state change.
     *
     * @param localEventJobInstance
     * @param newStatus
     */
    private void updateJobState(LocalEventJobInstance localEventJobInstance, InstanceStatus newStatus) {
        InstanceStatus previousStatus = localEventJobInstance.getStatus();
        localEventJobInstance.setStatus(newStatus);
        this.updateScheduledJob(localEventJobInstance, this.authentication);

        SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
            = new SchedulerJobInstanceStateChangeEventImpl(localEventJobInstance,
            this.contextInstance, previousStatus, newStatus);

        SchedulerJobStateChangeEventBroadcaster.broadcast(schedulerJobInstanceStateChangeEvent);
    }

    /**
     * Update and persist the associated job.
     *
     * @param localEventJobInstance
     */
    public void updateScheduledJob(LocalEventJobInstance localEventJobInstance, IkasanAuthentication authentication) {

        this.schedulerJobInstanceRecord.setModifiedTimestamp(System.currentTimeMillis());
        this.schedulerJobInstanceRecord.setSchedulerJobInstance(localEventJobInstance);
        this.schedulerJobInstanceRecord.setModifiedBy(authentication.getName());
        this.schedulerJobInstanceRecord.setStatus(localEventJobInstance.getStatus().name());

        this.schedulerJobInstanceService.save(this.schedulerJobInstanceRecord);
    }

    private void setButtonVisibility() {
        if(this.localEventJobInstance.getStatus().equals(InstanceStatus.RUNNING)) {
            this.submitButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(false);
        }
        else if(this.localEventJobInstance.getStatus().equals(InstanceStatus.ERROR)) {
            this.submitButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(false);
        }
        else if(this.localEventJobInstance.getStatus().equals(InstanceStatus.WAITING)) {
            this.submitButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.skipButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.holdButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.releaseButton.setVisible(false);
            this.enableButton.setVisible(false);
        }
        else if(this.localEventJobInstance.getStatus().equals(InstanceStatus.ON_HOLD)) {
            this.submitButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(this.authentication, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        }
        else if(this.localEventJobInstance.getStatus().equals(InstanceStatus.SKIPPED) ||
            this.localEventJobInstance.getStatus().equals(InstanceStatus.SKIPPED_RUNNING) ||
            this.localEventJobInstance.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE)) {
            this.submitButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(false);
        }
        else if(this.localEventJobInstance.getStatus().equals(InstanceStatus.COMPLETE)) {
            this.submitButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
            this.holdButton.setVisible(false);
            this.releaseButton.setVisible(false);
        }
    }

    private void setCatalystJobs(ContextualisedScheduledProcessEvent catalystEvent) {
        this.catalystJobNameTf.setValue(catalystEvent.getJobName());
        this.catalystContextNameTf.setValue(catalystEvent.getContextName());
        this.catalystContextIdentifierTf.setValue(catalystEvent.getContextInstanceId());
        this.catalystJobFireTimeTf.setValue(DateFormatter.instance().getFormattedDate(catalystEvent.getFireTime()));
        this.catalystJobCompletionTimeTf.setValue(DateFormatter.instance().getFormattedDate(catalystEvent.getCompletionTime()));
    }

    public void setJob(SchedulerJobInstanceRecord schedulerJobRecord) {
        this.schedulerJobInstanceRecord = schedulerJobRecord;
        this.setJob((LocalEventJobInstance) this.schedulerJobInstanceService.findById(schedulerJobRecord.getId())
            .getSchedulerJobInstance());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();
        SchedulerJobStateChangeEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        this.ui = null;
        SchedulerJobStateChangeEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent jobInstanceStateChangeEvent) {
        if (jobInstanceStateChangeEvent.getSchedulerJobInstance() != null
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextInstanceId().equals(this.localEventJobInstance.getContextInstanceId())
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName().equals(this.localEventJobInstance.getChildContextName())
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName().equals(this.localEventJobInstance.getJobName())) {
            if(this.ui.isAttached()) {
                this.ui.access(() -> {
                    this.localEventJobInstance.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                    this.statusDiv.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                    this.setButtonVisibility();

                    if(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent() != null) {
                        ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent =
                            (ContextualisedScheduledProcessEvent) jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent();

                        if(contextualisedScheduledProcessEvent.getCatalystEvent() != null) {
                            this.localEventJobInstance.setScheduledProcessEvent(contextualisedScheduledProcessEvent);
                            setCatalystJobs((ContextualisedScheduledProcessEvent) contextualisedScheduledProcessEvent.getCatalystEvent());

                            this.setEnabled(this.enabled);

                            this.submitButton.setVisible(!this.localEventJobInstance.getStatus().equals(InstanceStatus.COMPLETE));
                        }
                    }
                });
            }
        }
    }
}
