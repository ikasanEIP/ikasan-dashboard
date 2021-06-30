package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.converter.StringToLongConverter;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.model.ScheduleProcessAggregateConfiguration;
import org.ikasan.dashboard.ui.scheduler.util.ScheduledProcessConstants;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.*;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NewSchedulerJobDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(NewSchedulerJobDialog.class);

    private ComboBox<String> agentCb;

    // Fields to capture schedule job properties.
    private TextField jobNameTf;
    private TextField jobGroupTf;
    private TextArea jobDescriptionTa;
    private TextField cronExpressionTf;
    private ComboBox<DateTimeUtil.TimezonePair> timezoneCb;
    private Checkbox ignoreMisfireCb;
    private Checkbox eagerCb;
    private TextField maxEagerCallbacksTf;
    private List<TextFieldNameValuePair> passThroughProperties;


    // Fields to capture job execution properties.
    private TextArea commandLineTf;
    private TextField workingDirectoryTf;
    private TextField secondsToWaitForProcessStartTf;
    private TextField stdOutTf;
    private TextField stdErrTf;
    private Checkbox retryOnFailCb;
    private List<TextField> successfulReturnCodes;

    // Fields to capture blackout router properties.
    private Button addBlackoutCron;
    private List<TextField> blackoutCronExpressions;
    private Button addDateTimeRange;
    private List<DateTimeRange> dateTimeRanges;


    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleMetaData agent;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;

    private Binder<ScheduleProcessAggregateConfiguration> formBinder;


    public NewSchedulerJobDialog(ModuleMetaData agent, ScheduledProcessManagementService scheduledProcessManagementService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService) {
        super.showResize(false);
        super.title.setText("New Scheduler Job");

        this.agent = agent;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;

        this.formBinder
            = new Binder<>(ScheduleProcessAggregateConfiguration.class);

        this.blackoutCronExpressions = new ArrayList<>();
        this.dateTimeRanges = new ArrayList<>();
        this.passThroughProperties = new ArrayList<>();
        this.successfulReturnCodes = new ArrayList<>();



        this.setHeight("850px");
        this.setWidth("1000px");

        Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->  {

            ScheduleProcessAggregateConfiguration scheduleProcessAggregateConfiguration = new ScheduleProcessAggregateConfiguration();
            if(!this.performFormValidation(scheduleProcessAggregateConfiguration)) {
                return;
            }
            createNewScheduledJobFlow(scheduleProcessAggregateConfiguration);
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
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
        super.content.add(layout);
    }

    private FormLayout createConfigurationForm() {
        FormLayout formLayout = new FormLayout();
        this.agentCb = new ComboBox<>("Agent");
        this.agentCb.setRequired(true);
        this.agentCb.setClearButtonVisible(true);
        this.agentCb.setItems(this.scheduledProcessManagementService.getAllAgentNames());
        if(agent != null) {
            this.agentCb.setValue(agent.getName());
            this.agentCb.setEnabled(false);
        }
        formBinder.forField(this.agentCb)
            .withValidator(agentValue -> !agentValue.isEmpty(), "Agent is required!")
            .bind(ScheduleProcessAggregateConfiguration::getAgentName, ScheduleProcessAggregateConfiguration::setAgentName);
        formLayout.add(agentCb, 2);

        // Fields to capture schedule job properties.
        H3 scheduleDetailsLabel = new H3("Schedule Details");
        formLayout.add(scheduleDetailsLabel, 2);

        this.jobNameTf = new TextField("Job name");
        this.jobNameTf.setRequired(true);
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), "Job name is required!")
            .bind(ScheduleProcessAggregateConfiguration::getJobName, ScheduleProcessAggregateConfiguration::setJobName);
        formLayout.add(jobNameTf);


        this.jobGroupTf = new TextField("Job group");
        this.jobGroupTf.setRequired(true);
        formBinder.forField(this.jobGroupTf)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), "Job group is required!")
            .bind(ScheduleProcessAggregateConfiguration::getJobGroup, ScheduleProcessAggregateConfiguration::setJobGroup);
        formLayout.add(jobGroupTf);


        this.jobDescriptionTa = new TextArea("Job description");
        this.jobDescriptionTa.setRequired(true);
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), "Job description is required!")
            .bind(ScheduleProcessAggregateConfiguration::getJobDescription, ScheduleProcessAggregateConfiguration::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);


        this.cronExpressionTf = new TextField("Cron expression");
        this.cronExpressionTf.setRequired(true);
        formBinder.forField(this.cronExpressionTf)
            // todo some cron validation
            .withValidator(value -> !value.isEmpty(), "Cron expression is required!")
            .bind(ScheduleProcessAggregateConfiguration::getCronExpression, ScheduleProcessAggregateConfiguration::setCronExpression);
        formLayout.add(cronExpressionTf);


        this.timezoneCb = new ComboBox<>("Timezone");
        this.timezoneCb.setRequired(true);
        ComboBox.ItemFilter<DateTimeUtil.TimezonePair> filter = (element, filterString) ->
            element.zoneId.toLowerCase().contains(filterString.toLowerCase());
        this.timezoneCb.setItems(filter, DateTimeUtil.getAllZoneIdsAndItsOffSet());
        this.timezoneCb.setItemLabelGenerator((ItemLabelGenerator<DateTimeUtil.TimezonePair>) s -> String.format("%35s (UTC%s) %n", s.zoneId, s.offset).trim());
        this.timezoneCb.setClearButtonVisible(true);
        this.timezoneCb.setPlaceholder("Choose a timezone");
        formBinder.forField(this.timezoneCb)
            .withValidator(value -> !value.zoneId.isEmpty(), "Timezone is required!")
            .bind(ScheduleProcessAggregateConfiguration::getTimezone, ScheduleProcessAggregateConfiguration::setTimezone);
        formLayout.add(timezoneCb);

        this.eagerCb = new Checkbox();
        this.eagerCb.setLabel("Eager");
        this.eagerCb.getStyle().set("padding-top", "15px");
        formBinder.forField(this.eagerCb)
            .bind(ScheduleProcessAggregateConfiguration::isEager, ScheduleProcessAggregateConfiguration::setEager);
        formLayout.add(this.eagerCb);

        this.ignoreMisfireCb = new Checkbox();
        this.ignoreMisfireCb.setLabel("Ignore misfire");
        this.ignoreMisfireCb.getStyle().set("padding-top", "15px");
        formBinder.forField(this.ignoreMisfireCb)
            .bind(ScheduleProcessAggregateConfiguration::isIgnoreMisfire, ScheduleProcessAggregateConfiguration::setIgnoreMisfire);
        formLayout.add(this.ignoreMisfireCb);

        this.maxEagerCallbacksTf = new TextField("Max eager callbacks");
        formBinder.forField(this.maxEagerCallbacksTf)
            .withConverter(new StringToIntegerConverter("Must be a number!"))
            .bind(ScheduleProcessAggregateConfiguration::getMaxEagerCallbacks, ScheduleProcessAggregateConfiguration::setMaxEagerCallbacks);
        formLayout.add(maxEagerCallbacksTf, new Div());

        Label passThroughPropertiesLabel = new Label("Pass through properties");
        Button passThroughPropertiesButton = new Button(VaadinIcon.PLUS.create(), e -> {
            TextFieldNameValuePair nvp = new TextFieldNameValuePair();

            nvp.nameTf = new TextField("Property name");
            nvp.nameTf.setWidth("95%");
            nvp.nameTf.setErrorMessage("Property name is required!");
            HorizontalLayout startLayout = new HorizontalLayout();
            startLayout.add(nvp.nameTf);

            nvp.valueTf = new TextField("Property value");
            nvp.valueTf.setWidth("95%");
            nvp.valueTf.setErrorMessage("Property name is required!");
            HorizontalLayout endLayout = new HorizontalLayout();

            this.passThroughProperties.add(nvp);

            Button minusButton = new Button(VaadinIcon.MINUS.create(), ev -> {
                formLayout.remove(startLayout);
                formLayout.remove(endLayout);

                this.passThroughProperties.remove(nvp);
            });

            endLayout.add(nvp.valueTf, minusButton);
            formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(passThroughPropertiesLabel.getElement()) + 2, startLayout);
            formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(passThroughPropertiesLabel.getElement()) + 3, endLayout);

        });

        formLayout.add(passThroughPropertiesLabel, passThroughPropertiesButton);

        // Fields to capture job execution properties.
        H3 jobExecutionLabel = new H3("Job Execution Details");
        formLayout.add(jobExecutionLabel, 2);

        this.commandLineTf = new TextArea("Command line");
        this.commandLineTf.setRequired(true);
        formBinder.forField(this.commandLineTf)
            .withValidator(value -> !value.isEmpty(), "Command line is required!")
            .bind(ScheduleProcessAggregateConfiguration::getCommandLine, ScheduleProcessAggregateConfiguration::setCommandLine);
        formLayout.add(commandLineTf, 2);
        commandLineTf.getStyle().set("minHeight", "100px");

        this.workingDirectoryTf = new TextField("Working Directory");
        formBinder.forField(this.workingDirectoryTf)
