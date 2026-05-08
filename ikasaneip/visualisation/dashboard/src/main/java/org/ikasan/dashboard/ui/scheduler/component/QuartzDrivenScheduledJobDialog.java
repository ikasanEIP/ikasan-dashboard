package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.data.binder.ValidationException;
import org.apache.commons.lang3.SerializationUtils;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.listener.JobSynchronisationRequiredListener;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobRecordImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuartzDrivenScheduledJobDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(QuartzDrivenScheduledJobDialog.class);

    private ComboBox<String> agentCb;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextField jobNameAliasTf;
    private TextArea jobDescriptionTa;
    private TextField cronExpressionTf;
    private ComboBox<DateTimeUtil.TimezonePair> timezoneCb;

    private Button saveButton;
    private Button cancelButton;


    private ConfigurationService configurationRestService;
    private ModuleMetaData agent;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;

    private QuartzScheduleDrivenJob quartzScheduleDrivenJob;

    private Binder<QuartzScheduleDrivenJob> formBinder;

    private EditMode editMode = EditMode.NEW;

    private FormLayout formLayout;

    private boolean enabled = true;
    private boolean showDisplayName;

    private SystemEventLogger systemEventLogger;

    private SchedulerJobService schedulerJobService;

    private SchedulerJobRecord schedulerJobRecord;

    private List<SchedulerJobSelectedListener> schedulerJobSelectedListeners = new ArrayList<>();
    private List<JobSynchronisationRequiredListener> jobSynchronisationRequiredListeners = new ArrayList<>();
    private ContextTemplate contextTemplate;

    private boolean validateJobUniquenessAgainstContextJobs;

    /**
     * Constructor
     *
     * @param agent
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     */
    public QuartzDrivenScheduledJobDialog(ModuleMetaData agent,
                                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                          MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                          SchedulerJobService schedulerJobService, boolean showDisplayName, ContextTemplate contextTemplate,
                                          boolean validateJobUniquenessAgainstContextJobs) {
        super.showResize(false);
        super.title.setText(getTranslation("label.scheduled-job", UI.getCurrent().getLocale()));

        this.agent = agent;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerJobService = schedulerJobService;
        this.showDisplayName = showDisplayName;
        this.contextTemplate = contextTemplate;
        this.validateJobUniquenessAgainstContextJobs = validateJobUniquenessAgainstContextJobs;

        this.quartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();


        this.formBinder
            = new Binder<>(QuartzScheduleDrivenJob.class);

        this.setHeight("500px");
        this.setWidth("90vw");

        saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.setId("scheduledJobSaveButton");
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->  {

            IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

            QuartzScheduleDrivenJob priorToModification = SerializationUtils.clone(this.quartzScheduleDrivenJob);

            if(!this.performFormValidation(this.quartzScheduleDrivenJob)) {
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-configuration", UI.getCurrent().getLocale()));
                return;
            }

            try {
                createOrUpdateScheduledJob(this.quartzScheduleDrivenJob, authentication);
            }
            catch (Exception e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-creation", UI.getCurrent().getLocale()));
                return;
            }

            if (this.editMode == EditMode.NEW) {
                String action = String.format("New Quartz Scheduled Job [%s] created. Job Plan Name[%s].", this.quartzScheduleDrivenJob, this.quartzScheduleDrivenJob.getContextName());
                this.systemEventLogger.logEvent(SystemEventConstants.NEW_SCHEDULED_JOB_CREATED, action, authentication.getName());
            }
            else if (this.editMode == EditMode.EDIT) {
                String action = String.format("Quartz scheduled job edited. \nBefore [%s]\nAfter [%s].", this.schedulerJobRecord.getJob(),
                    this.quartzScheduleDrivenJob);
                this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_EDIT, action, authentication.getName());
            }

            this.schedulerJobSelectedListeners.forEach(listener -> listener.jobSelected(this.quartzScheduleDrivenJob));

            if(this.editMode.equals(EditMode.NEW) || this.isJobSynchronisationRequired(priorToModification, this.quartzScheduleDrivenJob)) {
                this.jobSynchronisationRequiredListeners.forEach(listener -> listener.jobSynchronisationRequired());
            }

            NotificationHelper.showErrorNotification(getTranslation("notification.scheduler-job-saved", UI.getCurrent().getLocale()));
            this.close();
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

    private boolean isJobSynchronisationRequired(QuartzScheduleDrivenJob priorToModification, QuartzScheduleDrivenJob afterModification) {
        boolean synchRequired = false;

        if(!priorToModification.getCronExpression().equals(afterModification.getCronExpression())) {
            synchRequired = true;
        }

        if((priorToModification.getTimeZone() == null && afterModification.getTimeZone() != null)
            || (priorToModification.getTimeZone() != null && afterModification.getTimeZone() == null)
            || (priorToModification.getTimeZone() != null && afterModification.getTimeZone() != null
                    && !priorToModification.getTimeZone().equals(afterModification.getTimeZone()))) {
            synchRequired = true;
        }

        return synchRequired;
    }

    /**
     * Initialise the form.
     *
     * @return
     */
    private FormLayout createConfigurationForm() {
        formLayout = new FormLayout();

        // Fields to capture schedule job properties.
        H3 scheduleDetailsLabel = new H3(getTranslation("header.schedule-details", UI.getCurrent().getLocale()));
        formLayout.add(scheduleDetailsLabel, 2);

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
            .bind(QuartzScheduleDrivenJob::getJobName, QuartzScheduleDrivenJob::setJobName);
        formLayout.add(jobNameTf);


        this.agentCb = new ComboBox<>(getTranslation("label.agent", UI.getCurrent().getLocale()));
        this.agentCb.setId("agentCb");
        this.agentCb.setRequired(true);
        this.agentCb.setClearButtonVisible(true);
        this.agentCb.setItems(this.schedulerJobService.getAllAgentNames());
        if(agent != null) {
            this.agentCb.setValue(agent.getName());
            this.agentCb.setEnabled(false);
        }
        formBinder.forField(this.agentCb)
            .withValidator(agentValue -> agentValue != null && !agentValue.isEmpty(), getTranslation("error.missing-agent", UI.getCurrent().getLocale()))
            .bind(QuartzScheduleDrivenJob::getAgentName, QuartzScheduleDrivenJob::setAgentName);
        formLayout.add(agentCb);


        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setRequired(true);
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(QuartzScheduleDrivenJob::getJobDescription, QuartzScheduleDrivenJob::setJobDescription);

        if(this.showDisplayName) {
            this.jobNameAliasTf = new TextField(getTranslation("label.job-name-alias", UI.getCurrent().getLocale()));
            this.jobNameAliasTf.setId("jobNameAliasTf");
            this.jobNameAliasTf.setRequired(false);
            this.jobNameAliasTf.setEnabled(this.editMode == EditMode.NEW &&
                ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
            formBinder.forField(this.jobNameAliasTf)
                .bind(QuartzScheduleDrivenJob::getDisplayName, QuartzScheduleDrivenJob::setDisplayName);
            formLayout.add(jobNameAliasTf, jobDescriptionTa);
        }
        else {
            formLayout.add(jobDescriptionTa, 2);
        }


        Icon builderIcon = IconDecorator.decorate(VaadinIcon.BUILDING_O.create()
            , getTranslation("tooltip.build-cron-expression", UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");
        builderIcon.addClickListener(event -> {
            CronBuilderDialog dialog = new CronBuilderDialog();
            dialog.init(this.cronExpressionTf.getValue());
            dialog.open();

            dialog.addOpenedChangeListener(openedChangeEvent -> {
               if(!openedChangeEvent.isOpened() && dialog.isSaveClose()) {
                   this.cronExpressionTf.setValue(dialog.getCronExpression());
               }
            });
        });

        ComponentSecurityVisibility.applySecurity(builderIcon, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        this.cronExpressionTf = new TextField(getTranslation("label.cron-expression", UI.getCurrent().getLocale()));
        this.cronExpressionTf.setRequired(true);
        this.cronExpressionTf.setId("cronExpressionTf");
        this.cronExpressionTf.setSuffixComponent(builderIcon);
        formBinder.forField(this.cronExpressionTf)
            .withValidator(value -> !value.isEmpty(), getTranslation("error.missing-cron-expression", UI.getCurrent().getLocale()))
            .withValidator(value -> CronExpression.isValidExpression(value), getTranslation("error.invalid-cron-expression", UI.getCurrent().getLocale()))
            .bind(QuartzScheduleDrivenJob::getCronExpression, QuartzScheduleDrivenJob::setCronExpression);
        formLayout.add(cronExpressionTf);




        this.timezoneCb = new ComboBox<>(getTranslation("label.timezone", UI.getCurrent().getLocale()));
        ComboBox.ItemFilter<DateTimeUtil.TimezonePair> filter = (element, filterString) ->
            element.zoneId.toLowerCase().contains(filterString.toLowerCase());
        this.timezoneCb.setId("timezoneCb");
        this.timezoneCb.setItems(filter, DateTimeUtil.getAllZoneIdsAndItsOffSet());
        this.timezoneCb.setItemLabelGenerator((ItemLabelGenerator<DateTimeUtil.TimezonePair>) s -> String.format("%35s (UTC%s) %n", s.zoneId, s.offset).trim());
        this.timezoneCb.setClearButtonVisible(true);
        this.timezoneCb.setPlaceholder(getTranslation("label.choose-a-timezone", UI.getCurrent().getLocale()));
        this.timezoneCb.setErrorMessage(getTranslation("error.timezone-required", UI.getCurrent().getLocale()));
        formLayout.add(timezoneCb);

        return formLayout;
    }

    /**
     * Perform validation of the form.
     *
     * @param quartzScheduleDrivenJob
     * @return
     */
    private boolean performFormValidation(QuartzScheduleDrivenJob quartzScheduleDrivenJob) {

        try {
            AtomicBoolean isValid = new AtomicBoolean(true);

            if(this.timezoneCb.getValue() != null) {
                quartzScheduleDrivenJob.setTimeZone(this.timezoneCb.getValue().zoneId);
            }
            else {
                quartzScheduleDrivenJob.setTimeZone(null);
            }

            formBinder.writeBean(quartzScheduleDrivenJob);

            if(this.editMode.equals(EditMode.NEW) || this.editMode.equals(EditMode.CLONE) || this.editMode.equals(EditMode.FROM_TEMPLATE)) {
                if(this.schedulerJobService.findByContextNameAndJobName
                    (quartzScheduleDrivenJob.getContextName(), quartzScheduleDrivenJob.getJobName()) != null ||
                    (this.validateJobUniquenessAgainstContextJobs && this.contextTemplate != null && this.contextTemplate.getScheduledJobs().stream()
                        .filter(job -> quartzScheduleDrivenJob.getJobName().equals(job.getJobName()))
                        .findFirst().isPresent())) {
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
     *
     *
     * @param solrQuartzScheduleDrivenJob
     */
    public void createOrUpdateScheduledJob(QuartzScheduleDrivenJob solrQuartzScheduleDrivenJob, IkasanAuthentication authentication) throws JsonProcessingException {
        solrQuartzScheduleDrivenJob.setIdentifier(solrQuartzScheduleDrivenJob.getAgentName()+"-"+solrQuartzScheduleDrivenJob.getJobName());

        QuartzScheduleDrivenJobRecord quartzScheduleDrivenJobRecord = new SolrQuartzScheduleDrivenJobRecordImpl();
        quartzScheduleDrivenJobRecord.setAgentName(solrQuartzScheduleDrivenJob.getAgentName());
        quartzScheduleDrivenJobRecord.setContextName(solrQuartzScheduleDrivenJob.getContextName());
        quartzScheduleDrivenJobRecord.setJobName(solrQuartzScheduleDrivenJob.getJobName());
        quartzScheduleDrivenJobRecord.setQuartzScheduleDrivenJob(solrQuartzScheduleDrivenJob);
        quartzScheduleDrivenJobRecord.setModifiedBy(authentication.getName());

        if(this.schedulerJobRecord != null) {
            quartzScheduleDrivenJobRecord.setTimestamp(this.schedulerJobRecord.getTimestamp());
        }
        else {
            quartzScheduleDrivenJobRecord.setTimestamp(System.currentTimeMillis());
        }

        this.schedulerJobService.saveQuartzScheduledJobRecord(quartzScheduleDrivenJobRecord);
     }


    /**
     * Helper method to set controls on the form elements if the form is read only
     * or editable.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        this.timezoneCb.setEnabled(enabled &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        this.jobDescriptionTa.setEnabled(enabled &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        this.cronExpressionTf.setEnabled(enabled &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        this.timezoneCb.setEnabled(enabled &&
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
     * @param quartzScheduleDrivenJob
     * @param editMode
     */
    public void setJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob, EditMode editMode) {
        this.enabled = editMode == EditMode.NEW || editMode == EditMode.EDIT ? true : false;
        this.quartzScheduleDrivenJob = quartzScheduleDrivenJob;
        this.formBinder.readBean(this.quartzScheduleDrivenJob);
        this.timezoneCb.setValue(DateTimeUtil.getTimezonePairForZoneId(quartzScheduleDrivenJob.getTimeZone()));
        this.editMode = editMode;

        // make sure all value are bound before calling set enabled
        this.setEnabled(this.enabled);
    }

    public void setJob(SchedulerJobRecord schedulerJobRecord, EditMode editMode) {
        this.schedulerJobRecord = schedulerJobRecord;
        this.setJob((QuartzScheduleDrivenJob)this.schedulerJobService.findById(schedulerJobRecord.getId()).getJob(), editMode);
    }

    public void addSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.schedulerJobSelectedListeners.add(listener);
    }

    public void addJobSynchronisationRequiredListener(JobSynchronisationRequiredListener listener) {
        this.jobSynchronisationRequiredListeners.add(listener);
    }
}
