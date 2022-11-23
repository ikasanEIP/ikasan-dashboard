package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import org.ikasan.dashboard.ui.UITest;
import org.ikasan.dashboard.ui.scheduler.component.ContextInstanceDashboardWidget;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.general.SchedulerService;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceAggregateJobStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


public class ContextInstanceDashboardWidgetTest extends UITest {
    @Resource
    ModuleMetaDataService moduleMetadataService;
    @MockBean
    ScheduledProcessManagementService scheduledProcessManagementService;
    @MockBean
    ConfigurationService configurationRestService;
    @MockBean
    ModuleControlService moduleControlRestService;
    @MockBean
    MetaDataService metaDataRestService;
    @MockBean
    SystemEventLogger systemEventLogger;
    @MockBean
    SchedulerService schedulerService;
    @MockBean
    SchedulerJobService schedulerJobService;
    @MockBean
    SchedulerJobInstanceService schedulerJobInstanceService;
    @MockBean
    ScheduledContextInstanceService scheduledContextInstanceService;
    String dynamicImagePath = "";
    @MockBean
    LogStreamingService logStreamingService;
    @MockBean
    JobInitiationService jobInitiationService;
    @MockBean
    ContextProfileService contextProfileService;
    @MockBean
    JobUtilsService jobUtilsService;
    @MockBean
    ScheduledContextService scheduledContextService;

    boolean fullscreen = false;


    @Test
    public void test() {

        UI.getCurrent().navigate("scheduler");

        ContextInstanceDashboardWidget contextInstanceDashboardWidget = _get(ContextInstanceDashboardWidget.class);

        Assertions.assertNotNull(contextInstanceDashboardWidget);

        Grid<ContextInstanceAggregateJobStatus> contextInstanceAggregateJobStatusGrid
            = _get(Grid.class, spec -> spec.withId("contextInstanceAggregateJobStatusGrid"));

        Assertions.assertNotNull(contextInstanceAggregateJobStatusGrid);
    }

    @Override
    public void setup_expectations() throws IOException {
        when(this.scheduledContextInstanceService.findAllAuditRecordsByFilter(any()
            , anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(new ArrayList<>(), 0, 1));

        when(scheduledProcessManagementService.getScheduledProcessEvents(isNull(),
            anyLong(), anyLong(), isNull(), anyBoolean(), anyInt(), anyInt(), anyString()))
            .thenReturn(new ScheduledProcessEventSearchResults<>(new ArrayList<>(), 0, 1));

        when(this.scheduledProcessManagementService.getUpComingScheduledProcesses(isNull(), isNull(), anyLong()
            , anyLong(), anyInt(), anyInt()))
            .thenReturn(new ScheduledProcessEventSearchResults<>(new ArrayList<>(), 0, 1));

        when(this.scheduledContextService.findByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(new ArrayList<>(), 0, 1));

        when(this.moduleMetadataService.find(anyList(), any(), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(new ArrayList<>(), 0, 1));
    }
}
