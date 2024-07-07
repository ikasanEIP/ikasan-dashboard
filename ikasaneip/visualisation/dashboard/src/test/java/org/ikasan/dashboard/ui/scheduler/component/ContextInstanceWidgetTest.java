package org.ikasan.dashboard.ui.scheduler.component;

import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import liquibase.pro.packaged.C;
import org.ikasan.dashboard.ui.scheduler.AbstractSchedulerViewTest;
import org.ikasan.dashboard.ui.scheduler.view.SchedulerView;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.security.model.IkasanPrincipal;
import org.ikasan.security.model.Role;
import org.ikasan.security.model.RoleJobPlan;
import org.ikasan.security.model.User;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceAggregateJobStatus;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.rules.TestName;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.github.mvysny.kaributesting.v10.LocatorJ._assertNone;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ContextInstanceWidgetTest extends AbstractSchedulerViewTest {

    @Rule
    public TestName testName = new TestName();

    @MockBean
    protected IkasanPrincipal ikasanPrincipal;

    @MockBean
    protected Role role;

    @MockBean
    protected RoleJobPlan roleJobPlan;

    @MockBean
    ContextParametersInstanceService contextParametersInstanceService;

    @MockBean
    JobLockCacheInitialisationService jobLockCacheInitialisationService;

    @MockBean
    JobUtilsService jobUtilsService;

    @Autowired
    ContextInstancePublicationService<ContextInstance> contextInstancePublicationService;

    @Override
    public void setup_expectations() throws IOException {
        this.setupSecurityExpectations();

        when(this.scheduledContextService.findByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(new ArrayList<>(), 0, 1));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(-1),eq(-1), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(5),eq(0), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(5),eq(0), anyString(), anyString()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(5), 5, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(0), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecords(1), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(1), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(1), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(2), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(2), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(3), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(3), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(4), Mockito.isNull(), Mockito.isNull()))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(4), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(0), anyString(), eq(SortDirection.DESCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(4), 0, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(1),  anyString(), eq(SortDirection.DESCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(3), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(2),  anyString(), eq(SortDirection.DESCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(2), 2, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(3),  anyString(), eq(SortDirection.DESCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(1), 3, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(4),  anyString(), eq(SortDirection.DESCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(0), 4, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(0), anyString(), eq(SortDirection.ASCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(0), 0, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(1),  anyString(), eq(SortDirection.ASCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(1), 1, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(2),  anyString(), eq(SortDirection.ASCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(2), 2, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(3),  anyString(), eq(SortDirection.ASCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(3), 3, 0));

        Mockito.when(this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(Mockito.any(),
                eq(1),eq(4),  anyString(), eq(SortDirection.ASCENDING.toString())))
            .thenReturn(new SearchResultsImpl<>(this.getScheduledContextInstanceRecordWithId(4), 4, 0));

        when(this.moduleMetadataService.find(anyList(), any(), anyInt(), anyInt()))
            .thenReturn(new ModuleMetadataSearchResults(new ArrayList<>(), 0, 1));

        Mockito.when(this.schedulerJobInstanceService.getJobStatusCountForContextInstances(Mockito.any()))
            .thenReturn(new ArrayList<>(this.getAggregateContextInstanceStatuses()));

        Mockito.when(this.scheduledContextInstanceService.findById(anyString()))
            .thenReturn(this.getScheduledContextInstanceRecordWithId(1).get(0));

        Mockito.when(this.scheduledContextService.findByName(anyString()))
            .thenReturn(this.getScheduledContextRecords(1).get(0));

        Mockito.when(this.schedulerJobInstanceService
            .getSchedulerJobInstancesByContextInstanceId(anyString(), anyInt(), anyInt(), isNull(),isNull()))
            .thenReturn(new SearchResultsImpl<>(new ArrayList<>(), 0, 0));

        Mockito.when(this.scheduledContextInstanceService.findAllAuditRecordsByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(new ArrayList<>(), 0, 0));

        Mockito.when(this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(any(), anyInt(), anyInt(), isNull(), isNull()))
            .thenReturn(new SearchResultsImpl<>(new ArrayList<>(), 0, 0));
    }

    private void setupSecurityExpectations() {
        IkasanAuthentication mockIkasanAuthentication = mock(IkasanAuthentication.class);
        // Setup the mock authentication.
        SecurityContextHolder.getContext().setAuthentication(mockIkasanAuthentication);

        // Mock some of the requisite behaviour.
        Mockito.when(mockIkasanAuthentication.getName())
            .thenReturn("username");
        Mockito.when(this.userService.loadUserByUsername("username"))
            .thenReturn(user);
        Mockito.when(user.isRequiresPasswordChange())
            .thenReturn(false);

        Mockito.when(mockIkasanAuthentication.getPrincipal()).thenReturn(this.user);
        when(this.user.getPrincipals()).thenReturn(Set.of(this.ikasanPrincipal));

        when(this.ikasanPrincipal.getRoles()).thenReturn(Set.of(this.role));

        when(this.role.getRoleJobPlans()).thenReturn(Set.of(this.roleJobPlan));

        when(this.roleJobPlan.getJobPlanName()).thenReturn("contextName");

        Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.DASHBOARD_READ))
            .thenReturn(true);

        if(testName.getMethodName().equals("test_admin_user_security")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(true);
            this.addContextInstanceToCache(false, false);
        }
        else if(testName.getMethodName().equals("test_admin_user_security_with_isRunContextUntilManuallyEnded_true")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(true);
            this.addContextInstanceToCache(true, true);
        }
        else if(testName.getMethodName().equals("test_scheduler_admin_security")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(false);
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ADMIN))
                .thenReturn(true);
            this.addContextInstanceToCache(false, false);
        }
        else if(testName.getMethodName().equals("test_scheduler_admin_security_with_isRunContextUntilManuallyEnded_true")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(false);
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ADMIN))
                .thenReturn(true);
            this.addContextInstanceToCache(true, true);
        }
        else if(testName.getMethodName().equals("test_scheduler_admin_all_security")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(false);
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ALL_ADMIN))
                .thenReturn(true);
            this.addContextInstanceToCache(false, false);
        }
        else if(testName.getMethodName().equals("test_scheduler_admin_all_security_with_isRunContextUntilManuallyEnded_true")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(false);
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ALL_ADMIN))
                .thenReturn(true);
            this.addContextInstanceToCache(true, true);
        }
        else if(testName.getMethodName().equals("test_scheduler_write_security")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(false);
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_WRITE))
                .thenReturn(true);
            this.addContextInstanceToCache(false, false);
        }
        else if(testName.getMethodName().equals("test_scheduler_write_security_with_isRunContextUntilManuallyEnded_true")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(false);
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_WRITE))
                .thenReturn(true);
            this.addContextInstanceToCache(true, true);
        }
        else if(testName.getMethodName().equals("test_scheduler_write_all_security")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(false);
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_WRITE))
                .thenReturn(true);
            this.addContextInstanceToCache(false, false);
        }
        else if(testName.getMethodName().equals("test_scheduler_write_all_security_with_isRunContextUntilManuallyEnded_true")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(false);
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_WRITE))
                .thenReturn(true);
            this.addContextInstanceToCache(true, true);
        }
        else if(testName.getMethodName().equals("test_scheduler_read_security")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(false);
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_READ))
                .thenReturn(true);
            this.addContextInstanceToCache(false, false);
        }
        else if(testName.getMethodName().equals("test_scheduler_read_security_with_isRunContextUntilManuallyEnded_true")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(false);
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_READ))
                .thenReturn(true);
            this.addContextInstanceToCache(true, true);
        }
        else if(testName.getMethodName().equals("test_scheduler_read_all_security")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(false);
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_READ))
                .thenReturn(true);
            this.addContextInstanceToCache(false, false);
        }
        else if(testName.getMethodName().equals("test_scheduler_read_all_security_with_isRunContextUntilManuallyEnded_true")) {
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY))
                .thenReturn(false);
            Mockito.when(mockIkasanAuthentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_READ))
                .thenReturn(true);
            this.addContextInstanceToCache(true, true);
        }
    }

    private void addContextInstanceToCache(boolean isRunContextUntilManuallyEnded,
                                           boolean quartzScheduleDrivenJobsDisabledForContext) {
        ContextInstanceImpl contextInstance =  new ContextInstanceImpl();
        contextInstance.setName("contextName1");
        contextInstance.setId("contextInstanceId1");
        contextInstance.setRunContextUntilManuallyEnded(isRunContextUntilManuallyEnded);
        contextInstance.setQuartzScheduleDrivenJobsDisabledForContext(quartzScheduleDrivenJobsDisabledForContext);

        ContextMachine contextMachine = new ContextMachine(new ContextTemplateImpl(), contextInstance
            , this.scheduledContextInstanceService, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>()
            , "this.queueDir", new HashMap<>(), null, JobLockCacheImpl.instance(), contextParametersInstanceService
            , this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService
            , this.contextInstancePublicationService, this.jobUtilsService);
        ContextMachineCache.instance().put(contextMachine);
    }

    private void setupUIComponentPrerequisiteStatesForTests() {
        UI.getCurrent().navigate("scheduler");

        SchedulerView schedulerView = _get(SchedulerView.class);
        Assertions.assertNotNull(schedulerView);

        Tabs schedulerDashboardTabs = _get(Tabs.class, spec -> spec.withId("schedulerViewTabs"));
        Tab schedulerDashboardTab = _get(Tab.class, spec -> spec.withId("schedulerDashboardTab"));
        Assertions.assertNotNull(schedulerDashboardTab);

        schedulerDashboardTabs.setSelectedTab(schedulerDashboardTab);
        Assertions.assertEquals(schedulerDashboardTab, schedulerDashboardTabs.getSelectedTab());

        Assertions.assertNotNull(schedulerDashboardTabs);

        Tabs contextInstanceTabs = _get(Tabs.class, spec -> spec.withId("contextInstancesTab"));
        Assert.assertNotNull(contextInstanceTabs);

        Tab activeJobPlanInstancesTab = _get(Tab.class, spec -> spec.withId("activeJobPlanInstancesTab"));
        Assertions.assertNotNull(contextInstanceTabs);

        contextInstanceTabs.setSelectedTab(activeJobPlanInstancesTab);
        Assertions.assertEquals(activeJobPlanInstancesTab, contextInstanceTabs.getSelectedTab());

        Grid contextInstanceAggregateJobStatusGrid = _get(Grid.class, spec -> spec.withId("contextInstanceAggregateJobStatusGrid"));
        Assertions.assertNotNull(contextInstanceAggregateJobStatusGrid);

        ContextInstanceAggregateJobStatus aggregateJobStatus = (ContextInstanceAggregateJobStatus) GridKt._get(contextInstanceAggregateJobStatusGrid, 0);

        Assertions.assertEquals(1, GridKt._size(contextInstanceAggregateJobStatusGrid));

        Assert.assertEquals("contextInstanceId", aggregateJobStatus.getContextInstanceId());
        Assert.assertEquals("contextName", aggregateJobStatus.getContextInstanceName());

        HorizontalLayout layout = (HorizontalLayout) GridKt._getCellComponent(contextInstanceAggregateJobStatusGrid, 0, "waitingStatusCounts");
        Assert.assertNotNull(layout);

        Button waitingStatusButton = _get(layout, Button.class, spec -> spec.withId("waitingStatusButton"));
        Assert.assertEquals("1 WAITING", waitingStatusButton.getElement().getText());

        layout = (HorizontalLayout) GridKt._getCellComponent(contextInstanceAggregateJobStatusGrid, 0, "completeStatusCounts");
        Assert.assertNotNull(layout);

        Button completeStatusButton = _get(layout, Button.class, spec -> spec.withId("completeStatusButton"));
        Assert.assertEquals("15 COMPLETE", completeStatusButton.getElement().getText());

        completeStatusButton.click();

        ContextInstanceWidget contextInstanceWidget = _get(ContextInstanceWidget.class);
        Assert.assertNotNull(contextInstanceWidget);

        Button actionsButton = _get(contextInstanceWidget
            , Button.class, spec -> spec.withId("actionsButton"));

        Assert.assertNotNull("actionsButton");
        actionsButton.click();
    }

    @Test
    public void test_admin_user_security() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        Button ignoreContextInstanceEndButton = _get(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Assert.assertTrue(ignoreContextInstanceEndButton.isVisible());

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        Button holdContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        Assert.assertTrue(holdContextButton.isVisible());

        Button releaseContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        Assert.assertTrue(releaseContextButton.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        Button resetContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));

        Assert.assertTrue(resetContextButton.isVisible());

    }

    @Test
    public void test_admin_user_security_with_isRunContextUntilManuallyEnded_true() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        Button holdContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        Assert.assertTrue(holdContextButton.isVisible());

        Button releaseContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        Assert.assertTrue(releaseContextButton.isVisible());

        Button contextInstanceEndButton = _get(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        Assert.assertTrue(contextInstanceEndButton.isVisible());

        Button resetContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));

        Assert.assertTrue(resetContextButton.isVisible());
    }


    @Test
    public void test_scheduler_admin_security() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        Button ignoreContextInstanceEndButton = _get(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Assert.assertTrue(ignoreContextInstanceEndButton.isVisible());

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        Button holdContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        Assert.assertTrue(holdContextButton.isVisible());

        Button releaseContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        Assert.assertTrue(releaseContextButton.isVisible());

        Button disableQuartzScheduledJobsButton = _get(actionPopup
            , Button.class, spec -> spec.withId("disableQuartzScheduledJobsButton"));

        Assert.assertTrue(disableQuartzScheduledJobsButton.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        Button resetContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));

        Assert.assertTrue(resetContextButton.isVisible());
    }

    @Test
    public void test_scheduler_admin_security_with_isRunContextUntilManuallyEnded_true() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        Button holdContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        Assert.assertTrue(holdContextButton.isVisible());

        Button releaseContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        Assert.assertTrue(releaseContextButton.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("disableQuartzScheduledJobsButton"));

        Button enableQuartzScheduledJobsButton = _get(actionPopup
            , Button.class, spec -> spec.withId("enableQuartzScheduledJobsButton"));

        Assert.assertTrue(enableQuartzScheduledJobsButton.isVisible());

        Button contextInstanceEndButton = _get(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        Assert.assertTrue(contextInstanceEndButton.isVisible());

        Button resetContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));

        Assert.assertTrue(resetContextButton.isVisible());
    }

    @Test
    public void test_scheduler_admin_all_security() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        Button ignoreContextInstanceEndButton = _get(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Assert.assertTrue(ignoreContextInstanceEndButton.isVisible());

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        Button holdContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        Assert.assertTrue(holdContextButton.isVisible());

        Button releaseContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        Assert.assertTrue(releaseContextButton.isVisible());

        Button disableQuartzScheduledJobsButton = _get(actionPopup
            , Button.class, spec -> spec.withId("disableQuartzScheduledJobsButton"));

        Assert.assertTrue(disableQuartzScheduledJobsButton.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        Button resetContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));

        Assert.assertTrue(resetContextButton.isVisible());
    }

    @Test
    public void test_scheduler_admin_all_security_with_isRunContextUntilManuallyEnded_true() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        Button holdContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        Assert.assertTrue(holdContextButton.isVisible());

        Button releaseContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        Assert.assertTrue(releaseContextButton.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("disableQuartzScheduledJobsButton"));

        Button enableQuartzScheduledJobsButton = _get(actionPopup
            , Button.class, spec -> spec.withId("enableQuartzScheduledJobsButton"));

        Assert.assertTrue(enableQuartzScheduledJobsButton.isVisible());

        Button contextInstanceEndButton = _get(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        Assert.assertTrue(contextInstanceEndButton.isVisible());

        Button resetContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));

        Assert.assertTrue(resetContextButton.isVisible());
    }

    @Test
    public void test_scheduler_write_security() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        Button ignoreContextInstanceEndButton = _get(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Assert.assertTrue(ignoreContextInstanceEndButton.isVisible());

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        Button holdContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        Assert.assertTrue(holdContextButton.isVisible());

        Button releaseContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        Assert.assertTrue(releaseContextButton.isVisible());

        Button disableQuartzScheduledJobsButton = _get(actionPopup
            , Button.class, spec -> spec.withId("disableQuartzScheduledJobsButton"));

        Assert.assertTrue(disableQuartzScheduledJobsButton.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        Button resetContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));

        Assert.assertTrue(resetContextButton.isVisible());
    }

    @Test
    public void test_scheduler_write_security_with_isRunContextUntilManuallyEnded_true() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        Button holdContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        Assert.assertTrue(holdContextButton.isVisible());

        Button releaseContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        Assert.assertTrue(releaseContextButton.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("disableQuartzScheduledJobsButton"));

        Button enableQuartzScheduledJobsButton = _get(actionPopup
            , Button.class, spec -> spec.withId("enableQuartzScheduledJobsButton"));

        Assert.assertTrue(enableQuartzScheduledJobsButton.isVisible());

        Button contextInstanceEndButton = _get(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        Assert.assertTrue(contextInstanceEndButton.isVisible());

        Button resetContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));

        Assert.assertTrue(resetContextButton.isVisible());
    }

    @Test
    public void test_scheduler_write_all_security() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        Button ignoreContextInstanceEndButton = _get(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Assert.assertTrue(ignoreContextInstanceEndButton.isVisible());

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        Button holdContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        Assert.assertTrue(holdContextButton.isVisible());

        Button releaseContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        Assert.assertTrue(releaseContextButton.isVisible());

        Button disableQuartzScheduledJobsButton = _get(actionPopup
            , Button.class, spec -> spec.withId("disableQuartzScheduledJobsButton"));

        Assert.assertTrue(disableQuartzScheduledJobsButton.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        Button resetContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));

        Assert.assertTrue(resetContextButton.isVisible());
    }

    @Test
    public void test_scheduler_write_all_security_with_isRunContextUntilManuallyEnded_true() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        Button holdContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        Assert.assertTrue(holdContextButton.isVisible());

        Button releaseContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        Assert.assertTrue(releaseContextButton.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("disableQuartzScheduledJobsButton"));

        Button enableQuartzScheduledJobsButton = _get(actionPopup
            , Button.class, spec -> spec.withId("enableQuartzScheduledJobsButton"));

        Assert.assertTrue(enableQuartzScheduledJobsButton.isVisible());

        Button contextInstanceEndButton = _get(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        Assert.assertTrue(contextInstanceEndButton.isVisible());

        Button resetContextButton = _get(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));

        Assert.assertTrue(resetContextButton.isVisible());
    }

    @Test
    public void test_scheduler_read_security() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("disableQuartzScheduledJobsButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));
    }

    @Test
    public void test_scheduler_read_security_with_isRunContextUntilManuallyEnded_true() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("disableQuartzScheduledJobsButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));
    }

    @Test
    public void test_scheduler_read_all_security() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("disableQuartzScheduledJobsButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));
    }

    @Test
    public void test_scheduler_read_all_security_with_isRunContextUntilManuallyEnded_true() throws IOException
    {
        this.setupUIComponentPrerequisiteStatesForTests();

        Dialog actionPopup = _get(Dialog.class, spec -> spec.withId("actionPopup"));
        Assert.assertNotNull(actionPopup);

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("ignoreContextInstanceEndButton"));

        Button jobLockDashboard = _get(actionPopup
            , Button.class, spec -> spec.withId("jobLockDashboard"));

        Assert.assertTrue(jobLockDashboard.isVisible());

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("holdContextButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("releaseContextButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("disableQuartzScheduledJobsButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("contextInstanceEndButton"));

        _assertNone(actionPopup
            , Button.class, spec -> spec.withId("resetContextButton"));
    }
}
