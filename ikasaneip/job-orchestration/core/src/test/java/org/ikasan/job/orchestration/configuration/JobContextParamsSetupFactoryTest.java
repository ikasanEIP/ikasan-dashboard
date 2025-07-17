package org.ikasan.job.orchestration.configuration;

import org.ikasan.spec.scheduled.job.service.SpringCloudConfigRefreshService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JobContextParamsSetupFactory.class)
@TestPropertySource(
    properties = """
            job.context.mapping.config.repo.environment=one,two,three
        """)
public class JobContextParamsSetupFactoryTest {

    @MockitoBean
    SpringCloudConfigRefreshService springCloudConfigRefreshService;

    @Autowired
    JobContextParamsSetupFactory jobContextParamsSetupFactory;

    /**
     * Due to Spring boot changes in 3.4.x @Value("#{T(java.util.Arrays).asList('${property}')}") no longer works.
     * This test is used to catch the new split implementation to formulate a list does not break functionality
     * <p>
     * job.context.mapping.config.repo.environment=one,two,three
     * <p>
     * functional test is covered:
     * SchedulerContextParametersPropertiesProviderTest.test_job_context_params_with_config_repo_refresh
     */
    @Test
    public void testJobContextMappingConfigRepoEnvironment() {

        List<String> values = (List<String>) ReflectionTestUtils.getField(jobContextParamsSetupFactory, "jobContextMappingConfigRepoEnvironment");
        System.out.println(values);
        Assert.assertEquals(values.size(), 3);
        Assert.assertEquals(values.get(0), "one");
        Assert.assertEquals(values.get(1), "two");
        Assert.assertEquals(values.get(2), "three");
    }

}
