package org.ikasan.dashboard.ui.scheduler.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import org.ikasan.dashboard.ui.scheduler.AbstractSchedulerViewTest;
import org.ikasan.dashboard.ui.scheduler.view.SchedulerView;
import org.ikasan.dashboard.ui.util.DateTimeUtil;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileWatcherJobDialogTest extends AbstractSchedulerViewTest {

    @Override
    public void setup_expectations() throws IOException {
        when(this.scheduledContextService.findByFilterLite(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(getScheduledContextRecordLites(15), 15, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(0), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(getScheduledContextRecordLites(15), 15, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(15), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(getScheduledContextRecordLites(15), 15, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(0)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(1)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(1), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(1)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(2), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(2)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(3), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(3)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(4), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(4)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(5), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(5)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(6), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(6)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(7), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(7)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(8), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(8)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(9), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(9)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(10), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(10)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(11), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(11)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(12), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(12)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(13), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(13)), 1, 1));

        when(this.scheduledContextService.findByFilterLite(any(), eq(1), eq(14), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(getScheduledContextRecordLites(15).get(14)), 1, 1));

        when(this.scheduledContextService.findByName(anyString())).thenReturn(super.getScheduledContextRecord());

        when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(-1),eq(-1), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(0),eq(0), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(5),eq(0), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        when(this.moduleMetadataService.find(eq(List.of()), any(), anyInt(), anyInt()))
            .thenReturn(this.getAgents(3));

        when(this.moduleMetadataService.find(eq(List.of("*agent0*")), any(), anyInt(), anyInt()))
            .thenReturn(this.getAgents(1));

        when(this.schedulerJobInstanceService.getJobStatusCountForContextInstances(Mockito.any()))
            .thenReturn(new ArrayList<>(this.getAggregateContextInstanceStatuses()));

        when(this.schedulerJobService
            .findByContext(anyString(), eq(-1), eq(-1))).thenReturn(super.getSchedulerJobs());

        when(this.schedulerJobService
            .findByFilter(any(), eq(0), eq(0), isNull(), isNull())).thenReturn(super.getSchedulerJobs());

        when(this.contextProfileService.findByFilter(any(), eq(-1), eq(-1), isNull(), isNull()))
            .thenReturn(super.getContextProfiles());

        when(super.scheduledProcessManagementService.getAllAgentNames()).thenReturn(super.getAgents(2).getResultList()
            .stream()
            .map(moduleMetaData -> moduleMetaData.getName())
            .collect(Collectors.toList()));
    }

    @Test
    public void test_simple_access() {

        UI.getCurrent().navigate("scheduler");

        // We open the scheduler view
        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        // Get the tabs on the scheduler view and do some assertions in order to confirm all in good order.
        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Assertions.assertNotNull(schedulerDashboardTabs);
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        Assertions.assertNotNull(contextTemplateTab);

        // Select the context template tab and assert success.
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);
        Assertions.assertEquals(contextTemplateTab, schedulerDashboardTabs.getSelectedTab());

        // Now we gat a handle to the template grid on that screen.
        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        Assertions.assertNotNull(contextTemplateFilteringGrid);

        // Get a handle to the actions that can be performed on the context template in the first row of the grid.
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Assert.assertNotNull(actionsLayout);

        // Get the action icon that opens the context template management dialog containing the management widget.
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        // Assert that we can get the ContextTemplateManagementWidget.
        ContextTemplateManagementWidget contextTemplateManagementWidget = _get(ContextTemplateManagementWidget.class);
        Assertions.assertNotNull(contextTemplateManagementWidget);

        MenuBar actionsMenuBar = _get(MenuBar.class, spec -> spec.withId("actionsMenuBar"));
        Assertions.assertNotNull(actionsMenuBar);

        MenuItem jobTypesMenuItem = _get(MenuItem.class, spec -> spec.withId("jobTypesMenuItem"));
        Assertions.assertNotNull(jobTypesMenuItem);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        Assertions.assertNotNull(newFileWatcherJobMenuItem);

        _click(newFileWatcherJobMenuItem);

        FileEventJobDialog fileEventJobDialog = _get(FileEventJobDialog.class);
        Assertions.assertNotNull(fileEventJobDialog);
    }

    @Test
    public void test_create_new_static_file_watcher_job() {

        UI.getCurrent().navigate("scheduler");

        // We open the scheduler view
        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        // Get the tabs on the scheduler view and do some assertions in order to confirm all in good order.
        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Assertions.assertNotNull(schedulerDashboardTabs);
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        Assertions.assertNotNull(contextTemplateTab);

        // Select the context template tab and assert success.
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);
        Assertions.assertEquals(contextTemplateTab, schedulerDashboardTabs.getSelectedTab());

        // Now we gat a handle to the template grid on that screen.
        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        Assertions.assertNotNull(contextTemplateFilteringGrid);

        // Get a handle to the actions that can be performed on the context template in the first row of the grid.
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Assert.assertNotNull(actionsLayout);

        // Get the action icon that opens the context template management dialog containing the management widget.
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        // Assert that we can get the ContextTemplateManagementWidget.
        ContextTemplateManagementWidget contextTemplateManagementWidget = _get(ContextTemplateManagementWidget.class);
        Assertions.assertNotNull(contextTemplateManagementWidget);

        MenuBar actionsMenuBar = _get(MenuBar.class, spec -> spec.withId("actionsMenuBar"));
        Assertions.assertNotNull(actionsMenuBar);

        MenuItem jobTypesMenuItem = _get(MenuItem.class, spec -> spec.withId("jobTypesMenuItem"));
        Assertions.assertNotNull(jobTypesMenuItem);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        Assertions.assertNotNull(newFileWatcherJobMenuItem);

        _click(newFileWatcherJobMenuItem);

        FileEventJobDialog fileEventJobDialog = _get(FileEventJobDialog.class);
        Assertions.assertNotNull(fileEventJobDialog);

        Checkbox isDynamicCheckbox = _get(Checkbox.class, spec -> spec.withId("isDynamicCheckbox"));
        Assertions.assertNotNull(isDynamicCheckbox);
        Assertions.assertFalse(isDynamicCheckbox.getValue());

        TextField jobNameTf = _get(TextField.class, spec -> spec.withId("jobNameTf"));
        Assertions.assertNotNull(jobNameTf);

        ComboBox agentCb = _get(ComboBox.class, spec -> spec.withId("agentCb"));
        Assertions.assertNotNull(agentCb);
        Assertions.assertEquals(2, (((ListDataProvider)agentCb.getDataProvider()).getItems().size()));

        TextArea jobDescriptionTa = _get(TextArea.class, spec -> spec.withId("jobDescriptionTa"));
        Assertions.assertNotNull(jobDescriptionTa);

        TextField filenameTf = _get(TextField.class, spec -> spec.withId("filenameTf"));
        Assertions.assertNotNull(filenameTf);

        TextField filePathTf = _get(TextField.class, spec -> spec.withId("filePathTf"));
        Assertions.assertNotNull(filePathTf);

        TextField archiveDirectoryTf = _get(TextField.class, spec -> spec.withId("archiveDirectoryTf"));
        Assertions.assertNotNull(archiveDirectoryTf);

        TextField cronExpressionTf = _get(TextField.class, spec -> spec.withId("cronExpressionTf"));
        Assertions.assertNotNull(cronExpressionTf);

        TextField slaCronExpressionTf = _get(TextField.class, spec -> spec.withId("slaCronExpressionTf"));
        Assertions.assertNotNull(slaCronExpressionTf);

        ComboBox timezoneCb = _get(ComboBox.class, spec -> spec.withId("timezoneCb"));
        Assertions.assertNotNull(timezoneCb);

        Button scheduledJobSaveButton = _get(Button.class, spec -> spec.withId("scheduledJobSaveButton"));
        Assertions.assertNotNull(scheduledJobSaveButton);

        Button scheduledJobCancelButton = _get(Button.class, spec -> spec.withId("scheduledJobCancelButton"));
        Assertions.assertNotNull(scheduledJobCancelButton);

        _setValue(agentCb, "agent0");
        _setValue(jobNameTf, "jobName");
        _setValue(jobDescriptionTa, "This is the job description");
        _setValue(filenameTf, "file.txt");
        _setValue(filePathTf, "/file/path");
        _setValue(archiveDirectoryTf, "/archive/path");
        _setValue(cronExpressionTf, "0 0/1 * 1/1 * ? *");
        _setValue(slaCronExpressionTf, "0 0/2 * 1/1 * ? *");
        _setValue(timezoneCb, DateTimeUtil.getTimezonePairForZoneId(ZoneId.of("Europe/London").getId()));
        _click(scheduledJobSaveButton);

        verify(this.schedulerJobService).findByContext(anyString(), anyInt(), anyInt());
        verify(this.schedulerJobService).findByFilter(any(), anyInt(), anyInt(), isNull(), isNull());
        verify(this.schedulerJobService).findByContextNameAndJobName(anyString(), anyString());
        verify(this.schedulerJobService).saveFileEventDrivenJobRecord(any());

        Mockito.verifyNoMoreInteractions(super.schedulerJobService);

        FileEventDrivenJob fileEventDrivenJob = (FileEventDrivenJob) ReflectionTestUtils
            .getField(fileEventJobDialog, "fileEventDrivenJob");
        Assertions.assertNotNull(fileEventDrivenJob);

        Assert.assertEquals("agent0", fileEventDrivenJob.getAgentName());
        Assert.assertEquals("jobName", fileEventDrivenJob.getJobName());
        Assert.assertEquals("This is the job description", fileEventDrivenJob.getJobDescription());
        Assert.assertEquals("file.txt", fileEventDrivenJob.getFilenames().get(0));
        Assert.assertEquals("/file/path", fileEventDrivenJob.getFilePath());
        Assert.assertEquals("/archive/path", fileEventDrivenJob.getMoveDirectory());
        Assert.assertEquals("0 0/1 * 1/1 * ? *", fileEventDrivenJob.getCronExpression());
        Assert.assertEquals("0 0/2 * 1/1 * ? *", fileEventDrivenJob.getSlaCronExpression());
        Assert.assertEquals("Europe/London", fileEventDrivenJob.getTimeZone());
    }

    @Test
    public void test_create_new_dynamic_file_watcher_job() {

        UI.getCurrent().navigate("scheduler");

        // We open the scheduler view
        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        // Get the tabs on the scheduler view and do some assertions in order to confirm all in good order.
        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Assertions.assertNotNull(schedulerDashboardTabs);
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        Assertions.assertNotNull(contextTemplateTab);

        // Select the context template tab and assert success.
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);
        Assertions.assertEquals(contextTemplateTab, schedulerDashboardTabs.getSelectedTab());

        // Now we gat a handle to the template grid on that screen.
        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        Assertions.assertNotNull(contextTemplateFilteringGrid);

        // Get a handle to the actions that can be performed on the context template in the first row of the grid.
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Assert.assertNotNull(actionsLayout);

        // Get the action icon that opens the context template management dialog containing the management widget.
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        // Assert that we can get the ContextTemplateManagementWidget.
        ContextTemplateManagementWidget contextTemplateManagementWidget = _get(ContextTemplateManagementWidget.class);
        Assertions.assertNotNull(contextTemplateManagementWidget);

        MenuBar actionsMenuBar = _get(MenuBar.class, spec -> spec.withId("actionsMenuBar"));
        Assertions.assertNotNull(actionsMenuBar);

        MenuItem jobTypesMenuItem = _get(MenuItem.class, spec -> spec.withId("jobTypesMenuItem"));
        Assertions.assertNotNull(jobTypesMenuItem);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        Assertions.assertNotNull(newFileWatcherJobMenuItem);

        _click(newFileWatcherJobMenuItem);

        FileEventJobDialog fileEventJobDialog = _get(FileEventJobDialog.class);
        Assertions.assertNotNull(fileEventJobDialog);

        Checkbox isDynamicCheckbox = _get(Checkbox.class, spec -> spec.withId("isDynamicCheckbox"));
        Assertions.assertNotNull(isDynamicCheckbox);
        Assertions.assertFalse(isDynamicCheckbox.getValue());

        isDynamicCheckbox.setValue(true);

        TextField jobNameTf = _get(TextField.class, spec -> spec.withId("jobNameTf"));
        Assertions.assertNotNull(jobNameTf);

        ComboBox agentCb = _get(ComboBox.class, spec -> spec.withId("agentCb"));
        Assertions.assertNotNull(agentCb);
        Assertions.assertEquals(2, (((ListDataProvider)agentCb.getDataProvider()).getItems().size()));

        TextArea jobDescriptionTa = _get(TextArea.class, spec -> spec.withId("jobDescriptionTa"));
        Assertions.assertNotNull(jobDescriptionTa);

        TextField filenameTf = _get(TextField.class, spec -> spec.withId("filenameTf"));
        Assertions.assertNotNull(filenameTf);

        MultiSelectComboBox filenamePairs = _get(MultiSelectComboBox.class, spec -> spec.withId("filenamePairs"));
        Assertions.assertNotNull(filenamePairs);
        Assertions.assertTrue(filenamePairs.isVisible());

        Button filenamePlus = _get(Button.class, spec -> spec.withId("filenamePlus"));
        Assertions.assertNotNull(filenamePlus);
        Assertions.assertTrue(filenamePlus.isVisible());

        TextField filePathTf = _get(TextField.class, spec -> spec.withId("filePathTf"));
        Assertions.assertNotNull(filePathTf);

        MultiSelectComboBox filepathPairs = _get(MultiSelectComboBox.class, spec -> spec.withId("filepathPairs"));
        Assertions.assertNotNull(filepathPairs);
        Assertions.assertTrue(filepathPairs.isVisible());

        Button filepathPlus = _get(Button.class, spec -> spec.withId("filepathPlus"));
        Assertions.assertNotNull(filepathPlus);
        Assertions.assertTrue(filepathPlus.isVisible());

        TextField archiveDirectoryTf = _get(TextField.class, spec -> spec.withId("archiveDirectoryTf"));
        Assertions.assertNotNull(archiveDirectoryTf);

        TextField cronExpressionTf = _get(TextField.class, spec -> spec.withId("cronExpressionTf"));
        Assertions.assertNotNull(cronExpressionTf);

        TextField slaCronExpressionTf = _get(TextField.class, spec -> spec.withId("slaCronExpressionTf"));
        Assertions.assertNotNull(slaCronExpressionTf);

        ComboBox timezoneCb = _get(ComboBox.class, spec -> spec.withId("timezoneCb"));
        Assertions.assertNotNull(timezoneCb);

        Button scheduledJobSaveButton = _get(Button.class, spec -> spec.withId("scheduledJobSaveButton"));
        Assertions.assertNotNull(scheduledJobSaveButton);

        Button scheduledJobCancelButton = _get(Button.class, spec -> spec.withId("scheduledJobCancelButton"));
        Assertions.assertNotNull(scheduledJobCancelButton);

        _click(filenamePlus);
        ReplacementPairDialog filenameReplacementPairDialog = _get(ReplacementPairDialog.class);
        Assertions.assertNotNull(filenameReplacementPairDialog);

        TextField replacementTokenTf = _get(TextField.class, spec -> spec.withId("replacementTokenTf"));
        Assertions.assertNotNull(replacementTokenTf);

        Select contextParameterSelect = _get(Select.class, spec -> spec.withId("contextParameterSelect"));
        Assertions.assertNotNull(contextParameterSelect);
        Assertions.assertEquals(2, (((ListDataProvider)contextParameterSelect.getDataProvider()).getItems().size()));

        Button replacementPairSaveButton = _get(Button.class, spec -> spec.withId("replacementPairSaveButton"));
        Assertions.assertNotNull(replacementPairSaveButton);

        _setValue(replacementTokenTf, "<file-token>");
        _setValue(contextParameterSelect, (((ListDataProvider)contextParameterSelect.getDataProvider()).getItems()
            .stream()
            .filter(item -> ((ContextParameter)item).getName().equals("filename_replacement"))
            .findFirst().get()));
        _click(replacementPairSaveButton);

        Assertions.assertEquals(1, filenamePairs.getValue().size());

        _click(filepathPlus);
        ReplacementPairDialog filpathReplacementPairDialog = _get(ReplacementPairDialog.class);
        Assertions.assertNotNull(filpathReplacementPairDialog);

        replacementTokenTf = _get(TextField.class, spec -> spec.withId("replacementTokenTf"));
        Assertions.assertNotNull(replacementTokenTf);

        contextParameterSelect = _get(Select.class, spec -> spec.withId("contextParameterSelect"));
        Assertions.assertNotNull(contextParameterSelect);
        Assertions.assertEquals(2, (((ListDataProvider)contextParameterSelect.getDataProvider()).getItems().size()));

        replacementPairSaveButton = _get(Button.class, spec -> spec.withId("replacementPairSaveButton"));
        Assertions.assertNotNull(replacementPairSaveButton);

        _setValue(replacementTokenTf, "<file-path-token>");
        _setValue(contextParameterSelect, (((ListDataProvider)contextParameterSelect.getDataProvider()).getItems()
            .stream()
            .filter(item -> ((ContextParameter)item).getName().equals("filepath_replacement"))
            .findFirst().get()));
        _click(replacementPairSaveButton);

        Assertions.assertEquals(1, filepathPairs.getValue().size());

        _setValue(agentCb, "agent0");
        _setValue(jobNameTf, "jobName");
        _setValue(jobDescriptionTa, "This is the job description");
        _setValue(filenameTf, "file-<file-token>.txt");
        _setValue(filePathTf, "/file/<file-path-token>");
        _setValue(archiveDirectoryTf, "/archive/path");
        _setValue(cronExpressionTf, "0 0/1 * 1/1 * ? *");
        _setValue(slaCronExpressionTf, "0 0/2 * 1/1 * ? *");
        _setValue(timezoneCb, DateTimeUtil.getTimezonePairForZoneId(ZoneId.of("Europe/London").getId()));
        _click(scheduledJobSaveButton);

        verify(this.schedulerJobService).findByContext(anyString(), anyInt(), anyInt());
        verify(this.schedulerJobService).findByFilter(any(), anyInt(), anyInt(), isNull(), isNull());
        verify(this.schedulerJobService).findByContextNameAndJobName(anyString(), anyString());
        verify(this.schedulerJobService).saveFileEventDrivenJobRecord(any());

        Mockito.verifyNoMoreInteractions(super.schedulerJobService);

        FileEventDrivenJob fileEventDrivenJob = (FileEventDrivenJob) ReflectionTestUtils
            .getField(fileEventJobDialog, "fileEventDrivenJob");
        Assertions.assertNotNull(fileEventDrivenJob);

        Assert.assertEquals("agent0", fileEventDrivenJob.getAgentName());
        Assert.assertEquals("jobName", fileEventDrivenJob.getJobName());
        Assert.assertEquals("This is the job description", fileEventDrivenJob.getJobDescription());
        Assert.assertEquals("file-<file-token>.txt", fileEventDrivenJob.getFilenames().get(0));
        Assert.assertEquals("#fileNamePattern.replace('<file-token>', T(org.ikasan.ootb.scheduler.agent.rest.cache.ContextInstanceCache)" +
            ".getContextParameter(#correlatingIdentifier, 'filename_replacement'))", fileEventDrivenJob.getFilenameSpel());
        Assert.assertEquals("/file/<file-path-token>", fileEventDrivenJob.getFilePath());
        Assert.assertEquals("#filePathPattern.replace('<file-path-token>', T(org.ikasan.ootb.scheduler.agent.rest.cache.ContextInstanceCache)" +
            ".getContextParameter(#correlatingIdentifier, 'filepath_replacement'))", fileEventDrivenJob.getFilePathSpel());
        Assert.assertEquals("/archive/path", fileEventDrivenJob.getMoveDirectory());
        Assert.assertEquals("0 0/1 * 1/1 * ? *", fileEventDrivenJob.getCronExpression());
        Assert.assertEquals("0 0/2 * 1/1 * ? *", fileEventDrivenJob.getSlaCronExpression());
        Assert.assertEquals("Europe/London", fileEventDrivenJob.getTimeZone());
    }

    @Test
    public void test_dialog_opens_successfully() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuBar actionsMenuBar = _get(MenuBar.class, spec -> spec.withId("actionsMenuBar"));
        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        FileEventJobDialog fileEventJobDialog = _get(FileEventJobDialog.class);
        Assertions.assertNotNull(fileEventJobDialog);
        Assert.assertTrue(fileEventJobDialog.isOpened());
    }

    @Test
    public void test_dialog_has_all_required_fields() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        // Verify all required fields exist
        Assertions.assertNotNull(_get(TextField.class, spec -> spec.withId("jobNameTf")));
        Assertions.assertNotNull(_get(ComboBox.class, spec -> spec.withId("agentCb")));
        Assertions.assertNotNull(_get(TextArea.class, spec -> spec.withId("jobDescriptionTa")));
        Assertions.assertNotNull(_get(TextField.class, spec -> spec.withId("filenameTf")));
        Assertions.assertNotNull(_get(TextField.class, spec -> spec.withId("filePathTf")));
        Assertions.assertNotNull(_get(TextField.class, spec -> spec.withId("archiveDirectoryTf")));
        Assertions.assertNotNull(_get(TextField.class, spec -> spec.withId("cronExpressionTf")));
        Assertions.assertNotNull(_get(ComboBox.class, spec -> spec.withId("timezoneCb")));
    }

    @Test
    public void test_dynamic_checkbox_default_value() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        Checkbox isDynamicCheckbox = _get(Checkbox.class, spec -> spec.withId("isDynamicCheckbox"));

        // Verify dynamic checkbox is unchecked by default
        Assertions.assertFalse(isDynamicCheckbox.getValue());
    }

    @Test
    public void test_agent_combobox_has_data() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        ComboBox agentCb = _get(ComboBox.class, spec -> spec.withId("agentCb"));

        // Verify agent combobox has 2 agents
        Assert.assertEquals(2, ((ListDataProvider)agentCb.getDataProvider()).getItems().size());
    }

    @Test
    public void test_save_button_exists() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        Button scheduledJobSaveButton = _get(Button.class, spec -> spec.withId("scheduledJobSaveButton"));
        Assertions.assertNotNull(scheduledJobSaveButton);
    }

    @Test
    public void test_cancel_button_exists() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        Button scheduledJobCancelButton = _get(Button.class, spec -> spec.withId("scheduledJobCancelButton"));
        Assertions.assertNotNull(scheduledJobCancelButton);
    }

    @Test
    public void test_dynamic_mode_shows_additional_fields() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        Checkbox isDynamicCheckbox = _get(Checkbox.class, spec -> spec.withId("isDynamicCheckbox"));
        isDynamicCheckbox.setValue(true);

        // Verify dynamic fields are visible
        MultiSelectComboBox filenamePairs = _get(MultiSelectComboBox.class, spec -> spec.withId("filenamePairs"));
        Assert.assertTrue(filenamePairs.isVisible());

        Button filenamePlus = _get(Button.class, spec -> spec.withId("filenamePlus"));
        Assert.assertTrue(filenamePlus.isVisible());

        MultiSelectComboBox filepathPairs = _get(MultiSelectComboBox.class, spec -> spec.withId("filepathPairs"));
        Assert.assertTrue(filepathPairs.isVisible());

        Button filepathPlus = _get(Button.class, spec -> spec.withId("filepathPlus"));
        Assert.assertTrue(filepathPlus.isVisible());
    }

    @Test
    public void test_replacement_pair_dialog_opens_for_filename() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        Checkbox isDynamicCheckbox = _get(Checkbox.class, spec -> spec.withId("isDynamicCheckbox"));
        isDynamicCheckbox.setValue(true);

        Button filenamePlus = _get(Button.class, spec -> spec.withId("filenamePlus"));
        _click(filenamePlus);

        ReplacementPairDialog replacementPairDialog = _get(ReplacementPairDialog.class);
        Assertions.assertNotNull(replacementPairDialog);
    }

    @Test
    public void test_replacement_pair_dialog_has_required_fields() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        Checkbox isDynamicCheckbox = _get(Checkbox.class, spec -> spec.withId("isDynamicCheckbox"));
        isDynamicCheckbox.setValue(true);

        Button filenamePlus = _get(Button.class, spec -> spec.withId("filenamePlus"));
        _click(filenamePlus);

        TextField replacementTokenTf = _get(TextField.class, spec -> spec.withId("replacementTokenTf"));
        Assertions.assertNotNull(replacementTokenTf);

        Select contextParameterSelect = _get(Select.class, spec -> spec.withId("contextParameterSelect"));
        Assertions.assertNotNull(contextParameterSelect);

        Button replacementPairSaveButton = _get(Button.class, spec -> spec.withId("replacementPairSaveButton"));
        Assertions.assertNotNull(replacementPairSaveButton);
    }

    @Test
    public void test_context_parameter_select_has_data() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        Checkbox isDynamicCheckbox = _get(Checkbox.class, spec -> spec.withId("isDynamicCheckbox"));
        isDynamicCheckbox.setValue(true);

        Button filenamePlus = _get(Button.class, spec -> spec.withId("filenamePlus"));
        _click(filenamePlus);

        Select contextParameterSelect = _get(Select.class, spec -> spec.withId("contextParameterSelect"));

        // Verify context parameter select has 2 items
        Assert.assertEquals(2, ((ListDataProvider)contextParameterSelect.getDataProvider()).getItems().size());
    }

    @Test
    public void test_sla_cron_expression_field_exists() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        TextField slaCronExpressionTf = _get(TextField.class, spec -> spec.withId("slaCronExpressionTf"));
        Assertions.assertNotNull(slaCronExpressionTf);
    }

    @Test
    public void test_timezone_combobox_exists() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        ComboBox timezoneCb = _get(ComboBox.class, spec -> spec.withId("timezoneCb"));
        Assertions.assertNotNull(timezoneCb);
    }

    @Test
    public void test_replacement_pair_dialog_opens_for_filepath() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        _click(newFileWatcherJobMenuItem);

        Checkbox isDynamicCheckbox = _get(Checkbox.class, spec -> spec.withId("isDynamicCheckbox"));
        isDynamicCheckbox.setValue(true);

        Button filepathPlus = _get(Button.class, spec -> spec.withId("filepathPlus"));
        _click(filepathPlus);

        ReplacementPairDialog replacementPairDialog = _get(ReplacementPairDialog.class);
        Assertions.assertNotNull(replacementPairDialog);
    }

    @Test
    public void test_menu_items_structure() {
        UI.getCurrent().navigate("scheduler");

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab contextTemplateTab = _get(Tab.class, spec -> spec.withId("contextTemplateTab"));
        schedulerDashboardTabs.setSelectedTab(contextTemplateTab);

        ContextTemplateFilteringGrid contextTemplateFilteringGrid = _get(ContextTemplateFilteringGrid.class);
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(contextTemplateFilteringGrid, 0, "actions");
        Icon openPlanManagementInNewWindow = (Icon) actionsLayout.getComponentAt(0);
        _click(openPlanManagementInNewWindow);

        MenuBar actionsMenuBar = _get(MenuBar.class, spec -> spec.withId("actionsMenuBar"));
        Assertions.assertNotNull(actionsMenuBar);

        MenuItem jobTypesMenuItem = _get(MenuItem.class, spec -> spec.withId("jobTypesMenuItem"));
        Assertions.assertNotNull(jobTypesMenuItem);

        MenuItem newFileWatcherJobMenuItem = _get(MenuItem.class, spec -> spec.withId("newFileWatcherJobMenuItem"));
        Assertions.assertNotNull(newFileWatcherJobMenuItem);
    }
}
