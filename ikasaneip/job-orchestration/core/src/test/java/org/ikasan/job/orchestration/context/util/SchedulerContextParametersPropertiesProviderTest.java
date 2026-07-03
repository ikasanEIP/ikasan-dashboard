package org.ikasan.job.orchestration.context.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.ikasan.job.orchestration.configuration.JobContextParamsSetupConfiguration;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.job.service.SpringCloudConfigRefreshService;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class SchedulerContextParametersPropertiesProviderTest {

    @Test
    public void should_Initialise_Empty_Maps_If_Null_Passed_In_Constructor() {
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration(null, null, null);
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(jobContextParamsSetupConfiguration, null);

        JobContextParamsSetupConfiguration paramConfig = (JobContextParamsSetupConfiguration) ReflectionTestUtils.getField(config, "jobContextParamsSetupConfiguration");
        assertTrue(paramConfig.getParamsToReplace().isEmpty());

        Map<String, String> spelExpressionMap = (Map<String, String>) ReflectionTestUtils.getField(config, "spelExpressionMap");
        assertTrue(spelExpressionMap.isEmpty());
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_false_emptyMap() {
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration(null, null, null);
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider( jobContextParamsSetupConfiguration, null);
        assertNull(config.getContextParameter(null, "ParamName"));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_true_emptyMap() {
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration(null, null, null);
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(jobContextParamsSetupConfiguration, null);
        assertNull(config.getContextParameter(null, "ParamName"));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_true_not_in_map() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName1", "ReplacementParam"));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration(null, null, null);
        jobContextParamsSetupConfiguration.setParamsToReplace(paramsToReplace);
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(jobContextParamsSetupConfiguration, null);
        assertNull(config.getContextParameter("Context1", "ParamName"));
    }

    @Test
    public void testNullValues_ParamContextAndName() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName", "ReplacementParam"));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration(null, null, null);
        jobContextParamsSetupConfiguration.setParamsToReplace(paramsToReplace);
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(jobContextParamsSetupConfiguration, null);
        assertNull(config.getContextParameter(null, "ParamName"));
        assertNull(config.getContextParameter("Context1", null));
    }

    @Test
    public void should_return_replacementParam_if_replaceContextParams_is_true_and_in_map() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName", "ReplacementParam"));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration(null, null, null);
        jobContextParamsSetupConfiguration.setParamsToReplace(paramsToReplace);
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(  jobContextParamsSetupConfiguration, null);
        assertEquals("ReplacementParam", config.getContextParameter("Context1", "ParamName"));
    }

    @Test
    public void should_return_null_if_replaceContextParams_is_true_and_unkown_context_in_map() {
        Map<String, Map<String, String>> paramsToReplace = Map.of("Context1", Map.of("ParamName", "ReplacementParam"));
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration(null, null, null);
        jobContextParamsSetupConfiguration.setParamsToReplace(paramsToReplace);
        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider( jobContextParamsSetupConfiguration, null);
        assertNull(config.getContextParameter("ContextUnkown", "ParamName"));
    }


    @Test
    public void should_return_spel_expression_value_if_set() {
        Map<String, Map<String, String>> params = Map.of("Context1", Map.of("ParamName11", "SomeCalculator", "ParamName12", "AnotherCalculator", "ParamName13", "Param13"));
        Map<String, String> spelExpressionsMap = Map.of(
            "SomeCalculator", "T(java.time.LocalDate).of(2022, 7, 31).format(T(java.time.format.DateTimeFormatter).BASIC_ISO_DATE)",
            "AnotherCalculator", "T(java.time.LocalDate).of(2022, 7, 31).plusDays(1).format(T(java.time.format.DateTimeFormatter).BASIC_ISO_DATE)"
        );
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration(null, null, null);
        jobContextParamsSetupConfiguration.setParamsToReplace(params);

        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider( jobContextParamsSetupConfiguration, spelExpressionsMap);
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

        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration(null, null, null);
        jobContextParamsSetupConfiguration.setParamsToReplace(params);

        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(  jobContextParamsSetupConfiguration, null);

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

    @Test
    public void test_job_context_params_with_config_repo_refresh()  {
        Map<String, Map<String, String>> params = Map.of("Context1", Map.of("ParamName11", "Param11"),
            "Context2", Map.of("ParamName21", "Param21", "ParamName22", "Param22"));

        // Setup a dummy JobContextParamsSetupConfiguration with properties to refresh config repo.
        JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration = new JobContextParamsSetupConfiguration(new SpringCloudConfigRefreshService() {
            @Override
            public void refreshConfigRepo(String contextUrl, String applicationPattern) {
                //Do Nothing
            }
            @Override
            public String decrypt(String contextUrl, String encryptedValue) {
                return encryptedValue;
            }

            @Override
            public String encrypt(String valueToEncrypt) {
                return valueToEncrypt;
            }

            @Override
            public void actuatorRefresh() {
                //Do Nothing
            }

            @Override
            public void actuatorRefreshAtUrl(String baseUrl) {
                //Do Nothing
            }
        },
            Arrays.asList("configRepo1", "configRepo2"), "http://someurl.com");
        jobContextParamsSetupConfiguration.setLocation(null); // force a config repo to dummy refresh
        jobContextParamsSetupConfiguration.setParamsToReplace(params);

        SchedulerContextParametersPropertiesProvider config = new SchedulerContextParametersPropertiesProvider(  jobContextParamsSetupConfiguration, null);

        assertEquals(0, config.getAllContextParameters(null).size());
        assertEquals(0, config.getAllContextParameters("UnknownContext").size());

        List<ContextParameterInstance> context1 = config.getAllContextParameters("Context1");
        assertEquals(1, context1.size());
        context1.sort(Comparator.comparing(ContextParameter::getName));
        assertEquals("ParamName11", context1.getFirst().getName());
        assertEquals("Param11", context1.getFirst().getValue());

        List<ContextParameterInstance> context2 = config.getAllContextParameters("Context2");
        assertEquals(2, context2.size());
        context2.sort(Comparator.comparing(ContextParameter::getName));
        assertEquals("ParamName21", context2.get(0).getName());
        assertEquals("Param21", context2.get(0).getValue());
        assertEquals("ParamName22", context2.get(1).getName());
        assertEquals("Param22", context2.get(1).getValue());
    }
}