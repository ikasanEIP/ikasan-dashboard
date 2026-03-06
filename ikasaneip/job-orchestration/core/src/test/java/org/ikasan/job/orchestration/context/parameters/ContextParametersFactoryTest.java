package org.ikasan.job.orchestration.context.parameters;

import org.ikasan.job.orchestration.context.util.SchedulerContextParametersPropertiesProvider;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class ContextParametersFactoryTest {

    @Mock
    private SchedulerContextParametersPropertiesProvider schedulerContextParametersPropertiesProvider;

    private ContextParametersFactory factory;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        factory = new ContextParametersFactory(schedulerContextParametersPropertiesProvider);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_provider() {
        new ContextParametersFactory(null);
    }

    @Test
    public void test_constructor_exception_message() {
        try {
            new ContextParametersFactory(null);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("schedulerContextParametersProvider cannot be null!", e.getMessage());
        }
    }

    @Test
    public void test_constructor_successful() {
        ContextParametersFactory testFactory = new ContextParametersFactory(schedulerContextParametersPropertiesProvider);

        Assert.assertNotNull(testFactory);
    }

    @Test
    public void test_populateContextParameters() {
        // Method does nothing currently, but should not throw exception
        factory.populateContextParameters();

        // Verify no interaction with provider
        verifyNoInteractions(schedulerContextParametersPropertiesProvider);
    }

    @Test
    public void test_getAllContextParameters() {
        List<ContextParameterInstance> expectedParameters = new ArrayList<>();
        ContextParameterInstance param1 = new ContextParameterInstanceImpl();
        param1.setName("param1");
        param1.setValue("value1");
        expectedParameters.add(param1);

        when(schedulerContextParametersPropertiesProvider.getAllContextParameters("testContext"))
            .thenReturn(expectedParameters);

        List<ContextParameterInstance> result = factory.getAllContextParameters("testContext");

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("param1", result.get(0).getName());
        verify(schedulerContextParametersPropertiesProvider, times(1)).getAllContextParameters("testContext");
    }

    @Test
    public void test_getAllContextParameters_empty_list() {
        when(schedulerContextParametersPropertiesProvider.getAllContextParameters("testContext"))
            .thenReturn(new ArrayList<>());

        List<ContextParameterInstance> result = factory.getAllContextParameters("testContext");

        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
        verify(schedulerContextParametersPropertiesProvider, times(1)).getAllContextParameters("testContext");
    }

    @Test
    public void test_getAllContextParameters_multiple_parameters() {
        List<ContextParameterInstance> expectedParameters = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ContextParameterInstance param = new ContextParameterInstanceImpl();
            param.setName("param" + i);
            param.setValue("value" + i);
            expectedParameters.add(param);
        }

        when(schedulerContextParametersPropertiesProvider.getAllContextParameters("testContext"))
            .thenReturn(expectedParameters);

        List<ContextParameterInstance> result = factory.getAllContextParameters("testContext");

        Assert.assertNotNull(result);
        Assert.assertEquals(5, result.size());
        verify(schedulerContextParametersPropertiesProvider, times(1)).getAllContextParameters("testContext");
    }

    @Test
    public void test_getAllContextParameters_null_context_name() {
        when(schedulerContextParametersPropertiesProvider.getAllContextParameters(null))
            .thenReturn(new ArrayList<>());

        List<ContextParameterInstance> result = factory.getAllContextParameters(null);

        Assert.assertNotNull(result);
        verify(schedulerContextParametersPropertiesProvider, times(1)).getAllContextParameters(null);
    }

    @Test
    public void test_getContextParameter() {
        when(schedulerContextParametersPropertiesProvider.getContextParameter("testContext", "param1"))
            .thenReturn("value1");

        String result = factory.getContextParameter("testContext", "param1");

        Assert.assertEquals("value1", result);
        verify(schedulerContextParametersPropertiesProvider, times(1))
            .getContextParameter("testContext", "param1");
    }

    @Test
    public void test_getContextParameter_not_found() {
        when(schedulerContextParametersPropertiesProvider.getContextParameter("testContext", "nonExistent"))
            .thenReturn(null);

        String result = factory.getContextParameter("testContext", "nonExistent");

        Assert.assertNull(result);
        verify(schedulerContextParametersPropertiesProvider, times(1))
            .getContextParameter("testContext", "nonExistent");
    }

    @Test
    public void test_getContextParameter_null_context_name() {
        when(schedulerContextParametersPropertiesProvider.getContextParameter(null, "param1"))
            .thenReturn(null);

        String result = factory.getContextParameter(null, "param1");

        Assert.assertNull(result);
        verify(schedulerContextParametersPropertiesProvider, times(1))
            .getContextParameter(null, "param1");
    }

    @Test
    public void test_getContextParameter_null_parameter_value() {
        when(schedulerContextParametersPropertiesProvider.getContextParameter("testContext", null))
            .thenReturn(null);

        String result = factory.getContextParameter("testContext", null);

        Assert.assertNull(result);
        verify(schedulerContextParametersPropertiesProvider, times(1))
            .getContextParameter("testContext", null);
    }

    @Test
    public void test_getContextParameter_empty_strings() {
        when(schedulerContextParametersPropertiesProvider.getContextParameter("", ""))
            .thenReturn("");

        String result = factory.getContextParameter("", "");

        Assert.assertEquals("", result);
        verify(schedulerContextParametersPropertiesProvider, times(1))
            .getContextParameter("", "");
    }

    @Test
    public void test_multiple_calls_to_getAllContextParameters() {
        List<ContextParameterInstance> params1 = new ArrayList<>();
        ContextParameterInstance param = new ContextParameterInstanceImpl();
        param.setName("param1");
        params1.add(param);

        when(schedulerContextParametersPropertiesProvider.getAllContextParameters("context1"))
            .thenReturn(params1);
        when(schedulerContextParametersPropertiesProvider.getAllContextParameters("context2"))
            .thenReturn(new ArrayList<>());

        List<ContextParameterInstance> result1 = factory.getAllContextParameters("context1");
        List<ContextParameterInstance> result2 = factory.getAllContextParameters("context2");

        Assert.assertEquals(1, result1.size());
        Assert.assertEquals(0, result2.size());
        verify(schedulerContextParametersPropertiesProvider, times(1)).getAllContextParameters("context1");
        verify(schedulerContextParametersPropertiesProvider, times(1)).getAllContextParameters("context2");
    }

    @Test
    public void test_multiple_calls_to_getContextParameter() {
        when(schedulerContextParametersPropertiesProvider.getContextParameter("context1", "param1"))
            .thenReturn("value1");
        when(schedulerContextParametersPropertiesProvider.getContextParameter("context1", "param2"))
            .thenReturn("value2");

        String result1 = factory.getContextParameter("context1", "param1");
        String result2 = factory.getContextParameter("context1", "param2");

        Assert.assertEquals("value1", result1);
        Assert.assertEquals("value2", result2);
        verify(schedulerContextParametersPropertiesProvider, times(1))
            .getContextParameter("context1", "param1");
        verify(schedulerContextParametersPropertiesProvider, times(1))
            .getContextParameter("context1", "param2");
    }

    @Test
    public void test_populateContextParameters_multiple_calls() {
        factory.populateContextParameters();
        factory.populateContextParameters();
        factory.populateContextParameters();

        // Should not interact with provider
        verifyNoInteractions(schedulerContextParametersPropertiesProvider);
    }
}
