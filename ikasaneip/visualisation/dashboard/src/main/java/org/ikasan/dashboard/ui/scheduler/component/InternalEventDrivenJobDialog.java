package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import com.vaadin.flow.data.converter.StringToLongConverter;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.EntityContentsViewDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.TableButton;
import org.ikasan.dashboard.ui.scheduler.util.ScheduledProcessConstants;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.event.model.ScheduledProcessConfigurationConstants;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ConfigurationMetaData;
import org.ikasan.spec.metadata.ConfigurationParameterMetaData;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
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
    private Checkbox startAutomaticCb;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextField jobGroupTf;
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

    private ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration = new ScheduledProcessAggregateConfiguration();
    private ScheduledProcessAggregateConfiguration oldScheduleProcessAggregateConfiguration;

    private Binder<ScheduledProcessAggregateConfiguration> formBinder;

    private EditMode editMode = EditMode.NEW;

    private FormLayout formLayout;

    private boolean enabled = true;

    private SystemEventLogger systemEventLogger;


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
                                        MetaDataService metaDataRestService, SystemEventLogger systemEventLogger) {
        super.showResize(false);
        super.title.setText("Scheduled Job");

        this.agent = agent;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;
        this.systemEventLogger = systemEventLogger;

        this.noReturnCodesLabel = new Label(getTranslation("label.no-return-codes", UI.getCurrent().getLocale()));
        this.noReturnCodesLabel.setVisible(false);
        this.noReturnCodesLabel.getStyle().set("color", "rgba(0, 0, 0, 0.38)");


        this.formBinder
            = new Binder<>(ScheduledProcessAggregateConfiguration.class);
        this.successfulReturnCodes = new ArrayList<>();

        this.setHeight("900px");
        this.setWidth("1200px");

        saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.setId("scheduledJobSaveButton");
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->  {

            if(!this.performFormValidation(this.scheduleProcessAggregateConfiguration)) {
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-configuration", UI.getCurrent().getLocale()));
                return;
            }

            try {
                createOrUpdateScheduledJob(this.scheduleProcessAggregateConfiguration);
            }
            catch (Exception e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification(getTranslation("error.scheduled-job-creation", UI.getCurrent().getLocale()));
                return;
            }

            IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

            if (this.editMode == EditMode.NEW) {
                String action = String.format("New scheduled job created [%s].", this.scheduleProcessAggregateConfiguration);
                this.systemEventLogger.logEvent(SystemEventConstants.NEW_SCHEDULED_JOB_CREATED, action, authentication.getName());
            }
            else if (this.editMode == EditMode.EDIT) {
                String action = String.format("Scheduled job edited. \nBefore [%s]\nAfter [%s].", this.oldScheduleProcessAggregateConfiguration,
                    this.scheduleProcessAggregateConfiguration);
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
            .bind(ScheduledProcessAggregateConfiguration::getAgentName, ScheduledProcessAggregateConfiguration::setAgentName);
        formLayout.add(agentCb, 2);

        this.startAutomaticCb = new Checkbox(getTranslation("label.start-automatically", UI.getCurrent().getLocale()));
        this.startAutomaticCb.setId("startAutomaticCb");
        this.startAutomaticCb.setValue(true);
        formBinder.forField(this.startAutomaticCb)
            .bind(ScheduledProcessAggregateConfiguration::isStartAutomatically, ScheduledProcessAggregateConfiguration::setStartAutomatically);
//        formLayout.add(this.startAutomaticCb);

        H3 jobExecutionLabel = new H3(getTranslation("header.job-execution-details", UI.getCurrent().getLocale()));
        formLayout.add(jobExecutionLabel, 2);

        this.jobNameTf = new TextField(getTranslation("label.job-name", UI.getCurrent().getLocale()));
        this.jobNameTf.setId("jobNameTf");
        this.jobNameTf.setRequired(true);
        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW);
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), getTranslation("error.missing-job-name", UI.getCurrent().getLocale()))
            .bind(ScheduledProcessAggregateConfiguration::getJobName, ScheduledProcessAggregateConfiguration::setJobName);
        formLayout.add(jobNameTf);


        this.jobGroupTf = new TextField(getTranslation("label.job-group", UI.getCurrent().getLocale()));
        this.jobGroupTf.setRequired(true);
        this.jobGroupTf.setId("jobGroupTf");
        formBinder.forField(this.jobGroupTf)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-group", UI.getCurrent().getLocale()))
            .bind(ScheduledProcessAggregateConfiguration::getJobGroup, ScheduledProcessAggregateConfiguration::setJobGroup);
        formLayout.add(jobGroupTf);


        this.jobDescriptionTa = new TextArea(getTranslation("label.job-description", UI.getCurrent().getLocale()));
        this.jobDescriptionTa.setRequired(true);
        this.jobDescriptionTa.setId("jobDescriptionTa");
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(ScheduledProcessAggregateConfiguration::getJobDescription, ScheduledProcessAggregateConfiguration::setJobDescription);
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
            .bind(ScheduledProcessAggregateConfiguration::getCommandLine, ScheduledProcessAggregateConfiguration::setCommandLine);
        formLayout.add(commandLineTa, 2);
        commandLineTa.getStyle().set("minHeight", "100px");

        this.workingDirectoryTf = new TextField(getTranslation("label.working-directory", UI.getCurrent().getLocale()));
        formBinder.forField(this.workingDirectoryTf)
            .withNullRepresentation("")
            .bind(ScheduledProcessAggregateConfiguration::getWorkingDirectory, ScheduledProcessAggregateConfiguration::setWorkingDirectory);
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
     * @param scheduleProcessAggregateConfiguration
     * @return
     */
    private boolean performFormValidation(ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {

        try {
            AtomicBoolean isValid = new AtomicBoolean(true);

            scheduleProcessAggregateConfiguration.getBlackoutDateTimeRanges().clear();

            formBinder.writeBean(scheduleProcessAggregateConfiguration);

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
     * This method interacts with with agent in order to create a new scheduler agent flow or update an existing flow and associated job.
     *
     * @param scheduleProcessAggregateConfiguration
     */
    public void createOrUpdateScheduledJob(ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
        // Get the module configuration from the module.
        ConfigurationMetaData<List<ConfigurationParameterMetaData>> moduleConfiguration
            = this.configurationRestService.getModuleConfiguration(this.agent.getUrl());

        try {

            if (moduleConfiguration == null) {
                throw new RuntimeException(String.format("Could not find module configuration for agent[%s]", agent));
            }

            logger.debug("Module Configuration: " + moduleConfiguration);

            if (this.editMode == EditMode.NEW) {
                // Get the flowDefinitions from the configuration metadata.
                moduleConfiguration.getParameters().stream()
                    .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitions"))
                    .findFirst().ifPresentOrElse(flowDefinitions -> {
                    // Add the new job flow to the map.
                    Map<String, String> configurationMap = (Map<String, String>) flowDefinitions.getValue();
                    configurationMap.put(scheduleProcessAggregateConfiguration.getJobName(), "MANUAL");
                    flowDefinitions.setValue(configurationMap);
                }, () -> {
                    throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s]", agent));
                });

                moduleConfiguration.getParameters().stream()
                    .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitionProfiles"))
                    .findFirst().ifPresentOrElse(flowDefinitions -> {
                    // Add the new job flow to the map.
                    Map<String, String> configurationMap = (Map<String, String>) flowDefinitions.getValue();
                    configurationMap.put(scheduleProcessAggregateConfiguration.getJobName(), "SCHEDULER_JOB");
                    flowDefinitions.setValue(configurationMap);
                }, () -> {
                    throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s]", agent));
                });

                logger.info("Module Configuration: " + moduleConfiguration);
                // update the configuration back onto the module.
                this.configurationRestService.storeConfiguration(this.agent.getUrl(), moduleConfiguration);
                // We need to deactivate and activate the module so the new flow is initialised
                this.changeActivation("deactivate");
                this.changeActivation("activate");
            }

            // Load the required configurations for a scheduled job.
            Optional<ModuleMetaData> moduleMetaData = this.metaDataRestService.getModuleMetadata(agent.getUrl(), agent.getName());

            ConfigurationMetaData<List<ConfigurationParameterMetaData>> processExecutionBrokerConfiguration = this.getConfigurationForAgentFlowComponent(moduleMetaData,
                this.jobNameTf.getValue(), ScheduledProcessConstants.PROCESS_EXECUTION_BROKER);

            // Update all the configurations with the configurations provided in the form.
            this.updateProcessExecutionBrokerConfiguration(processExecutionBrokerConfiguration, scheduleProcessAggregateConfiguration);

            logger.debug(processExecutionBrokerConfiguration.toString());
            if(!this.configurationRestService.storeConfiguration(this.agent.getUrl(), processExecutionBrokerConfiguration)) {
                throw new RuntimeException(String.format("Could not store process execution configuration [%s]", processExecutionBrokerConfiguration));
            }
            this.scheduledProcessManagementService.saveConfiguration(processExecutionBrokerConfiguration);


            if(this.startAutomaticCb.getValue()) {
                // Now that all configurations are applied we need to set up the startup type and restart the flow
                String startupType = this.startAutomaticCb.getValue() ? "AUTOMATIC" : "MANUAL";
                moduleConfiguration.getParameters().stream()
                    .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitions"))
                    .findFirst().ifPresentOrElse(flowDefinitions -> {
                    // Add the new job flow to the map.
                    Map<String, String> configurationMap = (Map<String, String>) flowDefinitions.getValue();
                    configurationMap.replace(scheduleProcessAggregateConfiguration.getJobName(), startupType);
                    flowDefinitions.setValue(configurationMap);

                    logger.info("Module Configuration: " + moduleConfiguration);
                    // update the configuration back onto the module.
                    this.configurationRestService.storeConfiguration(this.agent.getUrl(), moduleConfiguration);
                }, () -> {
                    throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s] " +
                        "when attempting to update start up control.", agent));
                });

                this.moduleControlRestService.changeFlowStartupType(this.agent.getUrl(), this.agent.getName(), scheduleProcessAggregateConfiguration.getJobName()
                    , startupType, "Scheduler flow requires automatic startup.");
            }

            // In order for the configuration to be applied the flow must be stopped and started.
            this.moduleControlRestService.changeFlowState(this.agent.getUrl(), this.agent.getName(), scheduleProcessAggregateConfiguration.getJobName(), "stop");
            this.moduleControlRestService.changeFlowState(this.agent.getUrl(), this.agent.getName(), scheduleProcessAggregateConfiguration.getJobName(), "start");
        }
        catch (Exception e) {
            // If any exceptions occur we are going to remove the job that we attempted to create.
            if(moduleConfiguration != null) {
                moduleConfiguration.getParameters().stream()
                    .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitions"))
                    .findFirst().ifPresentOrElse(flowDefinitions -> {
                    // Add the new job flow to the map.
                    Map<String, String> configurationMap = (Map<String, String>) flowDefinitions.getValue();
                    configurationMap.remove(scheduleProcessAggregateConfiguration.getJobName());
                    flowDefinitions.setValue(configurationMap);

                    logger.info("Module Configuration: " + moduleConfiguration);
                    // update the configuration back onto the module.
                    this.configurationRestService.storeConfiguration(this.agent.getUrl(), moduleConfiguration);
                }, () -> {
                    throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s]", agent));
                });


                // We need to deactivate and activate the module so the new flow is removed when initialisation occurs.
                this.changeActivation("deactivate");
                this.changeActivation("activate");
            }

            throw e;
        }
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
     * Update the execution broker configuration.
     *
     * @param scheduledConsumerConfiguration
     * @param scheduleProcessAggregateConfiguration
     */
    private void updateProcessExecutionBrokerConfiguration(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
        , ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.COMMAND_LINE,
            scheduleProcessAggregateConfiguration.getCommandLine());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.WORKING_DIRECTORY,
            scheduleProcessAggregateConfiguration.getWorkingDirectory());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.SUCCESSFUL_RETURN_CODES,
            scheduleProcessAggregateConfiguration.getSuccessfulReturnCodes());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.SECONDS_TO_WAIT_FOR_PROCESS_TO_START,
            scheduleProcessAggregateConfiguration.getSecondsToWaitForProcessStart());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.STD_ERR,
            scheduleProcessAggregateConfiguration.getStdErr());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.STD_OUT,
            scheduleProcessAggregateConfiguration.getStdOut());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.RETRY_ON_FAIL,
            scheduleProcessAggregateConfiguration.isRetryOnFail());
    }

    /**
     * General method to set parameters on a configuration meta data.
     *
     * @param params
     * @param paramName
     * @param value
     */
    private void setConfigurationParameterMetaDataValue(ConfigurationMetaData<List<ConfigurationParameterMetaData>> params
        , String paramName, Object value) {
        params.getParameters().stream()
            .filter(param -> param.getName().equals(paramName))
            .findFirst()
            .ifPresentOrElse(conf -> conf.setValue(value), () -> logger.warn(String.format("Failed to set configuration parameter[%s]" +
                ", value[%s], configuration[%s]", paramName, value, params)));
    }

    /**
     * Helper method to get a specific component configuration from the module.
     *
     * @param moduleMetaData
     * @param flow
     * @param component
     * @return
     */
    private ConfigurationMetaData getConfigurationForAgentFlowComponent(Optional<ModuleMetaData> moduleMetaData, String flow, String component) {
        AtomicReference<ConfigurationMetaData> configurationMetaData = new AtomicReference<>();

        moduleMetaData.ifPresentOrElse(metaData -> {
            metaData.getFlows().stream()
                .filter(flowMetaData -> flowMetaData.getName().equals(flow))
                .findFirst().ifPresentOrElse(flowMetaData -> {
                flowMetaData.getFlowElements().stream()
                    .filter(flowElementMetaData -> flowElementMetaData.getComponentName().equals(component))
                    .findFirst().ifPresentOrElse(id -> configurationMetaData.set(configurationRestService
                        .getConfiguredResourceConfiguration(agent.getUrl(), agent.getName(), flow, component))
                    , () -> {
                        throw new RuntimeException(String.format("Could not load configuration metadata for agent[%s], flow[%s], component[%s] at url[%s]!"
                            , agent.getName(), flow, component, agent.getUrl()));
                    });
            }, () -> {
                throw new RuntimeException(String.format("Could not load flow for agent[%s], flow[%s], component[%s] at url[%s]!"
                    , agent.getName(), flow, component, agent.getUrl()));
            });

        }, () -> {
            throw new RuntimeException(String.format("Could not load module metadata for agent[%s] at url[%s]!", agent.getName(), agent.getUrl()));
        });


        return configurationMetaData.get();
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
        this.jobGroupTf.setEnabled(enabled);
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
     * @param scheduleProcessAggregateConfiguration
     * @param editMode
     */
    public void setScheduleProcessAggregateConfiguration(ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration, EditMode editMode) {
        this.enabled = editMode == EditMode.NEW || editMode == EditMode.EDIT ? true : false;
        this.scheduleProcessAggregateConfiguration = scheduleProcessAggregateConfiguration;
        this.oldScheduleProcessAggregateConfiguration = scheduleProcessAggregateConfiguration;
        this.formBinder.readBean(this.scheduleProcessAggregateConfiguration);
        this.bindCollections(scheduleProcessAggregateConfiguration);
        this.editMode = editMode;

        // make sure all value are bound before calling set enabled
        this.setEnabled(this.enabled);
    }

    /**
     * Bind all collection fields to the form
     *
     * @param scheduleProcessAggregateConfiguration
     */
    private void bindCollections(ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
        scheduleProcessAggregateConfiguration.getSuccessfulReturnCodes().forEach(rc -> this.addSuccessfulReturnCodes(rc));

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
