package org.ikasan.job.orchestration.context.util;

import static org.junit.Assert.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
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
        assertNull(config.getReplacementForContextParamName(null, "ParamName"));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_true_emptyMap() {
        SchedulerOverrider config = new SchedulerOverrider(false, null, true, null);
        assertNull(config.getReplacementForContextParamName(null, "ParamName"));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_true_not_in_map() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName1", "ReplacementParam"));
        SchedulerOverrider config = new SchedulerOverrider(false, null, true, paramsToReplace);
        assertNull(config.getReplacementForContextParamName("Context1", "ParamName"));
    }

    @Test
    public void testNullValues_ParamContextAndName() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName", "ReplacementParam"));
        SchedulerOverrider config = new SchedulerOverrider(false, null, true, paramsToReplace);
        assertNull(config.getReplacementForContextParamName(null, "ParamName"));
        assertNull(config.getReplacementForContextParamName("Context1", null));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_false_and_in_map() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName", "ReplacementParam"));
        SchedulerOverrider config = new SchedulerOverrider(false, null, false, paramsToReplace);
        assertNull(config.getReplacementForContextParamName("Context1", "ParamName"));
    }

    @Test
    public void should_return_replacementParam_if_replaceContextParams_is_true_and_in_map() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName", "ReplacementParam"));
        SchedulerOverrider config = new SchedulerOverrider(false, null, true, paramsToReplace);
        assertEquals("ReplacementParam", config.getReplacementForContextParamName("Context1", "ParamName"));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_true_and_unkown_context_in_map() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName", "ReplacementParam"));
        SchedulerOverrider config = new SchedulerOverrider(false, null, true, paramsToReplace);
        assertNull(config.getReplacementForContextParamName("ContextUnkown", "ParamName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_false() {
        SchedulerOverrider config = new SchedulerOverrider(false, null, true, null);
        assertFalse(config.isSkipped(null, "JobName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_emptyMap() {
        SchedulerOverrider config = new SchedulerOverrider(true, null, true, null);
        assertFalse(config.isSkipped(null, "JobName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_not_in_map() {
        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("JobName1", true));
        SchedulerOverrider config = new SchedulerOverrider(true, jobsToSkip, true, null);
        assertFalse(config.isSkipped("Context1", "JobName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_in_map_and_false() {
        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("JobName", false));
        SchedulerOverrider config = new SchedulerOverrider(true, jobsToSkip, true, null);
        assertFalse(config.isSkipped("Context1", "JobName"));
    }

    @Test
    public void should_return_true_if_skip_jobs_is_in_map_and_true() {
        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("JobName", true));
        SchedulerOverrider config = new SchedulerOverrider(true, jobsToSkip, true, null);
        assertTrue(config.isSkipped("Context1", "JobName"));
    }

    @Test
    public void testNullValues_contextNameJobToSKip() {
        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("JobName", true));
        SchedulerOverrider config = new SchedulerOverrider(true, jobsToSkip, true, null);
        assertFalse(config.isSkipped(null, "JobName"));
        assertFalse(config.isSkipped("Context1", null));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_in_map_and_skipJobs_is_false() {
        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("JobName", true));
        SchedulerOverrider config = new SchedulerOverrider(false, jobsToSkip, true, null);
        assertFalse(config.isSkipped("Context1", "JobName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_in_map_and_contextUnknown_is_false() {
        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("JobName", true));
        SchedulerOverrider config = new SchedulerOverrider(true, jobsToSkip, true, null);
        assertFalse(config.isSkipped("ContextUnkown", "JobName"));
    }

    @Test
    public void getAllParams()  {
        Map<String, Map<String, String>> params = Map.of("Context1", Map.of("ParamName11", "Param11", "ParamName12", "Param12", "ParamName13", "Param13"),
            "Context2", Map.of("ParamName21", "Param21", "ParamName22", "Param22", "ParamName23", "Param23"));

        SchedulerOverrider config = new SchedulerOverrider(false, null, true, params);

        assertEquals(0, config.getAllContextParameters(null).size());
        assertEquals(0, config.getAllContextParameters("UnknownContext").size());

        List<ContextParameterInstance> context1 = config.getAllContextParameters("Context1");
        assertEquals(3, context1.size());
        context1.sort(Comparator.comparing(ContextParameter::getName));
        assertEquals("ParamName11", context1.get(0).getName());
        assertEquals("Param11", context1.get(0).getValue());
        assertEquals("java.lang.String", context1.get(0).getType());
        assertEquals("ParamName12", context1.get(1).getName());
        assertEquals("Param12", context1.get(1).getValue());
        assertEquals("java.lang.String", context1.get(1).getType());
        assertEquals("ParamName13", context1.get(2).getName());
        assertEquals("Param13", context1.get(2).getValue());
        assertEquals("java.lang.String", context1.get(2).getType());

        List<ContextParameterInstance> context2 = config.getAllContextParameters("Context2");
        assertEquals(3, context2.size());
        context2.sort(Comparator.comparing(ContextParameter::getName));
        assertEquals("ParamName21", context2.get(0).getName());
        assertEquals("Param21", context2.get(0).getValue());
        assertEquals("java.lang.String", context2.get(0).getType());
        assertEquals("ParamName22", context2.get(1).getName());
        assertEquals("Param22", context2.get(1).getValue());
        assertEquals("java.lang.String", context2.get(1).getType());
        assertEquals("ParamName23", context2.get(2).getName());
        assertEquals("Param23", context2.get(2).getValue());
        assertEquals("java.lang.String", context2.get(2).getType());

    }
}