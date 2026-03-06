package org.ikasan.job.orchestration.context.parameters;

import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class ContextParametersInstanceServiceImplTest {

    @Mock
    private ContextParametersFactory contextParametersFactory;

    private ContextParametersInstanceServiceImpl service;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        service = new ContextParametersInstanceServiceImpl(contextParametersFactory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_constructor_null_factory() {
        new ContextParametersInstanceServiceImpl(null);
    }

    @Test
    public void test_constructor_exception_message() {
        try {
            new ContextParametersInstanceServiceImpl(null);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("contextParametersFactory cannot be null!", e.getMessage());
        }
    }

    @Test
    public void test_populateContextParameters() {
        service.populateContextParameters();

        verify(contextParametersFactory, times(1)).populateContextParameters();
    }

    @Test
    public void test_getContextParameterValue() {
        when(contextParametersFactory.getContextParameter("context1", "param1"))
            .thenReturn("value1");

        String result = service.getContextParameterValue("context1", "param1");

        Assert.assertEquals("value1", result);
        verify(contextParametersFactory, times(1)).getContextParameter("context1", "param1");
    }

    @Test
    public void test_getAllContextParameters() {
        List<ContextParameterInstance> expectedParams = new ArrayList<>();
        ContextParameterInstance param = new ContextParameterInstanceImpl();
        param.setName("param1");
        expectedParams.add(param);

        when(contextParametersFactory.getAllContextParameters("context1"))
            .thenReturn(expectedParams);

        List<ContextParameterInstance> result = service.getAllContextParameters("context1");

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        verify(contextParametersFactory, times(1)).getAllContextParameters("context1");
    }

    @Test
    public void test_populateContextParametersOnContextInstance_with_property_backed_only() {
        ContextInstance contextInstance = new ContextInstanceImpl();
        contextInstance.setName("testContext");
        contextInstance.setContextParameters(new ArrayList<>());

        Map<String, InternalEventDrivenJobInstance> internalJobs = new HashMap<>();

        List<ContextParameterInstance> propertyBackedParams = new ArrayList<>();
        ContextParameterInstance param1 = new ContextParameterInstanceImpl();
        param1.setName("param1");
        param1.setValue("propertyValue1");
        propertyBackedParams.add(param1);

        when(contextParametersFactory.getAllContextParameters("testContext"))
            .thenReturn(propertyBackedParams);

        service.populateContextParametersOnContextInstance(contextInstance, internalJobs);

        Assert.assertEquals(1, contextInstance.getContextParameters().size());
        Assert.assertEquals("param1", contextInstance.getContextParameters().get(0).getName());
        Assert.assertEquals("propertyValue1", contextInstance.getContextParameters().get(0).getValue());
    }

    @Test
    public void test_populateContextParametersOnContextInstance_with_default_parameters() {
        ContextInstance contextInstance = new ContextInstanceImpl();
        contextInstance.setName("testContext");

        List<ContextParameterInstance> defaultParams = new ArrayList<>();
        ContextParameterInstance param1 = new ContextParameterInstanceImpl();
        param1.setName("param1");
        param1.setDefaultValue("defaultValue1");
        defaultParams.add(param1);
        contextInstance.setContextParameters(defaultParams);

        Map<String, InternalEventDrivenJobInstance> internalJobs = new HashMap<>();

        when(contextParametersFactory.getAllContextParameters("testContext"))
            .thenReturn(new ArrayList<>());

        service.populateContextParametersOnContextInstance(contextInstance, internalJobs);

        Assert.assertEquals(1, contextInstance.getContextParameters().size());
        Assert.assertEquals("param1", contextInstance.getContextParameters().get(0).getName());
        Assert.assertEquals("defaultValue1", contextInstance.getContextParameters().get(0).getValue());
    }

    @Test
    public void test_populateContextParametersOnContextInstance_property_overrides_default() {
        ContextInstance contextInstance = new ContextInstanceImpl();
        contextInstance.setName("testContext");

        List<ContextParameterInstance> defaultParams = new ArrayList<>();
        ContextParameterInstance defaultParam = new ContextParameterInstanceImpl();
        defaultParam.setName("param1");
        defaultParam.setDefaultValue("defaultValue1");
        defaultParams.add(defaultParam);
        contextInstance.setContextParameters(defaultParams);

        Map<String, InternalEventDrivenJobInstance> internalJobs = new HashMap<>();

        List<ContextParameterInstance> propertyBackedParams = new ArrayList<>();
        ContextParameterInstance propertyParam = new ContextParameterInstanceImpl();
        propertyParam.setName("param1");
        propertyParam.setValue("propertyValue1");
        propertyBackedParams.add(propertyParam);

        when(contextParametersFactory.getAllContextParameters("testContext"))
            .thenReturn(propertyBackedParams);

        service.populateContextParametersOnContextInstance(contextInstance, internalJobs);

        Assert.assertEquals(1, contextInstance.getContextParameters().size());
        Assert.assertEquals("param1", contextInstance.getContextParameters().get(0).getName());
        Assert.assertEquals("propertyValue1", contextInstance.getContextParameters().get(0).getValue());
    }

    @Test
    public void test_populateContextParametersOnContextInstance_with_job_parameters() {
        ContextInstance contextInstance = new ContextInstanceImpl();
        contextInstance.setName("testContext");
        contextInstance.setContextParameters(new ArrayList<>());

        Map<String, InternalEventDrivenJobInstance> internalJobs = new HashMap<>();
        InternalEventDrivenJobInstance job = new InternalEventDrivenJobInstanceImpl();
        job.setJobName("job1");

        List<ContextParameter> jobParams = new ArrayList<>();
        ContextParameterImpl jobParam = new ContextParameterImpl();
        jobParam.setName("jobParam1");
        jobParam.setDefaultValue("jobDefaultValue1");
        jobParams.add(jobParam);
        job.setContextParameters(jobParams);

        internalJobs.put("job1", job);

        when(contextParametersFactory.getAllContextParameters("testContext"))
            .thenReturn(new ArrayList<>());

        service.populateContextParametersOnContextInstance(contextInstance, internalJobs);

        // Find the job parameter in the result
        boolean foundJobParam = contextInstance.getContextParameters().stream()
            .anyMatch(p -> "jobParam1".equals(p.getName()));
        Assert.assertTrue("Job parameter should be included", foundJobParam);
    }

    @Test
    public void test_getContextParameterInstancesForContext_with_property_backed() {
        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName("testContext");
        contextTemplate.setContextParameters(new ArrayList<>());

        Map<String, InternalEventDrivenJob> internalJobs = new HashMap<>();

        List<ContextParameterInstance> propertyBackedParams = new ArrayList<>();
        ContextParameterInstance param1 = new ContextParameterInstanceImpl();
        param1.setName("param1");
        param1.setValue("propertyValue1");
        propertyBackedParams.add(param1);

        when(contextParametersFactory.getAllContextParameters("testContext"))
            .thenReturn(propertyBackedParams);

        List<ContextParameterInstance> result = service.getContextParameterInstancesForContext(contextTemplate, internalJobs);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("param1", result.get(0).getName());
        Assert.assertEquals("propertyValue1", result.get(0).getValue());
    }

    @Test
    public void test_getContextParameterInstancesForContext_with_default_parameters() {
        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName("testContext");

        List<ContextParameter> defaultParams = new ArrayList<>();
        ContextParameter param1 = new ContextParameterImpl();
        param1.setName("param1");
        param1.setDefaultValue("defaultValue1");
        defaultParams.add(param1);
        contextTemplate.setContextParameters(defaultParams);

        Map<String, InternalEventDrivenJob> internalJobs = new HashMap<>();

        when(contextParametersFactory.getAllContextParameters("testContext"))
            .thenReturn(new ArrayList<>());

        List<ContextParameterInstance> result = service.getContextParameterInstancesForContext(contextTemplate, internalJobs);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("param1", result.get(0).getName());
        Assert.assertEquals("defaultValue1", result.get(0).getValue());
    }

    @Test
    public void test_getContextParameterInstancesForContext_property_overrides_default() {
        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName("testContext");

        List<ContextParameter> defaultParams = new ArrayList<>();
        ContextParameter defaultParam = new ContextParameterImpl();
        defaultParam.setName("param1");
        defaultParam.setDefaultValue("defaultValue1");
        defaultParams.add(defaultParam);
        contextTemplate.setContextParameters(defaultParams);

        Map<String, InternalEventDrivenJob> internalJobs = new HashMap<>();

        List<ContextParameterInstance> propertyBackedParams = new ArrayList<>();
        ContextParameterInstance propertyParam = new ContextParameterInstanceImpl();
        propertyParam.setName("param1");
        propertyParam.setValue("propertyValue1");
        propertyBackedParams.add(propertyParam);

        when(contextParametersFactory.getAllContextParameters("testContext"))
            .thenReturn(propertyBackedParams);

        List<ContextParameterInstance> result = service.getContextParameterInstancesForContext(contextTemplate, internalJobs);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("param1", result.get(0).getName());
        Assert.assertEquals("propertyValue1", result.get(0).getValue());
    }

    @Test
    public void test_getContextParameterInstancesForContext_with_job_parameters() {
        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName("testContext");
        contextTemplate.setContextParameters(new ArrayList<>());

        Map<String, InternalEventDrivenJob> internalJobs = new HashMap<>();
        InternalEventDrivenJob job = new InternalEventDrivenJobImpl();
        job.setJobName("job1");

        List<ContextParameter> jobParams = new ArrayList<>();
        ContextParameterImpl jobParam = new ContextParameterImpl();
        jobParam.setName("jobParam1");
        jobParam.setDefaultValue("jobDefaultValue1");
        jobParams.add(jobParam);
        job.setContextParameters(jobParams);

        internalJobs.put("job1", job);

        when(contextParametersFactory.getAllContextParameters("testContext"))
            .thenReturn(new ArrayList<>());

        List<ContextParameterInstance> result = service.getContextParameterInstancesForContext(contextTemplate, internalJobs);

        // Find the job parameter in the result
        boolean foundJobParam = result.stream()
            .anyMatch(p -> "jobParam1".equals(p.getName()));
        Assert.assertTrue("Job parameter should be included", foundJobParam);
    }

    @Test
    public void test_getContextParameterInstancesForContext_multiple_parameters() {
        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName("testContext");

        List<ContextParameter> defaultParams = new ArrayList<>();
        ContextParameter param1 = new ContextParameterImpl();
        param1.setName("param1");
        param1.setDefaultValue("default1");
        defaultParams.add(param1);

        ContextParameter param2 = new ContextParameterImpl();
        param2.setName("param2");
        param2.setDefaultValue("default2");
        defaultParams.add(param2);

        contextTemplate.setContextParameters(defaultParams);

        Map<String, InternalEventDrivenJob> internalJobs = new HashMap<>();

        List<ContextParameterInstance> propertyBackedParams = new ArrayList<>();
        ContextParameterInstance propertyParam = new ContextParameterInstanceImpl();
        propertyParam.setName("param1");
        propertyParam.setValue("property1");
        propertyBackedParams.add(propertyParam);

        when(contextParametersFactory.getAllContextParameters("testContext"))
            .thenReturn(propertyBackedParams);

        List<ContextParameterInstance> result = service.getContextParameterInstancesForContext(contextTemplate, internalJobs);

        Assert.assertEquals(2, result.size());

        // param1 should have property value
        ContextParameterInstance resultParam1 = result.stream()
            .filter(p -> "param1".equals(p.getName()))
            .findFirst()
            .orElse(null);
        Assert.assertNotNull(resultParam1);
        Assert.assertEquals("property1", resultParam1.getValue());

        // param2 should have default value
        ContextParameterInstance resultParam2 = result.stream()
            .filter(p -> "param2".equals(p.getName()))
            .findFirst()
            .orElse(null);
        Assert.assertNotNull(resultParam2);
        Assert.assertEquals("default2", resultParam2.getValue());
    }

    @Test
    public void test_populateContextParametersOnContextInstance_empty_jobs() {
        ContextInstance contextInstance = new ContextInstanceImpl();
        contextInstance.setName("testContext");
        contextInstance.setContextParameters(new ArrayList<>());

        Map<String, InternalEventDrivenJobInstance> internalJobs = new HashMap<>();

        when(contextParametersFactory.getAllContextParameters("testContext"))
            .thenReturn(new ArrayList<>());

        service.populateContextParametersOnContextInstance(contextInstance, internalJobs);

        Assert.assertEquals(0, contextInstance.getContextParameters().size());
    }

    @Test
    public void test_getContextParameterInstancesForContext_empty_jobs() {
        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName("testContext");
        contextTemplate.setContextParameters(new ArrayList<>());

        Map<String, InternalEventDrivenJob> internalJobs = new HashMap<>();

        when(contextParametersFactory.getAllContextParameters("testContext"))
            .thenReturn(new ArrayList<>());

        List<ContextParameterInstance> result = service.getContextParameterInstancesForContext(contextTemplate, internalJobs);

        Assert.assertEquals(0, result.size());
    }
}
