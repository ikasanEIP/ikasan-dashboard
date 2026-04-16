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
import com.vaadin.flow.server.StreamResource;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.QuartzScheduleDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;

public class QuartzDrivenScheduledJobInstanceDialog extends AbstractCloseableResizableDialog implements SchedulerJobStateChangeEventLocalBroadcastListener {

    Logger logger = LoggerFactory.getLogger(QuartzDrivenScheduledJobInstanceDialog.class);

    private TextField agentTf;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextField jobNameAliasTf;
    private TextArea jobDescriptionTa;
    private TextField cronExpressionTf;
    private TextField timezoneCb;

    private ModuleMetaData agent;

    private QuartzScheduleDrivenJobInstance quartzScheduleDrivenJobInstance;

    private Binder<QuartzScheduleDrivenJobInstance> formBinder;

    private FormLayout formLayout;

    private SystemEventLogger systemEventLogger;

    private SchedulerJobInstanceService schedulerJobInstanceService;

    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;

    private SchedulerStatusDiv statusDiv;

    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private JobInitiationService jobInitiationService;

    private IkasanAuthentication authentication;

    private Button viewProcessEventButton;

    private ScheduledProcessEvent scheduledProcessEvent;
    private ContextInstance contextInstance;
    private UI ui;

    /**
     * Constructor
     *
     * @param agent
     * @param jobInitiationService
     * @param systemEventLogger
     * @param schedulerJobInstanceService
     */
    public QuartzDrivenScheduledJobInstanceDialog(ModuleMetaData agent, JobInitiationService jobInitiationService, SystemEventLogger systemEventLogger,
                                                  SchedulerJobInstanceService schedulerJobInstanceService, ContextInstance contextInstance) {
        super.showResize(false);
        super.title.setText(getTranslation("label.scheduled-job", UI.getCurrent().getLocale()));

        this.agent = agent;
        this.jobInitiationService = jobInitiationService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.contextInstance = contextInstance;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }

    private void init() {
        this.formBinder
            = new Binder<>(QuartzScheduleDrivenJobInstance.class);

        this.setHeight("550px");
        this.setWidth("90vw");

        VerticalLayout layout = new VerticalLayout();
        layout.getStyle().set("padding-top", "0px");
        layout.getStyle().set("padding-bottom", "10px");
        layout.setSizeFull();
        layout.add(this.createConfigurationForm());
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

        Button submitButton = new Button(getTranslation("button.submit", UI.getCurrent().getLocale()), new Icon(VaadinIcon.PAPERPLANE));
        submitButton.setIconAfterText(true);
        submitButton.getElement().setAttribute("title", "Submit Job");

        submitButton.addClickListener(event -> {
            if(!this.canPerformAction()) {
                return;
            }
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-quartz-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.submit-quartz-job", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                try {
                    this.jobInitiationService.raiseQuartzSchedulerJob(
                        this.agent.getUrl(), this.agent.getName(), this.quartzScheduleDrivenJobInstance, this.quartzScheduleDrivenJobInstance.getContextInstanceId());

                    this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s], Job Plan Name[%s], Job Plan Instance Id[%s]"
                        , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName()
                        , this.schedulerJobInstanceRecord.getSchedulerJobInstance().getContextName(), this.schedulerJobInstanceRecord.getSchedulerJobInstance().getContextInstanceId())
                        , this.authentication.getName());

                    this.schedulerJobInstanceRecord.setManuallySubmittedBy(authentication.getName());
                    this.schedulerJobInstanceService.save(this.schedulerJobInstanceRecord);

