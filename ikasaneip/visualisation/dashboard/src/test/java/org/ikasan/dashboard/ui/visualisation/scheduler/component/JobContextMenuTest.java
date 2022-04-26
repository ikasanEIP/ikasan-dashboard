package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.EnumSet;

import javax.annotation.Resource;

import org.ikasan.dashboard.ui.UITest;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.rest.dashboard.model.metadata.module.ModuleMetaDataImpl;
import org.ikasan.rest.dashboard.model.scheduled.ScheduledProcessEventImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.vaadin.flow.component.button.Button;

public class JobContextMenuTest extends UITest {

    @Resource
    private NotificationHelper notificationHelper;

    @MockBean
    private BatchInsert<ScheduledProcessEvent> scheduledProcessEventBatchInsert;

    @MockBean
    private SchedulerJobService schedulerJobService;

    @MockBean
    private ContextInstance rootContextInstance;

    @MockBean
    private LogStreamingService logStreamingService;

    @MockBean
    private SystemEventLogger systemEventLogger;

    @MockBean
    private ScheduledProcessManagementService scheduledProcessManagementService;

    @MockBean
    private ConfigurationService configurationRestService;

    @MockBean
    private ModuleControlService moduleControlRestService;

    @MockBean
    private MetaDataService metaDataRestService;

    private JobContextMenu jobContextMenu;

    @Test
    public void shouldDisplayNoOutputLogNotification_IfNotCorrectStatus() {
        EnumSet<InstanceStatus> instanceStatuses = EnumSet.complementOf(EnumSet.of(InstanceStatus.COMPLETE, InstanceStatus.RUNNING, InstanceStatus.ERROR));
        for (InstanceStatus status : instanceStatuses) {
            testStreamLog(false, status);
        }
    }

    @Test
    public void shouldDisplayNoErrorLogNotification_IfNotCorrectStatus() {
        EnumSet<InstanceStatus> instanceStatuses = EnumSet.complementOf(EnumSet.of(InstanceStatus.COMPLETE, InstanceStatus.RUNNING, InstanceStatus.ERROR));
        for (InstanceStatus status : instanceStatuses) {
            testStreamLog(true, status);
        }
    }

    @Test
    public void shouldStreamOutputLog_IfCorrectStatus() {
        notificationHelper.resetLastMessage();
        EnumSet<InstanceStatus> instanceStatuses = EnumSet.of(InstanceStatus.COMPLETE, InstanceStatus.RUNNING, InstanceStatus.ERROR);
        for (InstanceStatus status : instanceStatuses) {
            streamLogCorrectStatus(false, status);
        }
    }

    @Test
    public void shouldStreamErrorLog_IfCorrectStatus() {
        notificationHelper.resetLastMessage();
        EnumSet<InstanceStatus> instanceStatuses = EnumSet.of(InstanceStatus.COMPLETE, InstanceStatus.RUNNING, InstanceStatus.ERROR);
        for (InstanceStatus status : instanceStatuses) {
            streamLogCorrectStatus(true, status);
        }
    }

    @Test
    public void shouldNotStreamLog_IfNoSchedulerJobInstance() {
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("https:/url/scheduler/agent");
        when(moduleMetadataService.findById("agentName")).thenReturn(moduleMetaData);

        ContextInstance currentInstance = new ContextInstanceImpl();
        currentInstance.setStatus(InstanceStatus.COMPLETE);
        SchedulerJob schedulerJob = new SchedulerJobImpl();
        schedulerJob.setAgentName("agentName");
        schedulerJob.setJobName("jobName");
        schedulerJob.setIdentifier("jobIdentifier");
        SchedulerJobInstanceImpl instance = new SchedulerJobInstanceImpl();
        instance.setAgentName("agentName");
        ScheduledProcessEventImpl scheduledProcessEvent = new ScheduledProcessEventImpl();
        scheduledProcessEvent.setResultError("/path/to/errorLog");
        scheduledProcessEvent.setResultOutput("/path/to/outputLog");
        instance.setScheduledProcessEvent(scheduledProcessEvent);

        currentInstance.getScheduledJobsMap().clear();

        jobContextMenu = new JobContextMenu(schedulerJob, systemEventLogger, moduleMetadataService,
            scheduledProcessManagementService, configurationRestService, moduleControlRestService,
            metaDataRestService, schedulerJobService, rootContextInstance, currentInstance, logStreamingService);

        Button viewOutputLogButton = (Button) ReflectionTestUtils.getField(jobContextMenu, "viewOutputLogButton");
        assertNotNull(viewOutputLogButton);
        assertEquals("View Output Log", viewOutputLogButton.getText());

        viewOutputLogButton.click();

        SchedulerJobLogFileViewerDialog dialog = (SchedulerJobLogFileViewerDialog) ReflectionTestUtils.getField(jobContextMenu, "schedulerJobLogFileViewerDialog");
        assertNull(dialog);

        assertEquals("There is no output log for the job", notificationHelper.getLastMessage());
        verify(moduleMetadataService).findById("agentName");
    }

