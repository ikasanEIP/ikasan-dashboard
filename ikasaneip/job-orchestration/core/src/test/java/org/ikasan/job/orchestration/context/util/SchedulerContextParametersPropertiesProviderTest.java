package org.ikasan.job.orchestration.context.util;

import static org.junit.Assert.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.ikasan.job.orchestration.configuration.JobContextParamsSetupConfiguration;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class SchedulerContextParametersPropertiesProviderTest {

    @Test
    public void should_Initialise_Empty_Maps_If_Null_Passed_In_Constructor() {
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(true, null, true, jobContextParamsSetupConfiguration, null);

        assertTrue((Boolean) ReflectionTestUtils.getField(config, "useSkipJobs"));
        Map<String, Boolean> jobsToSkip = (Map<String, Boolean>) ReflectionTestUtils.getField(config, "jobsToSkip");
        assertTrue(jobsToSkip.isEmpty());

        assertTrue((Boolean) ReflectionTestUtils.getField(config, "replaceContextParameters"));
        JobContextParamsSetupConfiguration paramConfig = (JobContextParamsSetupConfiguration) ReflectionTestUtils.getField(config, "jobContextParamsSetupConfiguration");
        assertTrue(paramConfig.getParamsToReplace().isEmpty());

        Map<String, String> spelExpressionMap = (Map<String, String>) ReflectionTestUtils.getField(config, "spelExpressionMap");
        assertTrue(spelExpressionMap.isEmpty());
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_false_emptyMap() {
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(false, null, false, jobContextParamsSetupConfiguration, null);
        assertNull(config.getContextParameter(null, "ParamName"));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_true_emptyMap() {
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(false, null, true, jobContextParamsSetupConfiguration, null);
        assertNull(config.getContextParameter(null, "ParamName"));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_true_not_in_map() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName1", "ReplacementParam"));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        jobContextParamsSetupConfiguration.setParamsToReplace(paramsToReplace);
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(false, null, true, jobContextParamsSetupConfiguration, null);
        assertNull(config.getContextParameter("Context1", "ParamName"));
    }

    @Test
    public void testNullValues_ParamContextAndName() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName", "ReplacementParam"));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        jobContextParamsSetupConfiguration.setParamsToReplace(paramsToReplace);
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(false, null, true, jobContextParamsSetupConfiguration, null);
        assertNull(config.getContextParameter(null, "ParamName"));
        assertNull(config.getContextParameter("Context1", null));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_false_and_in_map() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName", "ReplacementParam"));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        jobContextParamsSetupConfiguration.setParamsToReplace(paramsToReplace);
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(false, null, false, jobContextParamsSetupConfiguration, null);
        assertNull(config.getContextParameter("Context1", "ParamName"));
    }

    @Test
    public void should_return_replacementParam_if_replaceContextParams_is_true_and_in_map() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName", "ReplacementParam"));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        jobContextParamsSetupConfiguration.setParamsToReplace(paramsToReplace);
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(false, null, true, jobContextParamsSetupConfiguration, null);
        assertEquals("ReplacementParam", config.getContextParameter("Context1", "ParamName"));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_true_and_unkown_context_in_map() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName", "ReplacementParam"));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        jobContextParamsSetupConfiguration.setParamsToReplace(paramsToReplace);
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(false, null, true, jobContextParamsSetupConfiguration, null);
        assertNull(config.getContextParameter("ContextUnkown", "ParamName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_false() {
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(false, null, true, jobContextParamsSetupConfiguration, null);
        assertFalse(config.isSkipped(null, "JobName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_emptyMap() {
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(true, null, true, jobContextParamsSetupConfiguration, null);
        assertFalse(config.isSkipped(null, "JobName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_not_in_map() {
        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("JobName1", true));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(true, jobsToSkip, true, jobContextParamsSetupConfiguration, null);
        assertFalse(config.isSkipped("Context1", "JobName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_in_map_and_false() {
        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("JobName", false));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(true, jobsToSkip, true, jobContextParamsSetupConfiguration, null);
        assertFalse(config.isSkipped("Context1", "JobName"));
    }

    @Test
    public void should_return_true_if_skip_jobs_is_in_map_and_true() {
        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("JobName", true));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(true, jobsToSkip, true, jobContextParamsSetupConfiguration, null);
        assertTrue(config.isSkipped("Context1", "JobName"));
    }

    @Test
    public void testNullValues_contextNameJobToSKip() {
        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("JobName", true));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(true, jobsToSkip, true, jobContextParamsSetupConfiguration, null);
        assertFalse(config.isSkipped(null, "JobName"));
        assertFalse(config.isSkipped("Context1", null));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_in_map_and_skipJobs_is_false() {
        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("JobName", true));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(false, jobsToSkip, true, jobContextParamsSetupConfiguration, null);
        assertFalse(config.isSkipped("Context1", "JobName"));
    }

    @Test
    public void should_return_false_if_skip_jobs_is_in_map_and_contextUnknown_is_false() {
        Map<String, Map<String, Boolean>> jobsToSkip = Map.of("Context1", Map.of("JobName", true));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(true, jobsToSkip, true, jobContextParamsSetupConfiguration, null);
        assertFalse(config.isSkipped("ContextUnknown", "JobName"));
    }

    @Test
    public void should_return_spel_expression_value_if_set() {
        Map<String, Map<String, String>> params = Map.of("Context1", Map.of("ParamName11", "SomeCalculator", "ParamName12", "AnotherCalculator", "ParamName13", "Param13"));
        Map<String, String> spelExpressionsMap = Map.of(
            "SomeCalculator", "T(java.time.LocalDate).of(2022, 7, 31).format(T(java.time.format.DateTimeFormatter).BASIC_ISO_DATE)",
            "AnotherCalculator", "T(java.time.LocalDate).of(2022, 7, 31).plusDays(1).format(T(java.time.format.DateTimeFormatter).BASIC_ISO_DATE)"
        );
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        jobContextParamsSetupConfiguration.setParamsToReplace(params);

        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(false, null, true, jobContextParamsSetupConfiguration, spelExpressionsMap);
        List<ContextParameterInstance> context1 = config.getAllContextParameters("Context1");
        assertEquals(3, context1.size());
        context1.sort(Comparator.comparing(ContextParameter::getName));
        assertEquals("ParamName11", context1.get(0).getName());
        assertEquals("20220731", context1.get(0).getValue());
        assertEquals("ParamName12", context1.get(1).getName());
        assertEquals("20220801", context1.get(1).getValue());
        assertEquals("ParamName13", context1.get(2).getName());
        assertEquals("Param13", context1.get(2).getValue());

        assertEquals("20220731", config.getContextParameter("Context1", "ParamName11"));
        assertEquals("20220801", config.getContextParameter("Context1", "ParamName12"));
        assertEquals("Param13", config.getContextParameter("Context1", "ParamName13"));
    }

    @Test
    public void getAllParams()  {
        Map<String, Map<String, String>> params = Map.of("Context1", Map.of("ParamName11", "Param11", "ParamName12", "Param12", "ParamName13", "Param13"),
            "Context2", Map.of("ParamName21", "Param21", "ParamName22", "Param22", "ParamName23", "Param23"));

        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration();
        jobContextParamsSetupConfiguration.setParamsToReplace(params);

        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(false, null, true, jobContextParamsSetupConfiguration, null);

        assertEquals(0, config.getAllContextParameters(null).size());
        assertEquals(0, config.getAllContextParameters("UnknownContext").size());

        List<ContextParameterInstance> context1 = config.getAllContextParameters("Context1");
        assertEquals(3, context1.size());
        context1.sort(Comparator.comparing(ContextParameter::getName));
        assertEquals("ParamName11", context1.get(0).getName());
        assertEquals("Param11", context1.get(0).getValue());
        assertEquals("ParamName12", context1.get(1).getName());
        assertEquals("Param12", context1.get(1).getValue());
        assertEquals("ParamName13", context1.get(2).getName());
        assertEquals("Param13", context1.get(2).getValue());

        List<ContextParameterInstance> context2 = config.getAllContextParameters("Context2");
        assertEquals(3, context2.size());
        context2.sort(Comparator.comparing(ContextParameter::getName));
        assertEquals("ParamName21", context2.get(0).getName());
        assertEquals("Param21", context2.get(0).getValue());
        assertEquals("ParamName22", context2.get(1).getName());
        assertEquals("Param22", context2.get(1).getValue());
        assertEquals("ParamName23", context2.get(2).getName());
        assertEquals("Param23", context2.get(2).getValue());
    }
}