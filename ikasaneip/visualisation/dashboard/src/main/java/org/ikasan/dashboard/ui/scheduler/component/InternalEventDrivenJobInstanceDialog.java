package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
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
import com.vaadin.flow.data.converter.StringToLongConverter;
import com.vaadin.flow.server.StreamResource;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.instance.model.SolrInternalEventDrivenJobInstanceImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class InternalEventDrivenJobInstanceDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(InternalEventDrivenJobInstanceDialog.class);

    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private ComboBox<String> agentCb;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextArea jobDescriptionTa;


    // Fields to capture job execution properties.
    private AceEditor commandLineTa;
    private TextField workingDirectoryTf;
    private TextField minExecutionTimeTf;
    private TextField maxExecutionTimeTf;

    private Checkbox holdCb;
    private Checkbox skipCb;

    private Button saveButton;
    private Button cancelButton;

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleMetaData agent;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;

    private InternalEventDrivenJobInstance internalEventDrivenJobInstance;
    private SchedulerJobInstanceRecord schedulerJobInstanceRecord;

    private Binder<InternalEventDrivenJobInstance> formBinder;

    private EditMode editMode = EditMode.NEW;

    private FormLayout formLayout;

    private boolean enabled = true;

    private SystemEventLogger systemEventLogger;

    private SchedulerJobInstanceService schedulerJobInstanceService;

    private SchedulerStatusDiv statusDiv;


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
    public InternalEventDrivenJobInstanceDialog(ModuleMetaData agent, ScheduledProcessManagementService scheduledProcessManagementService,
                                                ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                                SchedulerJobInstanceService schedulerJobInstanceService) {
        super.showResize(false);
        super.title.setText(getTranslation("label.command-execution-job-instance", UI.getCurrent().getLocale()));

        this.agent = agent;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerJobInstanceService = schedulerJobInstanceService;

        this.internalEventDrivenJobInstance = new SolrInternalEventDrivenJobInstanceImpl();
    }

    private void init() {
        this.formBinder
            = new Binder<>(InternalEventDrivenJobInstance.class);

        this.setHeight("1100px");
        this.setWidth("1400px");

        saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.setId("scheduledJobSaveButton");
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->  {

            IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

            if(!this.performFormValidation(this.internalEventDrivenJobInstance)) {
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-configuration", UI.getCurrent().getLocale()));
                return;
            }

            try {
                createOrUpdateScheduledJob(this.internalEventDrivenJobInstance, authentication);
            }
            catch (Exception e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-creation", UI.getCurrent().getLocale()));
                return;
            }

            if (this.editMode == EditMode.NEW) {
                String action = String.format("New scheduled job created [%s].", this.internalEventDrivenJobInstance);
                this.systemEventLogger.logEvent(SystemEventConstants.NEW_SCHEDULED_JOB_CREATED, action, authentication.getName());
            }
            else if (this.editMode == EditMode.EDIT) {
                String action = String.format("Scheduled job edited. \nBefore [%s]\nAfter [%s].", this.schedulerJobInstanceRecord.getSchedulerJobInstance(),
                    this.internalEventDrivenJobInstance);
                this.systemEventLogger.logEvent(SystemEventConstants.SCHEDULED_JOB_EDIT, action, authentication.getName());
            }

            this.close();
        });

        cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> this.close());

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(true);
        buttonLayout.setSpacing(true);
        buttonLayout.add(saveButton, cancelButton);
        buttonLayout.getStyle().set("padding-bottom", "20px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(false);
        layout.add(this.createConfigurationForm(), buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);
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
    private FormLayout createConfigurationForm() {
        formLayout = new FormLayout();
        formLayout.getStyle().set("padding-top", "0px");
        this.statusDiv = new SchedulerStatusDiv();
        this.statusDiv.setHeight("45px");
        this.statusDiv.setWidth("100%");
        this.statusDiv.setStatus(this.schedulerJobInstanceRecord.getStatus());

        formLayout.add(this.statusDiv, 2);

        H3 jobExecutionLabel = new H3(getTranslation("label.command-execution-job-instance", UI.getCurrent().getLocale()));
        formLayout.add(jobExecutionLabel, 2);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setId("jobNameTf");
        this.jobNameTf.setRequired(true);
        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW);
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), getTranslation("error.missing-job-name", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJobInstance::getJobName, InternalEventDrivenJobInstance::setJobName);
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
            .bind(InternalEventDrivenJobInstance::getAgentName, InternalEventDrivenJobInstance::setAgentName);
        formLayout.add(agentCb);

        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setRequired(true);
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJobInstance::getJobDescription, InternalEventDrivenJobInstance::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);

        this.minExecutionTimeTf = new TextField("Minimum execution time");
        formBinder.forField(this.minExecutionTimeTf)
            .withNullRepresentation("")
            .withConverter(
                new StringToLongConverter("Please enter a number"))
            .bind(InternalEventDrivenJobInstance::getMinExecutionTime, InternalEventDrivenJobInstance::setMinExecutionTime);

        this.maxExecutionTimeTf = new TextField("Maximum execution time");
        formBinder.forField(this.maxExecutionTimeTf)
            .withNullRepresentation("")
            .withConverter(
                new StringToLongConverter("Please enter a number"))
            .bind(InternalEventDrivenJobInstance::getMaxExecutionTime, InternalEventDrivenJobInstance::setMaxExecutionTime);

        formLayout.add(minExecutionTimeTf, maxExecutionTimeTf);

        this.workingDirectoryTf = new TextField(getTranslation("label.working-directory", UI.getCurrent().getLocale()));
        formBinder.forField(this.workingDirectoryTf)
            .withNullRepresentation("")
            .bind(InternalEventDrivenJobInstance::getWorkingDirectory, InternalEventDrivenJobInstance::setWorkingDirectory);
        formLayout.add(workingDirectoryTf, 2);

        Icon calendarIcon = IconDecorator.decorate(new Icon(VaadinIcon.CALENDAR), getTranslation("label.day-of-week-to-run", UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");
        calendarIcon.addClickListener(event -> {
            DayOfWeekJobDialog dayOfWeekJobDialog = new DayOfWeekJobDialog(this.internalEventDrivenJobInstance.getDaysOfWeekToRun() == null
                ? null : new ArrayList<>(this.internalEventDrivenJobInstance.getDaysOfWeekToRun()), false);
            dayOfWeekJobDialog.open();

            dayOfWeekJobDialog.addOpenedChangeListener(openedChangeEvent -> {
               if(!openedChangeEvent.isOpened() && dayOfWeekJobDialog.isSaveClose()) {
                   this.internalEventDrivenJobInstance.setDaysOfWeekToRun(dayOfWeekJobDialog.getDaysOfWeek());
               }
            });
        });

        Icon parametersIcon = IconDecorator.decorate(new Icon(VaadinIcon.SLIDERS), getTranslation("label.job-parameters", UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");
        parametersIcon.addClickListener(event -> {
            ContextParameterDialog contextParameterDialog = new ContextParameterDialog(false);
            contextParameterDialog.initParams(this.internalEventDrivenJobInstance.getContextParameters() == null ? new ArrayList<>() : this.internalEventDrivenJobInstance.getContextParameters());
            contextParameterDialog.open();

            contextParameterDialog.addOpenedChangeListener(changeEvent -> {
                if(!changeEvent.isOpened() && contextParameterDialog.isSaveClose()) {
                    this.internalEventDrivenJobInstance.setContextParameters(contextParameterDialog.getContextParameters());
                }
            });
        });

        Icon successfulReturnCodesIcon = IconDecorator.decorate(new Icon(VaadinIcon.CHECK), getTranslation("label.successful-return-codes", UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");
        successfulReturnCodesIcon.addClickListener(event -> {
            SuccessfulReturnCodesDialog successfulReturnCodesDialog = new SuccessfulReturnCodesDialog(false);
            successfulReturnCodesDialog.initReturnCodes(this.internalEventDrivenJobInstance.getSuccessfulReturnCodes());
            successfulReturnCodesDialog.open();

            successfulReturnCodesDialog.addOpenedChangeListener(changeEvent -> {
                if(!changeEvent.isOpened() && successfulReturnCodesDialog.isSaveClose()) {
                    this.internalEventDrivenJobInstance.setSuccessfulReturnCodes(successfulReturnCodesDialog.getSuccessfulReturnCodes());
                }
            });
        });

        Icon downloadIcon = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT), getTranslation("label.download-job", UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");
        StreamResource streamResource = new StreamResource(this.internalEventDrivenJobInstance.getJobName()+".json"
            , () -> {
            try {
                return new ByteArrayInputStream(this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(this.internalEventDrivenJobInstance));
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        });

        FileDownloadWrapper buttonWrapper = new FileDownloadWrapper(streamResource);
        buttonWrapper.wrapComponent(downloadIcon);

        Icon externalIcon = IconDecorator.decorate(new Icon(VaadinIcon.EXTERNAL_LINK), getTranslation("label.expand-text-editor", UI.getCurrent().getLocale()), "14pt", "rgba(241, 90, 35, 1.0)");

        HorizontalLayout holdSkipLayout = new HorizontalLayout();
        this.holdCb = new Checkbox("Hold");
        this.holdCb.addValueChangeListener(event -> {
            if (event.getValue() == true) {
                this.statusDiv.setStatus(InstanceStatus.ON_HOLD);
            }
            else {
                this.statusDiv.setStatus(InstanceStatus.RUNNING);
            }
        });

        this.skipCb = new Checkbox("Skip");
        holdSkipLayout.add(this.holdCb, this.skipCb);
        holdSkipLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, this.holdCb, this.skipCb);

        VerticalLayout cbLayout = new VerticalLayout();
        cbLayout.setWidth("100%");
        cbLayout.add(holdSkipLayout);
        cbLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.START, holdSkipLayout);

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.add(calendarIcon, parametersIcon, successfulReturnCodesIcon, buttonWrapper, externalIcon);

        VerticalLayout newButtonLayout = new VerticalLayout();
        newButtonLayout.setWidth("100%");
        newButtonLayout.add(horizontalLayout);
        newButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, horizontalLayout);

        formLayout.add(cbLayout, newButtonLayout);

        this.commandLineTa = new AceEditor();
        this.commandLineTa.setHeight("500px");
        this.commandLineTa.setMode(AceMode.batchfile);
        this.commandLineTa.setTheme(AceTheme.dracula);
        this.commandLineTa.setId("commandLineTa");

        formLayout.add(commandLineTa, 2);
        commandLineTa.getStyle().set("minHeight", "100px");

        return formLayout;
    }

    /**
     * Perform validation of the form.
     *
     * @param internalEventDrivenJobInstance
     * @return
     */
    private boolean performFormValidation(InternalEventDrivenJobInstance internalEventDrivenJobInstance) {

        try {
            AtomicBoolean isValid = new AtomicBoolean(true);

            formBinder.writeBean(internalEventDrivenJobInstance);

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
     * @param internalEventDrivenJobInstance
     */
    public void createOrUpdateScheduledJob(InternalEventDrivenJobInstance internalEventDrivenJobInstance, IkasanAuthentication authentication) throws JsonProcessingException {

        this.schedulerJobInstanceRecord.setModifiedTimestamp(System.currentTimeMillis());
        this.schedulerJobInstanceRecord.setSchedulerJobInstance(internalEventDrivenJobInstance);
        this.schedulerJobInstanceRecord.setModifiedBy(authentication.getName());

        this.schedulerJobInstanceService.save(this.schedulerJobInstanceRecord);
     }

    /**
     * Helper method to set controls on the form elements if the form is read only
     * or editable.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        this.agentCb.setEnabled(enabled);
        this.commandLineTa.setEnabled(enabled);
        this.commandLineTa.setReadOnly(!enabled);

        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW);
        this.jobDescriptionTa.setEnabled(enabled);

        this.commandLineTa.setEnabled(enabled);
        this.workingDirectoryTf.setEnabled(enabled);

        this.minExecutionTimeTf.setEnabled(enabled);
        this.maxExecutionTimeTf.setEnabled(enabled);

        this.saveButton.setVisible(enabled);
        this.cancelButton.setVisible(enabled);
    }

    /**
     * Set the underlying pojo for the form along with the edit mode.
     *
     * @param internalEventDrivenJob
     * @param editMode
     */
    private void setJob(InternalEventDrivenJobInstance internalEventDrivenJob, EditMode editMode) {
        this.enabled = editMode == EditMode.NEW || editMode == EditMode.EDIT ? true : false;
        this.internalEventDrivenJobInstance = internalEventDrivenJob;

        this.init();

        this.formBinder.readBean(this.internalEventDrivenJobInstance);
        this.commandLineTa.setValue(internalEventDrivenJob.getCommandLine());
        this.editMode = editMode;

        // make sure all value are bound before calling set enabled
        this.setEnabled(this.enabled);
    }

    public void setJob(SchedulerJobInstanceRecord internalEventDrivenJobRecord, EditMode editMode) {
        this.schedulerJobInstanceRecord = internalEventDrivenJobRecord;
        this.setJob((InternalEventDrivenJobInstance) this.schedulerJobInstanceService
            .findById(internalEventDrivenJobRecord.getId()).getSchedulerJobInstance(), editMode);
    }

}
