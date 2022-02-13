package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.util.ScheduledProcessConstants;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.event.model.ScheduledProcessConfigurationConstants;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobRecordImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ConfigurationMetaData;
import org.ikasan.spec.metadata.ConfigurationParameterMetaData;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.miki.superfields.dates.SuperDatePicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class InternalEventDrivenJobDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(InternalEventDrivenJobDialog.class);

    private ComboBox<String> agentCb;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextArea jobDescriptionTa;


    // Fields to capture job execution properties.
    private AceEditor commandLineTa;
    private TextField workingDirectoryTf;
    private List<TextField> successfulReturnCodes;

    private Label successfulReturnCodesLabel;
    private Button successfulReturnCodesButton;
    private Div returnCodesDiv;
    private Label noReturnCodesLabel;

    private Button saveButton;
    private Button cancelButton;

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleMetaData agent;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;

    private InternalEventDrivenJob internalEventDrivenJob = new SolrInternalEventDrivenJobImpl();

    private Binder<InternalEventDrivenJob> formBinder;

    private EditMode editMode = EditMode.NEW;

    private FormLayout formLayout;

    private boolean enabled = true;

    private SystemEventLogger systemEventLogger;

    private SchedulerJobService schedulerJobService;


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
    public InternalEventDrivenJobDialog(ModuleMetaData agent, ScheduledProcessManagementService scheduledProcessManagementService,
                                        ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                        MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService) {
        super.showResize(false);
        super.title.setText("Scheduled Job");

        this.agent = agent;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;
        this.schedulerJobService = schedulerJobService;

        this.internalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
        this.noReturnCodesLabel = new Label(getTranslation("label.no-return-codes", UI.getCurrent().getLocale()));
        this.noReturnCodesLabel.setVisible(false);
        this.noReturnCodesLabel.getStyle().set("color", "rgba(0, 0, 0, 0.38)");


        this.formBinder
            = new Binder<>(InternalEventDrivenJob.class);
        this.successfulReturnCodes = new ArrayList<>();

        this.setHeight("900px");
        this.setWidth("1200px");

        saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.setId("scheduledJobSaveButton");
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->  {

            if(!this.performFormValidation(this.internalEventDrivenJob)) {
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-configuration", UI.getCurrent().getLocale()));
                return;
            }

            try {
                createOrUpdateScheduledJob(this.internalEventDrivenJob);
            }
            catch (Exception e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-creation", UI.getCurrent().getLocale()));
                return;
            }

            IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

            if (this.editMode == EditMode.NEW) {
                String action = String.format("New scheduled job created [%s].", this.internalEventDrivenJob);
                this.systemEventLogger.logEvent(SystemEventConstants.NEW_SCHEDULED_JOB_CREATED, action, authentication.getName());
            }
            else if (this.editMode == EditMode.EDIT) {
                String action = String.format("Scheduled job edited. \nBefore [%s]\nAfter [%s].", this.internalEventDrivenJob,
                    this.internalEventDrivenJob);
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
            .withValidator(agentValue -> !agentValue.isEmpty(), getTranslation("error.missing-agent", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJob::getAgentName, InternalEventDrivenJob::setAgentName);
        formLayout.add(agentCb, 2);

        H3 jobExecutionLabel = new H3(getTranslation("header.job-execution-details", UI.getCurrent().getLocale()));
        formLayout.add(jobExecutionLabel, 2);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setId("jobNameTf");
        this.jobNameTf.setRequired(true);
        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW);
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), getTranslation("error.missing-job-name", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJob::getJobName, InternalEventDrivenJob::setJobName);
        formLayout.add(jobNameTf);

        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setRequired(true);
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJob::getJobDescription, InternalEventDrivenJob::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);


        // todo translation
        Icon parametersIcon = IconDecorator.decorate(new Icon(VaadinIcon.SLIDERS), "Job Parameters", "14pt", "rgba(241, 90, 35, 1.0)");

//        Button parametersButton = new Button(parametersIcon);
//        parametersButton.addClickListener(buttonClickEvent -> {
////            EntityContentsViewDialog entityContentsViewDialog = new EntityContentsViewDialog("Wiretap " + wiretapEvent.getEventId());
////            entityContentsViewDialog.populate(this.wiretapEvent);
//        });

        // todo translation
        Icon externalIcon = IconDecorator.decorate(new Icon(VaadinIcon.EXTERNAL_LINK), "Expand Text Editor", "14pt", "rgba(241, 90, 35, 1.0)");

//        Button newWindowButton = new Button(externalIcon);
//        newWindowButton.addClickListener(buttonClickEvent -> {
////            EntityContentsViewDialog entityContentsViewDialog = new EntityContentsViewDialog("Wiretap " + wiretapEvent.getEventId());
////            entityContentsViewDialog.populate(this.wiretapEvent);
//        });


        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.add(parametersIcon, externalIcon);

        VerticalLayout newButtonLayout = new VerticalLayout();
        newButtonLayout.setWidth("100%");
        newButtonLayout.add(horizontalLayout);
        newButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, horizontalLayout);

        formLayout.add(newButtonLayout, 2);

        this.commandLineTa = new AceEditor();
        this.commandLineTa.setHeight("300px");
        this.commandLineTa.setMode(AceMode.batchfile);
        this.commandLineTa.setTheme(AceTheme.dracula);
        this.commandLineTa.setId("commandLineTa");
        formBinder.forField(this.commandLineTa)
            .withValidator(value -> !value.isEmpty(), getTranslation("error.command-line-missing", UI.getCurrent().getLocale()))
            .bind(InternalEventDrivenJob::getCommandLine, InternalEventDrivenJob::setCommandLine);
        formLayout.add(commandLineTa, 2);
        commandLineTa.getStyle().set("minHeight", "100px");

        this.workingDirectoryTf = new TextField(getTranslation("label.working-directory", UI.getCurrent().getLocale()));
        formBinder.forField(this.workingDirectoryTf)
            .withNullRepresentation("")
            .bind(InternalEventDrivenJob::getWorkingDirectory, InternalEventDrivenJob::setWorkingDirectory);
        formLayout.add(workingDirectoryTf, 2);

        this.successfulReturnCodesLabel = new Label(getTranslation("label.successful-return-codes", UI.getCurrent().getLocale()));
        this.successfulReturnCodesLabel.getStyle().set("color", "rgba(0, 0, 0, 0.54)");
        this.successfulReturnCodesLabel.getStyle().set("margin-top", "30px");

        this.successfulReturnCodesButton = new Button(VaadinIcon.PLUS.create(), e -> {
            this.addSuccessfulReturnCodes(null);
        });
        this.successfulReturnCodesButton.setId("successfulReturnCodesButton");
        this.successfulReturnCodesButton.getStyle().set("margin-top", "30px");

        this.returnCodesDiv = new Div();
        this.returnCodesDiv.setVisible(false);
        formLayout.add(successfulReturnCodesLabel, successfulReturnCodesButton, this.noReturnCodesLabel, returnCodesDiv);

        return formLayout;
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


            formBinder.writeBean(internalEventDrivenJob);

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
     * @param internalEventDrivenJob
     */
    public void createOrUpdateScheduledJob(InternalEventDrivenJob internalEventDrivenJob) throws JsonProcessingException {
        // Get the module configuration from the module.
        SolrInternalEventDrivenJobRecordImpl solrInternalEventDrivenJobRecord = new SolrInternalEventDrivenJobRecordImpl();
        solrInternalEventDrivenJobRecord.setAgentName(internalEventDrivenJob.getAgentName());
        solrInternalEventDrivenJobRecord.setJobName(internalEventDrivenJob.getJobName());
        // sort out context
        solrInternalEventDrivenJobRecord.setContextId("TBD");
        solrInternalEventDrivenJobRecord.setTimestamp(System.currentTimeMillis());
        solrInternalEventDrivenJobRecord.setInternalEventDrivenJob(internalEventDrivenJob);

        this.schedulerJobService.saveInternalEventDrivenJobRecord(solrInternalEventDrivenJobRecord);
     }

    /**
     * Helper method to call activation endpoint on the scheduler agent.
     *
     * @param action
     */
    private void changeActivation(String action) {
        boolean success = this.moduleControlRestService.changeModuleActivationState(this.agent.getUrl(), this.agent.getName(), action);
        if (!success) {
            throw new RuntimeException(String.format("Could not %s agent[%s]", action, agent));
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
        this.successfulReturnCodes.forEach(successfulReturnCode -> successfulReturnCode.setEnabled(enabled));

        this.saveButton.setVisible(enabled);
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
        this.formBinder.readBean(this.internalEventDrivenJob);
        this.bindCollections(internalEventDrivenJob);
        this.editMode = editMode;

        // make sure all value are bound before calling set enabled
        this.setEnabled(this.enabled);
    }

    /**
     * Bind all collection fields to the form
     *
     * @param internalEventDrivenJob
     */
    private void bindCollections(InternalEventDrivenJob internalEventDrivenJob) {
        internalEventDrivenJob.getSuccessfulReturnCodes().forEach(rc -> this.addSuccessfulReturnCodes(rc));
    }


    /**
     * Helper method to add the controls for the return codes
     *
     * @param returnCode
     */
    private void addSuccessfulReturnCodes(String returnCode) {
        TextField successfulReturnCodeTf = new TextField(getTranslation("label.successful-return-code", UI.getCurrent().getLocale()));
        successfulReturnCodeTf.setId("successfulReturnCodeTf"+this.successfulReturnCodes.size());
        successfulReturnCodeTf.setEnabled(this.enabled);
        this.successfulReturnCodes.add(successfulReturnCodeTf);
        successfulReturnCodeTf.setErrorMessage(getTranslation("error.missing-return-code", UI.getCurrent().getLocale()) );
        if(returnCode!=null)successfulReturnCodeTf.setValue(returnCode);

        Button minusButton = new Button(VaadinIcon.MINUS.create(), ev -> {
            formLayout.remove(successfulReturnCodeTf);
            formLayout.remove(ev.getSource());
            this.successfulReturnCodes.remove(successfulReturnCodeTf);
        });
        minusButton.setVisible(this.enabled);
        formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(successfulReturnCodesLabel.getElement()) + (!this.enabled ? 4 : 3), successfulReturnCodeTf);
        formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(successfulReturnCodesLabel.getElement()) + (!this.enabled ? 5 : 4), minusButton);
    }


    /** Private helper classes */
    private class TextFieldNameValuePair {
        public TextField nameTf;
        public TextField valueTf;
    }

    private class DateTimeRange {
        public SuperDatePicker startDate;
        public TimePicker startTime;
        public SuperDatePicker endDate;
        public TimePicker endTime;
    }
}
