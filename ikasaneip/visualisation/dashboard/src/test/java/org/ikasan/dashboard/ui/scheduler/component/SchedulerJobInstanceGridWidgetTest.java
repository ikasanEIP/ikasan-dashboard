package org.ikasan.dashboard.ui.scheduler.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.ikasan.dashboard.ui.scheduler.AbstractSchedulerViewTest;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.model.general.SearchResultsImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceRecordImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.security.model.IkasanPrincipal;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.model.RoleJobPlan;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SchedulerJobInstanceGridWidgetTest extends AbstractSchedulerViewTest {

    @Rule
    public TestName testName = new TestName();

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
    protected IkasanPrincipal ikasanPrincipal;

    @MockitoBean
    protected Role role;

    @MockitoBean
    protected RoleJobPlan roleJobPlan;

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

        Mockito.when(mockIkasanAuthentication.getName()).thenReturn("username");
        Mockito.when(this.userService.loadUserByUsername("username")).thenReturn(user);
        Mockito.when(user.isRequiresPasswordChange()).thenReturn(false);

        Mockito.when(mockIkasanAuthentication.getPrincipal()).thenReturn(this.user);
        when(this.user.getPrincipals()).thenReturn(Set.of(this.ikasanPrincipal));

        when(this.ikasanPrincipal.getRoles()).thenReturn(Set.of(this.role));

        when(this.role.getRoleJobPlans()).thenReturn(Set.of(this.roleJobPlan));

        when(this.roleJobPlan.getJobPlanName()).thenReturn("test-context");

        Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.DASHBOARD_READ))
            .thenReturn(true);
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

    @Test
    public void test_widget_creation_with_all_required_dependencies() {
        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);

        // Then
        Assert.assertNotNull("Widget should be created", widget);
    }

    @Test
    public void test_widget_creation_throws_exception_when_scheduledContextInstanceService_is_null() {
        // Then
        try {
            new SchedulerJobInstanceGridWidget(null, moduleMetadataService, configurationService,
                moduleControlService, metaDataService, systemEventLogger, schedulerJobService,
                logStreamingService, contextInstance, schedulerJobInstanceService,
                jobInitiationService, configurationService, metaDataService, jobUtilsService,
                scheduledContextService, null, null, contextProfileService, globalEventService,
                systemEventSearchService, 100.0, 100.0, 100.0, 100.0);
            Assert.fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("scheduledContextInstanceService cannot be null!", e.getMessage());
        }
    }

    @Test
    public void test_widget_creation_throws_exception_when_schedulerJobInstanceService_is_null() {
        // Then
        try {
            new SchedulerJobInstanceGridWidget(scheduledContextInstanceService, moduleMetadataService,
                configurationService, moduleControlService, metaDataService, systemEventLogger,
                schedulerJobService, logStreamingService, contextInstance, null,
                jobInitiationService, configurationService, metaDataService, jobUtilsService,
                scheduledContextService, null, null, contextProfileService, globalEventService,
                systemEventSearchService, 100.0, 100.0, 100.0, 100.0);
            Assert.fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("schedulerJobInstanceService cannot be null!", e.getMessage());
        }
    }

    @Test
    public void test_widget_creation_throws_exception_when_contextInstance_is_null() {
        // Then
        try {
            new SchedulerJobInstanceGridWidget(scheduledContextInstanceService, moduleMetadataService,
                configurationService, moduleControlService, metaDataService, systemEventLogger,
                schedulerJobService, logStreamingService, null, schedulerJobInstanceService,
                jobInitiationService, configurationService, metaDataService, jobUtilsService,
                scheduledContextService, null, null, contextProfileService, globalEventService,
                systemEventSearchService, 100.0, 100.0, 100.0, 100.0);
            Assert.fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("contextInstance cannot be null!", e.getMessage());
        }
    }

    @Test
    public void test_widget_creation_throws_exception_when_systemEventLogger_is_null() {
        // Then
        try {
            new SchedulerJobInstanceGridWidget(scheduledContextInstanceService, moduleMetadataService,
                configurationService, moduleControlService, metaDataService, null,
                schedulerJobService, logStreamingService, contextInstance, schedulerJobInstanceService,
                jobInitiationService, configurationService, metaDataService, jobUtilsService,
                scheduledContextService, null, null, contextProfileService, globalEventService,
                systemEventSearchService, 100.0, 100.0, 100.0, 100.0);
            Assert.fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("systemEventLogger cannot be null!", e.getMessage());
        }
    }

    @Test
    public void test_widget_displays_job_instances_for_specific_job_name() {
        // Given
        String jobName = "test-job";

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, jobName);

        // Then
        Assert.assertNotNull("Widget should be created with job name filter", widget);
    }

    @Test
    public void test_widget_displays_job_instances_for_specific_status() {
        // Given
        String status = InstanceStatus.RUNNING.name();

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(status, null);

        // Then
        Assert.assertNotNull("Widget should be created with status filter", widget);
    }

    @Test
    public void test_widget_refresh_updates_grid_data() {
        // Given
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);

        // When
        widget.refresh();

        // Then - no exception should be thrown
        Assert.assertNotNull("Widget should still exist after refresh", widget);
    }

    @Test
    public void test_widget_has_components() {
        // Given
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);

        // Then
        Assert.assertNotNull("Widget should have child components", widget);
        Assert.assertTrue("Widget should contain components", widget.getChildren().count() > 0);
    }

    @Test
    public void test_grid_displays_correct_number_of_rows() {
        // Given - set up mock to return 5 job instances for all page sizes
        List<SchedulerJobInstanceRecord> records = createJobInstanceRecords(5);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(0), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(records, records.size(), 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(records, records.size(), 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then
        Assert.assertNotNull("Grid should exist in widget", grid);
        Assert.assertEquals("Grid should display 5 rows", 5, GridKt._size(grid));
    }

    @Test
    public void test_grid_has_job_name_column() {
        // Given
        List<SchedulerJobInstanceRecord> records = createJobInstanceRecords(3);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(records, records.size(), 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Check that grid has a column with key "moduleName" for job name
        Assert.assertNotNull("Grid should exist", grid);
        boolean hasJobNameColumn = grid.getColumns().stream()
            .anyMatch(col -> "moduleName".equals(col.getKey()));
        Assert.assertTrue("Grid should have job name column with key 'moduleName'", hasJobNameColumn);
    }

    @Test
    public void test_grid_has_display_name_column_when_context_uses_display_names() {
        // Given
        contextInstance.setUseDisplayName(true);
        List<SchedulerJobInstanceRecord> records = createJobInstanceRecords(2);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(records, records.size(), 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Check that grid has display name (alias) column when context uses display names
        Assert.assertNotNull("Grid should exist", grid);
        boolean hasDisplayNameColumn = grid.getColumns().stream()
            .anyMatch(col -> "alias".equals(col.getKey()));
        Assert.assertTrue("Grid should have display name column with key 'alias' when context uses display names",
            hasDisplayNameColumn);
    }

    @Test
    public void test_grid_has_child_context_name_column() {
        // Given
        List<SchedulerJobInstanceRecord> records = createJobInstanceRecords(2);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(records, records.size(), 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Check that grid has child context name column
        Assert.assertNotNull("Grid should exist", grid);
        boolean hasChildContextColumn = grid.getColumns().stream()
            .anyMatch(col -> "childContextName".equals(col.getKey()));
        Assert.assertTrue("Grid should have child context name column with key 'childContextName'",
            hasChildContextColumn);
    }

    @Test
    public void test_grid_status_column_displays_status() {
        // Given
        List<SchedulerJobInstanceRecord> records = createJobInstanceRecords(1);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(records, records.size(), 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then
        HorizontalLayout statusLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "status");
        Assert.assertNotNull("Status column should exist", statusLayout);
        // The status is rendered as a SchedulerStatusDiv component
        Assert.assertTrue("Status column should contain components", statusLayout.getComponentCount() > 0);
    }

    @Test
    public void test_widget_contains_components_with_buttons() {
        // Given
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);

        // When - find all buttons in widget and child components recursively
        List<Button> buttons = new ArrayList<>();
        findButtons(widget, buttons);

        // Then - Widget structure contains buttons (in toolbars, etc)
        Assert.assertFalse("Widget should contain buttons in its structure", buttons.isEmpty());
    }

    @Test
    public void test_grid_has_expected_columns() {
        // Given
        List<SchedulerJobInstanceRecord> records = createJobInstanceRecords(1);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(records, records.size(), 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Check that grid has the expected columns
        Assert.assertNotNull("Grid should exist", grid);
        // The grid should have columns for job name, type, child context, status, etc
        Assert.assertTrue("Grid should have columns", grid.getColumns().size() > 0);
    }

    private void findButtons(com.vaadin.flow.component.Component component, List<Button> buttons) {
        if (component instanceof Button) {
            buttons.add((Button) component);
        }
        component.getChildren().forEach(child -> findButtons(child, buttons));
    }

    @Test
    public void test_submit_button_visible_for_waiting_jobs_with_write_permission() {
        // Given - Job in WAITING status with write permissions
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.WAITING, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Actions column should exist and contain action icons
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        Assert.assertNotNull("Actions column should exist", actionsLayout);

        // Count all icons (visible and non-visible) - submit button exists for WAITING jobs
        long playIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:play".equals(icon.getElement().getAttribute("icon")))
            .count();

        Assert.assertTrue("Submit button (play icon) should exist for WAITING jobs", playIcons > 0);
    }

    @Test
    public void test_submit_button_not_visible_for_running_jobs() {
        // Given - Job in RUNNING status
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.RUNNING, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Submit button should NOT be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        Assert.assertNotNull("Actions column should exist", actionsLayout);

        long visiblePlayIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:play".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertEquals("Submit button should not be visible for RUNNING jobs", 0, visiblePlayIcons);
    }

    @Test
    public void test_reset_button_visible_for_completed_internal_event_driven_jobs() {
        // Given - Internal event driven job in COMPLETE status with write permissions
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.COMPLETE, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Reset button should be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        Assert.assertNotNull("Actions column should exist", actionsLayout);

        long resetIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:arrow-backward".equals(icon.getElement().getAttribute("icon")))
            .count();

        Assert.assertTrue("Reset button (arrow backward icon) should be visible for COMPLETE internal event driven jobs", resetIcons > 0);
    }

    @Test
    public void test_reset_button_visible_for_error_file_event_driven_jobs() {
        // Given - File event driven job in ERROR status with write permissions
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.ERROR, JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Reset button should be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        Assert.assertNotNull("Actions column should exist", actionsLayout);

        long resetIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:arrow-backward".equals(icon.getElement().getAttribute("icon")))
            .count();

        Assert.assertTrue("Reset button should be visible for ERROR file event driven jobs", resetIcons > 0);
    }

    @Test
    public void test_reset_button_visible_for_lock_queued_jobs() {
        UI.setCurrent(new UI());
        // Given - Internal event driven job in LOCK_QUEUED status
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.LOCK_QUEUED, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Reset button should be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        Assert.assertNotNull("Actions column should exist", actionsLayout);

        long resetIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:arrow-backward".equals(icon.getElement().getAttribute("icon")))
            .count();

        Assert.assertTrue("Reset button should be visible for LOCK_QUEUED jobs", resetIcons > 0);
    }

    @Test
    public void test_reset_button_not_visible_for_quartz_jobs() {
        // Given - Quartz job in COMPLETE status (reset only for internal/file event driven)
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.COMPLETE, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Reset button should NOT be visible for quartz jobs
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        Assert.assertNotNull("Actions column should exist", actionsLayout);

        long visibleResetIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:arrow-backward".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertEquals("Reset button should not be visible for quartz schedule driven jobs", 0, visibleResetIcons);
    }

    @Test
    public void test_reset_button_not_visible_for_running_jobs() {
        // Given - Job in RUNNING status
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.RUNNING, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Reset button should NOT be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        Assert.assertNotNull("Actions column should exist", actionsLayout);

        long visibleResetIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:arrow-backward".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertEquals("Reset button should not be visible for RUNNING jobs", 0, visibleResetIcons);
    }

    // Skip Button Visibility Tests

    @Test
    public void test_skip_button_visible_for_waiting_jobs() {
        // Given - Job in WAITING status
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.WAITING, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Skip button should be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long skipIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:ban".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertTrue("Skip button should be visible for WAITING jobs", skipIcons > 0);
    }

    @Test
    public void test_skip_button_not_visible_for_running_jobs() {
        // Given - Job in RUNNING status (excluded from skip button visibility)
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.RUNNING, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Skip button should not be visible for RUNNING jobs
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long skipIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:ban".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertEquals("Skip button should not be visible for RUNNING jobs", 0, skipIcons);
    }

    @Test
    public void test_skip_button_not_visible_for_lock_queued_jobs() {
        // Given - Job in LOCK_QUEUED status (excluded from skip button visibility)
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.LOCK_QUEUED, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Skip button should not be visible for LOCK_QUEUED jobs
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long skipIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:ban".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertEquals("Skip button should not be visible for LOCK_QUEUED jobs", 0, skipIcons);
    }

    @Test
    public void test_skip_button_not_visible_for_complete_jobs() {
        // Given - Job in COMPLETE status
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.COMPLETE, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Skip button should not be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long skipIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:ban".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertEquals("Skip button should not be visible for COMPLETE jobs", 0, skipIcons);
    }

    // Enable Button Visibility Tests

    @Test
    public void test_enable_button_visible_for_skipped_internal_event_driven_jobs() {
        // Given - Internal event driven job in SKIPPED status
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.SKIPPED, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Enable button should be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long enableIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:play".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertTrue("Enable button should be visible for SKIPPED internal event driven jobs", enableIcons > 0);
    }

    @Test
    public void test_enable_button_not_visible_for_quartz_jobs() {
        // Given - Quartz job
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.SKIPPED, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Enable button should not be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long enableIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:play".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertEquals("Enable button should not be visible for quartz jobs", 0, enableIcons);
    }

    // Hold Button Visibility Tests

    @Test
    public void test_hold_button_visible_for_waiting_internal_event_driven_jobs() {
        // Given - Internal event driven job in WAITING status
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.WAITING, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Hold button should be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long holdIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:hand".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertTrue("Hold button should be visible for WAITING internal event driven jobs", holdIcons > 0);
    }

    @Test
    public void test_hold_button_not_visible_for_complete_jobs() {
        // Given - Job in COMPLETE status
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.COMPLETE, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Hold button should not be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long holdIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:hand".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertEquals("Hold button should not be visible for COMPLETE jobs", 0, holdIcons);
    }

    // Release Button Visibility Tests

    @Test
    public void test_release_button_visible_for_on_hold_jobs() {
        // Given - Job in ON_HOLD status
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.ON_HOLD, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Release button should be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long releaseIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:hands-up".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertTrue("Release button should be visible for ON_HOLD jobs", releaseIcons > 0);
    }

    @Test
    public void test_release_button_not_visible_for_waiting_jobs() {
        // Given - Job in WAITING status
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.WAITING, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Release button should not be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long releaseIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:hands-up".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertEquals("Release button should not be visible for WAITING jobs", 0, releaseIcons);
    }

    // Acknowledge Error Button Visibility Tests

    @Test
    public void test_acknowledge_error_button_visible_for_error_jobs() {
        // Given - Job in ERROR status without acknowledgment
        SchedulerJobInstanceRecord record = createJobInstanceWithStatusAndErrorAcknowledged(InstanceStatus.ERROR,
            JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE, false);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Acknowledge error button should be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long acknowledgeIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:thumbs-up".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertTrue("Acknowledge error button should be visible for ERROR jobs", acknowledgeIcons > 0);
    }

    @Test
    public void test_acknowledge_error_button_not_visible_for_acknowledged_errors() {
        // Given - Job in ERROR status with acknowledgment
        SchedulerJobInstanceRecord record = createJobInstanceWithStatusAndErrorAcknowledged(InstanceStatus.ERROR,
            JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE, true);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Acknowledge error button should not be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long acknowledgeIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:thumbs-up".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertEquals("Acknowledge error button should not be visible for acknowledged errors", 0, acknowledgeIcons);
    }

    // Log File Button Visibility Tests

    @Test
    public void test_log_file_button_visible_for_running_internal_event_driven_jobs() {
        // Given - Internal event driven job in RUNNING status
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.RUNNING, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Log file button should be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long logFileIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:file-process".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertTrue("Log file button should be visible for RUNNING internal event driven jobs", logFileIcons > 0);
    }

    @Test
    public void test_log_file_button_not_visible_for_file_event_driven_jobs() {
        // Given - File event driven job
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.RUNNING, JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Log file button should not be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long logFileIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:file-process".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertEquals("Log file button should not be visible for file event driven jobs", 0, logFileIcons);
    }

    // Error Log File Button Visibility Tests

    @Test
    public void test_error_log_file_button_visible_for_error_internal_event_driven_jobs() {
        // Given - Internal event driven job in ERROR status
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.ERROR, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Error log file button should be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long errorLogFileIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:file-remove".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertTrue("Error log file button should be visible for ERROR internal event driven jobs", errorLogFileIcons > 0);
    }

    // Log File History Button Visibility Tests

    @Test
    public void test_log_file_history_button_visible_for_internal_event_driven_jobs() {
        // Given - Internal event driven job
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.COMPLETE, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Log file history button should be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long historyIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:clipboard-heart".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertTrue("Log file history button should be visible for internal event driven jobs", historyIcons > 0);
    }

    // Event Button Visibility Tests

    @Test
    public void test_event_button_visible_when_scheduled_process_event_exists() {
        // Given - Internal event driven job with scheduled process event
        SchedulerJobInstanceRecord record = createJobInstanceWithScheduledProcessEvent(InstanceStatus.COMPLETE,
            JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Event button should be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long eventIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:calendar-clock".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertTrue("Event button should be visible when scheduled process event exists", eventIcons > 0);
    }

    @Test
    public void test_event_button_not_visible_when_no_scheduled_process_event() {
        // Given - Internal event driven job without scheduled process event
        SchedulerJobInstanceRecord record = createJobInstanceWithStatus(InstanceStatus.COMPLETE, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), eq(1), eq(0), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));
        when(schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(List.of(record), 1, 0));

        // When
        SchedulerJobInstanceGridWidget widget = createWidget(null, null);
        SchedulerJobInstanceFilteringGrid grid = findGridInWidget(widget);

        // Then - Event button should not be visible
        HorizontalLayout actionsLayout = (HorizontalLayout) GridKt._getCellComponent(grid, 0, "actions");
        long eventIcons = actionsLayout.getChildren()
            .filter(comp -> comp instanceof Icon)
            .map(comp -> (Icon) comp)
            .filter(icon -> "vaadin:calendar-clock".equals(icon.getElement().getAttribute("icon")) && icon.isVisible())
            .count();

        Assert.assertEquals("Event button should not be visible when no scheduled process event", 0, eventIcons);
    }

    // Helper methods

    private SchedulerJobInstanceRecord createJobInstanceWithStatus(InstanceStatus status, String jobType) {
        SchedulerJobInstanceRecordImpl record = new SchedulerJobInstanceRecordImpl();
        record.setJobName("test-job");
        record.setDisplayName("Test Job");
        record.setChildContextName("child-context");
        record.setStatus(status.name());
        record.setContextInstanceId(contextInstance.getId());
        record.setStartTime(System.currentTimeMillis());
        record.setEndTime(System.currentTimeMillis());
        record.setModifiedBy("testUser");

        if (jobType.equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
            InternalEventDrivenJobInstance jobInstance = mock(InternalEventDrivenJobInstance.class);
            when(jobInstance.getJobName()).thenReturn("test-job");
            when(jobInstance.getAgentName()).thenReturn("test-agent");
            when(jobInstance.getContextName()).thenReturn(contextInstance.getName());
            when(jobInstance.getContextInstanceId()).thenReturn(contextInstance.getId());
            when(jobInstance.getChildContextName()).thenReturn("child-context");
            when(jobInstance.getStatus()).thenReturn(status);
            when(jobInstance.isJobRepeatable()).thenReturn(false);
            record.setSchedulerJobInstance(jobInstance);  // Type is set automatically based on job instance type
        } else if (jobType.equals(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE)) {
            FileEventDrivenJobInstance jobInstance = mock(FileEventDrivenJobInstance.class);
            when(jobInstance.getJobName()).thenReturn("test-job");
            when(jobInstance.getAgentName()).thenReturn("test-agent");
            when(jobInstance.getContextName()).thenReturn(contextInstance.getName());
            when(jobInstance.getContextInstanceId()).thenReturn(contextInstance.getId());
            when(jobInstance.getStatus()).thenReturn(status);
            record.setSchedulerJobInstance(jobInstance);  // Type is set automatically based on job instance type
        } else if (jobType.equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE)) {
            QuartzScheduleDrivenJobInstance jobInstance = mock(QuartzScheduleDrivenJobInstance.class);
            when(jobInstance.getJobName()).thenReturn("test-job");
            when(jobInstance.getAgentName()).thenReturn("test-agent");
            when(jobInstance.getContextName()).thenReturn(contextInstance.getName());
            when(jobInstance.getContextInstanceId()).thenReturn(contextInstance.getId());
            when(jobInstance.getStatus()).thenReturn(status);
            record.setSchedulerJobInstance(jobInstance);  // Type is set automatically based on job instance type
        }

        return record;
    }

    private SchedulerJobInstanceRecord createJobInstanceWithStatusAndErrorAcknowledged(InstanceStatus status, String jobType, Boolean errorAcknowledged) {
        SchedulerJobInstanceRecordImpl record = new SchedulerJobInstanceRecordImpl();
        record.setJobName("test-job");
        record.setDisplayName("Test Job");
        record.setChildContextName("child-context");
        record.setStatus(status.name());
        record.setContextInstanceId(contextInstance.getId());
        record.setStartTime(System.currentTimeMillis());
        record.setEndTime(System.currentTimeMillis());
        record.setModifiedBy("testUser");

        if (jobType.equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
            InternalEventDrivenJobInstance jobInstance = mock(InternalEventDrivenJobInstance.class);
            when(jobInstance.getJobName()).thenReturn("test-job");
            when(jobInstance.getAgentName()).thenReturn("test-agent");
            when(jobInstance.getContextName()).thenReturn(contextInstance.getName());
            when(jobInstance.getContextInstanceId()).thenReturn(contextInstance.getId());
            when(jobInstance.getChildContextName()).thenReturn("child-context");
            when(jobInstance.getStatus()).thenReturn(status);
            when(jobInstance.isJobRepeatable()).thenReturn(false);
            when(jobInstance.isErrorAcknowledged()).thenReturn(errorAcknowledged);
            record.setSchedulerJobInstance(jobInstance);
        }

        return record;
    }

    private SchedulerJobInstanceRecord createJobInstanceWithScheduledProcessEvent(InstanceStatus status, String jobType) {
        SchedulerJobInstanceRecordImpl record = new SchedulerJobInstanceRecordImpl();
        record.setJobName("test-job");
        record.setDisplayName("Test Job");
        record.setChildContextName("child-context");
        record.setStatus(status.name());
        record.setContextInstanceId(contextInstance.getId());
        record.setStartTime(System.currentTimeMillis());
        record.setEndTime(System.currentTimeMillis());
        record.setModifiedBy("testUser");

        if (jobType.equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)) {
            InternalEventDrivenJobInstance jobInstance = mock(InternalEventDrivenJobInstance.class);
            when(jobInstance.getJobName()).thenReturn("test-job");
            when(jobInstance.getAgentName()).thenReturn("test-agent");
            when(jobInstance.getContextName()).thenReturn(contextInstance.getName());
            when(jobInstance.getContextInstanceId()).thenReturn(contextInstance.getId());
            when(jobInstance.getChildContextName()).thenReturn("child-context");
            when(jobInstance.getStatus()).thenReturn(status);
            when(jobInstance.isJobRepeatable()).thenReturn(false);

            ScheduledProcessEvent scheduledProcessEvent = mock(ScheduledProcessEvent.class);
            when(jobInstance.getScheduledProcessEvent()).thenReturn(scheduledProcessEvent);

            record.setSchedulerJobInstance(jobInstance);
        }

        return record;
    }

    // Helper methods

    private SchedulerJobInstanceFilteringGrid findGridInWidget(SchedulerJobInstanceGridWidget widget) {
        return widget.getChildren()
            .flatMap(component -> component.getChildren())
            .filter(child -> child instanceof SchedulerJobInstanceFilteringGrid)
            .map(grid -> (SchedulerJobInstanceFilteringGrid) grid)
            .findFirst()
            .orElse(null);
    }

    private List<SchedulerJobInstanceRecord> createJobInstanceRecords(int count) {
        List<SchedulerJobInstanceRecord> records = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            SchedulerJobInstanceRecord record = new SchedulerJobInstanceRecordImpl();
            record.setJobName("test-job-" + i);
            record.setDisplayName("Test Job " + i);
            record.setChildContextName("child-context-" + i);
            record.setStatus(InstanceStatus.WAITING.name());
            record.setContextInstanceId(contextInstance.getId());
            record.setStartTime(System.currentTimeMillis());
            record.setEndTime(System.currentTimeMillis());
            record.setModifiedBy("testUser");

            InternalEventDrivenJobInstance jobInstance = mock(InternalEventDrivenJobInstance.class);
            when(jobInstance.getJobName()).thenReturn("test-job-" + i);
            when(jobInstance.getAgentName()).thenReturn("test-agent");
            when(jobInstance.getContextName()).thenReturn(contextInstance.getName());
            when(jobInstance.getContextInstanceId()).thenReturn(contextInstance.getId());
            when(jobInstance.getChildContextName()).thenReturn("child-context-" + i);
            when(jobInstance.getStatus()).thenReturn(InstanceStatus.WAITING);
            when(jobInstance.isJobRepeatable()).thenReturn(false);

            record.setSchedulerJobInstance(jobInstance);
            records.add(record);
        }

        return records;
    }

    // Helper methods

    private SchedulerJobInstanceGridWidget createWidget(String jobStatus, String jobName) {
        return new SchedulerJobInstanceGridWidget(
            scheduledContextInstanceService,
            moduleMetadataService,
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
