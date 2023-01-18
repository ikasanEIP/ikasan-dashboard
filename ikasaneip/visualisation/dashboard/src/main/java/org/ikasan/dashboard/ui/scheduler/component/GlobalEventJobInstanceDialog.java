package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
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
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
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

public class GlobalEventJobInstanceDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(GlobalEventJobInstanceDialog.class);

    private Registration schedulerJobStateChangeRegistration;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextArea jobDescriptionTa;
    private Button submitButton;
    private IkasanAuthentication authentication;


//    private ScheduledProcessManagementService scheduledProcessManagementService;
//    private ConfigurationService configurationRestService;
//    private ModuleMetaData agent;
//    private ModuleControlService moduleControlRestService;
//    private MetaDataService metaDataRestService;
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

        this.setHeight("500px");
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

        this.submitButton = new Button(getTranslation("button.submit", UI.getCurrent().getLocale()), new Icon(VaadinIcon.PAPERPLANE));
        this.submitButton.setIconAfterText(true);

        this.submitButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-header.submit-global-job", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog-text.submit-global-job", UI.getCurrent().getLocale()));

            confirmDialog.setCancelable(true);

            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                try {
                    GlobalEventJobInstance globalEventJobInstance = (GlobalEventJobInstance)schedulerJobInstanceRecord
                        .getSchedulerJobInstance();

                    this.globalEventService.raiseGlobalEventJob(globalEventJobInstance,
                        this.contextInstance.getId());

                    this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_SUBMITTED, String.format("Agent Name[%s], Scheduled Job Name[%s]"
                            , schedulerJobInstanceRecord.getSchedulerJobInstance().getAgentName(), schedulerJobInstanceRecord.getSchedulerJobInstance().getJobName())
                        , this.authentication.getName());

                    globalEventJobInstance.setStatus(InstanceStatus.COMPLETE);
                    schedulerJobInstanceRecord.setSchedulerJobInstance(globalEventJobInstance);
                    schedulerJobInstanceRecord.setStatus(InstanceStatus.COMPLETE.toString());
                    schedulerJobInstanceRecord.setManuallySubmittedBy(this.authentication.getName());
                    schedulerJobInstanceService.save(schedulerJobInstanceRecord);

                    NotificationHelper.showUserNotification(getTranslation("notification.job-submitted-successfully", UI.getCurrent().getLocale()));
                } catch (Exception e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("error.job-submission-error", UI.getCurrent().getLocale()));
                }
            });
        });

        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.add(this.submitButton);
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
        this.jobNameTf.setRequired(true);
        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), getTranslation("error.missing-job-name", UI.getCurrent().getLocale()))
            .bind(GlobalEventJob::getJobName, GlobalEventJob::setJobName);
        formLayout.add(jobNameTf, 2);


        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setRequired(true);
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(GlobalEventJob::getJobDescription, GlobalEventJob::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);


        return formLayout;
    }

    /**
     * Helper method to set controls on the form elements if the form is read only
     * or editable.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        this.jobDescriptionTa.setEnabled(enabled &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
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

        // make sure all value are bound before calling set enabled
        this.setEnabled(this.enabled);
    }

    public void setJob(SchedulerJobInstanceRecord schedulerJobRecord) {
        this.schedulerJobInstanceRecord = schedulerJobRecord;
        this.setJob((GlobalEventJobInstance) this.schedulerJobInstanceService.findById(schedulerJobRecord.getId())
            .getSchedulerJobInstance());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        schedulerJobStateChangeRegistration = SchedulerJobStateChangeEventBroadcaster.register(jobInstanceStateChangeEvent -> {
            if (jobInstanceStateChangeEvent.getSchedulerJobInstance() != null
                && jobInstanceStateChangeEvent.getSchedulerJobInstance().getContextInstanceId().equals(this.globalEventJob.getContextInstanceId())
                && jobInstanceStateChangeEvent.getSchedulerJobInstance().getChildContextName().equals(this.globalEventJob.getChildContextName())
                && jobInstanceStateChangeEvent.getSchedulerJobInstance().getJobName().equals(this.globalEventJob.getJobName())) {
                if(ui.isAttached()) {
                    ui.access(() -> {
                        this.globalEventJob.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                        this.statusDiv.setStatus(jobInstanceStateChangeEvent.getNewStatus());
                    });
                }
            }
        });
    }
}
