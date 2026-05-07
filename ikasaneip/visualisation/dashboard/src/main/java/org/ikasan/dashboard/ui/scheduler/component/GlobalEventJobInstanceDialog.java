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
import com.vaadin.flow.router.RouteConfiguration;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.orchestration.service.context.global.GlobalEventServiceImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.GlobalEventJobInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class GlobalEventJobInstanceDialog extends AbstractCloseableResizableDialog implements SchedulerJobStateChangeEventLocalBroadcastListener {

    Logger logger = LoggerFactory.getLogger(GlobalEventJobInstanceDialog.class);

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextField jobNameAliasTf;
    private TextArea jobDescriptionTa;

    private TextField catalystJobNameTf;
    private TextField catalystContextNameTf;
    private TextField catalystContextIdentifierTf;
    private TextField catalystJobFireTimeTf;
    private TextField catalystJobCompletionTimeTf;
    private Icon openCatalystJobButton;
    private Icon viewRawJobButton;
    private Icon viewProcessExecutionButton;

    private Button submitButton;
    private Button skipButton;
    private Button enableButton;

    private IkasanAuthentication authentication;
    private GlobalEventJobInstance globalEventJob;
    private Binder<GlobalEventJobInstance> formBinder;
    private EditMode editMode = EditMode.NEW;
    private FormLayout formLayout;
    private boolean enabled = true;
    private SystemEventLogger systemEventLogger;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;
    private SchedulerStatusDiv statusDiv;
    private GlobalEventService globalEventService;
    private ContextInstance contextInstance;
    private UI ui;

    /**
     * Constructor
     *
     * @param systemEventLogger
     * @param schedulerJobInstanceService
     * @param globalEventService
     * @param contextInstance
     */
    public GlobalEventJobInstanceDialog(SystemEventLogger systemEventLogger, SchedulerJobInstanceService schedulerJobInstanceService,
                                        GlobalEventService globalEventService, ContextInstance contextInstance) {
        super.showResize(false);
        super.title.setText(getTranslation("header.global-job", UI.getCurrent().getLocale()));

        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger ==  null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if(this.schedulerJobInstanceService ==  null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.globalEventService = globalEventService;
        if(this.globalEventService ==  null) {
            throw new IllegalArgumentException("globalEventService cannot be null!");
        }
        this.contextInstance = contextInstance;
        if(this.contextInstance ==  null) {
            throw new IllegalArgumentException("contextInstance cannot be null!");
        }

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }

    private void init() {
        this.formBinder = new Binder<>(GlobalEventJobInstance.class);

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
                    this.globalEventJob.setStatus(InstanceStatus.SKIPPED);
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
                    this.globalEventJob.setStatus(InstanceStatus.WAITING);
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
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-global-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.submit-global-job", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                try {
                    GlobalEventJobInstance globalEventJobInstance = (GlobalEventJobInstance)schedulerJobInstanceRecord
                        .getSchedulerJobInstance();

                    this.globalEventService.raiseGlobalEventJob(globalEventJobInstance,
                        this.contextInstance.getId(), SecurityContextHolder.getContext().getAuthentication().getName());

                    this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Job Plan Instance Id[%s]"
                            , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName()
                            , schedulerJobInstanceRecord.getSchedulerJobInstance().getContextName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getContextInstanceId())
                        , this.authentication.getName());

                    globalEventJobInstance.setStatus(InstanceStatus.COMPLETE);
                    schedulerJobInstanceRecord.setSchedulerJobInstance(globalEventJobInstance);
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
        actionsLayout.add(this.skipButton, this.enableButton, this.submitButton);
        actionsLayout.setMargin(false);

        VerticalLayout actionsButtonLayout = new VerticalLayout();
        actionsButtonLayout.setWidth("100%");
        actionsButtonLayout.add(actionsLayout);
        actionsButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, actionsLayout);
        actionsButtonLayout.setMargin(false);

        formLayout.add(actionsButtonLayout, 2);

        // Fields to capture schedule job properties.
        H3 globaljobLabel = new H3(getTranslation("header.global-job", UI.getCurrent().getLocale()));
        formLayout.add(globaljobLabel, 2);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setId("jobNameTf");
        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), getTranslation("error.missing-job-name", UI.getCurrent().getLocale()))
            .bind(GlobalEventJob::getJobName, GlobalEventJob::setJobName);
        formLayout.add(jobNameTf, 2);


        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(GlobalEventJob::getJobDescription, GlobalEventJob::setJobDescription);

        if(this.contextInstance.isUseDisplayName()) {
            this.jobNameAliasTf = new TextField(getTranslation("label.job-name-alias", UI.getCurrent().getLocale()));
            this.jobNameAliasTf.setId("jobNameAliasTf");
            this.jobNameAliasTf.setRequired(false);
            this.jobNameAliasTf.setEnabled(false);
            formBinder.forField(this.jobNameAliasTf)
                .bind(GlobalEventJob::getDisplayName, GlobalEventJob::setDisplayName);
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

        this.openCatalystJobButton = IconDecorator.decorate(VaadinIcon.EXTERNAL_LINK.create(), getTranslation("tooltip.open-catalyst-job", UI.getCurrent().getLocale())
            , "16pt", IkasanColours.IKASAN_ORANGE);
        openCatalystJobButton.addClickListener(event -> {
            ContextualisedScheduledProcessEvent catalystEvent = (ContextualisedScheduledProcessEvent) ((ContextualisedScheduledProcessEvent)this.globalEventJob.getScheduledProcessEvent())
                .getCatalystEvent();
            String route = RouteConfiguration.forSessionScope()
                .getUrl(ContextInstanceView.class, List.of("job", catalystEvent.getContextInstanceId() +"_scheduledContextInstance"
                    , ContextInstanceWidget.JOB_INSTANCE_TAB, catalystEvent.getJobName()));

            getUI().ifPresent(ui -> ui.getPage().open(route));
        });

        this.viewRawJobButton = IconDecorator.decorate(VaadinIcon.CALENDAR_CLOCK.create(), getTranslation("tooltip.open-raw-catalyst-job-event", UI.getCurrent().getLocale())
            , "16pt", IkasanColours.IKASAN_ORANGE);
        viewRawJobButton.addClickListener(event -> {
            ContextualisedScheduledProcessEvent catalystEvent = (ContextualisedScheduledProcessEvent) ((ContextualisedScheduledProcessEvent)this.globalEventJob.getScheduledProcessEvent())
                .getCatalystEvent();

            JsonViewerDialog viewerDialog = new JsonViewerDialog(catalystEvent, getTranslation("header.catalyst-scheduled-process-event", UI.getCurrent().getLocale()));
            viewerDialog.open();
        });

        this.viewProcessExecutionButton = IconDecorator.decorate(VaadinIcon.COG.create(), getTranslation("tooltip.open-raw-catalyst-job-event", UI.getCurrent().getLocale())
            , "16pt", IkasanColours.IKASAN_ORANGE);
        viewProcessExecutionButton.addClickListener(event -> {
            ContextualisedScheduledProcessEvent catalystEvent = (ContextualisedScheduledProcessEvent) ((ContextualisedScheduledProcessEvent)this.globalEventJob.getScheduledProcessEvent())
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
            , viewProcessExecutionButton, openCatalystJobButton);
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

        if(this.globalEventJob.getScheduledProcessEvent() == null
            || ((ContextualisedScheduledProcessEvent)this.globalEventJob.getScheduledProcessEvent()).getCatalystEvent() == null) {
            this.viewProcessExecutionButton.setVisible(false);
            this.viewRawJobButton.setVisible(false);
            this.openCatalystJobButton.setVisible(false);
        }
        else {
            this.viewProcessExecutionButton.setVisible(true);
            if(((ContextualisedScheduledProcessEvent)((ContextualisedScheduledProcessEvent)this.globalEventJob
                .getScheduledProcessEvent()).getCatalystEvent()).getContextName().equals(GlobalEventServiceImpl.GLOBAL_EVENT_MANUALLY_RAISED)) {
                this.openCatalystJobButton.setVisible(false);
                this.viewRawJobButton.setVisible(false);
            }
            else {
                this.openCatalystJobButton.setVisible(true);
                this.viewRawJobButton.setVisible(true);
            }
        }
    }

    /**
     * Set the underlying pojo for the form along with the edit mode.
     *
     * @param globalEventJob
     */
    public void setJob(GlobalEventJobInstance globalEventJob) {
        this.enabled = false;
        this.globalEventJob = globalEventJob;

        this.init();

        this.formBinder.readBean(this.globalEventJob);

        if(this.globalEventJob.getScheduledProcessEvent() != null
            && ((ContextualisedScheduledProcessEvent)this.globalEventJob.getScheduledProcessEvent()).getCatalystEvent() != null) {
            ContextualisedScheduledProcessEvent catalystEvent = (ContextualisedScheduledProcessEvent) ((ContextualisedScheduledProcessEvent)this.globalEventJob.getScheduledProcessEvent())
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
            contextMachine.skipJob(this.globalEventJob.getIdentifier(), this.globalEventJob.getChildContextName(), true);
            this.updateJobState(this.globalEventJob, InstanceStatus.SKIPPED);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Job Plan Instance Id[%s], Skipped[%s]"
                , this.globalEventJob.getAgentName(), globalEventJob.getJobName(), globalEventJob.getContextName(), globalEventJob.getContextInstanceId(), true), this.authentication.getName());
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
            contextMachine.skipJob(this.globalEventJob.getIdentifier(), this.globalEventJob.getChildContextName(), false);
            this.updateJobState(this.globalEventJob, InstanceStatus.WAITING);

            this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SKIPPED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Job Plan Instance Id[%s], Skipped[%s]"
                , this.globalEventJob.getAgentName(), globalEventJob.getJobName(), globalEventJob.getContextName(), globalEventJob.getContextInstanceId(), false), this.authentication.getName());
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("error.enabled-general-error", UI.getCurrent().getLocale()));
            return false;
        }

        return true;
    }

    /**
     * Helper method to update a jobs state and persist it before broadcasting the state change.
     *
     * @param globalEventJobInstance
     * @param newStatus
     */
    private void updateJobState(GlobalEventJobInstance globalEventJobInstance, InstanceStatus newStatus) {
        InstanceStatus previousStatus = globalEventJobInstance.getStatus();
        globalEventJobInstance.setStatus(newStatus);
        this.updateScheduledJob(globalEventJobInstance, this.authentication);

        SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent
            = new SchedulerJobInstanceStateChangeEventImpl(globalEventJobInstance,
            this.contextInstance, previousStatus, newStatus);

        SchedulerJobStateChangeEventBroadcaster.broadcast(schedulerJobInstanceStateChangeEvent);
    }

    /**
     * Update and persist the associated job.
     *
     * @param globalJobInstance
     */
    public void updateScheduledJob(GlobalEventJobInstance globalJobInstance, IkasanAuthentication authentication) {

        this.schedulerJobInstanceRecord.setModifiedTimestamp(System.currentTimeMillis());
        this.schedulerJobInstanceRecord.setSchedulerJobInstance(globalJobInstance);
        this.schedulerJobInstanceRecord.setModifiedBy(authentication.getName());
        this.schedulerJobInstanceRecord.setStatus(globalJobInstance.getStatus().name());

        this.schedulerJobInstanceService.save(this.schedulerJobInstanceRecord);
    }

    private void setButtonVisibility() {
        if(this.globalEventJob.getStatus().equals(InstanceStatus.RUNNING)) {
            this.submitButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
        }
        else if(this.globalEventJob.getStatus().equals(InstanceStatus.ERROR)) {
            this.submitButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
        }
        else if(this.globalEventJob.getStatus().equals(InstanceStatus.WAITING)) {
            this.submitButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.skipButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            this.enableButton.setVisible(false);
        }
        else if(this.globalEventJob.getStatus().equals(InstanceStatus.ON_HOLD)) {
            this.submitButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
        }
        else if(this.globalEventJob.getStatus().equals(InstanceStatus.SKIPPED) ||
            this.globalEventJob.getStatus().equals(InstanceStatus.SKIPPED_RUNNING) ||
            this.globalEventJob.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE)) {
            this.submitButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(true &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        }
        else if(this.globalEventJob.getStatus().equals(InstanceStatus.COMPLETE)) {
            this.submitButton.setVisible(false);
            this.skipButton.setVisible(false);
            this.enableButton.setVisible(false);
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
        this.setJob((GlobalEventJobInstance) this.schedulerJobInstanceService.findById(schedulerJobRecord.getId())
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
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextInstanceId().equals(this.globalEventJob.getContextInstanceId())
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName().equals(this.globalEventJob.getChildContextName())
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName().equals(this.globalEventJob.getJobName())) {
            if(this.ui.isAttached()) {
                this.ui.access(() -> {
                    this.globalEventJob.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                    this.statusDiv.setStatus(jobInstanceStateChangeEvent.getNewStatus());

                    if(jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent() != null) {
                        ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent =
                            (ContextualisedScheduledProcessEvent) jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent();

                        if(contextualisedScheduledProcessEvent.getCatalystEvent() != null) {
                            this.globalEventJob.setScheduledProcessEvent(contextualisedScheduledProcessEvent);
                            setCatalystJobs((ContextualisedScheduledProcessEvent) contextualisedScheduledProcessEvent.getCatalystEvent());

                            this.setEnabled(this.enabled);

                            this.submitButton.setVisible(!this.globalEventJob.getStatus().equals(InstanceStatus.COMPLETE));
                        }
                    }
                });
            }
        }
    }
}