//            .withValidator(value -> !value.isEmpty(), "Working directory is required!")
            .bind(ScheduleProcessAggregateConfiguration::getWorkingDirectory, ScheduleProcessAggregateConfiguration::setWorkingDirectory);
        formLayout.add(workingDirectoryTf);

        this.secondsToWaitForProcessStartTf = new TextField("Seconds to wait for process start");
        formBinder.forField(this.secondsToWaitForProcessStartTf)
//            .withValidator(value -> !value.isEmpty(), "Seconds to wait for process start is required!")
            .withConverter(new StringToLongConverter("Must be a number!"))
            .bind(ScheduleProcessAggregateConfiguration::getSecondsToWaitForProcessStart, ScheduleProcessAggregateConfiguration::setSecondsToWaitForProcessStart);
        formLayout.add(secondsToWaitForProcessStartTf);

        this.stdOutTf = new TextField("Std out");
        formBinder.forField(this.stdOutTf)
            .withValidator(value -> !value.isEmpty(), "Standard out is required!")
            .bind(ScheduleProcessAggregateConfiguration::getStdOut, ScheduleProcessAggregateConfiguration::setStdOut);
        formLayout.add(stdOutTf);

        this.stdErrTf = new TextField("Std err");
        formBinder.forField(this.stdErrTf)
