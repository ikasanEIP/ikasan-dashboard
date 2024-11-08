package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
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
import com.vaadin.flow.data.binder.ValidationException;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.apache.commons.lang3.SerializationUtils;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.scheduler.component.validator.StringToDefaultLongConverter;
import org.ikasan.dashboard.ui.scheduler.listener.JobSynchronisationRequiredListener;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class InternalEventDrivenJobTemplateDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(InternalEventDrivenJobTemplateDialog.class);

    private ComboBox<String> agentCb;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextField jobNameAliasTf;
    private TextArea jobDescriptionTa;


    // Fields to capture job execution properties.
    private AceEditor commandLineTa;
    private TextField workingDirectoryTf;
    private TextField minExecutionTimeTf;
    private TextField maxExecutionTimeTf;
    private ComboBox<String> executionEnvironmentPropertiesCb;
    private Button saveButton;
    private Button cancelButton;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleMetaData agent;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;

    private InternalEventDrivenJob internalEventDrivenJob;
    private SchedulerJobRecord schedulerJobRecord;

    private Binder<InternalEventDrivenJob> formBinder;

    private EditMode editMode = EditMode.NEW;

    private FormLayout formLayout;

    private boolean enabled = true;

    private SystemEventLogger systemEventLogger;

    private SchedulerJobService schedulerJobService;

    private List<SchedulerJobSelectedListener> schedulerJobSelectedListeners = new ArrayList<>();
    private List<JobSynchronisationRequiredListener> jobSynchronisationRequiredListeners = new ArrayList<>();

    private Context contextTemplate;

    private Context parentContextTemplate;

    private Map<String, String> schedulerJobExecutionEnvironmentLabel;

    private String jobContextErrorMessage;

    private boolean showDisplayName;


    /**
     * Constructor for InternalEventDrivenJobTemplateDialog.
     *
     * @param agent Module metadata for the agent
     * @param scheduledProcessManagementService Service for managing scheduled processes
     * @param configurationRestService Service for configuration
     * @param moduleControlRestService Service for module control
     * @param metaDataRestService Service for metadata
     * @param systemEventLogger Logger for system events
     * @param schedulerJobService Service for scheduler jobs
     * @param parentContextTemplate Parent context template
     * @param contextTemplate Context template
     * @param schedulerJobExecutionEnvironmentLabel Map of scheduler job execution environment labels
     */
    public InternalEventDrivenJobTemplateDialog(ModuleMetaData agent, ScheduledProcessManagementService scheduledProcessManagementService,
                                                ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                                Context parentContextTemplate, Context contextTemplate, Map<String, String> schedulerJobExecutionEnvironmentLabel) {
        super.showResize(false);
        super.title.setText(getTranslation("label.command-execution-job-template", UI.getCurrent().getLocale()));

        this.agent = agent;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerJobService = schedulerJobService;
        this.schedulerJobExecutionEnvironmentLabel = schedulerJobExecutionEnvironmentLabel;
        this.internalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
        this.showDisplayName = showDisplayName;
        this.contextTemplate = contextTemplate;
        this.parentContextTemplate = parentContextTemplate;
    }

    private void init() {
        this.formBinder
            = new Binder<>(InternalEventDrivenJob.class);

        this.jobContextErrorMessage = this.getTranslation("error.job-contents-must-be-provided"
            , UI.getCurrent().getLocale());

        this.setHeight("95vh");
        this.setWidth("95vw");

        saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.setId("scheduledJobSaveButton");
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->  {
            IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

            if(!this.performFormValidation(this.internalEventDrivenJob)) {
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-configuration", UI.getCurrent().getLocale()));
                return;
            }

            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog-update-job-template-header"));
            confirmDialog.setText(getTranslation("confirm-dialog-update-job-template"));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);
                progressIndicatorDialog.open(getTranslation("message.updating-all-jobs-dependent-on-template"), null);

                final UI current = UI.getCurrent();
                Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("TemplateJobUpdateThread"));
                executor.execute(() -> {
                    try {
                        createOrUpdateScheduledJob(this.internalEventDrivenJob, authentication);
                        Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = this.schedulerJobService
                            .getCommandExecutionJobsForContext(this.contextTemplate.getName());

                        internalEventDrivenJobMap.values().forEach(job -> {
                            if (job.isTemplateBased() != null && job.isTemplateBased()
                                && job.getTemplateName() != null && job.getTemplateName()
                                .equals(this.internalEventDrivenJob.getJobName())) {
                                job.setCommandLine(this.internalEventDrivenJob.getCommandLine());
                                job.setContextParameters(this.manageContextParamUpdate
                                    (this.internalEventDrivenJob.getContextParameters(), job.getContextParameters()));
                                this.schedulerJobService.saveInternalEventDrivenJob(job
                                    , authentication.getName());
                            }
                        });
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        current.access(() -> {
                            progressIndicatorDialog.close();
                            NotificationHelper.showErrorNotification(
                                getTranslation("error.scheduled-job-creation", UI.getCurrent().getLocale()));
                        });
                    }
                    if (this.editMode == EditMode.NEW) {
                        String action = String.format("New scheduled job created [%s].", this.internalEventDrivenJob);
                        this.systemEventLogger.logEvent(SystemEventConstants.NEW_SCHEDULED_JOB_CREATED, action, authentication.getName());
                    }
                    else if (this.editMode == EditMode.EDIT) {
                        String action = String.format("Scheduled job edited. \nBefore [%s]\nAfter [%s].", this.schedulerJobRecord.getJob(),
                            this.internalEventDrivenJob);
                        this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_EDIT, action, authentication.getName());
                    }

                    this.schedulerJobSelectedListeners.forEach(listener -> listener.jobSelected(this.internalEventDrivenJob));
                    this.jobSynchronisationRequiredListeners.forEach(listener -> listener.jobSynchronisationRequired());
                    this.editMode = EditMode.EDIT;
                    current.access(() -> {
                        progressIndicatorDialog.close();
                        NotificationHelper.showUserNotification(
                            getTranslation("notification.scheduler-job-saved", UI.getCurrent().getLocale()));
                    });
                });
            });
        });

        ComponentSecurityVisibility.applySecurity(saveButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        cancelButton = new Button(getTranslation("button.close", UI.getCurrent().getLocale()));
        cancelButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(false);
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(true);
        buttonLayout.add(saveButton, cancelButton);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(false);
        layout.add(this.createConfigurationForm(), createEditorLayout(), buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);
        super.content.add(layout);
    }

    private VerticalLayout createEditorLayout() {
        VerticalLayout editorLayout = new VerticalLayout();
        editorLayout.setPadding(false);
        editorLayout.setSpacing(false);
        editorLayout.setSizeFull();

        this.commandLineTa = new AceEditor();
        this.commandLineTa.setSizeFull();
        this.commandLineTa.setMode(AceMode.batchfile);
        this.commandLineTa.setTheme(AceTheme.dracula);
        this.commandLineTa.setId("commandLineTa");

        editorLayout.add(commandLineTa);
        editorLayout.expand(this.commandLineTa);

        commandLineTa.getStyle().set("minHeight", "100px");

        return editorLayout;
    }

    /**
     * Initialise the form.
     *
     * @return
     */
    private FormLayout createConfigurationForm() {
        formLayout = new FormLayout();
        H3 jobExecutionLabel = new H3(getTranslation("label.command-execution-job-template", UI.getCurrent().getLocale()));

        formLayout.add(jobExecutionLabel, 2);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setId("jobNameTf");
        this.jobNameTf.setRequired(true);
        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW);
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), getTranslation("error.missing-job-name", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJob::getJobName, InternalEventDrivenJob::setJobName);
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
            .bind(InternalEventDrivenJob::getAgentName, InternalEventDrivenJob::setAgentName);
        formLayout.add(agentCb);

        ComponentSecurityVisibility.applyEnabledSecurity(this.agentCb, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setRequired(true);
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJob::getJobDescription, InternalEventDrivenJob::setJobDescription);

        if(this.showDisplayName) {
            this.jobNameAliasTf = new TextField(getTranslation("label.job-name-alias", UI.getCurrent().getLocale()));
            this.jobNameAliasTf.setId("jobNameAliasTf");
            this.jobNameAliasTf.setRequired(false);
            this.jobNameAliasTf.setEnabled(this.editMode == EditMode.NEW);
            formBinder.forField(this.jobNameAliasTf)
                .bind(InternalEventDrivenJob::getDisplayName, InternalEventDrivenJob::setDisplayName);
            formLayout.add(jobNameAliasTf, jobDescriptionTa);
        }
        else {
            formLayout.add(jobDescriptionTa, 2);
        }

        ComponentSecurityVisibility.applyEnabledSecurity(this.jobDescriptionTa, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);


        this.minExecutionTimeTf = new TextField(getTranslation("label.minimum-execution-time", UI.getCurrent().getLocale()));
        formBinder.forField(this.minExecutionTimeTf)
            .withNullRepresentation("")
            .withConverter(
                new StringToDefaultLongConverter(getTranslation("error.please-enter-a-number", UI.getCurrent().getLocale()), -1))
            .bind(InternalEventDrivenJob::getMinExecutionTime, InternalEventDrivenJob::setMinExecutionTime);

        ComponentSecurityVisibility.applyEnabledSecurity(this.minExecutionTimeTf, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        this.maxExecutionTimeTf = new TextField(getTranslation("label.maximum-execution-time", UI.getCurrent().getLocale()));
        formBinder.forField(this.maxExecutionTimeTf)
            .withNullRepresentation("")
            .withConverter(
                new StringToDefaultLongConverter(getTranslation("error.please-enter-a-number", UI.getCurrent().getLocale()), -1))
            .bind(InternalEventDrivenJob::getMaxExecutionTime, InternalEventDrivenJob::setMaxExecutionTime);

        ComponentSecurityVisibility.applyEnabledSecurity(this.maxExecutionTimeTf, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        formLayout.add(minExecutionTimeTf, maxExecutionTimeTf);

        this.workingDirectoryTf = new TextField(getTranslation("label.working-directory", UI.getCurrent().getLocale()));
        formBinder.forField(this.workingDirectoryTf)
            .withNullRepresentation("")
            .bind(InternalEventDrivenJob::getWorkingDirectory, InternalEventDrivenJob::setWorkingDirectory);

        ComponentSecurityVisibility.applyEnabledSecurity(workingDirectoryTf, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        Set<String> executionEnvironmentLabelSet = new HashSet<>();
        if (schedulerJobExecutionEnvironmentLabel != null) {
            executionEnvironmentLabelSet = schedulerJobExecutionEnvironmentLabel.keySet();
        }

        this.executionEnvironmentPropertiesCb = new ComboBox<>(getTranslation("label.execution-environment-properties", UI.getCurrent().getLocale()));
        this.executionEnvironmentPropertiesCb.setId("executionEnvCb");
        this.executionEnvironmentPropertiesCb.setRequired(false);
        this.executionEnvironmentPropertiesCb.setItems(executionEnvironmentLabelSet);
        this.executionEnvironmentPropertiesCb.setEnabled(true);
        this.executionEnvironmentPropertiesCb.setAllowCustomValue(true);
        this.executionEnvironmentPropertiesCb.addCustomValueSetListener(listener -> {
            this.executionEnvironmentPropertiesCb.setValue(listener.getDetail());
        });

        formBinder.forField(this.executionEnvironmentPropertiesCb)
            .withNullRepresentation("")
            .bind(InternalEventDrivenJob::getExecutionEnvironmentProperties, InternalEventDrivenJob::setExecutionEnvironmentProperties);

        ComponentSecurityVisibility.applyEnabledSecurity(executionEnvironmentPropertiesCb, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

        formLayout.add(workingDirectoryTf, executionEnvironmentPropertiesCb);

        Button executionDaysButton = new Button(getTranslation("button.execution-days", UI.getCurrent().getLocale()), new Icon(VaadinIcon.CALENDAR));
        executionDaysButton.setIconAfterText(true);
        executionDaysButton.addClickListener(event -> {
            DayOfWeekJobDialog dayOfWeekJobDialog = new DayOfWeekJobDialog(this.internalEventDrivenJob.getDaysOfWeekToRun() == null
                ? null : new ArrayList<>(this.internalEventDrivenJob.getDaysOfWeekToRun()), true);
            dayOfWeekJobDialog.open();

            dayOfWeekJobDialog.addOpenedChangeListener(openedChangeEvent -> {
               if(!openedChangeEvent.isOpened() && dayOfWeekJobDialog.isSaveClose()) {
                   this.internalEventDrivenJob.setDaysOfWeekToRun(dayOfWeekJobDialog.getDaysOfWeek());
               }
            });
        });

        ComponentSecurityVisibility.applyEnabledSecurity(executionDaysButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

        Button parametersButton = new Button(getTranslation("button.parameters", UI.getCurrent().getLocale()), new Icon(VaadinIcon.SLIDERS));
        parametersButton.setIconAfterText(true);
        parametersButton.addClickListener(event -> {
            boolean isTemplateBased = false;
            if(this.internalEventDrivenJob.isTemplateBased() != null) {
                isTemplateBased = this.internalEventDrivenJob.isTemplateBased();
            }
            ContextParameterDialog contextParameterDialog = new ContextParameterDialog(true, !isTemplateBased);
            contextParameterDialog.initParams(this.internalEventDrivenJob.getContextParameters() == null ? new ArrayList<>() : this.internalEventDrivenJob.getContextParameters());
            contextParameterDialog.open();

            contextParameterDialog.addOpenedChangeListener(changeEvent -> {
                if(!changeEvent.isOpened() && contextParameterDialog.isSaveClose()) {
                    this.internalEventDrivenJob.setContextParameters(contextParameterDialog.getContextParameters());
                }
            });
        });

        ComponentSecurityVisibility.applyEnabledSecurity(parametersButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

        Button successfulReturnCodesButton = new Button(getTranslation("button.return-codes", UI.getCurrent().getLocale()), new Icon(VaadinIcon.CHECK));
        successfulReturnCodesButton.setIconAfterText(true);
        successfulReturnCodesButton.addClickListener(event -> {
            SuccessfulReturnCodesDialog successfulReturnCodesDialog = new SuccessfulReturnCodesDialog(true);
            successfulReturnCodesDialog.initReturnCodes(this.internalEventDrivenJob.getSuccessfulReturnCodes());
            successfulReturnCodesDialog.open();

            successfulReturnCodesDialog.addOpenedChangeListener(changeEvent -> {
                if(!changeEvent.isOpened() && successfulReturnCodesDialog.isSaveClose()) {
                    this.internalEventDrivenJob.setSuccessfulReturnCodes(successfulReturnCodesDialog.getSuccessfulReturnCodes());
                }
            });
        });

        ComponentSecurityVisibility.applyEnabledSecurity(successfulReturnCodesButton, SecurityConstants.ALL_AUTHORITY,
            SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.add(executionDaysButton, parametersButton, successfulReturnCodesButton);

        VerticalLayout newButtonLayout = new VerticalLayout();
        newButtonLayout.setWidth("100%");
        newButtonLayout.add(horizontalLayout);

        formLayout.add(newButtonLayout, 2);

        return formLayout;
    }

    /**
     * Manages the update of context parameters based on the provided template parameters and derived parameters.
     *
     * @param templateParams List of context parameters from the template
     * @param derivedParams List of context parameters derived from other sources
     * @return List of updated context parameters with the default values updated as needed
     */
    private List<ContextParameter> manageContextParamUpdate(List<ContextParameter> templateParams, List<ContextParameter> derivedParams) {
        List<ContextParameter> updatedParams = new ArrayList<>();

        templateParams.forEach(param -> {
            Optional<ContextParameter> p = derivedParams.stream()
                .filter(derivedParam -> derivedParam.getName().equals(param.getName()))
                .findFirst();
            ContextParameter cloned = SerializationUtils.clone(param);
            cloned.setDefaultValue(p.isPresent() ? p.get().getDefaultValue() : param.getDefaultValue());
            updatedParams.add(cloned);
        });

        return updatedParams;
    }

    /**
     * Perform validation of the form.
     *
     * @param internalEventDrivenJob
     * @return
     */
    private boolean performFormValidation(InternalEventDrivenJob internalEventDrivenJob) {

        try {
            AtomicBoolean isValid = new AtomicBoolean(true);
            this.jobNameTf.setInvalid(false);

            // Get the module configuration from the module.
            if(this.commandLineTa.getValue() == null || this.commandLineTa.getValue().isEmpty()
                || this.commandLineTa.getValue().equals(this.jobContextErrorMessage)) {
                this.commandLineTa.setValue(this.jobContextErrorMessage);
                isValid.set(false);
            }

            formBinder.writeBean(internalEventDrivenJob);

            if(this.editMode.equals(EditMode.NEW)) {
                if(this.schedulerJobService.findByContextNameAndJobName
                    (this.parentContextTemplate.getName(), internalEventDrivenJob.getJobName()) != null) {
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
     * Updates or creates a scheduled job based on the provided InternalEventDrivenJob and authentication.
     *
     * @param internalEventDrivenJob The InternalEventDrivenJob to be updated or created
     * @param authentication The IkasanAuthentication object for the user performing the action
     * @throws JsonProcessingException If there is an issue with processing JSON data
     */
    public void createOrUpdateScheduledJob(InternalEventDrivenJob internalEventDrivenJob, IkasanAuthentication authentication) throws JsonProcessingException {
        internalEventDrivenJob.setCommandLine(this.commandLineTa.getValue());
        internalEventDrivenJob.setIdentifier(internalEventDrivenJob.getAgentName()+"-"+internalEventDrivenJob.getJobName());
        internalEventDrivenJob.setTemplateJob(true);

        this.schedulerJobService.saveInternalEventDrivenJobTemplate(internalEventDrivenJob, authentication.getName());
        if(parentContextTemplate != null) {
            this.schedulerJobRecord = this.schedulerJobService.findByContextNameAndJobName(this.parentContextTemplate.getName(),
                internalEventDrivenJob.getJobName());
        }
     }

    /**
     * Helper method to set controls on the form elements if the form is read only
     * or editable.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW);
        this.jobDescriptionTa.setEnabled(enabled);

        this.commandLineTa.setEnabled(enabled);
        this.workingDirectoryTf.setEnabled(enabled);

        this.saveButton.setVisible(enabled &&
            ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));

        this.cancelButton.setVisible(enabled);
    }

    /**
     * Set the underlying pojo for the form along with the edit mode.
     *
     * @param internalEventDrivenJob
     * @param editMode
     */
    public void setJob(InternalEventDrivenJob internalEventDrivenJob, EditMode editMode) {
        this.enabled = editMode == EditMode.NEW || editMode == EditMode.EDIT ? true : false;
        this.internalEventDrivenJob = internalEventDrivenJob;

        this.init();

        this.formBinder.readBean(this.internalEventDrivenJob);
        this.commandLineTa.setValue(internalEventDrivenJob.getCommandLine());
        this.editMode = editMode;

        // make sure all value are bound before calling set enabled
        this.setEnabled(this.enabled);
    }

    public void setJob(SchedulerJobRecord internalEventDrivenJobRecord, EditMode editMode) {
        this.schedulerJobRecord = internalEventDrivenJobRecord;
        this.setJob((InternalEventDrivenJob)this.schedulerJobService.findById(internalEventDrivenJobRecord.getId()).getJob(), editMode);
    }

    public void addSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.schedulerJobSelectedListeners.add(listener);
    }

    public void addJobSynchronisationRequiredListener(JobSynchronisationRequiredListener listener) {
        this.jobSynchronisationRequiredListeners.add(listener);
    }
}
