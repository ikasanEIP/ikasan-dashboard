package org.ikasan.dashboard.ui.scheduler.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.github.mvysny.kaributesting.v10.LocatorJ;
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
import org.ikasan.dashboard.ui.scheduler.component.*;
import org.ikasan.dashboard.ui.scheduler.util.ScheduledProcessConstants;
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
import org.vaadin.stefan.fullcalendar.FullCalendar;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.IntStream;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.eq;

public class SchedulerViewTest extends UITest {

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

        Mockito.when(this.scheduledProcessEventBatchInsert.getUpComingScheduledProcesses(Mockito.isNull(), Mockito.isNull(), Mockito.anyLong(),
            Mockito.anyLong(), Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn(this.getUpcomingScheduledEventsResults(25));

        Mockito.when(this.scheduledProcessEventBatchInsert.getScheduleProcessAggregateConfigurations(Mockito.anyString(), Mockito.isNull()))
            .thenReturn(new ScheduledProcessEventSearchResults<>(List.of(), 0, 0));

        Mockito.when(this.moduleMetadataService.find(Mockito.any(ArrayList.class), Mockito.any(ModuleType.class),
            Mockito.anyInt(), Mockito.anyInt()))
            .thenReturn(this.getAgents(1));

    }

    @Test
    public void test_scheduler_view_scheduler_dashboard_tab() throws IOException
    {
        UI.getCurrent().navigate("scheduler");

        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        Tabs tabs = _get(Tabs.class);

        Assertions.assertNotNull(tabs);

        ScheduledAgentsFilteringGrid agentsFilteringGrid = _get(ScheduledAgentsFilteringGrid.class);

        Assertions.assertNotNull(agentsFilteringGrid);

        Assertions.assertEquals(1, agentsFilteringGrid.getDataProvider().size(new Query<>()));

        _get(Tabs.class).setSelectedTab(_get(Tab.class, spec -> spec.withId("scheduledJobsTab")));

        UpcomingJobExecutionFilteringGrid upcomingJobExecutionFilteringGrid = _get(UpcomingJobExecutionFilteringGrid.class);
        Assertions.assertNotNull(upcomingJobExecutionFilteringGrid);

        Assertions.assertEquals(25, upcomingJobExecutionFilteringGrid.getDataProvider().size(new Query<>()));

        RunningAndRecentlyCompletedJobExecutionFilteringGrid runningAndRecentlyCompletedGrid
            = _get(RunningAndRecentlyCompletedJobExecutionFilteringGrid.class);

        Assertions.assertNotNull(runningAndRecentlyCompletedGrid);
        Assertions.assertEquals(50, runningAndRecentlyCompletedGrid.getDataProvider().size(new Query<>()));
    }

    @Test
    public void test_scheduler_view_scheduled_jobs_tab() throws IOException
    {
        UI.getCurrent().navigate("scheduler");

        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        Tabs tabs = _get(Tabs.class);

        Assertions.assertNotNull(tabs);

        _get(Tabs.class).setSelectedTab(_get(Tab.class, spec -> spec.withId("scheduledJobsTab")));

        UpcomingJobExecutionFilteringGrid upcomingJobExecutionFilteringGrid = _get(UpcomingJobExecutionFilteringGrid.class);
        Assertions.assertNotNull(upcomingJobExecutionFilteringGrid);

        Assertions.assertEquals(25, upcomingJobExecutionFilteringGrid.getDataProvider().size(new Query<>()));

        RunningAndRecentlyCompletedJobExecutionFilteringGrid runningAndRecentlyCompletedGrid
            = _get(RunningAndRecentlyCompletedJobExecutionFilteringGrid.class);

        Assertions.assertNotNull(runningAndRecentlyCompletedGrid);
        Assertions.assertEquals(50, runningAndRecentlyCompletedGrid.getDataProvider().size(new Query<>()));
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
            UpcomingScheduledProcess document = new UpcomingScheduledProcess("agentName", "hostname", "jobName",
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