                    NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                }
            });
        });

        ComponentSecurityVisibility.applySecurity(submitButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        this.viewProcessEventButton = new Button(getTranslation
            ("button.view-event", UI.getCurrent().getLocale()), VaadinIcon.CALENDAR_CLOCK.create());
        this.viewProcessEventButton.setVisible(this.scheduledProcessEvent != null &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ));

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
        actionsLayout.add(submitButton, this.viewProcessEventButton, exportWrapper);
        actionsLayout.setMargin(false);
        actionsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, submitButton);
        actionsLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, exportWrapper);

        VerticalLayout actionsButtonLayout = new VerticalLayout();
        actionsButtonLayout.setWidth("100%");
        actionsButtonLayout.add(actionsLayout);
        actionsButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, actionsLayout);
        actionsButtonLayout.setMargin(false);

        // Fields to capture schedule job properties.
        H3 scheduleDetailsLabel = new H3(getTranslation("header.schedule-details", UI.getCurrent().getLocale()));
        formLayout.add(scheduleDetailsLabel, actionsButtonLayout);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setId("jobNameTf");
        this.jobNameTf.setRequired(true);
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), getTranslation("error.missing-job-name", UI.getCurrent().getLocale()))
            .bind(QuartzScheduleDrivenJobInstance::getJobName, QuartzScheduleDrivenJobInstance::setJobName);
        formLayout.add(jobNameTf);


        this.agentTf = new TextField(getTranslation("label.agent", UI.getCurrent().getLocale()));
        this.agentTf.setId("agentCb");
        this.agentTf.setRequired(true);
        this.agentTf.setClearButtonVisible(true);
        formBinder.forField(this.agentTf)
            .withValidator(agentValue -> !agentValue.isEmpty(), getTranslation("error.missing-agent", UI.getCurrent().getLocale()))
            .bind(QuartzScheduleDrivenJobInstance::getAgentName, QuartzScheduleDrivenJobInstance::setAgentName);
        formLayout.add(agentTf);


        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setRequired(true);
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(QuartzScheduleDrivenJobInstance::getJobDescription, QuartzScheduleDrivenJobInstance::setJobDescription);

        if(this.contextInstance.isUseDisplayName()) {
            this.jobNameAliasTf = new TextField(getTranslation("label.job-name-alias", UI.getCurrent().getLocale()));
            this.jobNameAliasTf.setId("jobNameAliasTf");
            this.jobNameAliasTf.setRequired(false);
            this.jobNameAliasTf.setEnabled(false);
            formBinder.forField(this.jobNameAliasTf)
                .bind(QuartzScheduleDrivenJob::getDisplayName, QuartzScheduleDrivenJob::setDisplayName);
            formLayout.add(jobNameAliasTf, jobDescriptionTa);
        }
        else {
            formLayout.add(jobDescriptionTa, 2);
        }

        this.cronExpressionTf = new TextField(getTranslation("label.cron-expression", UI.getCurrent().getLocale()));
        this.cronExpressionTf.setRequired(true);
        this.cronExpressionTf.setId("cronExpressionTf");
        formBinder.forField(this.cronExpressionTf)
            .withValidator(value -> !value.isEmpty(), getTranslation("error.missing-cron-expression", UI.getCurrent().getLocale()))
            .withValidator(value -> CronExpression.isValidExpression(value), getTranslation("error.invalid-cron-expression", UI.getCurrent().getLocale()))
            .bind(QuartzScheduleDrivenJobInstance::getCronExpression, QuartzScheduleDrivenJobInstance::setCronExpression);
        formLayout.add(cronExpressionTf);


        this.timezoneCb = new TextField(getTranslation("label.timezone", UI.getCurrent().getLocale()));
        ComboBox.ItemFilter<DateTimeUtil.TimezonePair> filter = (element, filterString) ->
            element.zoneId.toLowerCase().contains(filterString.toLowerCase());
        this.timezoneCb.setId("timezoneCb");
        this.timezoneCb.setClearButtonVisible(true);
        formBinder.forField(this.timezoneCb)
            .bind(QuartzScheduleDrivenJobInstance::getTimeZone, QuartzScheduleDrivenJobInstance::setTimeZone);
        this.timezoneCb.setPlaceholder(getTranslation("label.choose-a-timezone", UI.getCurrent().getLocale()));
        this.timezoneCb.setErrorMessage(getTranslation("error.timezone-required", UI.getCurrent().getLocale()));
        formLayout.add(timezoneCb);

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
            }
            else {
                NotificationHelper.showErrorNotification(getTranslation("error.cannot-locate-job-plan-instance-in-cache-and-is-not-ended"
                    , UI.getCurrent().getLocale()));
            }
            return false;
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
        this.timezoneCb.setEnabled(enabled);
        this.jobNameTf.setEnabled(enabled);
        this.agentTf.setEnabled(enabled);
        this.jobDescriptionTa.setEnabled(enabled);
        this.cronExpressionTf.setEnabled(enabled);
        this.timezoneCb.setEnabled(enabled);
    }

    /**
     * Set the underlying pojo for the form along with the edit mode.
     *
     * @param quartzScheduleDrivenJobInstance
     */
    private void setJob(QuartzScheduleDrivenJobInstance quartzScheduleDrivenJobInstance) {
        this.quartzScheduleDrivenJobInstance = quartzScheduleDrivenJobInstance;

        this.init();

        this.formBinder.readBean(this.quartzScheduleDrivenJobInstance);
        // make sure all value are bound before calling set enabled
        this.setEnabled(false);

        this.statusDiv.setStatus(this.quartzScheduleDrivenJobInstance.getStatus());
    }

    public void setJob(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        this.schedulerJobInstanceRecord = schedulerJobInstanceRecord;
        this.scheduledProcessEvent = schedulerJobInstanceRecord.getSchedulerJobInstance()
            .getScheduledProcessEvent();
        this.setJob((QuartzScheduleDrivenJobInstance) this.schedulerJobInstanceService
            .findById(schedulerJobInstanceRecord.getId()).getSchedulerJobInstance());
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
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextInstanceId().equals(this.quartzScheduleDrivenJobInstance.getContextInstanceId())
            && jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName().equals(this.quartzScheduleDrivenJobInstance.getJobName())) {
            this.scheduledProcessEvent = jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent();

            if(this.ui.isAttached()) {
                this.ui.access(() -> {
                    this.quartzScheduleDrivenJobInstance.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                    this.statusDiv.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                    this.viewProcessEventButton.setVisible(this.scheduledProcessEvent != null);
                });
            }
        }
    }
}