    @Test
    public void shouldNotStreamLog_IfNoProcessEvent() {
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("https:/url/scheduler/agent");
        when(moduleMetadataService.findById("agentName")).thenReturn(moduleMetaData);
        ContextInstance currentInstance = new ContextInstanceImpl();
        currentInstance.setStatus(InstanceStatus.ERROR);
        SchedulerJob schedulerJob = new SchedulerJobImpl();
        schedulerJob.setAgentName("agentName");
        schedulerJob.setJobName("jobName");
        schedulerJob.setIdentifier("jobIdentifier");
        SchedulerJobInstanceImpl instance = new SchedulerJobInstanceImpl();
        instance.setAgentName("agentName");
        ScheduledProcessEventImpl scheduledProcessEvent = new ScheduledProcessEventImpl();
        scheduledProcessEvent.setResultError("/path/to/errorLog");
        scheduledProcessEvent.setResultOutput("/path/to/outputLog");
        currentInstance.getScheduledJobsMap().put("jobIdentifier", instance);

        instance.setScheduledProcessEvent(null);

        jobContextMenu = new JobContextMenu(schedulerJob, systemEventLogger, moduleMetadataService,
            scheduledProcessManagementService, configurationRestService, moduleControlRestService,
            metaDataRestService, schedulerJobService, rootContextInstance, currentInstance, logStreamingService);

        Button button = (Button) ReflectionTestUtils.getField(jobContextMenu, "viewErrorLogButton");
        assertNotNull(button);
        assertEquals("View Error Log", button.getText());

        button.click();

        SchedulerJobLogFileViewerDialog dialog = (SchedulerJobLogFileViewerDialog) ReflectionTestUtils.getField(jobContextMenu, "schedulerJobLogFileViewerDialog");
        assertNull(dialog);

        assertEquals("There is no error log for the job", notificationHelper.getLastMessage());
        verify(moduleMetadataService).findById("agentName");
    }

    @Test
    public void shouldNotStreamLog_IfNoAgent() {
        when(moduleMetadataService.findById("agentName")).thenReturn(null);
        ContextInstance currentInstance = new ContextInstanceImpl();
        currentInstance.setStatus(InstanceStatus.RUNNING);
        SchedulerJob schedulerJob = new SchedulerJobImpl();
        schedulerJob.setAgentName("agentName");
        schedulerJob.setJobName("jobName");
        schedulerJob.setIdentifier("jobIdentifier");
        SchedulerJobInstanceImpl instance = new SchedulerJobInstanceImpl();
        instance.setAgentName("agentName");
        ScheduledProcessEventImpl scheduledProcessEvent = new ScheduledProcessEventImpl();
        scheduledProcessEvent.setResultError("/path/to/errorLog");
        scheduledProcessEvent.setResultOutput("/path/to/outputLog");
        currentInstance.getScheduledJobsMap().put("jobIdentifier", instance);
        instance.setScheduledProcessEvent(scheduledProcessEvent);

        jobContextMenu = new JobContextMenu(schedulerJob, systemEventLogger, moduleMetadataService,
            scheduledProcessManagementService, configurationRestService, moduleControlRestService,
            metaDataRestService, schedulerJobService, rootContextInstance, currentInstance, logStreamingService);

        Button viewOutputLogButton = (Button) ReflectionTestUtils.getField(jobContextMenu, "viewOutputLogButton");
        assertNotNull(viewOutputLogButton);
        assertEquals("View Output Log", viewOutputLogButton.getText());

        viewOutputLogButton.click();

        SchedulerJobLogFileViewerDialog dialog = (SchedulerJobLogFileViewerDialog) ReflectionTestUtils.getField(jobContextMenu, "schedulerJobLogFileViewerDialog");
        assertNull(dialog);

        assertEquals("There is no output log for the job", notificationHelper.getLastMessage());
        verify(moduleMetadataService).findById("agentName");
    }