//            .withValidator(value -> !value.isEmpty(), "Standard err is required!")
            .bind(ScheduleProcessAggregateConfiguration::getStdErr, ScheduleProcessAggregateConfiguration::setStdErr);
        formLayout.add(this.stdErrTf);

        this.retryOnFailCb = new Checkbox("Retry on fail");
        formBinder.forField(this.retryOnFailCb)
            .bind(ScheduleProcessAggregateConfiguration::isRetryOnFail, ScheduleProcessAggregateConfiguration::setRetryOnFail);
        this.retryOnFailCb.getStyle().set("padding-top", "15px");
        formLayout.add(this.retryOnFailCb, new Div());

        Label successfulReturnCodesLabel = new Label("Successful return codes");
        Button successfulReturnCodesButton = new Button(VaadinIcon.PLUS.create(), e -> {
            TextField successfulReturnCodeTf = new TextField("Successful return code");
            this.successfulReturnCodes.add(successfulReturnCodeTf);
            successfulReturnCodeTf.setErrorMessage("Return code is required!");

            Button minusButton = new Button(VaadinIcon.MINUS.create(), ev -> {
                formLayout.remove(successfulReturnCodeTf);
                formLayout.remove(ev.getSource());
                this.successfulReturnCodes.remove(successfulReturnCodeTf);
            });
            formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(successfulReturnCodesLabel.getElement()) + 2, successfulReturnCodeTf);
            formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(successfulReturnCodesLabel.getElement()) + 3, minusButton);
        });

        formLayout.add(successfulReturnCodesLabel, successfulReturnCodesButton);

        H3 blackoutLabel = new H3("Blackout Execution Details");
        formLayout.add(blackoutLabel, 2);


        Label blackOutCronExpressionLabel = new Label("Blackout cron expressions");
        this.addBlackoutCron = new Button(VaadinIcon.PLUS.create(), e -> {
            TextField blackoutCronExpressionTf = new TextField("Blackout cron expression");
            this.blackoutCronExpressions.add(blackoutCronExpressionTf);
            blackoutCronExpressionTf.setErrorMessage("Cron expression required!");

            Button minusButton = new Button(VaadinIcon.MINUS.create(), ev -> {
                formLayout.remove(blackoutCronExpressionTf);
                formLayout.remove(ev.getSource());
                this.blackoutCronExpressions.remove(blackoutCronExpressionTf);
            });
            formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(blackOutCronExpressionLabel.getElement()) + 2, blackoutCronExpressionTf);
            formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(blackOutCronExpressionLabel.getElement()) + 3, minusButton);
        });

        formLayout.add(blackOutCronExpressionLabel, addBlackoutCron);

        Label blackOutDateTimeRangesLabel = new Label("Blackout date time ranges");
        this.addDateTimeRange = new Button(VaadinIcon.PLUS.create(), e -> {
            DateTimeRange dateTimeRange = new DateTimeRange();
            dateTimeRange.startDate = new DatePicker("Start date");
            dateTimeRange.startDate.setErrorMessage("Start date is required!");
            dateTimeRange.startDate.setLocale(UI.getCurrent().getLocale());
            dateTimeRange.startTime = new TimePicker("From");
            dateTimeRange.startTime.setErrorMessage("Start time is required!");
            dateTimeRange.startTime.setLocale(UI.getCurrent().getLocale());
            HorizontalLayout startLayout = new HorizontalLayout();
            startLayout.add(dateTimeRange.startDate, dateTimeRange.startTime);

            dateTimeRange.endDate = new DatePicker("End date");
            dateTimeRange.endDate.setErrorMessage("End date is required!");
            dateTimeRange.endDate.setLocale(UI.getCurrent().getLocale());
            dateTimeRange.endTime = new TimePicker("To");
            dateTimeRange.endTime.setErrorMessage("End date is required!");
            dateTimeRange.endTime.setLocale(UI.getCurrent().getLocale());
            HorizontalLayout endLayout = new HorizontalLayout();

            this.dateTimeRanges.add(dateTimeRange);

            Button minusButton = new Button(VaadinIcon.MINUS.create(), ev -> {
                formLayout.remove(startLayout);
                formLayout.remove(endLayout);

                this.dateTimeRanges.remove(dateTimeRange);
            });

            endLayout.add(dateTimeRange.endDate, dateTimeRange.endTime, minusButton);

            formLayout.add(startLayout, endLayout);
        });

        formLayout.add(blackOutDateTimeRangesLabel, this.addDateTimeRange);

        return formLayout;
    }

    private boolean performFormValidation(ScheduleProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {

        try {
            AtomicBoolean isValid = new AtomicBoolean(true);

            scheduleProcessAggregateConfiguration.getBlackoutDateTimeRanges().clear();
            this.dateTimeRanges.forEach(dateTimeRange -> {
                if(dateTimeRange.startDate.getValue() == null){
                    dateTimeRange.startDate.setInvalid(true);
                    isValid.set(false);
                }
                if(dateTimeRange.startTime.getValue() == null){
                    dateTimeRange.startTime.setInvalid(true);
                    isValid.set(false);
                }
                if(dateTimeRange.endDate.getValue() == null){
                    dateTimeRange.endDate.setInvalid(true);
                    isValid.set(false);
                }
                if(dateTimeRange.endTime.getValue() == null){
                    dateTimeRange.endTime.setInvalid(true);
                    isValid.set(false);
                }

                if(isValid.get()) {
                    long startMilli = (dateTimeRange.startDate.getValue().atStartOfDay(DateTimeUtil.getZoneId()).toEpochSecond() * 1000) +
                        (dateTimeRange.startTime.getValue().toSecondOfDay()*1000);
                    long endMilli = (dateTimeRange.endDate.getValue().atStartOfDay(DateTimeUtil.getZoneId()).toEpochSecond() * 1000) +
                        (dateTimeRange.endTime.getValue().toSecondOfDay()*1000);

                    scheduleProcessAggregateConfiguration.getBlackoutDateTimeRanges().put(startMilli, endMilli);
                }
            });

            scheduleProcessAggregateConfiguration.getBlackoutCronExpressions().clear();
            this.blackoutCronExpressions.forEach(expression -> {
                if(expression.getValue() == null || expression.getValue().isEmpty()) {
                    expression.setInvalid(true);
                    isValid.set(false);
                } else if(!CronExpression.isValidExpression(expression.getValue())){
                    expression.setErrorMessage("Must be a valid cron expression!");
                    expression.setInvalid(true);
                    isValid.set(false);
                }

                if(isValid.get()) {
                    scheduleProcessAggregateConfiguration.getBlackoutCronExpressions().add(expression.getValue());
                }
            });

            scheduleProcessAggregateConfiguration.getSuccessfulReturnCodes().clear();
            this.successfulReturnCodes.forEach(expression -> {
                if(expression.getValue() == null || expression.getValue().isEmpty()) {
                    expression.setInvalid(true);
                    isValid.set(false);
                }
                else {
                    try {
                        scheduleProcessAggregateConfiguration.getSuccessfulReturnCodes().add(Integer.parseInt(expression.getValue()));
                    }
                    catch (NumberFormatException e) {
                        expression.setErrorMessage("Return codes must be a number!");
                        expression.setInvalid(true);
                        isValid.set(false);
                    }
                }
            });

            scheduleProcessAggregateConfiguration.getPassthroughProperties().clear();
            this.passThroughProperties.forEach(expression -> {
                if(expression.nameTf.getValue() == null || expression.nameTf.getValue().isEmpty()) {
                    expression.nameTf.setInvalid(true);
                    isValid.set(false);
                }

                if(expression.valueTf.getValue() == null || expression.valueTf.getValue().isEmpty()) {
                    expression.valueTf.setInvalid(true);
                    isValid.set(false);
                }

                if(isValid.get()) {
                    scheduleProcessAggregateConfiguration.getPassthroughProperties().put(expression.nameTf.getValue(), expression.valueTf.getValue());
                }
            });

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
     * This method interacts with with agent in order to create a new scheduler agent flow and associated job.
     *
     * @param scheduleProcessAggregateConfiguration
     */
    public void createNewScheduledJobFlow(ScheduleProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
        // Get the module configuration from the module.
        ConfigurationMetaData<List<ConfigurationParameterMetaData>> moduleConfiguration
            = this.configurationRestService.getModuleConfiguration(this.agent.getUrl());

        if(moduleConfiguration == null) {
            throw new RuntimeException(String.format("Could not find module configuration for agent[%s]", agent));
        }

        logger.info("Module Configuration: " + moduleConfiguration);

        // Get the flowDefinitions from the configuration metadata.
        moduleConfiguration.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitions"))
            .findFirst().ifPresentOrElse(flowDefinitions -> {
                // Add the new job flow to the map.
                Map<String, String> configurationMap = (Map<String, String>)flowDefinitions.getValue();
                configurationMap.put(scheduleProcessAggregateConfiguration.getJobName(), "MANUAL");
                flowDefinitions.setValue(configurationMap);

                logger.info("Module Configuration: " + moduleConfiguration);
                // update the configuration back onto the module.
                this.configurationRestService.storeConfiguration(this.agent.getUrl(), moduleConfiguration);
            }, () -> {throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s]", agent));});


        // We need to deactivate and activate the module so the new flow is initialised
        boolean deactivateSuccess = this.moduleControlRestService.changeModuleActivationState(this.agent.getUrl(), this.agent.getName(), "deactivate");
        if(!deactivateSuccess) {
            throw new RuntimeException(String.format("Could not deactivate agent[%s]", agent));
        }
        boolean activateSuccess = this.moduleControlRestService.changeModuleActivationState(this.agent.getUrl(), this.agent.getName(), "activate");
        if(!activateSuccess) {
            throw new RuntimeException(String.format("Could not activate agent[%s]", agent));
        }

        /// Load the required configurations for a scheduled job.
        ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration = this.getConfigurationForAgentFlowComponent(this.agent,
            this.jobNameTf.getValue(), ScheduledProcessConstants.SCHEDULED_CONSUMER);
        ConfigurationMetaData<List<ConfigurationParameterMetaData>> blackoutRouterConfiguration = this.getConfigurationForAgentFlowComponent(this.agent,
            this.jobNameTf.getValue(), ScheduledProcessConstants.BLACKOUT_ROUTER);
        ConfigurationMetaData<List<ConfigurationParameterMetaData>> processExecutionBrokerConfiguration = this.getConfigurationForAgentFlowComponent(this.agent,
            this.jobNameTf.getValue(), ScheduledProcessConstants.PROCESS_EXECUTION_BROKER);

        // Update all the configurations with the configurations provided in the form.
        this.updateScheduleConsumerConfiguration(scheduledConsumerConfiguration, scheduleProcessAggregateConfiguration);
        this.updateBlackoutRouterConfiguration(blackoutRouterConfiguration, scheduleProcessAggregateConfiguration);
        this.updateProcessExecutionBrokerConfiguration(processExecutionBrokerConfiguration, scheduleProcessAggregateConfiguration);

        // Save all the configurations back to the agent.
        logger.debug(scheduledConsumerConfiguration.toString());
        this.configurationRestService.storeConfiguration(this.agent.getUrl(), scheduledConsumerConfiguration);
        logger.debug(blackoutRouterConfiguration.toString());
        this.configurationRestService.storeConfiguration(this.agent.getUrl(), blackoutRouterConfiguration);
        this.configurationRestService.storeConfiguration(this.agent.getUrl(), processExecutionBrokerConfiguration);
        logger.debug(processExecutionBrokerConfiguration.toString());
    }

    /**
     * Update the scheduled consumer configuration.
     *
     * @param scheduledConsumerConfiguration
     * @param scheduleProcessAggregateConfiguration
     */
    private void updateScheduleConsumerConfiguration(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
        , ScheduleProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "jobName",
            scheduleProcessAggregateConfiguration.getJobName());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "jobGroupName",
            scheduleProcessAggregateConfiguration.getJobGroup());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "description",
            scheduleProcessAggregateConfiguration.getJobDescription());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "cronExpression",
            scheduleProcessAggregateConfiguration.getCronExpression());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "timezone",
            scheduleProcessAggregateConfiguration.getTimezone().zoneId);
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "ignoreMisfire",
            scheduleProcessAggregateConfiguration.isIgnoreMisfire());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "eager",
            scheduleProcessAggregateConfiguration.isEager());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "maxEagerCallbacks",
            scheduleProcessAggregateConfiguration.getMaxEagerCallbacks());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "passthroughProperties",
            scheduleProcessAggregateConfiguration.getPassthroughProperties());
    }

    /**
     * Update the execution broker configuration.
     *
     * @param scheduledConsumerConfiguration
     * @param scheduleProcessAggregateConfiguration
     */
    private void updateProcessExecutionBrokerConfiguration(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
        , ScheduleProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "commandLine",
            scheduleProcessAggregateConfiguration.getCommandLine());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "workingDirectory",
            scheduleProcessAggregateConfiguration.getJobGroup());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "successfulReturnCodes",
            scheduleProcessAggregateConfiguration.getSuccessfulReturnCodes());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "secondsToWaitForProcessStart",
            scheduleProcessAggregateConfiguration.getSecondsToWaitForProcessStart());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "stdErr",
            scheduleProcessAggregateConfiguration.getStdErr());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "stdOut",
            scheduleProcessAggregateConfiguration.getStdOut());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "retryOnFail",
            scheduleProcessAggregateConfiguration.isRetryOnFail());
    }

    /**
     * Update the blackout router configuration.
     *
     * @param scheduledConsumerConfiguration
     * @param scheduleProcessAggregateConfiguration
     */
    private void updateBlackoutRouterConfiguration(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
        , ScheduleProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "cronExpressions",
            scheduleProcessAggregateConfiguration.getBlackoutCronExpressions());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, "dateTimeRanges",
            scheduleProcessAggregateConfiguration.getBlackoutDateTimeRanges());
    }

    /**
     * General method to set paramaters on a configuration meta data.
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
     * @param agent
     * @param flow
     * @param component
     * @return
     */
    private ConfigurationMetaData getConfigurationForAgentFlowComponent(ModuleMetaData agent, String flow, String component) {
        Optional<ModuleMetaData> moduleMetaData = this.metaDataRestService.getModuleMetadata(agent.getUrl(), agent.getName());

        AtomicReference<ConfigurationMetaData> configurationMetaData = new AtomicReference<>();

        moduleMetaData.ifPresentOrElse(metaData -> {
            metaData.getFlows().stream()
                .filter(flowMetaData -> flowMetaData.getName().equals(flow))
                .findFirst().get()
                .getFlowElements().stream()
                .filter(flowElementMetaData -> flowElementMetaData.getComponentName().equals(component))
                .findFirst().ifPresentOrElse(id -> configurationMetaData.set(configurationRestService
                    .getConfiguredResourceConfiguration(agent.getUrl(), agent.getName(), flow, component))
                        , () -> {throw new RuntimeException(String.format("Could not load configuration metadata for agent[%s], flow[%s], component[%s] at url[%s]!"
                            , agent.getName(), flow, component, agent.getUrl()));});
        },() -> {
            throw new RuntimeException(String.format("Could not load module metadata for agent[%s] at url[%s]!", agent.getName(), agent.getUrl()));
        });


        return configurationMetaData.get();
    }

    private class TextFieldNameValuePair {
        public TextField nameTf;
        public TextField valueTf;
    }

    private class DateTimeRange {
        public DatePicker startDate;
        public TimePicker startTime;
        public DatePicker endDate;
        public TimePicker endTime;
    }
}
