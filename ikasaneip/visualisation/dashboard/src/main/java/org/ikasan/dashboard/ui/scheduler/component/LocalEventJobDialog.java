package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.model.job.LocalEventJobImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.LocalEventJob;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalEventJobDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(LocalEventJobDialog.class);

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextArea jobDescriptionTa;
    private Button saveButton;
    private Button cancelButton;

    private LocalEventJob localEventJob;
    private Binder<LocalEventJob> formBinder;
    private EditMode editMode = EditMode.NEW;
    private FormLayout formLayout;
    private boolean enabled = true;
    private SystemEventLogger systemEventLogger;
    private SchedulerJobService schedulerJobService;
    private ContextTemplate parentContext;
    private ContextTemplate contextTemplate;
    private List<SchedulerJobSelectedListener> schedulerJobSelectedListeners = new ArrayList<>();


    /**
     * Constructor
     *
     * @param systemEventLogger
     */
    public LocalEventJobDialog(SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                               ContextTemplate parentContext, ContextTemplate contextTemplate) {
        super.showResize(false);
        super.title.setText(getTranslation("header.local-event-job", UI.getCurrent().getLocale()));

        this.systemEventLogger = systemEventLogger;
        this.schedulerJobService = schedulerJobService;
        this.parentContext = parentContext;
        this.contextTemplate = contextTemplate;
        this.localEventJob = new LocalEventJobImpl();

        this.formBinder = new Binder<>(LocalEventJob.class);

        this.setHeight("500px");
        this.setWidth("90vw");

        saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.setId("globalEventJobSaveButton");
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->  {

            IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

            if(!this.performFormValidation(this.localEventJob)) {
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-configuration", UI.getCurrent().getLocale()));
                return;
            }

            try {
                createOrUpdateScheduledJob(this.localEventJob, authentication);
            }
            catch (Exception e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-creation", UI.getCurrent().getLocale()));
                return;
            }

            if (this.editMode == EditMode.NEW) {
                String action = String.format("New local event job created [%s].", this.localEventJob);
                this.systemEventLogger.logEvent(SystemEventConstants.NEW_SCHEDULED_JOB_CREATED, action, authentication.getName());
            }
            else if (this.editMode == EditMode.EDIT) {
                // todo need to sort out action
                String action = String.format("Local event job edited. \nBefore [%s]\nAfter [%s].", this.localEventJob,
                    this.localEventJob);
                this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_EDIT, action, authentication.getName());
            }

            this.schedulerJobSelectedListeners.forEach(listener -> listener.jobSelected(this.localEventJob));

            this.close();
            NotificationHelper.showErrorNotification(getTranslation("notification.scheduler-job-saved", UI.getCurrent().getLocale()));
        });

        ComponentSecurityVisibility.applySecurity(saveButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        cancelButton = new Button(getTranslation("button.close", UI.getCurrent().getLocale()));
        cancelButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(true);
        buttonLayout.setSpacing(true);
        buttonLayout.add(saveButton, cancelButton);
        buttonLayout.getStyle().set("padding-bottom", "20px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(this.createConfigurationForm());
        layout.getStyle().set("padding-bottom", "20px");
        super.content.add(layout);
        super.content.add(buttonLayout);
        super.content.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);
    }

    /**
     * Initialise the form.
     *
     * @return
     */
    private FormLayout createConfigurationForm() {
        formLayout = new FormLayout();

        // Fields to capture schedule job properties.
        H3 globaljobLabel = new H3(getTranslation("header.local-event-job", UI.getCurrent().getLocale()));
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
            .withValidator(jobName -> !jobName.contains(" "), getTranslation("error.job-name-cannot-contain-whitespace", UI.getCurrent().getLocale()))
            .bind(LocalEventJob::getJobName, LocalEventJob::setJobName);
        formLayout.add(jobNameTf, 2);


        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setRequired(true);
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(LocalEventJob::getJobDescription, LocalEventJob::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);

        return formLayout;
    }

    /**
     * Perform validation of the form.
     *
     * @param localEventJob
     * @return
     */
    private boolean performFormValidation(LocalEventJob localEventJob) {

        try {
            AtomicBoolean isValid = new AtomicBoolean(true);
            formBinder.writeBean(localEventJob);

            if(this.editMode.equals(EditMode.NEW) || this.editMode.equals(EditMode.CLONE) || this.editMode.equals(EditMode.FROM_TEMPLATE)) {
                if(this.schedulerJobService.findByContextNameAndJobName
                    (this.parentContext.getName(), localEventJob.getJobName()) != null ||
                    this.contextTemplate.getScheduledJobs().stream()
                        .filter(job -> localEventJob.getJobName().equals(job.getJobName()))
                        .findFirst().isPresent()) {
                    isValid.set(false);
                    this.jobNameTf.setErrorMessage(getTranslation("error.job-name-exists", UI.getCurrent().getLocale()));
                    this.jobNameTf.setInvalid(true);
                }
            }

            if(!isValid.get()){
                return false;
            }
        }
        catch (ValidationException e) {
            return false;
        }

        return true;
    }
    


    /**
     * Creates or updates a scheduled job.
     *
     * @param localEventJob The LocalEventJob to be created or updated.
     * @param authentication The IkasanAuthentication object used for authorization.
     */
    public void createOrUpdateScheduledJob(LocalEventJob localEventJob, IkasanAuthentication authentication) {
        localEventJob.setIdentifier(localEventJob.getAgentName()+"-"+ localEventJob.getJobName());
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

        this.saveButton.setVisible(enabled &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        this.cancelButton.setVisible(enabled);
    }

    /**
     * Set the underlying pojo for the form along with the edit mode.
     *
     * @param localEventJob
     * @param editMode
     */
    public void setJob(LocalEventJob localEventJob, EditMode editMode) {
        this.enabled = editMode == EditMode.NEW || editMode == EditMode.EDIT ? true : false;
        this.localEventJob = localEventJob;
        this.formBinder.readBean(this.localEventJob);
        this.editMode = editMode;

        // make sure all value are bound before calling set enabled
        this.setEnabled(this.enabled);
    }

    public void addSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.schedulerJobSelectedListeners.add(listener);
    }
}
