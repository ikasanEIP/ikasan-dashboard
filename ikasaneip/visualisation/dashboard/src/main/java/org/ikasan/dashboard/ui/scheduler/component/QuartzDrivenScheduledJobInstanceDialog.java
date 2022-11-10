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
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.QuartzScheduleDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;

public class QuartzDrivenScheduledJobInstanceDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(QuartzDrivenScheduledJobInstanceDialog.class);

    private Registration schedulerJobStateChangeRegistration;

    private TextField agentTf;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
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

    /**
     * Constructor
     *
     * @param agent
     * @param jobInitiationService
     * @param systemEventLogger
     * @param schedulerJobInstanceService
     */
    public QuartzDrivenScheduledJobInstanceDialog(ModuleMetaData agent, JobInitiationService jobInitiationService, SystemEventLogger systemEventLogger,
                                                  SchedulerJobInstanceService schedulerJobInstanceService) {
        super.showResize(false);
        super.title.setText(getTranslation("label.scheduled-job", UI.getCurrent().getLocale()));

        this.agent = agent;
        this.jobInitiationService = jobInitiationService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerJobInstanceService = schedulerJobInstanceService;

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
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-quartz-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.submit-quartz-job", UI.getCurrent().getLocale()));

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                try {
                    this.jobInitiationService.raiseQuartzSchedulerJob(this.agent.getUrl(), this.agent.getName(), this.quartzScheduleDrivenJobInstance.getJobName());

                    this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s]"
                        , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName())
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

        this.viewProcessEventButton = new Button(getTranslation
            ("button.view-event", UI.getCurrent().getLocale()), VaadinIcon.CALENDAR_CLOCK.create());
        this.viewProcessEventButton.setVisible(this.scheduledProcessEvent != null);
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
        formLayout.add(jobDescriptionTa, 2);


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
        UI ui = attachEvent.getUI();
        schedulerJobStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(jobInstanceStateChangeEvent -> {
            if (jobInstanceStateChangeEvent.getSchedulerJobInstance() != null
                && jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextInstanceId().equals(this.quartzScheduleDrivenJobInstance.getContextInstanceId())
                && jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName().equals(this.quartzScheduleDrivenJobInstance.getJobName())) {
                this.scheduledProcessEvent = jobInstanceStateChangeEvent.getSchedulerJobInstance().getScheduledProcessEvent();

                ui.access(() -> {
                    this.quartzScheduleDrivenJobInstance.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                    this.statusDiv.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                    this.viewProcessEventButton.setVisible(this.scheduledProcessEvent != null);
                });
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.schedulerJobStateChangeRegistration.remove();
        this.schedulerJobStateChangeRegistration = null;
    }
}
