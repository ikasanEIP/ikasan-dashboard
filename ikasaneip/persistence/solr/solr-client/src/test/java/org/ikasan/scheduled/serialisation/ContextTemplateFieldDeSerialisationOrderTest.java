package org.ikasan.scheduled.serialisation;

import org.apache.commons.io.IOUtils;
import org.ikasan.job.orchestration.model.context.*;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobLockParticipantImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.context.model.*;
import org.ikasan.scheduled.job.model.SolrSchedulerJobImpl;
import org.ikasan.scheduled.job.model.SolrSchedulerJobLockParticipantImpl;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextTemplateFieldDeSerialisationOrderTest {

    ContextService contextService = new ContextService();

    @Test
    public void test_context_template_sorted_base() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/context-sorted-base.json"));

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/context-sorted-base-result.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_context_template_add_child_contexts() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/context-sorted-base.json"));

        ContextTemplate zChild = new SolrContextTemplateImpl();
        zChild.setName("Z-CHILD");

        contextTemplate.getContexts().add(zChild);

        ContextTemplate aChild = new SolrContextTemplateImpl();
        aChild.setName("A-CHILD");

        contextTemplate.getContexts().add(aChild);

        // Context templates are sorted by name so the 2 contexts above should
        // bookend the resulting de-serialised context template
        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/context-sorted-with-added-child-contexts.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_context_template_add_scheduler_jobs() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/context-sorted-base.json"));

        ContextTemplate child = ContextHelper.getChildContextTemplate("CONTEXT-176164463", contextTemplate);

        SchedulerJob schedulerJob1 = new SolrSchedulerJobImpl();
        schedulerJob1.setJobName("zzzz-job-name");
        schedulerJob1.setAgentName("agent");
        schedulerJob1.setIdentifier("agent-zzzz-job-name");

        SchedulerJob schedulerJob2 = new SolrSchedulerJobImpl();
        schedulerJob2.setJobName("aaaa-job-name");
        schedulerJob2.setAgentName("agent");
        schedulerJob2.setIdentifier("agent-aaaa-job-name");

        child.getScheduledJobs().add(schedulerJob1);
        child.getScheduledJobs().add(schedulerJob2);

        // Context templates are sorted by name so the 2 contexts above should
        // bookend the resulting de-serialised context template
        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/context-sorted-with-added-scheduler-jobs.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_context_template_add_scheduler_jobs_and_job_dependency() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/context-sorted-base.json"));

        ContextTemplate child = ContextHelper.getChildContextTemplate("CONTEXT-176164463", contextTemplate);

        SchedulerJob schedulerJob1 = new SolrSchedulerJobImpl();
        schedulerJob1.setJobName("zzzz-job-name");
        schedulerJob1.setAgentName("agent");
        schedulerJob1.setIdentifier("agent-zzzz-job-name");

        SchedulerJob schedulerJob2 = new SolrSchedulerJobImpl();
        schedulerJob2.setJobName("aaaa-job-name");
        schedulerJob2.setAgentName("agent");
        schedulerJob2.setIdentifier("agent-aaaa-job-name");

        SchedulerJob schedulerJob3 = new SolrSchedulerJobImpl();
        schedulerJob3.setJobName("xxxx-job-name");
        schedulerJob3.setAgentName("agent");
        schedulerJob3.setIdentifier("agent-xxxx-job-name");

        child.getScheduledJobs().add(schedulerJob1);
        child.getScheduledJobs().add(schedulerJob2);
        child.getScheduledJobs().add(schedulerJob3);

        child.getJobDependencies().forEach(jobDependency -> {
            if (jobDependency.getLogicalGrouping() != null && jobDependency.getLogicalGrouping().getAnd() != null) {
                And and = new SolrAndImpl();
                and.setIdentifier("agent-zzzz-job-name");
                jobDependency.getLogicalGrouping().getAnd().add(and);
                and = new SolrAndImpl();
                and.setIdentifier("agent-aaaa-job-name");
                jobDependency.getLogicalGrouping().getAnd().add(and);
            }
        });

        JobDependency jobDependency = new SolrJobDependencyImpl();
        jobDependency.setJobIdentifier("scheduler-agent-176164463_ScheduledJob_10:30:00");
        And and = new SolrAndImpl();
        and.setIdentifier("agent-zzzz-job-name");
        LogicalGrouping logicalGrouping = new LogicalGroupingImpl();
        List<And> andList = new ArrayList<>();
        andList.add(and);
        logicalGrouping.setAnd(andList);
        Or or = new OrImpl();
        or.setIdentifier("agent-xxxx-job-name");
        List<Or> orList = new ArrayList<>();
        orList.add(or);
        logicalGrouping.setOr(orList);
        Not not = new NotImpl();
        not.setIdentifier("agent-xxxx-job-name");
        List<Not> notList = new ArrayList<>();
        notList.add(not);
        logicalGrouping.setNot(notList);
        jobDependency.setLogicalGrouping(logicalGrouping);

        child.getJobDependencies().add(jobDependency);

        jobDependency = new SolrJobDependencyImpl();
        jobDependency.setJobIdentifier("scheduler-agent-176164463_ScheduledJob_10:30:00");
        and = new SolrAndImpl();
        and.setIdentifier("agent-aaaa-job-name");
        logicalGrouping = new SolrLogicalGroupingImpl();
        andList = new ArrayList<>();
        andList.add(and);
        logicalGrouping.setAnd(andList);
        jobDependency.setLogicalGrouping(logicalGrouping);

        child.getJobDependencies().add(jobDependency);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/context-sorted-with-added-scheduler-jobs-and-job-dependencies.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_context_template_add_context_parameters() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/context-sorted-base.json"));

        ContextParameter contextParameter = new SolrContextParameterImpl();
        contextParameter.setName("aaaaa");
        contextParameter.setDefaultValue("value");

        contextTemplate.getContextParameters().add(contextParameter);

        contextParameter = new SolrContextParameterImpl();
        contextParameter.setName("ffffff");
        contextParameter.setDefaultValue("value");

        contextTemplate.getContextParameters().add(contextParameter);

        contextParameter = new SolrContextParameterImpl();
        contextParameter.setName("bbbbbb");
        contextParameter.setDefaultValue("value");

        contextTemplate.getContextParameters().add(contextParameter);

        contextParameter = new SolrContextParameterImpl();
        contextParameter.setName("nnnnnn");
        contextParameter.setDefaultValue("value");

        contextTemplate.getContextParameters().add(contextParameter);

        contextParameter = new SolrContextParameterImpl();
        contextParameter.setName("zzzzzz");
        contextParameter.setDefaultValue("value");

        contextTemplate.getContextParameters().add(contextParameter);

        contextParameter = new SolrContextParameterImpl();
        contextParameter.setName("qqqqqq");
        contextParameter.setDefaultValue("value");

        contextTemplate.getContextParameters().add(contextParameter);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/context-sorted-with-added-context-parameters.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_context_template_add_job_locks() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/context-sorted-base.json"));

        JobLock jobLock = new SolrJobLockImpl();
        jobLock.setName("%Partition%.ZZZZZZZ");
        jobLock.setLockCount(100l);
        Map<String, List<SchedulerJobLockParticipant>> jobLockJobs = new HashMap<>();
        List<SchedulerJobLockParticipant> schedulerJobLockParticipants = new ArrayList<>();
        SchedulerJobLockParticipant jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("zzzzzz");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        schedulerJobLockParticipants = new ArrayList<>();
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("qqqqqq");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("aaaaaa");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("rrrrrr");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);

        jobLockJobs.put("CONTEXT1", schedulerJobLockParticipants);

        schedulerJobLockParticipants = new ArrayList<>();
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("ttttt");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        schedulerJobLockParticipants = new ArrayList<>();
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("cccccccc");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("nnnnnnn");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("eeeeeee");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        jobLockJobs.put("CONTEXT2", schedulerJobLockParticipants);
        jobLock.setJobs(jobLockJobs);

        contextTemplate.getJobLocks().add(jobLock);

        jobLock = new JobLockImpl();
        jobLock.setName("%Partition%.AAAAA");
        jobLock.setLockCount(100l);
        jobLockJobs = new HashMap<>();
        schedulerJobLockParticipants = new ArrayList<>();
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("aaaa");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        schedulerJobLockParticipants = new ArrayList<>();
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("qqqqqq");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("aaaaaa");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("rrrrrr");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);

        jobLockJobs.put("CONTEXT1", schedulerJobLockParticipants);

        schedulerJobLockParticipants = new ArrayList<>();
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("ttttt");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        schedulerJobLockParticipants = new ArrayList<>();
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("cccccccc");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("nnnnnnn");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        jobLockParticipant = new SolrSchedulerJobLockParticipantImpl();
        jobLockParticipant.setLockCount(1);
        jobLockParticipant.setJobName("eeeeeee");
        jobLockParticipant.setAgentName("agent-name");
        jobLockParticipant.setContextName("context");
        schedulerJobLockParticipants.add(jobLockParticipant);
        jobLockJobs.put("CONTEXT2", schedulerJobLockParticipants);
        jobLock.setJobs(jobLockJobs);

        contextTemplate.getJobLocks().add(jobLock);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/context-sorted-with-additional-job-lock.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_context_template_add_blackout_window_date_time_ranges() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/context-sorted-base.json"));

        contextTemplate.getBlackoutWindowDateTimeRanges().put(999999L, 9999999L);
        contextTemplate.getBlackoutWindowDateTimeRanges().put(1L, 1L);
        contextTemplate.getBlackoutWindowDateTimeRanges().put(00L, 00L);
        contextTemplate.getBlackoutWindowDateTimeRanges().put(12345L, 12345L);
        contextTemplate.getBlackoutWindowDateTimeRanges().put(10101010101L, 10101010101L);

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/context-sorted-with-blackout-time-windows.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_context_template_add_blackout_window_crons() throws IOException, JSONException {
        ContextTemplate contextTemplate = this.contextService
            .getContextTemplate(loadDataFile("/data/context-sorted-base.json"));

        contextTemplate.getBlackoutWindowCronExpressions().add("59 0 1 * ? * *");
        contextTemplate.getBlackoutWindowCronExpressions().add("0 0 1 * ? * *");
        contextTemplate.getBlackoutWindowCronExpressions().add("* 0/1 1 * ? * *");
        contextTemplate.getBlackoutWindowCronExpressions().add("* 0/1 1 * ? * 2027");

        Assert.assertNotNull(contextTemplate);
        JSONAssert.assertEquals(loadDataFile("/data/context-sorted-with-blackout-cron-expressions.json")
            , ConcurrentObjectMapperFactory.newInstance().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    protected String loadDataFile(String fileName) throws IOException
    {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException
    {
        return getClass().getResourceAsStream(fileName);
    }
}
