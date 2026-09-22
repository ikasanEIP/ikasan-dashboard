package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import org.ikasan.dashboard.ui.scheduler.AbstractSchedulerViewTest;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SchedulerJobInstanceGridWidgetTest extends AbstractSchedulerViewTest {

    @MockitoBean
    protected ConfigurationService configurationService;

    @MockitoBean
    protected ModuleControlService moduleControlService;

    @MockitoBean
    protected MetaDataService metaDataService;

    @MockitoBean
    protected SystemEventLogger systemEventLogger;

    @MockitoBean
    protected LogStreamingService logStreamingService;

    @MockitoBean
    protected JobInitiationService jobInitiationService;

    @MockitoBean
    protected JobUtilsService jobUtilsService;

    @MockitoBean
    protected GlobalEventService globalEventService;

    @MockitoBean
    protected SystemEventSearchService systemEventSearchService;

    @MockitoBean
    protected ModuleMetaDataService moduleMetaDataService;

    private ContextInstance contextInstance;
    private IkasanAuthentication authentication;

    @Override
    public void setup_expectations() throws IOException {
        authentication = setupSecurityExpectations();

        contextInstance = new ContextInstanceImpl();
        contextInstance.setId("test-context-id");
        contextInstance.setName("test-context");
        contextInstance.setStatus(InstanceStatus.RUNNING);
        contextInstance.setUseDisplayName(true);

        // Mock job instance service to return empty results by default
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(new ArrayList<>(), 0, 0));
    }

    private IkasanAuthentication setupSecurityExpectations() {
        IkasanAuthentication mockIkasanAuthentication = mock(IkasanAuthentication.class);
        SecurityContextHolder.getContext().setAuthentication(mockIkasanAuthentication);

        Mockito.when(mockIkasanAuthentication.getName()).thenReturn("testUser");
        Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
            .thenReturn(true);
        Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_WRITE))
            .thenReturn(true);
        Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ADMIN))
            .thenReturn(true);
        Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ALL_ADMIN))
            .thenReturn(true);
        Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ALL_WRITE))
            .thenReturn(true);
        Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_READ))
            .thenReturn(true);
        Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ALL_READ))
            .thenReturn(true);

        return mockIkasanAuthentication;
    }

    @Before
    public void setupBeforeEachTest() {
        UI.setCurrent(new UI());
    }

    // ========== Constructor and General Tests ==========

    @Test
    public void test_widget_creation_with_all_required_dependencies() {
        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);

        // Then
        Assert.assertNotNull("Widget should be created", widget);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_widget_creation_throws_exception_when_scheduledContextInstanceService_is_null() {
        new SchedulerJobInstanceGridWidget(null, moduleMetaDataService, scheduledProcessManagementService,
            configurationService, moduleControlService, metaDataService, systemEventLogger, schedulerJobService,
            logStreamingService, contextInstance, schedulerJobInstanceService, jobInitiationService,
            configurationService, metaDataService, jobUtilsService, scheduledContextService, null, null,
            contextProfileService, globalEventService, systemEventSearchService, 100.0, 100.0, 100.0, 100.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_widget_creation_throws_exception_when_schedulerJobInstanceService_is_null() {
        new SchedulerJobInstanceGridWidget(scheduledContextInstanceService, moduleMetaDataService, scheduledProcessManagementService,
            configurationService, moduleControlService, metaDataService, systemEventLogger, schedulerJobService,
            logStreamingService, contextInstance, null, jobInitiationService,
            configurationService, metaDataService, jobUtilsService, scheduledContextService, null, null,
            contextProfileService, globalEventService, systemEventSearchService, 100.0, 100.0, 100.0, 100.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_widget_creation_throws_exception_when_contextInstance_is_null() {
        new SchedulerJobInstanceGridWidget(scheduledContextInstanceService, moduleMetaDataService, scheduledProcessManagementService,
            configurationService, moduleControlService, metaDataService, systemEventLogger, schedulerJobService,
            logStreamingService, null, schedulerJobInstanceService, jobInitiationService,
            configurationService, metaDataService, jobUtilsService, scheduledContextService, null, null,
            contextProfileService, globalEventService, systemEventSearchService, 100.0, 100.0, 100.0, 100.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_widget_creation_throws_exception_when_systemEventLogger_is_null() {
        new SchedulerJobInstanceGridWidget(scheduledContextInstanceService, moduleMetaDataService, scheduledProcessManagementService,
            configurationService, moduleControlService, metaDataService, null, schedulerJobService,
            logStreamingService, contextInstance, schedulerJobInstanceService, jobInitiationService,
            configurationService, metaDataService, jobUtilsService, scheduledContextService, null, null,
            contextProfileService, globalEventService, systemEventSearchService, 100.0, 100.0, 100.0, 100.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_widget_creation_throws_exception_when_jobInitiationService_is_null() {
        new SchedulerJobInstanceGridWidget(scheduledContextInstanceService, moduleMetaDataService, scheduledProcessManagementService,
            configurationService, moduleControlService, metaDataService, systemEventLogger, schedulerJobService,
            logStreamingService, contextInstance, schedulerJobInstanceService, null,
            configurationService, metaDataService, jobUtilsService, scheduledContextService, null, null,
            contextProfileService, globalEventService, systemEventSearchService, 100.0, 100.0, 100.0, 100.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_widget_creation_throws_exception_when_moduleMetaDataService_is_null() {
        new SchedulerJobInstanceGridWidget(scheduledContextInstanceService, null, scheduledProcessManagementService,
            configurationService, moduleControlService, metaDataService, systemEventLogger, schedulerJobService,
            logStreamingService, contextInstance, schedulerJobInstanceService, jobInitiationService,
            configurationService, metaDataService, jobUtilsService, scheduledContextService, null, null,
            contextProfileService, globalEventService, systemEventSearchService, 100.0, 100.0, 100.0, 100.0);
    }

    @Test
    public void test_widget_creation_with_job_status_filter() {
        // When
        SchedulerJobInstanceGridWidget widget = createWidget("RUNNING", null);

        // Then
        Assert.assertNotNull("Widget should be created with status filter", widget);
    }

    @Test
    public void test_widget_creation_with_job_name_filter() {
        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, "test-job");

        // Then
        Assert.assertNotNull("Widget should be created with name filter", widget);
    }

    @Test
    public void test_widget_creation_with_both_filters() {
        // When
        SchedulerJobInstanceGridWidget widget = createWidget("COMPLETE", "test-job");

        // Then
        Assert.assertNotNull("Widget should be created with both filters", widget);
    }

    @Test
    public void test_widget_has_children_components() {
        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);

        // Then
        Assert.assertTrue("Widget should have child components", widget.getChildren().count() > 0);
    }

    @Test
    public void test_grid_can_be_found_in_widget() {
        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then
        Assert.assertNotNull("Grid should exist in widget", grid);
    }

    @Test
    public void test_grid_has_columns() {
        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then
        Assert.assertTrue("Grid should have columns", grid.getColumns().size() > 0);
    }

    @Test
    public void test_grid_has_actions_column() {
        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Check that actions column exists by key
        boolean hasActionsColumn = grid.getColumns().stream()
            .anyMatch(col -> "actions".equals(col.getKey()));

        Assert.assertTrue("Grid should have actions column with key 'actions'", hasActionsColumn);
    }

    @Test
    public void test_widget_is_visible() {
        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);

        // Then
        Assert.assertTrue("Widget should be visible", widget.isVisible());
    }

    @Test
    public void test_security_context_is_set() {
        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);

        // Then
        Assert.assertNotNull("Security context should be set", SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void test_authentication_has_scheduler_write_authority() {
        // When
        IkasanAuthentication auth = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        // Then
        Assert.assertTrue("Authentication should have SCHEDULER_WRITE authority",
            auth.hasGrantedAuthority(SecurityConstants.SCHEDULER_WRITE));
    }

    @Test
    public void test_authentication_has_scheduler_admin_authority() {
        // When
        IkasanAuthentication auth = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        // Then
        Assert.assertTrue("Authentication should have SCHEDULER_ADMIN authority",
            auth.hasGrantedAuthority(SecurityConstants.SCHEDULER_ADMIN));
    }

    // ========== Helper Methods ==========

    private SchedulerJobInstanceFilteringGrid findGridInWidget(SchedulerJobInstanceGridWidget widget) {
        return widget.getChildren()
            .flatMap(component -> component.getChildren())
            .filter(child -> child instanceof SchedulerJobInstanceFilteringGrid)
            .map(grid -> (SchedulerJobInstanceFilteringGrid) grid)
            .findFirst()
            .orElse(null);
    }

    private SchedulerJobInstanceGridWidget createWidget(String jobStatus, String jobName) {
        return new SchedulerJobInstanceGridWidget(
            scheduledContextInstanceService,
            moduleMetaDataService,
            scheduledProcessManagementService,
            configurationService,
            moduleControlService,
            metaDataService,
            systemEventLogger,
            schedulerJobService,
            logStreamingService,
            contextInstance,
            schedulerJobInstanceService,
            jobInitiationService,
            configurationService,
            metaDataService,
            jobUtilsService,
            scheduledContextService,
            jobStatus,
            jobName,
            contextProfileService,
            globalEventService,
            systemEventSearchService,
            100.0,
            100.0,
            100.0,
            100.0
        );
    }
}