    @Test
    public void shouldNotStreamLog_IfNoLog() {
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("https:/url/scheduler/agent");
        when(moduleMetadataService.findById("agentName")).thenReturn(moduleMetaData);
        ContextInstance currentInstance = new ContextInstanceImpl();
        currentInstance.setStatus(InstanceStatus.COMPLETE);
        SchedulerJob schedulerJob = new SchedulerJobImpl();
        schedulerJob.setAgentName("agentName");
        schedulerJob.setJobName("jobName");
        schedulerJob.setIdentifier("jobIdentifier");
        SchedulerJobInstanceImpl instance = new SchedulerJobInstanceImpl();
        instance.setAgentName("agentName");
        ScheduledProcessEventImpl scheduledProcessEvent = new ScheduledProcessEventImpl();
        scheduledProcessEvent.setResultError(null);
        scheduledProcessEvent.setResultOutput(null);
        currentInstance.getScheduledJobsMap().put("jobIdentifier", instance);
        instance.setScheduledProcessEvent(scheduledProcessEvent);

        jobContextMenu = new JobContextMenu(schedulerJob, systemEventLogger, moduleMetadataService,
            scheduledProcessManagementService, configurationRestService, moduleControlRestService,
            metaDataRestService, schedulerJobService, rootContextInstance, currentInstance, logStreamingService);

        Button viewOutputLogButton = (Button) ReflectionTestUtils.getField(jobContextMenu, "viewOutputLogButton");
        assertNotNull(viewOutputLogButton);
        assertEquals("View Output Log", viewOutputLogButton.getText());

        viewOutputLogButton.click();

        SchedulerJobLogFileViewerDialog dialog = (SchedulerJobLogFileViewerDialog) ReflectionTestUtils.getField(jobContextMenu, "schedulerJobLogFileViewerDialog");
        assertNull(dialog);

        assertEquals("There is no output log for the job", notificationHelper.getLastMessage());
        verify(moduleMetadataService).findById("agentName");
    }

    @Test
    public void shouldNotStreamLog_IfNoUrl() {
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl(null);
        when(moduleMetadataService.findById("agentName")).thenReturn(moduleMetaData);
        ContextInstance currentInstance = new ContextInstanceImpl();
        currentInstance.setStatus(InstanceStatus.ERROR);
        SchedulerJob schedulerJob = new SchedulerJobImpl();
        schedulerJob.setAgentName("agentName");
        schedulerJob.setJobName("jobName");
        schedulerJob.setIdentifier("jobIdentifier");
        SchedulerJobInstanceImpl instance = new SchedulerJobInstanceImpl();
        instance.setAgentName("agentName");
        ScheduledProcessEventImpl scheduledProcessEvent = new ScheduledProcessEventImpl();
        scheduledProcessEvent.setResultError(null);
        scheduledProcessEvent.setResultOutput(null);
        currentInstance.getScheduledJobsMap().put("jobIdentifier", instance);
        instance.setScheduledProcessEvent(scheduledProcessEvent);

        jobContextMenu = new JobContextMenu(schedulerJob, systemEventLogger, moduleMetadataService,
            scheduledProcessManagementService, configurationRestService, moduleControlRestService,
            metaDataRestService, schedulerJobService, rootContextInstance, currentInstance, logStreamingService);

        Button button = (Button) ReflectionTestUtils.getField(jobContextMenu, "viewErrorLogButton");
        assertNotNull(button);
        assertEquals("View Error Log", button.getText());

        button.click();

        SchedulerJobLogFileViewerDialog dialog = (SchedulerJobLogFileViewerDialog) ReflectionTestUtils.getField(jobContextMenu, "schedulerJobLogFileViewerDialog");
        assertNull(dialog);

        assertEquals("There is no error log for the job", notificationHelper.getLastMessage());
        verify(moduleMetadataService).findById("agentName");
    }

