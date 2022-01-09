package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.provider.Query;
import org.apache.commons.io.IOUtils;
import org.ikasan.configuration.metadata.model.SolrConfigurationMetaData;
import org.ikasan.configuration.metadata.model.SolrConfigurationParameterMetaData;
import org.ikasan.dashboard.ui.UITest;
import org.ikasan.dashboard.ui.scheduler.util.ScheduledProcessConstants;
import org.ikasan.dashboard.ui.scheduler.view.SchedulerView;
import org.ikasan.module.metadata.model.SolrFlowElementMetaDataImpl;
import org.ikasan.module.metadata.model.SolrFlowMetaDataImpl;
import org.ikasan.module.metadata.model.SolrModuleMetaDataImpl;
import org.ikasan.module.metadata.model.SolrTransitionImpl;
import org.ikasan.scheduled.model.ScheduledProcessAggregateConfiguration;
import org.ikasan.scheduled.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.model.SolrScheduledProcessEvent;
import org.ikasan.scheduled.model.UpcomingScheduledProcess;
import org.ikasan.scheduled.service.SolrScheduledProcessServiceImpl;
import org.ikasan.spec.metadata.*;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.vaadin.miki.superfields.dates.SuperDatePicker;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.IntStream;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.eq;

public class ScheduledJobDialogTest extends UITest {

    @MockBean
    private SolrScheduledProcessServiceImpl scheduledProcessEventBatchInsert;

    @MockBean
    private ConfigurationService configurationRestService;

    @MockBean
    private ModuleControlService moduleControlRestService;

    @MockBean
    private MetaDataService metaDataApplicationRestService;

    @Autowired
    private ModuleMetaDataService moduleMetadataService;

