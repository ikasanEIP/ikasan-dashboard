package org.ikasan.job.orchestration.context.util;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class SchedulerOverriderTest {

    @Test
    public void should_Initialise_Empty_Maps_If_Null_Passed_In_Constructor() {
        SchedulerOverrider config = new SchedulerOverrider(true, null, true, null);

        assertTrue((Boolean) ReflectionTestUtils.getField(config, "useSkipJobs"));
        Map<String, Boolean> jobsToSkip = (Map<String, Boolean>) ReflectionTestUtils.getField(config, "jobsToSkip");
        assertTrue(jobsToSkip.isEmpty());

        assertTrue((Boolean) ReflectionTestUtils.getField(config, "replaceContextParams"));
        Map<String, Boolean> paramsToReplace = (Map<String, Boolean>) ReflectionTestUtils.getField(config, "paramsToReplace");
        assertTrue(paramsToReplace.isEmpty());
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_false_emptyMap() {
        SchedulerOverrider config = new SchedulerOverrider(false, null, false, null);
        assertNull(config.getReplacementForContextParamName("ParamName"));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_true_emptyMap() {
        SchedulerOverrider config = new SchedulerOverrider(false, null, true, null);
        assertNull(config.getReplacementForContextParamName("ParamName"));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_true_not_in_map() {
        Map<String, String> paramsToReplace = Map.of("ParamName1", "ReplacementParam");
        SchedulerOverrider config = new SchedulerOverrider(false, null, true, paramsToReplace);
        assertNull(config.getReplacementForContextParamName("ParamName"));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_false_and_in_map() {
        Map<String, String> paramsToReplace = Map.of("ParamName", "ReplacementParam");
        SchedulerOverrider config = new SchedulerOverrider(false, null, false, paramsToReplace);
        assertNull(config.getReplacementForContextParamName("ParamName"));
    }

    @Test
    public void should_return_replacementParam_if_replaceContextParams_is_true_and_in_map() {
        Map<String, String> paramsToReplace = Map.of("ParamName", "ReplacementParam");
        SchedulerOverrider config = new SchedulerOverrider(false, null, true, paramsToReplace);
        assertEquals("ReplacementParam", config.getReplacementForContextParamName("ParamName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_false() {
        SchedulerOverrider config = new SchedulerOverrider(false, null, true, null);
        assertFalse(config.isSkipped("JobName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_emptyMap() {
        SchedulerOverrider config = new SchedulerOverrider(true, null, true, null);
        assertFalse(config.isSkipped("JobName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_not_in_map() {
        Map<String, Boolean> jobsToSkip = Map.of("JobName1", true);
        SchedulerOverrider config = new SchedulerOverrider(true, jobsToSkip, true, null);
        assertFalse(config.isSkipped("JobName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_in_map_and_false() {
        Map<String, Boolean> jobsToSkip = Map.of("JobName", false);
        SchedulerOverrider config = new SchedulerOverrider(true, jobsToSkip, true, null);
        assertFalse(config.isSkipped("JobName"));
    }

    @Test
    public void should_return_true_if_skip_jobs_is_in_map_and_true() {
        Map<String, Boolean> jobsToSkip = Map.of("JobName", true);
        SchedulerOverrider config = new SchedulerOverrider(true, jobsToSkip, true, null);
        assertTrue(config.isSkipped("JobName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_in_map_and_skipJobs_is_false() {
        Map<String, Boolean> jobsToSkip = Map.of("JobName", true);
        SchedulerOverrider config = new SchedulerOverrider(false, jobsToSkip, true, null);
        assertFalse(config.isSkipped("JobName"));
    }
}