    private void testStreamLog(boolean errorLog, InstanceStatus status) {
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("https:/url/scheduler/agent");
        when(moduleMetadataService.findById("agentName")).thenReturn(moduleMetaData);
        SchedulerJob schedulerJob = new SchedulerJobImpl();
        schedulerJob.setAgentName("agentName");
        schedulerJob.setJobName("jobName");
        schedulerJob.setIdentifier("jobIdentifier");
        ContextInstance currentInstance = new ContextInstanceImpl();
        currentInstance.setStatus(status);
        SchedulerJobInstanceImpl instance = new SchedulerJobInstanceImpl();
        instance.setAgentName("agentName");
        ScheduledProcessEventImpl scheduledProcessEvent = new ScheduledProcessEventImpl();
        scheduledProcessEvent.setResultError("/path/to/errorLog");
        scheduledProcessEvent.setResultOutput("/path/to/outputLog");
        instance.setScheduledProcessEvent(scheduledProcessEvent);
        currentInstance.getScheduledJobsMap().put("jobIdentifier", instance);

        currentInstance.setStatus(status);
        jobContextMenu = new JobContextMenu(schedulerJob, systemEventLogger, moduleMetadataService,
            scheduledProcessManagementService, configurationRestService, moduleControlRestService,
            metaDataRestService, schedulerJobService, rootContextInstance, currentInstance, logStreamingService);

        Button button;

        if (errorLog) {
            button = (Button) ReflectionTestUtils.getField(jobContextMenu, "viewErrorLogButton");
        } else {
            button = (Button) ReflectionTestUtils.getField(jobContextMenu, "viewOutputLogButton");
        }

        assertNotNull(button);
        String expectedButtonText = errorLog ? "View Error Log" : "View Output Log";
        assertEquals(expectedButtonText, button.getText());

        button.click();

        SchedulerJobLogFileViewerDialog dialog = (SchedulerJobLogFileViewerDialog) ReflectionTestUtils.getField(jobContextMenu, "schedulerJobLogFileViewerDialog");
        assertNull(dialog);

        String expectedNotificationText = errorLog ? "There is no error log for the job" : "There is no output log for the job";
        assertEquals(expectedNotificationText, notificationHelper.getLastMessage());
        verify(moduleMetadataService).findAll();
        verifyNoMoreInteractions(moduleMetadataService);
    }

    private void streamLogCorrectStatus(boolean errorLog, InstanceStatus status) {
        ModuleMetaData moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl("https:/url/scheduler/agent");
        when(moduleMetadataService.findById("agentName")).thenReturn(moduleMetaData);
        SchedulerJob schedulerJob = new SchedulerJobImpl();
        schedulerJob.setAgentName("agentName");
        schedulerJob.setJobName("jobName");
        schedulerJob.setIdentifier("jobIdentifier");
        ContextInstance currentInstance = new ContextInstanceImpl();
        currentInstance.setStatus(status);
        SchedulerJobInstanceImpl instance = new SchedulerJobInstanceImpl();
        instance.setAgentName("agentName");
        ScheduledProcessEventImpl scheduledProcessEvent = new ScheduledProcessEventImpl();
        scheduledProcessEvent.setResultError("/path/to/errorLog");
        scheduledProcessEvent.setResultOutput("/path/to/outputLog");
        instance.setScheduledProcessEvent(scheduledProcessEvent);
        currentInstance.getScheduledJobsMap().put("jobIdentifier", instance);

        jobContextMenu = new JobContextMenu(schedulerJob, systemEventLogger, moduleMetadataService,
            scheduledProcessManagementService, configurationRestService, moduleControlRestService,
            metaDataRestService, schedulerJobService, rootContextInstance, currentInstance, logStreamingService);

        Button button;

        if (errorLog) {
            button = (Button) ReflectionTestUtils.getField(jobContextMenu, "viewErrorLogButton");
        } else {
            button = (Button) ReflectionTestUtils.getField(jobContextMenu, "viewOutputLogButton");
        }

        assertNotNull(button);

        button.click();

        SchedulerJobLogFileViewerDialog dialog = (SchedulerJobLogFileViewerDialog) ReflectionTestUtils.getField(jobContextMenu, "schedulerJobLogFileViewerDialog");
        assertNotNull(dialog);

        assertEquals("https:/url/scheduler/agent", dialog.getHost());
        String expected = errorLog ? "/path/to/errorLog" : "/path/to/outputLog";
        assertEquals(expected, dialog.getLogFile());
        assertEquals("/rest/logs", dialog.getEndPoint());
        assertEquals(logStreamingService, dialog.getLogStreamingService());

        verify(moduleMetadataService).findById("agentName");
        reset(moduleMetadataService);
        assertNull(notificationHelper.getLastMessage());
    }

    @Override
    public void setup_expectations() throws IOException {

    }
}