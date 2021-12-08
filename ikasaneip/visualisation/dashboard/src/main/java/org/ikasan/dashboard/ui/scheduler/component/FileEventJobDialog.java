package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.util.ScheduledProcessConstants;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
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
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.miki.superfields.dates.SuperDatePicker;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class FileEventJobDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(FileEventJobDialog.class);

    private ComboBox<String> agentCb;
    private Checkbox startAutomaticCb;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextField jobGroupTf;
    private TextArea jobDescriptionTa;
    private TextField filePathTf;
    private TextField cronExpressionTf;
    private ComboBox<DateTimeUtil.TimezonePair> timezoneCb;

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
    public FileEventJobDialog(ModuleMetaData agent, ScheduledProcessManagementService scheduledProcessManagementService,
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


        this.formBinder
            = new Binder<>(ScheduledProcessAggregateConfiguration.class);



        this.setHeight("650px");
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
        formLayout.add(this.startAutomaticCb);

        // Fields to capture schedule job properties.
        H3 scheduleDetailsLabel = new H3(getTranslation("header.schedule-details", UI.getCurrent().getLocale()));
        formLayout.add(scheduleDetailsLabel, 2);

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
            .withValidator(jobDescription -> !jobDescription.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(ScheduledProcessAggregateConfiguration::getJobDescription, ScheduledProcessAggregateConfiguration::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);

        this.filePathTf = new TextField("File path");
        this.filePathTf.setRequired(true);
        this.filePathTf.setId("filePathTf");
        formBinder.forField(this.filePathTf)
            .withValidator(filePath -> !filePath.isEmpty(), getTranslation("error.missing-job-description", UI.getCurrent().getLocale()))
            .bind(ScheduledProcessAggregateConfiguration::getJobDescription, ScheduledProcessAggregateConfiguration::setJobDescription);
        formLayout.add(filePathTf, 2);


        this.cronExpressionTf = new TextField(getTranslation("label.cron-expression", UI.getCurrent().getLocale()));
        this.cronExpressionTf.setRequired(true);
        this.cronExpressionTf.setId("cronExpressionTf");
        formBinder.forField(this.cronExpressionTf)
            .withValidator(value -> !value.isEmpty(), getTranslation("error.missing-cron-expression", UI.getCurrent().getLocale()))
            .withValidator(value -> CronExpression.isValidExpression(value), getTranslation("error.invalid-cron-expression", UI.getCurrent().getLocale()))
            .bind(ScheduledProcessAggregateConfiguration::getCronExpression, ScheduledProcessAggregateConfiguration::setCronExpression);
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
     * @param scheduleProcessAggregateConfiguration
     * @return
     */
    private boolean performFormValidation(ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {

        try {
            AtomicBoolean isValid = new AtomicBoolean(true);

            if(this.timezoneCb.getValue() != null) {
                scheduleProcessAggregateConfiguration.setTimezone(this.timezoneCb.getValue().zoneId);
            }

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
                    configurationMap.put(scheduleProcessAggregateConfiguration.getJobName(), "FILE");
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

            /// Load the required configurations for a scheduled job.
            Optional<ModuleMetaData> moduleMetaData = this.metaDataRestService.getModuleMetadata(agent.getUrl(), agent.getName());

            ConfigurationMetaData<List<ConfigurationParameterMetaData>> moduleConsumerConfiguration = this.getConfigurationForAgentFlowComponent(moduleMetaData,
                this.jobNameTf.getValue(), ScheduledProcessConstants.FILE_CONSUMER);

            // Update all the configurations with the configurations provided in the form.
            this.updateFileConsumerConfiguration(moduleConsumerConfiguration, scheduleProcessAggregateConfiguration);

            // Save all the configurations back to the agent.
            logger.debug(moduleConsumerConfiguration.toString());
            if(!this.configurationRestService.storeConfiguration(this.agent.getUrl(), moduleConsumerConfiguration)) {
                throw new RuntimeException(String.format("Could not store scheduled consumer configuration [%s]", moduleConsumerConfiguration));
            }
            this.scheduledProcessManagementService.saveConfiguration(moduleConsumerConfiguration);

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
                }, () -> {
                    throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s]", agent));
                });


                moduleConfiguration.getParameters().stream()
                    .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitionProfiles"))
                    .findFirst().ifPresentOrElse(flowDefinitions -> {
                    // Add the new job flow to the map.
                    Map<String, String> configurationMap = (Map<String, String>) flowDefinitions.getValue();
                    configurationMap.remove(scheduleProcessAggregateConfiguration.getJobName());
                    flowDefinitions.setValue(configurationMap);
                }, () -> {
                    throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s]", agent));
                });

                logger.info("Module Configuration: " + moduleConfiguration);
                // update the configuration back onto the module.
                this.configurationRestService.storeConfiguration(this.agent.getUrl(), moduleConfiguration);
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
     * Update the scheduled consumer configuration.
     *
     * @param scheduledConsumerConfiguration
     * @param scheduleProcessAggregateConfiguration
     */
    private void updateFileConsumerConfiguration(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
        , ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.JOB_NAME,
            scheduleProcessAggregateConfiguration.getJobName());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.JOB_GROUP_NAME,
            scheduleProcessAggregateConfiguration.getJobGroup());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.JOB_DESCRIPTION,
            scheduleProcessAggregateConfiguration.getJobDescription());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.CRON_EXPRESSION,
            scheduleProcessAggregateConfiguration.getCronExpression());
        if(scheduleProcessAggregateConfiguration.getTimezone() != null) {
            this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.TIMEZONE,
                scheduleProcessAggregateConfiguration.getTimezone());
        }
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.FILENAMES,
            List.of(scheduleProcessAggregateConfiguration.getCronExpression()));
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

        this.timezoneCb.setEnabled(enabled);

        this.jobNameTf.setEnabled(this.editMode == EditMode.NEW);
        this.jobGroupTf.setEnabled(enabled);
        this.jobDescriptionTa.setEnabled(enabled);
        this.cronExpressionTf.setEnabled(enabled);
        this.timezoneCb.setEnabled(enabled);

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
        this.timezoneCb.setValue(DateTimeUtil.getTimezonePairForZoneId(scheduleProcessAggregateConfiguration.getTimezone()));
        this.editMode = editMode;

        // make sure all value are bound before calling set enabled
        this.setEnabled(this.enabled);
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
