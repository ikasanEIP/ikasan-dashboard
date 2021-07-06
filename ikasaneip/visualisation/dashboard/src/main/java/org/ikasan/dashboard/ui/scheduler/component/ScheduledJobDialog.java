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
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.util.ScheduledProcessConstants;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.scheduled.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.model.ScheduledProcessConfigurationConstants;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ConfigurationMetaData;
import org.ikasan.spec.metadata.ConfigurationParameterMetaData;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.miki.shared.dates.DatePattern;
import org.vaadin.miki.shared.dates.DatePatterns;
import org.vaadin.miki.superfields.dates.SuperDatePicker;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ScheduledJobDialog extends AbstractCloseableResizableDialog {

    Logger logger = LoggerFactory.getLogger(ScheduledJobDialog.class);

    private ComboBox<String> agentCb;
    private Checkbox startAutomaticCb;

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

    private Button saveButton;
    private Button cancelButton;
    private Button successfulReturnCodesButton;
    private Button passThroughPropertiesButton;

    private Label noBlackOutCronExpressionLabel;
    private Label noBlackOutDateTimeRangesLabel;
    private Label noPassThoughPropertiesLabel;
    private Label noReturnCodesLabel;

    private Label passThroughPropertiesLabel;
    private Label successfulReturnCodesLabel;
    private Label blackOutCronExpressionLabel;
    private Div passThroughPropertiesDiv;
    private Label blackOutCronExpressionDiv;
    private Div returnCodesDiv;


    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleMetaData agent;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;

    private ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration = new ScheduledProcessAggregateConfiguration();

    private Binder<ScheduledProcessAggregateConfiguration> formBinder;

    private EditMode editMode = EditMode.NEW;

    private FormLayout formLayout;

    private boolean enabled = true;


    public ScheduledJobDialog(ModuleMetaData agent, ScheduledProcessManagementService scheduledProcessManagementService,
                              ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                              MetaDataService metaDataRestService) {
        super.showResize(false);
        super.title.setText("Scheduled Job");

        this.agent = agent;
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.configurationRestService = configurationRestService;
        this.moduleControlRestService = moduleControlRestService;
        this.metaDataRestService = metaDataRestService;

        this.noBlackOutCronExpressionLabel = new Label("no blackout cron expressions");
        this.noBlackOutCronExpressionLabel.setVisible(false);
        this.noBlackOutCronExpressionLabel.getStyle().set("color", "rgba(0, 0, 0, 0.38)");
        this.noBlackOutDateTimeRangesLabel = new Label("no blackout date time ranges");
        this.noBlackOutDateTimeRangesLabel.setVisible(false);
        this.noBlackOutDateTimeRangesLabel.getStyle().set("color", "rgba(0, 0, 0, 0.38)");
        this.noBlackOutDateTimeRangesLabel.getStyle().set("padding-bottom", "20px");
        this.noPassThoughPropertiesLabel = new Label("no pass through properties");
        this.noPassThoughPropertiesLabel.setVisible(false);
        this.noPassThoughPropertiesLabel.getStyle().set("color", "rgba(0, 0, 0, 0.38)");
        this.noReturnCodesLabel = new Label("no return codes");
        this.noReturnCodesLabel.setVisible(false);
        this.noReturnCodesLabel.getStyle().set("color", "rgba(0, 0, 0, 0.38)");


        this.formBinder
            = new Binder<>(ScheduledProcessAggregateConfiguration.class);

        this.blackoutCronExpressions = new ArrayList<>();
        this.dateTimeRanges = new ArrayList<>();
        this.passThroughProperties = new ArrayList<>();
        this.successfulReturnCodes = new ArrayList<>();



        this.setHeight("900px");
        this.setWidth("1200px");

        saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->  {

            if(!this.performFormValidation(this.scheduleProcessAggregateConfiguration)) {
                NotificationHelper.showErrorNotification("There are errors in the scheduled job configuration! " +
                    "Please rectify these before saving the form.");
                return;
            }

            try {
                createNewScheduledJobFlow(this.scheduleProcessAggregateConfiguration);
            }
            catch (Exception e) {
                e.printStackTrace();
                NotificationHelper.showErrorNotification("An error has occurred creating a new scheduled job. Please contact Ikasan support.");
                return;
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
            .bind(ScheduledProcessAggregateConfiguration::getAgentName, ScheduledProcessAggregateConfiguration::setAgentName);
        formLayout.add(agentCb, 2);

        this.startAutomaticCb = new Checkbox("Start automatically");
        formBinder.forField(this.startAutomaticCb)
            .bind(ScheduledProcessAggregateConfiguration::isStartAutomatically, ScheduledProcessAggregateConfiguration::setStartAutomatically);
        formLayout.add(this.startAutomaticCb);

        // Fields to capture schedule job properties.
        H3 scheduleDetailsLabel = new H3("Schedule Details");
        formLayout.add(scheduleDetailsLabel, 2);

        this.jobNameTf = new TextField("Job name");
        this.jobNameTf.setRequired(true);
        formBinder.forField(this.jobNameTf)
            .withValidator(jobName -> !jobName.isEmpty(), "Job name is required!")
            .bind(ScheduledProcessAggregateConfiguration::getJobName, ScheduledProcessAggregateConfiguration::setJobName);
        formLayout.add(jobNameTf);


        this.jobGroupTf = new TextField("Job group");
        this.jobGroupTf.setRequired(true);
        formBinder.forField(this.jobGroupTf)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), "Job group is required!")
            .bind(ScheduledProcessAggregateConfiguration::getJobGroup, ScheduledProcessAggregateConfiguration::setJobGroup);
        formLayout.add(jobGroupTf);


        this.jobDescriptionTa = new TextArea("Job description");
        this.jobDescriptionTa.setRequired(true);
        jobDescriptionTa.getStyle().set("minHeight", "100px");
        formBinder.forField(this.jobDescriptionTa)
            .withValidator(jobGroup -> !jobGroup.isEmpty(), "Job description is required!")
            .bind(ScheduledProcessAggregateConfiguration::getJobDescription, ScheduledProcessAggregateConfiguration::setJobDescription);
        formLayout.add(jobDescriptionTa, 2);


        this.cronExpressionTf = new TextField("Cron expression");
        this.cronExpressionTf.setRequired(true);
        formBinder.forField(this.cronExpressionTf)
            .withValidator(value -> !value.isEmpty(), "Cron expression is required!")
            .withValidator(value -> CronExpression.isValidExpression(value), "Must be a valid cron expression!")
            .bind(ScheduledProcessAggregateConfiguration::getCronExpression, ScheduledProcessAggregateConfiguration::setCronExpression);
        formLayout.add(cronExpressionTf);


        this.timezoneCb = new ComboBox<>("Timezone");
        this.timezoneCb.setRequired(true);
        ComboBox.ItemFilter<DateTimeUtil.TimezonePair> filter = (element, filterString) ->
            element.zoneId.toLowerCase().contains(filterString.toLowerCase());
        this.timezoneCb.setItems(filter, DateTimeUtil.getAllZoneIdsAndItsOffSet());
        this.timezoneCb.setItemLabelGenerator((ItemLabelGenerator<DateTimeUtil.TimezonePair>) s -> String.format("%35s (UTC%s) %n", s.zoneId, s.offset).trim());
        this.timezoneCb.setClearButtonVisible(true);
        this.timezoneCb.setPlaceholder("Choose a timezone");
        this.timezoneCb.setErrorMessage("Timezone is required!");
        formLayout.add(timezoneCb);

        this.eagerCb = new Checkbox();
        this.eagerCb.setLabel("Eager");
        this.eagerCb.getStyle().set("padding-top", "15px");
        formBinder.forField(this.eagerCb)
            .withNullRepresentation(false)
            .bind(ScheduledProcessAggregateConfiguration::isEager, ScheduledProcessAggregateConfiguration::setEager);
        formLayout.add(this.eagerCb);

        this.ignoreMisfireCb = new Checkbox();
        this.ignoreMisfireCb.setLabel("Ignore misfire");
        this.ignoreMisfireCb.getStyle().set("padding-top", "15px");
        formBinder.forField(this.ignoreMisfireCb)
            .withNullRepresentation(false)
            .bind(ScheduledProcessAggregateConfiguration::isIgnoreMisfire, ScheduledProcessAggregateConfiguration::setIgnoreMisfire);
        formLayout.add(this.ignoreMisfireCb);

        this.maxEagerCallbacksTf = new TextField("Max eager callbacks");
        formBinder.forField(this.maxEagerCallbacksTf)
            .withConverter(new StringToIntegerConverter("Must be a number!"))
            .withNullRepresentation(0)
            .bind(ScheduledProcessAggregateConfiguration::getMaxEagerCallbacks, ScheduledProcessAggregateConfiguration::setMaxEagerCallbacks);
        formLayout.add(maxEagerCallbacksTf, new Div());

        passThroughPropertiesLabel = new Label("Pass through properties");
        passThroughPropertiesButton = new Button(VaadinIcon.PLUS.create(), e -> {
            this.addPassThroughProperties(null,  null);
        });

        this.passThroughPropertiesDiv = new Div();
        this.passThroughPropertiesDiv.setVisible(false);
        formLayout.add(passThroughPropertiesLabel, passThroughPropertiesButton, noPassThoughPropertiesLabel, passThroughPropertiesDiv);

        // Fields to capture job execution properties.
        H3 jobExecutionLabel = new H3("Job Execution Details");
        formLayout.add(jobExecutionLabel, 2);

        this.commandLineTf = new TextArea("Command line");
        this.commandLineTf.setRequired(true);
        formBinder.forField(this.commandLineTf)
            .withValidator(value -> !value.isEmpty(), "Command line is required!")
            .bind(ScheduledProcessAggregateConfiguration::getCommandLine, ScheduledProcessAggregateConfiguration::setCommandLine);
        formLayout.add(commandLineTf, 2);
        commandLineTf.getStyle().set("minHeight", "100px");

        this.workingDirectoryTf = new TextField("Working Directory");
        this.workingDirectoryTf.setRequired(true);
        formBinder.forField(this.workingDirectoryTf)
            .withNullRepresentation("")
            .withValidator(value -> !value.isEmpty(), "Working directory is required!")
            .bind(ScheduledProcessAggregateConfiguration::getWorkingDirectory, ScheduledProcessAggregateConfiguration::setWorkingDirectory);
        formLayout.add(workingDirectoryTf);

        this.secondsToWaitForProcessStartTf = new TextField("Seconds to wait for process start");
        formBinder.forField(this.secondsToWaitForProcessStartTf)
            .withNullRepresentation("")
            .withConverter(new StringToLongConverter("Must be a number!"))
            .bind(ScheduledProcessAggregateConfiguration::getSecondsToWaitForProcessStart, ScheduledProcessAggregateConfiguration::setSecondsToWaitForProcessStart);
        formLayout.add(secondsToWaitForProcessStartTf);

        this.stdOutTf = new TextField("Std out");
        formBinder.forField(this.stdOutTf)
            .withNullRepresentation("")
            .withValidator(value -> !value.isEmpty(), "Standard out is required!")
            .bind(ScheduledProcessAggregateConfiguration::getStdOut, ScheduledProcessAggregateConfiguration::setStdOut);
        formLayout.add(stdOutTf);

        this.stdErrTf = new TextField("Std err");
        formBinder.forField(this.stdErrTf)
            .withNullRepresentation("")
            .bind(ScheduledProcessAggregateConfiguration::getStdErr, ScheduledProcessAggregateConfiguration::setStdErr);
        formLayout.add(this.stdErrTf);

        this.retryOnFailCb = new Checkbox("Retry on fail");
        formBinder.forField(this.retryOnFailCb)
            .withNullRepresentation(false)
            .bind(ScheduledProcessAggregateConfiguration::isRetryOnFail, ScheduledProcessAggregateConfiguration::setRetryOnFail);
        this.retryOnFailCb.getStyle().set("padding-top", "15px");
        formLayout.add(this.retryOnFailCb, new Div());

        successfulReturnCodesLabel = new Label("Successful return codes");
        successfulReturnCodesButton = new Button(VaadinIcon.PLUS.create(), e -> {
            this.addSuccessfulReturnCodes(null);
        });

        this.returnCodesDiv = new Div();
        this.returnCodesDiv.setVisible(false);
        formLayout.add(successfulReturnCodesLabel, successfulReturnCodesButton, this.noReturnCodesLabel, returnCodesDiv);

        H3 blackoutLabel = new H3("Blackout Execution Details");
        formLayout.add(blackoutLabel, 2);


        blackOutCronExpressionLabel = new Label("Blackout cron expressions");
        this.addBlackoutCron = new Button(VaadinIcon.PLUS.create(), e -> {
            this.addBlackoutCronExpression(null);
        });

        this.blackOutCronExpressionDiv = new Label("");
        this.blackOutCronExpressionDiv.setVisible(false);
        formLayout.add(blackOutCronExpressionLabel, addBlackoutCron, this.noBlackOutCronExpressionLabel, blackOutCronExpressionDiv);

        Label blackOutDateTimeRangesLabel = new Label("Blackout date time ranges");
        blackOutDateTimeRangesLabel.getStyle().set("padding-top", "30px");
        this.addDateTimeRange = new Button(VaadinIcon.PLUS.create(), e -> {
            this.addDateTimeRange(-1, -1);
        });

        formLayout.add(blackOutDateTimeRangesLabel, this.addDateTimeRange, this.noBlackOutDateTimeRangesLabel);

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
                    String startMilli = String.valueOf((dateTimeRange.startDate.getValue().atStartOfDay(DateTimeUtil.getZoneId()).toEpochSecond() * 1000) +
                        (dateTimeRange.startTime.getValue().toSecondOfDay()*1000));
                    String endMilli = String.valueOf((dateTimeRange.endDate.getValue().atStartOfDay(DateTimeUtil.getZoneId()).toEpochSecond() * 1000) +
                        (dateTimeRange.endTime.getValue().toSecondOfDay()*1000));

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
                        Integer.parseInt(expression.getValue());
                        scheduleProcessAggregateConfiguration.getSuccessfulReturnCodes().add(expression.getValue());
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

            if(this.timezoneCb.getValue() == null) {
                this.timezoneCb.setInvalid(true);
                isValid.set(false);
            }
            else{
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
     * This method interacts with with agent in order to create a new scheduler agent flow and associated job.
     *
     * @param scheduleProcessAggregateConfiguration
     */
    public void createNewScheduledJobFlow(ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
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

                    logger.info("Module Configuration: " + moduleConfiguration);
                    // update the configuration back onto the module.
                    this.configurationRestService.storeConfiguration(this.agent.getUrl(), moduleConfiguration);
                }, () -> {
                    throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s]", agent));
                });


                // We need to deactivate and activate the module so the new flow is initialised
                this.changeActivation("deactivate");
                this.changeActivation("activate");
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
            if(!this.configurationRestService.storeConfiguration(this.agent.getUrl(), scheduledConsumerConfiguration)) {
                throw new RuntimeException(String.format("Could not store scheduled consumer configuration [%s]", scheduledConsumerConfiguration));
            }
            this.scheduledProcessManagementService.saveConfiguration(scheduledConsumerConfiguration);

            logger.debug(blackoutRouterConfiguration.toString());
            if(!this.configurationRestService.storeConfiguration(this.agent.getUrl(), blackoutRouterConfiguration)) {
                throw new RuntimeException(String.format("Could not store blackout router configuration [%s]", blackoutRouterConfiguration));
            }
            this.scheduledProcessManagementService.saveConfiguration(blackoutRouterConfiguration);

            logger.debug(processExecutionBrokerConfiguration.toString());
            if(!this.configurationRestService.storeConfiguration(this.agent.getUrl(), processExecutionBrokerConfiguration)) {
                throw new RuntimeException(String.format("Could not store process execution configuration [%s]", blackoutRouterConfiguration));
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
    private void updateScheduleConsumerConfiguration(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
        , ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.JOB_NAME,
            scheduleProcessAggregateConfiguration.getJobName());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.JOB_GROUP_NAME,
            scheduleProcessAggregateConfiguration.getJobGroup());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.JOB_DESCRIPTION,
            scheduleProcessAggregateConfiguration.getJobDescription());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.CRON_EXPRESSION,
            scheduleProcessAggregateConfiguration.getCronExpression());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.TIMEZONE,
            scheduleProcessAggregateConfiguration.getTimezone());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.IGNORE_MISFIRE,
            scheduleProcessAggregateConfiguration.isIgnoreMisfire());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.EAGER,
            scheduleProcessAggregateConfiguration.isEager());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.MAX_EAGER_CALLBACKS,
            scheduleProcessAggregateConfiguration.getMaxEagerCallbacks());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.PASS_THROUGH_PROPERTIES,
            scheduleProcessAggregateConfiguration.getPassthroughProperties());
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
     * Update the blackout router configuration.
     *
     * @param scheduledConsumerConfiguration
     * @param scheduleProcessAggregateConfiguration
     */
    private void updateBlackoutRouterConfiguration(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
        , ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.CRON_EXPRESSIONS,
            scheduleProcessAggregateConfiguration.getBlackoutCronExpressions());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.DATE_TIME_RANGES,
            scheduleProcessAggregateConfiguration.getBlackoutDateTimeRanges());
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

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        this.timezoneCb.setEnabled(enabled);

        this.jobNameTf.setEnabled(enabled);
        this.jobGroupTf.setEnabled(enabled);
        this.jobDescriptionTa.setEnabled(enabled);
        this.cronExpressionTf.setEnabled(enabled);
        this.timezoneCb.setEnabled(enabled);
        this.ignoreMisfireCb.setEnabled(enabled);
        this.eagerCb.setEnabled(enabled);
        this.maxEagerCallbacksTf.setEnabled(enabled);

        this.passThroughPropertiesButton.setVisible(enabled);
        this.passThroughProperties.forEach(passThroughProperty -> {
            passThroughProperty.nameTf.setEnabled(enabled);
            passThroughProperty.valueTf.setEnabled(enabled);
        });
        if(this.passThroughProperties.size() == 0 && !enabled) {
            this.noPassThoughPropertiesLabel.setVisible(true);
        }
        else {
            this.noPassThoughPropertiesLabel.setVisible(false);
            this.passThroughPropertiesDiv.setVisible(true);
        }


        this.commandLineTf.setEnabled(enabled);
        this.workingDirectoryTf.setEnabled(enabled);
        this.secondsToWaitForProcessStartTf.setEnabled(enabled);
        this.stdOutTf.setEnabled(enabled);
        this.stdErrTf.setEnabled(enabled);
        this.retryOnFailCb.setEnabled(enabled);
        this.successfulReturnCodes.forEach(successfulReturnCode -> successfulReturnCode.setEnabled(enabled));
        this.successfulReturnCodesButton.setVisible(enabled);
        if(this.successfulReturnCodes.size() == 0 && !enabled) {
            this.noReturnCodesLabel.setVisible(true);
        }
        else {
            this.noReturnCodesLabel.setVisible(false);
            this.returnCodesDiv.setVisible(true);
        }

        this.addBlackoutCron.setVisible(enabled);
        this.blackoutCronExpressions.forEach(blackoutCronExpression -> blackoutCronExpression.setEnabled(enabled));
        if(this.blackoutCronExpressions.size() == 0 && !enabled) {
            this.noBlackOutCronExpressionLabel.setVisible(true);
        }
        else {
            this.noBlackOutCronExpressionLabel.setVisible(false);
        }
        this.addDateTimeRange.setVisible(enabled);
        this.dateTimeRanges.forEach(blackoutCronExpression -> {
            blackoutCronExpression.startDate.setEnabled(enabled);
            blackoutCronExpression.startTime.setEnabled(enabled);
            blackoutCronExpression.endDate.setEnabled(enabled);
            blackoutCronExpression.endTime.setEnabled(enabled);
        });
        if(this.dateTimeRanges.size() == 0 && !enabled) {
            this.noBlackOutDateTimeRangesLabel.setVisible(true);
        }
        else {
            this.noBlackOutDateTimeRangesLabel.setVisible(false);
        }

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
        this.formBinder.readBean(this.scheduleProcessAggregateConfiguration);
        this.timezoneCb.setValue(DateTimeUtil.getTimezonePairForZoneId(scheduleProcessAggregateConfiguration.getTimezone()));
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
        scheduleProcessAggregateConfiguration.getPassthroughProperties().forEach((k,v) -> this.addPassThroughProperties(k,v));
        scheduleProcessAggregateConfiguration.getSuccessfulReturnCodes().forEach(rc -> this.addSuccessfulReturnCodes(rc));
        scheduleProcessAggregateConfiguration.getBlackoutCronExpressions().forEach(ce -> this.addBlackoutCronExpression(ce));
        scheduleProcessAggregateConfiguration.getBlackoutDateTimeRanges().forEach((k,v) -> this.addDateTimeRange(Long.parseLong(k),
            Long.parseLong(v)));

    }

    /**
     * Helper method to add the controls for a blackout date time ranges
     *
     * @param startMilli
     * @param endMilli
     */
    private void addDateTimeRange(long startMilli, long endMilli) {
        DateTimeRange dateTimeRange = new DateTimeRange();
        dateTimeRange.startDate = new SuperDatePicker("Start date");
        dateTimeRange.startDate.setEnabled(this.enabled);
        dateTimeRange.startDate.setDatePattern(DatePatterns.D_MMMM_YYYY);
        dateTimeRange.startDate.setErrorMessage("Start date is required!");
        dateTimeRange.startDate.setLocale(UI.getCurrent().getLocale());
        if(startMilli > 0) {
            dateTimeRange.startDate.setValue(Instant.ofEpochMilli(startMilli).atZone(ZoneId.systemDefault()).toLocalDate());
        }
        dateTimeRange.startTime = new TimePicker("From");
        dateTimeRange.startTime.setEnabled(this.enabled);
        dateTimeRange.startTime.setErrorMessage("Start time is required!");
        dateTimeRange.startTime.setLocale(UI.getCurrent().getLocale());
        if(startMilli > 0) {
            dateTimeRange.startTime.setValue(Instant.ofEpochMilli(startMilli).atZone(ZoneId.systemDefault()).toLocalTime());
        }
        HorizontalLayout startLayout = new HorizontalLayout();
        startLayout.add(dateTimeRange.startDate, dateTimeRange.startTime);

        dateTimeRange.endDate = new SuperDatePicker("End date");
        dateTimeRange.endDate.setEnabled(this.enabled);
        dateTimeRange.endDate.setDatePattern(DatePatterns.D_MMMM_YYYY);
        dateTimeRange.endDate.setErrorMessage("End date is required!");
        dateTimeRange.endDate.setLocale(UI.getCurrent().getLocale());
        if(endMilli > 0) {
            dateTimeRange.endDate.setValue(Instant.ofEpochMilli(endMilli).atZone(ZoneId.systemDefault()).toLocalDate());
        }
        dateTimeRange.endTime = new TimePicker("To");
        dateTimeRange.endTime.setEnabled(this.enabled);
        dateTimeRange.endTime.setErrorMessage("End date is required!");
        dateTimeRange.endTime.setLocale(UI.getCurrent().getLocale());
        if(endMilli > 0) {
            dateTimeRange.endTime.setValue(Instant.ofEpochMilli(endMilli).atZone(ZoneId.systemDefault()).toLocalTime());
        }
        HorizontalLayout endLayout = new HorizontalLayout();

        this.dateTimeRanges.add(dateTimeRange);

        Button minusButton = new Button(VaadinIcon.MINUS.create(), ev -> {
            formLayout.remove(startLayout);
            formLayout.remove(endLayout);

            this.dateTimeRanges.remove(dateTimeRange);
        });
        minusButton.setVisible(this.enabled);

        endLayout.add(dateTimeRange.endDate, dateTimeRange.endTime, minusButton);
        formLayout.add(startLayout, endLayout);

        if(!this.enabled){
            formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(this.noBlackOutDateTimeRangesLabel.getElement()) + 1, new Div());
        }
    }

    /**
     * Helper method to add the controls for pass through properties
     *
     * @param name
     * @param value
     */
    private void addPassThroughProperties(String name, String value) {
        TextFieldNameValuePair nvp = new TextFieldNameValuePair();

        nvp.nameTf = new TextField("Property name");
        nvp.nameTf.setWidth("95%");
        nvp.nameTf.setEnabled(this.enabled);
        nvp.nameTf.setErrorMessage("Property name is required!");
        if(name!=null)nvp.nameTf.setValue(name);
        HorizontalLayout startLayout = new HorizontalLayout();
        startLayout.add(nvp.nameTf);

        nvp.valueTf = new TextField("Property value");
        nvp.valueTf.setEnabled(this.enabled);
        nvp.valueTf.setWidth("95%");
        nvp.valueTf.setErrorMessage("Property name is required!");
        if(value!=null)nvp.valueTf.setValue(value);
        HorizontalLayout endLayout = new HorizontalLayout();

        this.passThroughProperties.add(nvp);

        Button minusButton = new Button(VaadinIcon.MINUS.create(), ev -> {
            formLayout.remove(startLayout);
            formLayout.remove(endLayout);

            this.passThroughProperties.remove(nvp);
        });
        minusButton.setVisible(this.enabled);

        endLayout.add(nvp.valueTf, minusButton);
        formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(passThroughPropertiesLabel.getElement()) + (!this.enabled ? 4 : 3), startLayout);
        formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(passThroughPropertiesLabel.getElement()) + (!this.enabled ? 5 : 4), endLayout);
    }

    /**
     * Helper method to add the controls for the return codes
     *
     * @param returnCode
     */
    private void addSuccessfulReturnCodes(String returnCode) {
        TextField successfulReturnCodeTf = new TextField("Successful return code");
        successfulReturnCodeTf.setEnabled(this.enabled);
        this.successfulReturnCodes.add(successfulReturnCodeTf);
        successfulReturnCodeTf.setErrorMessage("Return code is required!");
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

    /**
     * Helper method to add the controls for a blackout cron expression
     *
     * @param cron
     */
    private void addBlackoutCronExpression(String cron) {
        TextField blackoutCronExpressionTf = new TextField("Blackout cron expression");
        blackoutCronExpressionTf.setEnabled(this.enabled);
        this.blackoutCronExpressions.add(blackoutCronExpressionTf);
        blackoutCronExpressionTf.setErrorMessage("Cron expression required!");
        if(cron!=null)blackoutCronExpressionTf.setValue(cron);

        Button minusButton = new Button(VaadinIcon.MINUS.create(), ev -> {
            formLayout.remove(blackoutCronExpressionTf);
            formLayout.remove(ev.getSource());
            this.blackoutCronExpressions.remove(blackoutCronExpressionTf);
        });
        minusButton.setVisible(this.enabled);
        this.blackOutCronExpressionDiv.setVisible(!this.enabled);
        formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(blackOutCronExpressionLabel.getElement()) + (!this.enabled ? 2 : 3), blackoutCronExpressionTf);
        formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(blackOutCronExpressionLabel.getElement()) + (!this.enabled ? 5 : 4), minusButton);
        formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(blackOutCronExpressionLabel.getElement()) + (!this.enabled ? 1 : 4), blackOutCronExpressionDiv);
        if(!this.enabled){
            formLayout.addComponentAtIndex(formLayout.getElement().indexOfChild(blackoutCronExpressionTf.getElement()) + 1, new Div());
        }

    }

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
