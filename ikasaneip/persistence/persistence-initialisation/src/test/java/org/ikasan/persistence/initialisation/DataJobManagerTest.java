package org.ikasan.persistence.initialisation;

import org.ikasan.persistence.initialisation.core.DataJob;
import org.ikasan.persistence.initialisation.core.DataJobException;
import org.ikasan.persistence.initialisation.core.DataJobManager;
import org.ikasan.persistence.initialisation.core.InitialDataJobStatusConstants;
import org.ikasan.persistence.initialisation.model.DashboardPlatformSetupImpl;
import org.ikasan.persistence.initialisation.model.DashboardSetupItemImpl;
import org.ikasan.spec.persistence.model.DashboardPlatformSetup;
import org.ikasan.spec.persistence.model.DashboardSetupItem;
import org.ikasan.spec.persistence.service.SetupService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test for DataJobManager
 */
public class DataJobManagerTest {

    private Mockery mockery = new Mockery() {
        {
            setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
            setThreadingPolicy(new Synchroniser());
        }
    };

    private SetupService mockSetupService;
    private DataJob mockJob1;
    private DataJob mockJob2;
    private DataJob mockJob3;

    @Before
    public void setup() {
        mockSetupService = mockery.mock(SetupService.class);
        mockJob1 = mockery.mock(DataJob.class, "job1");
        mockJob2 = mockery.mock(DataJob.class, "job2");
        mockJob3 = mockery.mock(DataJob.class, "job3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_throws_exception_when_setupService_is_null() {
        new DataJobManager(null, new ArrayList<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_throws_exception_when_solrDataJobs_is_null() {
        new DataJobManager(mockSetupService, null);
    }

    @Test
    public void test_constructor_accepts_valid_parameters() {
        List<DataJob> jobs = Arrays.asList(mockJob1, mockJob2);
        DataJobManager manager = new DataJobManager(mockSetupService, jobs);

        Assert.assertNotNull(manager);
    }

    @Test
    public void test_execute_runs_all_jobs_when_platform_setup_is_null() throws Exception {
        List<DataJob> jobs = Arrays.asList(mockJob1, mockJob2);
        DataJobManager manager = new DataJobManager(mockSetupService, jobs);

        mockery.checking(new Expectations() {{
            // First call returns null (no existing setup)
            oneOf(mockSetupService).getDashboardPlatformSetup();
            will(returnValue(null));

            // Job 1 execution
            oneOf(mockJob1).getJobName();
            will(returnValue("JOB_1"));
            oneOf(mockJob1).execute();

            // Save after job 1
            oneOf(mockSetupService).save(with(any(DashboardPlatformSetup.class)));

            // Job 2 execution
            oneOf(mockJob2).getJobName();
            will(returnValue("JOB_2"));
            oneOf(mockJob2).execute();
            oneOf(mockJob2).getJobName();
            will(returnValue("JOB_2"));
            // Save after job 2
            oneOf(mockSetupService).save(with(any(DashboardPlatformSetup.class)));
        }});

        manager.execute();

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_execute_skips_jobs_already_completed() throws Exception {
        // Create platform setup with job1 already completed
        DashboardPlatformSetupImpl existingSetup = new DashboardPlatformSetupImpl();
        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new DashboardSetupItemImpl("JOB_1",
            InitialDataJobStatusConstants.COMPLETE_SUCCESS, System.currentTimeMillis()));
        existingSetup.setPlatformSetupItems(items);

        List<DataJob> jobs = Arrays.asList(mockJob1, mockJob2);
        DataJobManager manager = new DataJobManager(mockSetupService, jobs);

        mockery.checking(new Expectations() {{
            // Get existing setup
            oneOf(mockSetupService).getDashboardPlatformSetup();
            will(returnValue(existingSetup));

            // Job 1 should be skipped (already completed)
            oneOf(mockJob1).getJobName();
            will(returnValue("JOB_1"));

            // Job 2 should execute
            oneOf(mockJob2).getJobName();
            will(returnValue("JOB_2"));
            oneOf(mockJob2).execute();

            oneOf(mockJob2).getJobName();
            will(returnValue("JOB_2"));

            // Save after job 2
            oneOf(mockSetupService).save(with(any(DashboardPlatformSetup.class)));
        }});

        manager.execute();

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_execute_runs_job_with_error_status() throws Exception {
        // Create platform setup with job1 in ERROR status
        DashboardPlatformSetupImpl existingSetup = new DashboardPlatformSetupImpl();
        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new DashboardSetupItemImpl("JOB_1",
            InitialDataJobStatusConstants.ERROR, System.currentTimeMillis()));
        existingSetup.setPlatformSetupItems(items);

        List<DataJob> jobs = Arrays.asList(mockJob1);
        DataJobManager manager = new DataJobManager(mockSetupService, jobs);

        mockery.checking(new Expectations() {{
            // Get existing setup
            oneOf(mockSetupService).getDashboardPlatformSetup();
            will(returnValue(existingSetup));

            // Job 1 should execute (because it was in ERROR state)
            oneOf(mockJob1).getJobName();
            will(returnValue("JOB_1"));
            oneOf(mockJob1).execute();

            oneOf(mockJob1).getJobName();
            will(returnValue("JOB_1"));

            // Save after job 1
            oneOf(mockSetupService).save(with(any(DashboardPlatformSetup.class)));
        }});

        manager.execute();

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_execute_with_empty_job_list() throws Exception {
        List<DataJob> jobs = new ArrayList<>();
        DataJobManager manager = new DataJobManager(mockSetupService, jobs);

        mockery.checking(new Expectations() {{
            oneOf(mockSetupService).getDashboardPlatformSetup();
            will(returnValue(null));
        }});

        manager.execute();

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_execute_saves_setup_with_correct_status() throws Exception {
        final List<DashboardPlatformSetup>[] capturedSetup = new List[]{new ArrayList<>()};

        List<DataJob> jobs = Arrays.asList(mockJob1);
        DataJobManager manager = new DataJobManager(mockSetupService, jobs);

        mockery.checking(new Expectations() {{
            oneOf(mockSetupService).getDashboardPlatformSetup();
            will(returnValue(null));

            oneOf(mockJob1).getJobName();
            will(returnValue("TEST_JOB"));
            oneOf(mockJob1).execute();

            oneOf(mockSetupService).save(with(any(DashboardPlatformSetup.class)));
            will(new org.jmock.api.Action() {
                @Override
                public Object invoke(org.jmock.api.Invocation invocation) throws Throwable {
                    capturedSetup[0].add((DashboardPlatformSetup) invocation.getParameter(0));
                    return null;
                }

                @Override
                public void describeTo(org.hamcrest.Description description) {
                    description.appendText("captures setup");
                }
            });
        }});

        manager.execute();

        // Verify saved setup
        Assert.assertEquals(1, capturedSetup[0].size());
        DashboardPlatformSetup saved = capturedSetup[0].get(0);
        Assert.assertNotNull(saved.getPlatformSetupItems());
        Assert.assertEquals(1, saved.getPlatformSetupItems().size());
        Assert.assertEquals("TEST_JOB", saved.getPlatformSetupItems().get(0).getName());
        Assert.assertEquals(InitialDataJobStatusConstants.COMPLETE_SUCCESS,
            saved.getPlatformSetupItems().get(0).getStatus());
        Assert.assertTrue(saved.getPlatformSetupItems().get(0).getExecutionTimestamp() > 0);

        mockery.assertIsSatisfied();
    }

    @Test(expected = DataJobException.class)
    public void test_execute_propagates_job_exception() throws Exception {
        List<DataJob> jobs = Arrays.asList(mockJob1);
        DataJobManager manager = new DataJobManager(mockSetupService, jobs);

        mockery.checking(new Expectations() {{
            oneOf(mockSetupService).getDashboardPlatformSetup();
            will(returnValue(null));

            oneOf(mockJob1).getJobName();
            will(returnValue("FAILING_JOB"));
            oneOf(mockJob1).execute();
            will(throwException(new DataJobException("Job failed")));
            oneOf(mockJob1).getJobName();
            will(returnValue("FAILING_JOB"));

            oneOf(mockSetupService).save(with(any(DashboardPlatformSetup.class)));
        }});

        manager.execute();
    }

    @Test
    public void test_execute_processes_multiple_jobs_in_order() throws Exception {
        final List<String> executionOrder = new ArrayList<>();

        List<DataJob> jobs = Arrays.asList(mockJob1, mockJob2, mockJob3);
        DataJobManager manager = new DataJobManager(mockSetupService, jobs);

        mockery.checking(new Expectations() {{
            oneOf(mockSetupService).getDashboardPlatformSetup();
            will(returnValue(null));

            // Job 1
            oneOf(mockJob1).getJobName();
            will(returnValue("JOB_1"));
            oneOf(mockJob1).execute();
            will(new org.jmock.api.Action() {
                @Override
                public Object invoke(org.jmock.api.Invocation invocation) {
                    executionOrder.add("JOB_1");
                    return null;
                }

                @Override
                public void describeTo(org.hamcrest.Description description) {
                    description.appendText("records job1 execution");
                }
            });
            oneOf(mockSetupService).save(with(any(DashboardPlatformSetup.class)));

            // Job 2
            oneOf(mockJob2).getJobName();
            will(returnValue("JOB_2"));
            oneOf(mockJob2).execute();
            oneOf(mockJob2).getJobName();
            will(returnValue("JOB_2"));
            will(new org.jmock.api.Action() {
                @Override
                public Object invoke(org.jmock.api.Invocation invocation) {
                    executionOrder.add("JOB_2");
                    return null;
                }

                @Override
                public void describeTo(org.hamcrest.Description description) {
                    description.appendText("records job2 execution");
                }
            });
            oneOf(mockSetupService).save(with(any(DashboardPlatformSetup.class)));

            // Job 3
            oneOf(mockJob3).getJobName();
            will(returnValue("JOB_3"));
            oneOf(mockJob3).execute();
            oneOf(mockJob3).getJobName();
            will(returnValue("JOB_3"));
            will(new org.jmock.api.Action() {
                @Override
                public Object invoke(org.jmock.api.Invocation invocation) {
                    executionOrder.add("JOB_3");
                    return null;
                }

                @Override
                public void describeTo(org.hamcrest.Description description) {
                    description.appendText("records job3 execution");
                }
            });
            oneOf(mockSetupService).save(with(any(DashboardPlatformSetup.class)));
        }});

        manager.execute();

        // Verify execution order
        Assert.assertEquals(Arrays.asList("JOB_1", "JOB_2", "JOB_3"), executionOrder);

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_execute_accumulates_setup_items() throws Exception {
        final List<DashboardPlatformSetup> savedSetups = new ArrayList<>();

        List<DataJob> jobs = Arrays.asList(mockJob1, mockJob2);
        DataJobManager manager = new DataJobManager(mockSetupService, jobs);

        mockery.checking(new Expectations() {{
            oneOf(mockSetupService).getDashboardPlatformSetup();
            will(returnValue(null));

            // Job 1
            oneOf(mockJob1).getJobName();
            will(returnValue("JOB_1"));
            oneOf(mockJob1).execute();
            oneOf(mockSetupService).save(with(any(DashboardPlatformSetup.class)));
            will(new org.jmock.api.Action() {
                @Override
                public Object invoke(org.jmock.api.Invocation invocation) {
                    savedSetups.add((DashboardPlatformSetup) invocation.getParameter(0));
                    return null;
                }

                @Override
                public void describeTo(org.hamcrest.Description description) {
                    description.appendText("captures first save");
                }
            });

            // Job 2
            oneOf(mockJob2).getJobName();
            will(returnValue("JOB_2"));
            oneOf(mockJob2).execute();
            oneOf(mockJob2).getJobName();
            will(returnValue("JOB_2"));
            oneOf(mockSetupService).save(with(any(DashboardPlatformSetup.class)));
            will(new org.jmock.api.Action() {
                @Override
                public Object invoke(org.jmock.api.Invocation invocation) {
                    savedSetups.add((DashboardPlatformSetup) invocation.getParameter(0));
                    return null;
                }

                @Override
                public void describeTo(org.hamcrest.Description description) {
                    description.appendText("captures second save");
                }
            });
        }});

        manager.execute();

        // Verify that items accumulate
        Assert.assertEquals(2, savedSetups.size());
        Assert.assertEquals(2, savedSetups.get(0).getPlatformSetupItems().size());
        Assert.assertEquals(2, savedSetups.get(1).getPlatformSetupItems().size());

        mockery.assertIsSatisfied();
    }
}
