package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
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
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.job.model.SolrGlobalEventJobImpl;
import org.ikasan.scheduled.job.model.SolrGlobalEventJobRecordImpl;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobRecordImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class GlobalEventJobDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(GlobalEventJobDialog.class);

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextArea jobDescriptionTa;
    private Button saveButton;
    private Button cancelButton;


    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleMetaData agent;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private GlobalEventJob globalEventJob;
    private Binder<GlobalEventJob> formBinder;
    private EditMode editMode = EditMode.NEW;
    private FormLayout formLayout;
    private boolean enabled = true;
    private SystemEventLogger systemEventLogger;
    private SchedulerJobService schedulerJobService;
    private SchedulerJobRecord schedulerJobRecord;
    private List<SchedulerJobSelectedListener> schedulerJobSelectedListeners = new ArrayList<>();


    /**
     * Constructor
     *
     * @param agent
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     */
    public GlobalEventJobDialog(ModuleMetaData agent, ScheduledProcessManagementService scheduledProcessManagementService,
                                ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService) {
        super.showResize(false);
        super.title.setText(getTranslation("header.global-job", UI.getCurrent().getLocale()));

        this.agent = agent;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerJobService = schedulerJobService;

        this.globalEventJob = new SolrGlobalEventJobImpl();


        this.formBinder = new Binder<>(GlobalEventJob.class);

        this.setHeight("500px");
        this.setWidth("90vw");

        saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.setId("globalEventJobSaveButton");
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->  {

            IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

            if(!this.performFormValidation(this.globalEventJob)) {
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-configuration", UI.getCurrent().getLocale()));
                return;
            }

            try {
                createOrUpdateScheduledJob(this.globalEventJob, authentication);
            }
            catch (Exception e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-creation", UI.getCurrent().getLocale()));
                return;
            }

            if (this.editMode == EditMode.NEW) {
                String action = String.format("New quartz scheduled job created [%s].", this.globalEventJob);
                this.systemEventLogger.logEvent(SystemEventConstants.NEW_SCHEDULED_JOB_CREATED, action, authentication.getName());
            }
            else if (this.editMode == EditMode.EDIT) {
                String action = String.format("Quartz scheduled job edited. \nBefore [%s]\nAfter [%s].", this.schedulerJobRecord.getJob(),
                    this.globalEventJob);
                this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_EDIT, action, authentication.getName());
            }

            this.schedulerJobSelectedListeners.forEach(listener -> listener.jobSelected(this.globalEventJob));

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
        layout.add(this.createConfigurationForm(), buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);
        layout.getStyle().set("padding-bottom", "20px");
        super.content.add(layout);
    }

    /**
     * Initialise the form.
     *
     * @return
     */
    private FormLayout createConfigurationForm() {
        formLayout = new FormLayout();

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
     * Perform validation of the form.
     *
     * @param globalEventJob
     * @return
     */
    private boolean performFormValidation(GlobalEventJob globalEventJob) {

        try {
            AtomicBoolean isValid = new AtomicBoolean(true);
            formBinder.writeBean(globalEventJob);

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
     *
     *
     * @param globalEventJob
     */
    public void createOrUpdateScheduledJob(GlobalEventJob globalEventJob, IkasanAuthentication authentication) throws JsonProcessingException {
        globalEventJob.setIdentifier(globalEventJob.getAgentName()+"-"+ globalEventJob.getJobName());

        GlobalEventJobRecord globalEventJobRecord = new SolrGlobalEventJobRecordImpl();
        globalEventJobRecord.setAgentName(globalEventJob.getAgentName());
//        globalEventJobRecord.setContextName(globalEventJob.getContextName());
        globalEventJobRecord.setJobName(globalEventJob.getJobName());
        globalEventJobRecord.setGlobalEventJob(globalEventJob);
        globalEventJobRecord.setModifiedBy(authentication.getName());

        if(this.schedulerJobRecord != null) {
            globalEventJobRecord.setTimestamp(this.schedulerJobRecord.getTimestamp());
        }
        else {
            globalEventJobRecord.setTimestamp(System.currentTimeMillis());
        }

        this.schedulerJobService.saveGlobalEventJobRecord(globalEventJobRecord);
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
     * @param globalEventJob
     * @param editMode
     */
    public void setJob(GlobalEventJob globalEventJob, EditMode editMode) {
        this.enabled = editMode == EditMode.NEW || editMode == EditMode.EDIT ? true : false;
        this.globalEventJob = globalEventJob;
        this.formBinder.readBean(this.globalEventJob);
        this.editMode = editMode;

        // make sure all value are bound before calling set enabled
        this.setEnabled(this.enabled);
    }

    public void setJob(SchedulerJobRecord schedulerJobRecord, EditMode editMode) {
        this.schedulerJobRecord = schedulerJobRecord;
        this.setJob((GlobalEventJob)this.schedulerJobService.findById(schedulerJobRecord.getId()).getJob(), editMode);
    }

    public void addSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.schedulerJobSelectedListeners.add(listener);
    }
}
