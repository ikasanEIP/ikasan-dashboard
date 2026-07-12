package org.ikasan.job.orchestration.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.job.orchestration.core.AbstractTest;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextServiceTest extends AbstractTest {

    private ContextService contextService = new ContextService();

    @Test
    public void test_load_context() throws IOException {
        ContextTemplate context = this.contextService.getContextTemplate(loadDataFile("/data/context.json"));

        Assert.assertNotNull(context);
    }

    @Test
    public void test_load_context_instance() throws IOException {
        ContextInstance context = this.contextService.getContextInstance(loadDataFile("/data/context.json"));

        List<SchedulerJobInstance> schedulerJobInstances = context.getAllSchedulerJobInstances();

        String contextString = this.contextService.getContextInstanceString(context);

        Assert.assertNotNull(contextString);
    }

    @Test
    public void test_isValidJSON_valid() {
        String validJson = "{\"name\":\"test\",\"value\":123}";

        boolean result = contextService.isValidJSON(validJson);

        Assert.assertTrue(result);
    }

    @Test
    public void test_isValidJSON_invalid() {
        String invalidJson = "{name:test,value:123}";

        Assert.assertFalse(contextService.isValidJSON(invalidJson));
    }

    @Test
    public void test_getContextTemplateString() throws JsonProcessingException {
        ContextTemplate context = new ContextTemplateImpl();
        context.setName("testContext");

        String result = contextService.getContextTemplateString(context);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("testContext"));
    }

    @Test
    public void test_getContextInstanceString() throws JsonProcessingException {
        ContextInstance context = new ContextInstanceImpl();
        context.setName("testInstance");

        String result = contextService.getContextInstanceString(context);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("testInstance"));
    }

    @Test
    public void test_getContextInstanceString_map() throws JsonProcessingException {
        Map<String, ContextInstance> contextMap = new HashMap<>();
        ContextInstance context = new ContextInstanceImpl();
        context.setName("testInstance");
        contextMap.put("key1", context);

        String result = contextService.getContextInstanceString(contextMap);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("testInstance"));
    }

    @Test
    public void test_getSchedulerJobInstance() throws JsonProcessingException {
        SchedulerJobInstance jobInstance = new InternalEventDrivenJobInstanceImpl();
        jobInstance.setJobName("testJob");

        String result = contextService.getSchedulerJobInstance(jobInstance);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("testJob"));
    }

    @Test
    public void test_getSchedulerJobInstance_map() throws JsonProcessingException {
        Map<String, SchedulerJobInstance> jobMap = new HashMap<>();
        SchedulerJobInstance jobInstance = new InternalEventDrivenJobInstanceImpl();
        jobInstance.setJobName("testJob");
        jobMap.put("key1", jobInstance);

        String result = contextService.getSchedulerJobInstance(jobMap);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("testJob"));
    }

    @Test
    public void test_getSchedulerJobString() throws JsonProcessingException {
        SchedulerJob job = new SchedulerJobImpl();
        job.setJobName("testJob");

        String result = contextService.getSchedulerJobString(job);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("testJob"));
    }

    @Test
    public void test_getQuartzScheduleDrivenJob_and_getString() throws JsonProcessingException {
        QuartzScheduleDrivenJob job = new QuartzScheduleDrivenJobImpl();
        job.setJobName("quartzJob");

        String jsonString = contextService.getQuartzScheduleDrivenJobString(job);
        Assert.assertNotNull(jsonString);

        QuartzScheduleDrivenJob parsedJob = contextService.getQuartzScheduleDrivenJob(jsonString);
        Assert.assertNotNull(parsedJob);
        Assert.assertEquals("quartzJob", parsedJob.getJobName());
    }

    @Test
    public void test_getFileEventDrivenJob_and_getString() throws JsonProcessingException {
        FileEventDrivenJob job = new FileEventDrivenJobImpl();
        job.setJobName("fileJob");

        String jsonString = contextService.getFileEventDrivenJobString(job);
        Assert.assertNotNull(jsonString);

        FileEventDrivenJob parsedJob = contextService.getFileEventDrivenJob(jsonString);
        Assert.assertNotNull(parsedJob);
        Assert.assertEquals("fileJob", parsedJob.getJobName());
    }

    @Test
    public void test_getInternalEventDrivenJob_and_getString() throws JsonProcessingException {
        InternalEventDrivenJob job = new InternalEventDrivenJobImpl();
        job.setJobName("internalJob");

        String jsonString = contextService.getInternalEventDrivenJobString(job);
        Assert.assertNotNull(jsonString);

        InternalEventDrivenJob parsedJob = contextService.getInternalEventDrivenJob(jsonString);
        Assert.assertNotNull(parsedJob);
        Assert.assertEquals("internalJob", parsedJob.getJobName());
    }

    @Test
    public void test_getGlobalEventJob_and_getString() throws JsonProcessingException {
        GlobalEventJob job = new GlobalEventJobImpl();
        job.setJobName("globalJob");

        String jsonString = contextService.getGlobalEventJobString(job);
        Assert.assertNotNull(jsonString);

        GlobalEventJob parsedJob = contextService.getGlobalEventJob(jsonString);
        Assert.assertNotNull(parsedJob);
        Assert.assertEquals("globalJob", parsedJob.getJobName());
    }

    @Test
    public void test_getJobLock() throws JsonProcessingException {
        String jsonString = "{\"name\":\"testLock\",\"lockCount\":5}";

        JobLock parsedLock = contextService.getJobLock(jsonString);
        Assert.assertNotNull(parsedLock);
    }

    @Test
    public void test_getContextProfileRecord() throws JsonProcessingException {
        String json = "{\"contextName\":\"testContext\",\"profileName\":\"testProfile\"}";

        ContextProfileRecord record = contextService.getContextProfileRecord(json);
        Assert.assertNotNull(record);
    }

    @Test
    public void test_getEmailNotificationDetails() throws JsonProcessingException {
        String json = "{}";

        EmailNotificationDetails parsed = contextService.getEmailNotificationDetails(json);
        Assert.assertNotNull(parsed);
    }

    @Test
    public void test_getEmailNotificationContext() throws JsonProcessingException {
        String json = "{\"contextName\":\"testContext\"}";

        EmailNotificationContext context = contextService.getEmailNotificationContext(json);
        Assert.assertNotNull(context);
    }

    @Test
    public void test_getParent_contextTemplate_direct_child() {
        ContextTemplate parent = new ContextTemplateImpl();
        parent.setName("parent");

        ContextTemplate child = new ContextTemplateImpl();
        child.setName("child");

        List<ContextTemplate> children = new ArrayList<>();
        children.add(child);
        parent.setContexts(children);

        ContextTemplate result = contextService.getParent(parent, child);

        Assert.assertNotNull(result);
        Assert.assertEquals("parent", result.getName());
    }

    @Test
    public void test_getParent_contextTemplate_nested_child() {
        ContextTemplate grandparent = new ContextTemplateImpl();
        grandparent.setName("grandparent");

        ContextTemplate parent = new ContextTemplateImpl();
        parent.setName("parent");

        ContextTemplate child = new ContextTemplateImpl();
        child.setName("child");

        List<ContextTemplate> parentChildren = new ArrayList<>();
        parentChildren.add(child);
        parent.setContexts(parentChildren);

        List<ContextTemplate> grandparentChildren = new ArrayList<>();
        grandparentChildren.add(parent);
        grandparent.setContexts(grandparentChildren);

        ContextTemplate result = contextService.getParent(grandparent, child);

        Assert.assertNotNull(result);
        Assert.assertEquals("parent", result.getName());
    }

    @Test
    public void test_getParent_contextTemplate_not_found() {
        ContextTemplate parent = new ContextTemplateImpl();
        parent.setName("parent");

        ContextTemplate child1 = new ContextTemplateImpl();
        child1.setName("child1");

        ContextTemplate child2 = new ContextTemplateImpl();
        child2.setName("child2");

        List<ContextTemplate> children = new ArrayList<>();
        children.add(child1);
        parent.setContexts(children);

        ContextTemplate result = contextService.getParent(parent, child2);

        Assert.assertNull(result);
    }

    @Test
    public void test_getParent_contextInstance_direct_child() {
        ContextInstance parent = new ContextInstanceImpl();
        parent.setName("parent");

        ContextInstance child = new ContextInstanceImpl();
        child.setName("child");

        List<ContextInstance> children = new ArrayList<>();
        children.add(child);
        parent.setContexts(children);

        ContextInstance result = contextService.getParent(parent, child);

        Assert.assertNotNull(result);
        Assert.assertEquals("parent", result.getName());
    }

    @Test
    public void test_getParent_contextInstance_nested_child() {
        ContextInstance grandparent = new ContextInstanceImpl();
        grandparent.setName("grandparent");

        ContextInstance parent = new ContextInstanceImpl();
        parent.setName("parent");

        ContextInstance child = new ContextInstanceImpl();
        child.setName("child");

        List<ContextInstance> parentChildren = new ArrayList<>();
        parentChildren.add(child);
        parent.setContexts(parentChildren);

        List<ContextInstance> grandparentChildren = new ArrayList<>();
        grandparentChildren.add(parent);
        grandparent.setContexts(grandparentChildren);

        ContextInstance result = contextService.getParent(grandparent, child);

        Assert.assertNotNull(result);
        Assert.assertEquals("parent", result.getName());
    }

    @Test
    public void test_getParent_contextInstance_not_found() {
        ContextInstance parent = new ContextInstanceImpl();
        parent.setName("parent");

        ContextInstance child1 = new ContextInstanceImpl();
        child1.setName("child1");

        ContextInstance child2 = new ContextInstanceImpl();
        child2.setName("child2");

        List<ContextInstance> children = new ArrayList<>();
        children.add(child1);
        parent.setContexts(children);

        ContextInstance result = contextService.getParent(parent, child2);

        Assert.assertNull(result);
    }

    @Test
    public void test_getParent_contextTemplate_null_contexts() {
        ContextTemplate parent = new ContextTemplateImpl();
        parent.setName("parent");
        parent.setContexts(null);

        ContextTemplate child = new ContextTemplateImpl();
        child.setName("child");

        ContextTemplate result = contextService.getParent(parent, child);

        Assert.assertNull(result);
    }

    @Test
    public void test_getParent_contextInstance_null_contexts() {
        ContextInstance parent = new ContextInstanceImpl();
        parent.setName("parent");
        parent.setContexts(null);

        ContextInstance child = new ContextInstanceImpl();
        child.setName("child");

        ContextInstance result = contextService.getParent(parent, child);

        Assert.assertNull(result);
    }
}