    @Override
    public void setup_expectations() throws IOException {
        Mockito.when(this.scheduledProcessEventBatchInsert.getScheduledProcessEvents(Mockito.isNull(), Mockito.anyLong(),
            Mockito.anyLong(), Mockito.isNull(), Mockito.anyBoolean(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString()))
            .thenReturn(this.getScheduledEventsResults(50));

        Mockito.when(this.scheduledProcessEventBatchInsert.getUpComingScheduledProcesses(Mockito.isNull(), Mockito.anyLong(),
            Mockito.anyLong(), Mockito.isNull(), Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn(this.getUpcomingScheduledEventsResults(25));

        Mockito.when(this.scheduledProcessEventBatchInsert.getScheduleProcessAggregateConfigurations(Mockito.anyString(), Mockito.isNull()))
            .thenReturn(new ScheduledProcessEventSearchResults<>(List.of(), 0, 0));

        Mockito.when(this.moduleMetadataService.find(Mockito.any(ArrayList.class), Mockito.any(ModuleType.class),
            Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn(this.getAgents(1));

    }

    @Test
    public void test_create_new_scheduled_job_failed_form_validation() throws IOException
    {
        // Navigate to the scheduler view.
        UI.getCurrent().navigate("scheduler");

        // Get a handle to the agents grid and make sure there is some data in it.
        ScheduledAgentsFilteringGrid agentsFilteringGrid = _get(ScheduledAgentsFilteringGrid.class);
        Assertions.assertEquals(1, GridKt._size(agentsFilteringGrid));

        // Double click on the agent in the grid.
        GridKt._doubleClickItem(agentsFilteringGrid, 0);

        // We expect the agent management grid to open. Make sure that it has.
        SchedulerAgentManagementDialog schedulerAgentManagementDialog = _get(SchedulerAgentManagementDialog.class);
        Assertions.assertNotNull(schedulerAgentManagementDialog);

        // The purpose of the test is to make sure that form validation fails
        // when creating a new job. So we need to click the new job button.
        _click(_get(Button.class, spec -> spec.withId("newScheduledJobButton")));

        // Make sure the dialog for creating the new job has opened.
        ScheduledJobDialog scheduledJobDialog = _get(ScheduledJobDialog.class);
        Assertions.assertNotNull(scheduledJobDialog);

        // Make sure the agent combo box is populated as expected.
        Assertions.assertEquals("scheduler-agent", _get(ComboBox.class, spec -> spec.withId("agentCb")).getValue());

        // We want to add a pass through properties so we can check it fails validation
        _click(_get(Button.class, spec -> spec.withId("passThroughPropertiesButton")));

        // We want to add a couple return codes so we can check they fail validation
        _click(_get(Button.class, spec -> spec.withId("successfulReturnCodesButton")));
        _click(_get(Button.class, spec -> spec.withId("successfulReturnCodesButton")));

        // We want to add a blackout cron so we can check it fails validation
        _click(_get(Button.class, spec -> spec.withId("addBlackoutCron")));

        // We want to add a blackout date time range so we can check it fails validation
        _click(_get(Button.class, spec -> spec.withId("addDateTimeRange")));

        // Click the new job save button. This initiates the form validation.
        _click(_get(Button.class, spec -> spec.withId("scheduledJobSaveButton")));

        // Assert that are required fields have failed validation!
        Assertions.assertTrue(_get(TextField.class, spec -> spec.withId("jobNameTf")).isInvalid());
        Assertions.assertTrue(_get(TextField.class, spec -> spec.withId("jobGroupTf")).isInvalid());
        Assertions.assertTrue(_get(TextArea.class, spec -> spec.withId("jobDescriptionTa")).isInvalid());
        Assertions.assertTrue(_get(TextField.class, spec -> spec.withId("cronExpressionTf")).isInvalid());
        Assertions.assertTrue(_get(TextArea.class, spec -> spec.withId("commandLineTa")).isInvalid());
        Assertions.assertTrue(_get(TextField.class, spec -> spec.withId("stdOutTf")).isInvalid());
        Assertions.assertTrue(_get(TextField.class, spec -> spec.withId("stdErrTf")).isInvalid());
        Assertions.assertTrue(_get(TextField.class, spec -> spec.withId("successfulReturnCodeTf0")).isInvalid());
        Assertions.assertTrue(_get(TextField.class, spec -> spec.withId("successfulReturnCodeTf1")).isInvalid());
        Assertions.assertTrue(_get(TextField.class, spec -> spec.withId("blackoutCronExpressionTf0")).isInvalid());
        Assertions.assertTrue(_get(SuperDatePicker.class, spec -> spec.withId("dateTimeRange.startDate0")).isInvalid());
        Assertions.assertTrue(_get(TimePicker.class, spec -> spec.withId("dateTimeRange.startTime0")).isInvalid());
        Assertions.assertTrue(_get(SuperDatePicker.class, spec -> spec.withId("dateTimeRange.endDate0")).isInvalid());
        Assertions.assertTrue(_get(TimePicker.class, spec -> spec.withId("dateTimeRange.endTime0")).isInvalid());
        Assertions.assertTrue(_get(TextField.class, spec -> spec.withId("passThroughProperty.nameTf0")).isInvalid());
        Assertions.assertTrue(_get(TextField.class, spec -> spec.withId("passThroughProperty.valueTf0")).isInvalid());

        // Assert that there is a notification displayed!
        Assertions.assertNotNull(_get(Notification.class));
    }

    @Test
    public void test_create_new_scheduled_job_failed_form_validation_bad_cron_expreession() throws IOException
    {
        // Navigate to the scheduler view.
        UI.getCurrent().navigate("scheduler");

        // Get a handle to the agents grid and make sure there is some data in it.
        ScheduledAgentsFilteringGrid agentsFilteringGrid = _get(ScheduledAgentsFilteringGrid.class);
        Assertions.assertEquals(1, GridKt._size(agentsFilteringGrid));

        // Double click on the agent in the grid.
        GridKt._doubleClickItem(agentsFilteringGrid, 0);

        // We expect the agent management grid to open. Make sure that it has.
        SchedulerAgentManagementDialog schedulerAgentManagementDialog = _get(SchedulerAgentManagementDialog.class);
        Assertions.assertNotNull(schedulerAgentManagementDialog);

        // The purpose of the test is to make sure that form validation fails
        // when creating a new job. So we need to click the new job button.
        _click(_get(Button.class, spec -> spec.withId("newScheduledJobButton")));

        // Make sure the dialog for creating the new job has opened.
        ScheduledJobDialog scheduledJobDialog = _get(ScheduledJobDialog.class);
        Assertions.assertNotNull(scheduledJobDialog);

        // Make sure the agent combo box is populated as expected.
        Assertions.assertEquals("scheduler-agent", _get(ComboBox.class, spec -> spec.withId("agentCb")).getValue());

        // We want to add a pass through properties so we can check it fails validation
        _click(_get(Button.class, spec -> spec.withId("passThroughPropertiesButton")));

        // We want to add a couple return codes so we can check they fail validation
        _click(_get(Button.class, spec -> spec.withId("successfulReturnCodesButton")));
        _click(_get(Button.class, spec -> spec.withId("successfulReturnCodesButton")));

        // We want to add a blackout cron so we can check it fails validation
        _click(_get(Button.class, spec -> spec.withId("addBlackoutCron")));

        // We want to add a blackout date time range so we can check it fails validation
        _click(_get(Button.class, spec -> spec.withId("addDateTimeRange")));

        // Click the new job save button. This initiates the form validation.
        _click(_get(Button.class, spec -> spec.withId("scheduledJobSaveButton")));

        // Set values on all required fields valid except bad cron expreession
        _get(Checkbox.class, spec -> spec.withId("startAutomaticCb")).setValue(false);
        _get(TextField.class, spec -> spec.withId("jobNameTf")).setValue("20 minute");
        _get(TextField.class, spec -> spec.withId("jobGroupTf")).setValue("Job group");
        _get(TextArea.class, spec -> spec.withId("jobDescriptionTa")).setValue("Job description");
        _get(TextField.class, spec -> spec.withId("cronExpressionTf")).setValue("bad cron");
        _get(TextArea.class, spec -> spec.withId("commandLineTa")).setValue("ls -la");
        _get(TextField.class, spec -> spec.withId("stdOutTf")).setValue("out");
        _get(TextField.class, spec -> spec.withId("stdErrTf")).setValue("err");
        _get(TextField.class, spec -> spec.withId("successfulReturnCodeTf0")).setValue("0");
        _get(TextField.class, spec -> spec.withId("successfulReturnCodeTf1")).setValue("00");
        _get(TextField.class, spec -> spec.withId("blackoutCronExpressionTf0")).setValue("0 0/30 * * * ?");
        _get(SuperDatePicker.class, spec -> spec.withId("dateTimeRange.startDate0")).setValue(LocalDate.now());
        _get(TimePicker.class, spec -> spec.withId("dateTimeRange.startTime0")).setValue(LocalTime.now());
        _get(SuperDatePicker.class, spec -> spec.withId("dateTimeRange.endDate0")).setValue(LocalDate.now());
        _get(TimePicker.class, spec -> spec.withId("dateTimeRange.endTime0")).setValue(LocalTime.now());
        _get(TextField.class, spec -> spec.withId("passThroughProperty.nameTf0")).setValue("name");
        _get(TextField.class, spec -> spec.withId("passThroughProperty.valueTf0")).setValue("value");

        // Assert that are required fields have failed validation!
        Assertions.assertTrue(_get(TextField.class, spec -> spec.withId("cronExpressionTf")).isInvalid());


        Assertions.assertFalse(_get(TextField.class, spec -> spec.withId("jobNameTf")).isInvalid());
        Assertions.assertFalse(_get(TextField.class, spec -> spec.withId("jobGroupTf")).isInvalid());
        Assertions.assertFalse(_get(TextArea.class, spec -> spec.withId("jobDescriptionTa")).isInvalid());
        Assertions.assertFalse(_get(TextArea.class, spec -> spec.withId("commandLineTa")).isInvalid());
        Assertions.assertFalse(_get(TextField.class, spec -> spec.withId("stdOutTf")).isInvalid());
        Assertions.assertFalse(_get(TextField.class, spec -> spec.withId("stdErrTf")).isInvalid());
        Assertions.assertFalse(_get(TextField.class, spec -> spec.withId("successfulReturnCodeTf0")).isInvalid());
        Assertions.assertFalse(_get(TextField.class, spec -> spec.withId("successfulReturnCodeTf1")).isInvalid());
        Assertions.assertFalse(_get(TextField.class, spec -> spec.withId("blackoutCronExpressionTf0")).isInvalid());
        Assertions.assertFalse(_get(SuperDatePicker.class, spec -> spec.withId("dateTimeRange.startDate0")).isInvalid());
        Assertions.assertFalse(_get(TimePicker.class, spec -> spec.withId("dateTimeRange.startTime0")).isInvalid());
        Assertions.assertFalse(_get(SuperDatePicker.class, spec -> spec.withId("dateTimeRange.endDate0")).isInvalid());
        Assertions.assertFalse(_get(TimePicker.class, spec -> spec.withId("dateTimeRange.endTime0")).isInvalid());
        Assertions.assertFalse(_get(TextField.class, spec -> spec.withId("passThroughProperty.nameTf0")).isInvalid());
        Assertions.assertFalse(_get(TextField.class, spec -> spec.withId("passThroughProperty.valueTf0")).isInvalid());

        // Assert that there is a notification displayed!
        Assertions.assertNotNull(_get(Notification.class));
    }

    @Test
    public void test_create_new_scheduled_job_success() throws IOException
    {
        Mockito.when(this.configurationRestService.getModuleConfiguration(Mockito.anyString()))
            .thenReturn(this.getModuleConfiguration());

        Mockito.when(this.moduleControlRestService.changeModuleActivationState(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(true);

        Mockito.when(this.metaDataApplicationRestService.getModuleMetadata(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Optional.of(this.getModuleMetadata()));

        Mockito.when(this.configurationRestService.getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.SCHEDULED_CONSUMER)))
            .thenReturn(this.getModuleConfiguration("/data/scheduled-consumer-configuration.json"));

        Mockito.when(this.configurationRestService.getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.BLACKOUT_ROUTER)))
            .thenReturn(this.getModuleConfiguration("/data/blackout-router-configuration.json"));

        Mockito.when(this.configurationRestService.getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.PROCESS_EXECUTION_BROKER)))
            .thenReturn(this.getModuleConfiguration("/data/process-execution-broker-configuration.json"));

        Mockito.when(this.configurationRestService.storeConfiguration(Mockito.anyString(), Mockito.any(ConfigurationMetaData.class)))
           .thenReturn(true);


        // Navigate to the scheduler view.
        UI.getCurrent().navigate("scheduler");

        // Get a handle to the agents grid and make sure there is some data in it.
        ScheduledAgentsFilteringGrid agentsFilteringGrid = _get(ScheduledAgentsFilteringGrid.class);
        Assertions.assertEquals(1, GridKt._size(agentsFilteringGrid));

        // Double click on the agent in the grid.
        GridKt._doubleClickItem(agentsFilteringGrid, 0);

        // We expect the agent management grid to open. Make sure that it has.
        SchedulerAgentManagementDialog schedulerAgentManagementDialog = _get(SchedulerAgentManagementDialog.class);
        Assertions.assertNotNull(schedulerAgentManagementDialog);

        // The purpose of the test is to make sure that form validation fails
        // when creating a new job. So we need to click the new job button.
        _click(_get(Button.class, spec -> spec.withId("newScheduledJobButton")));

        // Make sure the dialog for creating the new job has opened.
        ScheduledJobDialog scheduledJobDialog = _get(ScheduledJobDialog.class);
        Assertions.assertNotNull(scheduledJobDialog);

        // Make sure the agent combo box is populated as expected.
        Assertions.assertEquals("scheduler-agent", _get(ComboBox.class, spec -> spec.withId("agentCb")).getValue());

        // We want to add a pass through properties so we can check it fails validation
        _click(_get(Button.class, spec -> spec.withId("passThroughPropertiesButton")));

        // We want to add a couple return codes so we can check they fail validation
        _click(_get(Button.class, spec -> spec.withId("successfulReturnCodesButton")));
        _click(_get(Button.class, spec -> spec.withId("successfulReturnCodesButton")));

        // We want to add a blackout cron so we can check it fails validation
        _click(_get(Button.class, spec -> spec.withId("addBlackoutCron")));

        // We want to add a blackout date time range so we can check it fails validation
        _click(_get(Button.class, spec -> spec.withId("addDateTimeRange")));

        // Set values on all required fields
        _get(Checkbox.class, spec -> spec.withId("startAutomaticCb")).setValue(false);
        _get(TextField.class, spec -> spec.withId("jobNameTf")).setValue("20 minute");
        _get(TextField.class, spec -> spec.withId("jobGroupTf")).setValue("Job group");
        _get(TextArea.class, spec -> spec.withId("jobDescriptionTa")).setValue("Job description");
        _get(TextField.class, spec -> spec.withId("cronExpressionTf")).setValue("0 0/30 * * * ?");
        _get(TextArea.class, spec -> spec.withId("commandLineTa")).setValue("ls -la");
        _get(TextField.class, spec -> spec.withId("stdOutTf")).setValue("out");
        _get(TextField.class, spec -> spec.withId("stdErrTf")).setValue("err");
        _get(TextField.class, spec -> spec.withId("successfulReturnCodeTf0")).setValue("0");
        _get(TextField.class, spec -> spec.withId("successfulReturnCodeTf1")).setValue("00");
        _get(TextField.class, spec -> spec.withId("blackoutCronExpressionTf0")).setValue("0 0/30 * * * ?");
        _get(SuperDatePicker.class, spec -> spec.withId("dateTimeRange.startDate0")).setValue(LocalDate.now());
        _get(TimePicker.class, spec -> spec.withId("dateTimeRange.startTime0")).setValue(LocalTime.now());
        _get(SuperDatePicker.class, spec -> spec.withId("dateTimeRange.endDate0")).setValue(LocalDate.now());
        _get(TimePicker.class, spec -> spec.withId("dateTimeRange.endTime0")).setValue(LocalTime.now());
        _get(TextField.class, spec -> spec.withId("passThroughProperty.nameTf0")).setValue("name");
        _get(TextField.class, spec -> spec.withId("passThroughProperty.valueTf0")).setValue("value");

        // Click the new job save button. This initiates the form validation.
        _click(_get(Button.class, spec -> spec.withId("scheduledJobSaveButton")));

        Mockito.verify(this.configurationRestService, Mockito.times(1))
            .getModuleConfiguration(Mockito.anyString());
        Mockito.verify(this.moduleControlRestService, Mockito.times(2))
            .changeModuleActivationState(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.metaDataApplicationRestService, Mockito.times(1))
            .getModuleMetadata(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.configurationRestService, Mockito.times(1))
            .getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.SCHEDULED_CONSUMER));
        Mockito.verify(this.configurationRestService, Mockito.times(1))
            .getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.BLACKOUT_ROUTER));
        Mockito.verify(this.configurationRestService, Mockito.times(1))
            .getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.PROCESS_EXECUTION_BROKER));
        Mockito.verify(this.configurationRestService, Mockito.times(4))
            .storeConfiguration(Mockito.anyString(), Mockito.any(ConfigurationMetaData.class));
        Mockito.verify(this.scheduledProcessEventBatchInsert, Mockito.times(3))
            .saveConfiguration(Mockito.any(ConfigurationMetaData.class));
        Mockito.verify(this.moduleControlRestService, Mockito.times(0))
            .changeFlowStartupType(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.moduleControlRestService, Mockito.times(2))
            .changeFlowState(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void test_create_new_scheduled_job_success_start_automatically() throws IOException
    {
        Mockito.when(this.configurationRestService.getModuleConfiguration(Mockito.anyString()))
            .thenReturn(this.getModuleConfiguration());

        Mockito.when(this.moduleControlRestService.changeModuleActivationState(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(true);

        Mockito.when(this.metaDataApplicationRestService.getModuleMetadata(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Optional.of(this.getModuleMetadata()));

        Mockito.when(this.configurationRestService.getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.SCHEDULED_CONSUMER)))
            .thenReturn(this.getModuleConfiguration("/data/scheduled-consumer-configuration.json"));

        Mockito.when(this.configurationRestService.getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.BLACKOUT_ROUTER)))
            .thenReturn(this.getModuleConfiguration("/data/blackout-router-configuration.json"));

        Mockito.when(this.configurationRestService.getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.PROCESS_EXECUTION_BROKER)))
            .thenReturn(this.getModuleConfiguration("/data/process-execution-broker-configuration.json"));

        Mockito.when(this.configurationRestService.storeConfiguration(Mockito.anyString(), Mockito.any(ConfigurationMetaData.class)))
            .thenReturn(true);


        // Navigate to the scheduler view.
        UI.getCurrent().navigate("scheduler");

        // Get a handle to the agents grid and make sure there is some data in it.
        ScheduledAgentsFilteringGrid agentsFilteringGrid = _get(ScheduledAgentsFilteringGrid.class);
        Assertions.assertEquals(1, GridKt._size(agentsFilteringGrid));

        // Double click on the agent in the grid.
        GridKt._doubleClickItem(agentsFilteringGrid, 0);

        // We expect the agent management grid to open. Make sure that it has.
        SchedulerAgentManagementDialog schedulerAgentManagementDialog = _get(SchedulerAgentManagementDialog.class);
        Assertions.assertNotNull(schedulerAgentManagementDialog);

        // The purpose of the test is to make sure that form validation fails
        // when creating a new job. So we need to click the new job button.
        _click(_get(Button.class, spec -> spec.withId("newScheduledJobButton")));

        // Make sure the dialog for creating the new job has opened.
        ScheduledJobDialog scheduledJobDialog = _get(ScheduledJobDialog.class);
        Assertions.assertNotNull(scheduledJobDialog);

        // Make sure the agent combo box is populated as expected.
        Assertions.assertEquals("scheduler-agent", _get(ComboBox.class, spec -> spec.withId("agentCb")).getValue());

        // We want to add a pass through properties so we can check it fails validation
        _click(_get(Button.class, spec -> spec.withId("passThroughPropertiesButton")));

        // We want to add a couple return codes so we can check they fail validation
        _click(_get(Button.class, spec -> spec.withId("successfulReturnCodesButton")));
        _click(_get(Button.class, spec -> spec.withId("successfulReturnCodesButton")));

        // We want to add a blackout cron so we can check it fails validation
        _click(_get(Button.class, spec -> spec.withId("addBlackoutCron")));

        // We want to add a blackout date time range so we can check it fails validation
        _click(_get(Button.class, spec -> spec.withId("addDateTimeRange")));

        // Set values on all required fields
        _get(Checkbox.class, spec -> spec.withId("startAutomaticCb")).setValue(true);
        _get(TextField.class, spec -> spec.withId("jobNameTf")).setValue("20 minute");
        _get(TextField.class, spec -> spec.withId("jobGroupTf")).setValue("Job group");
        _get(TextArea.class, spec -> spec.withId("jobDescriptionTa")).setValue("Job description");
        _get(TextField.class, spec -> spec.withId("cronExpressionTf")).setValue("0 0/30 * * * ?");
        _get(TextArea.class, spec -> spec.withId("commandLineTa")).setValue("ls -la");
        _get(TextField.class, spec -> spec.withId("stdOutTf")).setValue("out");
        _get(TextField.class, spec -> spec.withId("stdErrTf")).setValue("err");
        _get(TextField.class, spec -> spec.withId("successfulReturnCodeTf0")).setValue("0");
        _get(TextField.class, spec -> spec.withId("successfulReturnCodeTf1")).setValue("00");
        _get(TextField.class, spec -> spec.withId("blackoutCronExpressionTf0")).setValue("0 0/30 * * * ?");
        _get(SuperDatePicker.class, spec -> spec.withId("dateTimeRange.startDate0")).setValue(LocalDate.now());
        _get(TimePicker.class, spec -> spec.withId("dateTimeRange.startTime0")).setValue(LocalTime.now());
        _get(SuperDatePicker.class, spec -> spec.withId("dateTimeRange.endDate0")).setValue(LocalDate.now());
        _get(TimePicker.class, spec -> spec.withId("dateTimeRange.endTime0")).setValue(LocalTime.now());
        _get(TextField.class, spec -> spec.withId("passThroughProperty.nameTf0")).setValue("name");
        _get(TextField.class, spec -> spec.withId("passThroughProperty.valueTf0")).setValue("value");

        // Click the new job save button. This initiates the form validation.
        _click(_get(Button.class, spec -> spec.withId("scheduledJobSaveButton")));

        Mockito.verify(this.configurationRestService, Mockito.times(1))
            .getModuleConfiguration(Mockito.anyString());
        Mockito.verify(this.moduleControlRestService, Mockito.times(2))
            .changeModuleActivationState(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.metaDataApplicationRestService, Mockito.times(1))
            .getModuleMetadata(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.configurationRestService, Mockito.times(1))
            .getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.SCHEDULED_CONSUMER));
        Mockito.verify(this.configurationRestService, Mockito.times(1))
            .getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.BLACKOUT_ROUTER));
        Mockito.verify(this.configurationRestService, Mockito.times(1))
            .getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.PROCESS_EXECUTION_BROKER));
        Mockito.verify(this.configurationRestService, Mockito.times(5))
            .storeConfiguration(Mockito.anyString(), Mockito.any(ConfigurationMetaData.class));
        Mockito.verify(this.scheduledProcessEventBatchInsert, Mockito.times(3))
            .saveConfiguration(Mockito.any(ConfigurationMetaData.class));
        Mockito.verify(this.moduleControlRestService, Mockito.times(1))
            .changeFlowStartupType(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.moduleControlRestService, Mockito.times(2))
            .changeFlowState(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void test_edit_existing_scheduled_job() throws IOException
    {
        Mockito.when(this.configurationRestService.getModuleConfiguration(Mockito.anyString()))
            .thenReturn(this.getModuleConfiguration());

        Mockito.when(this.moduleControlRestService.changeModuleActivationState(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
            .thenReturn(true);

        Mockito.when(this.metaDataApplicationRestService.getModuleMetadata(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(Optional.of(this.getModuleMetadata()));

        Mockito.when(this.configurationRestService.getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.SCHEDULED_CONSUMER)))
            .thenReturn(this.getModuleConfiguration("/data/scheduled-consumer-configuration.json"));

        Mockito.when(this.configurationRestService.getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.BLACKOUT_ROUTER)))
            .thenReturn(this.getModuleConfiguration("/data/blackout-router-configuration.json"));

        Mockito.when(this.configurationRestService.getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.PROCESS_EXECUTION_BROKER)))
            .thenReturn(this.getModuleConfiguration("/data/process-execution-broker-configuration.json"));

        Mockito.when(this.configurationRestService.storeConfiguration(Mockito.anyString(), Mockito.any(ConfigurationMetaData.class)))
            .thenReturn(true);

        Mockito.when(this.scheduledProcessEventBatchInsert.getScheduleProcessAggregateConfigurations(Mockito.anyString(), Mockito.isNull()))
            .thenReturn(this.getScheduledProcessAggregateConfiguration());


        // Navigate to the scheduler view.
        UI.getCurrent().navigate("scheduler");

        // Get a handle to the agents grid and make sure there is some data in it.
        ScheduledAgentsFilteringGrid agentsFilteringGrid = _get(ScheduledAgentsFilteringGrid.class);
        Assertions.assertEquals(1, GridKt._size(agentsFilteringGrid));

        // Double click on the agent in the grid.
        GridKt._doubleClickItem(agentsFilteringGrid, 0);

        // We expect the agent management grid to open. Make sure that it has.
        SchedulerAgentManagementDialog schedulerAgentManagementDialog = _get(SchedulerAgentManagementDialog.class);
        Assertions.assertNotNull(schedulerAgentManagementDialog);

        // Get a handle to the agents grid and make sure there is some data in it.
        AgentJobFilteringGrid agentJobFilteringGrid = _get(AgentJobFilteringGrid.class);
        Assertions.assertEquals(1, GridKt._size(agentJobFilteringGrid));

        // Do some funky stuff to get a handle to the edit icon in the grid row and fire the click event.
        HorizontalLayout actions = ((HorizontalLayout) GridKt._getCellComponent(agentJobFilteringGrid, 0, "actions"));
        Icon edit = (Icon)actions.getChildren()
            .filter(component ->  {
                if(component.getId().isPresent()) {
                    return component.getId().get().equals("editScheduledJob");
                }

                return false;
            })
            .findFirst()
            .get();
        ComponentUtil.fireEvent(edit, new ClickEvent<>(edit));

        // Make sure the dialog for creating the new job has opened.
        ScheduledJobDialog scheduledJobDialog = _get(ScheduledJobDialog.class);
        Assertions.assertNotNull(scheduledJobDialog);

        // Make sure the fields are populated as expected.
        Assertions.assertEquals("scheduler-agent", _get(ComboBox.class, spec -> spec.withId("agentCb")).getValue());
        Assertions.assertEquals("20 minute", _get(TextField.class, spec -> spec.withId("jobNameTf")).getValue());

        // Click the new job save button. This initiates the form validation.
        _click(_get(Button.class, spec -> spec.withId("scheduledJobSaveButton")));

        Mockito.verify(this.configurationRestService, Mockito.times(1))
            .getModuleConfiguration(Mockito.anyString());
        Mockito.verify(this.moduleControlRestService, Mockito.times(0))
            .changeModuleActivationState(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.metaDataApplicationRestService, Mockito.times(1))
            .getModuleMetadata(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.configurationRestService, Mockito.times(1))
            .getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.SCHEDULED_CONSUMER));
        Mockito.verify(this.configurationRestService, Mockito.times(1))
            .getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.BLACKOUT_ROUTER));
        Mockito.verify(this.configurationRestService, Mockito.times(1))
            .getConfiguredResourceConfiguration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), eq(ScheduledProcessConstants.PROCESS_EXECUTION_BROKER));
        Mockito.verify(this.configurationRestService, Mockito.times(4))
            .storeConfiguration(Mockito.anyString(), Mockito.any(ConfigurationMetaData.class));
        Mockito.verify(this.scheduledProcessEventBatchInsert, Mockito.times(3))
            .saveConfiguration(Mockito.any(ConfigurationMetaData.class));
        Mockito.verify(this.moduleControlRestService, Mockito.times(1))
            .changeFlowStartupType(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.moduleControlRestService, Mockito.times(2))
            .changeFlowState(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    protected ScheduledProcessEventSearchResults<ScheduledProcessEvent> getScheduledEventsResults(int size) {

        ArrayList<ScheduledProcessEvent> ikasanSolrDocuments = new ArrayList<>();

        IntStream.range(0, size).forEach(i -> {
            ScheduledProcessEvent document = new SolrScheduledProcessEvent();
            document.setAgentName("agentName");
            document.setJobName("jobName");
            document.setJobDescription("job description");
            document.setCommandLine("command line");
            document.setUser("user");
            document.setCompletionTime(System.currentTimeMillis()+i);
            document.setNextFireTime(System.currentTimeMillis()+(i*1000));

            ikasanSolrDocuments.add(document);
        });

        return new ScheduledProcessEventSearchResults(ikasanSolrDocuments
            , ikasanSolrDocuments.size(), 1);
    }

    protected ScheduledProcessEventSearchResults<UpcomingScheduledProcess> getUpcomingScheduledEventsResults(int size) {

        ArrayList<UpcomingScheduledProcess> ikasanSolrDocuments = new ArrayList<>();

        IntStream.range(0, size).forEach(i -> {
            UpcomingScheduledProcess document = new UpcomingScheduledProcess("agentName", "jobName",
                "job group", "job description", System.currentTimeMillis(), null
                , null, null, "UTC");


            ikasanSolrDocuments.add(document);
        });

        return new ScheduledProcessEventSearchResults(ikasanSolrDocuments
            , ikasanSolrDocuments.size(), 1);
    }

    private ModuleMetadataSearchResults getAgents(int size) {
        ArrayList<ModuleMetaData> ikasanSolrDocuments = new ArrayList<>();

        IntStream.range(0, size).forEach(i -> {
            ModuleMetaData document = new SolrModuleMetaDataImpl();
            document.setName("scheduler-agent");
            document.setConfiguredResourceId("id");
            document.setDescription("description");
            document.setUrl("http://localhost:8080/agent");

            ikasanSolrDocuments.add(document);
        });

        return new ModuleMetadataSearchResults(ikasanSolrDocuments
            , ikasanSolrDocuments.size(), 1);
    }

    private ModuleMetaData getModuleMetadata() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        SimpleModule m = new SimpleModule();
        m.addAbstractTypeMapping(ModuleMetaData.class, SolrModuleMetaDataImpl.class);
        m.addAbstractTypeMapping(FlowMetaData.class, SolrFlowMetaDataImpl.class);
        m.addAbstractTypeMapping(FlowElementMetaData.class, SolrFlowElementMetaDataImpl.class);
        m.addAbstractTypeMapping(Transition.class, SolrTransitionImpl.class);

        objectMapper.registerModule(m);
        return objectMapper.readValue(this.loadDataFile("/data/scheduler-module-metadata.json"), SolrModuleMetaDataImpl.class);
    }

    private SolrConfigurationMetaData getModuleConfiguration() {
        SolrConfigurationParameterMetaData configurationParameterMetaData = new SolrConfigurationParameterMetaData();
        configurationParameterMetaData.setDescription("description");
        configurationParameterMetaData.setImplementingClass(Map.class.getName());
        configurationParameterMetaData.setName("flowDefinitions");
        configurationParameterMetaData.setValue(new HashMap<>());

        ArrayList<SolrConfigurationParameterMetaData> params = new ArrayList<>();
        params.add(configurationParameterMetaData);

        SolrConfigurationMetaData configurationMetaData = new SolrConfigurationMetaData();
        configurationMetaData.setParameters(params);

        return configurationMetaData;
    }

    private SolrConfigurationMetaData getModuleConfiguration(String filename) throws IOException {
        String configuration = this.loadDataFile(filename);

        ObjectMapper objectMapper = new ObjectMapper();

        SolrConfigurationMetaData configurationMetaData
            = objectMapper.readValue(configuration, SolrConfigurationMetaData.class);

        return configurationMetaData;
    }

    private ScheduledProcessEventSearchResults<ScheduledProcessAggregateConfiguration> getScheduledProcessAggregateConfiguration() {

        ScheduledProcessAggregateConfiguration scheduledProcessAggregateConfiguration = new ScheduledProcessAggregateConfiguration();
        scheduledProcessAggregateConfiguration.setAgentName("scheduler-agent");
        scheduledProcessAggregateConfiguration.setJobName("20 minute");
        scheduledProcessAggregateConfiguration.setCommandLine("commandLine");
        scheduledProcessAggregateConfiguration.setCronExpression("0 0/30 * * * ?");
        scheduledProcessAggregateConfiguration.setJobGroup("jobGroup");
        scheduledProcessAggregateConfiguration.setJobDescription("jobDescription");
        scheduledProcessAggregateConfiguration.setStartAutomatically(true);
        scheduledProcessAggregateConfiguration.setStdOut("out");
        scheduledProcessAggregateConfiguration.setStdErr("err");

        ArrayList<ScheduledProcessAggregateConfiguration> results = new ArrayList<>();
        results.add(scheduledProcessAggregateConfiguration);

        return new ScheduledProcessEventSearchResults(results, 1, 1);
    }


    protected String loadDataFile(String fileName) throws IOException
    {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName));

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException
    {
        return getClass().getResourceAsStream(fileName);
    }
}
