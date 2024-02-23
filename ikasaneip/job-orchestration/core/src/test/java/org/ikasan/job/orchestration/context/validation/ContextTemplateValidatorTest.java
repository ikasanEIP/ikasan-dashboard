package org.ikasan.job.orchestration.context.validation;

import org.apache.commons.lang.SerializationUtils;
import org.ikasan.job.orchestration.core.AbstractTest;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ContextTemplateValidatorTest extends AbstractTest {

    Logger logger = LoggerFactory.getLogger(ContextTemplateValidatorTest.class);

    @Test
    public void test_simple_context_validation_success() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/simple-context-chained-jobs-with-context-parameters.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();
        validator.validate(contextTemplate);
    }

    @Test
    public void test_simple_context_validation_success_2() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/context.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();
        validator.validate(contextTemplate);
    }

    @Test(expected = InvalidContextTemplateException.class)
    public void test_simple_context_validation_fail_null_job_names() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/context_null_job_names.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();

        try {
            validator.validateJobs(contextTemplate, this.createJobs(contextTemplate, false, false));
        }
        catch (InvalidContextTemplateException e) {
            Assert.assertEquals(4, e.getContextErrors().size());
            Assert.assertEquals("Job[{\"agentName\":\"agentName1\",\"startupControlType\":\"AUTOMATIC\",\"ordinal\":-1,\"identifier\":\"agentName1-jobName1\"}] " +
                "sourced from the job definition artefact is missing a job name. This is a mandatory field!\n", e.getContextErrors().get(0).getErrorMessage());
            Assert.assertEquals("Job[{\"agentName\":\"agentName2\",\"startupControlType\":\"AUTOMATIC\",\"ordinal\":-1,\"identifier\":\"agentName2-jobName2\"}] " +
                "sourced from the job definition artefact is missing a job name. This is a mandatory field!\n", e.getContextErrors().get(1).getErrorMessage());
            Assert.assertEquals("Job[{\"agentName\":\"agentName1\",\"startupControlType\":\"AUTOMATIC\",\"ordinal\":-1,\"identifier\":\"agentName1-jobName1\"}] " +
                "sourced from the job plan template is missing a job name. This is a mandatory field!\n", e.getContextErrors().get(2).getErrorMessage());
            Assert.assertEquals("Job[{\"agentName\":\"agentName2\",\"startupControlType\":\"AUTOMATIC\",\"ordinal\":-1,\"identifier\":\"agentName2-jobName2\"}] " +
                "sourced from the job plan template is missing a job name. This is a mandatory field!\n", e.getContextErrors().get(3).getErrorMessage());

            throw e;
        }
    }

    @Test(expected = InvalidContextTemplateException.class)
    public void test_simple_context_validation_fail_with_bad_identifiers() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/context.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();

        try {
            validator.validateJobs(contextTemplate, this.createJobs(contextTemplate, false, true));
        }
        catch (InvalidContextTemplateException e) {
            Assert.assertEquals(32, e.getContextErrors().size());
            Assert.assertEquals("Job[jobName5] defined in the job plan template with identifier[agentName5-jobName5] " +
                "does not have a job defined with the same identifier! This job resides within the following child contexts " +
                "within the job plan[Context3]. Please check the job definition artefact and confirm that the identifier in " +
                "the artefact is correct.\n", e.getContextErrors().get(0).getErrorMessage());
            Assert.assertEquals("Job[jobName1] defined in the job plan template with identifier[agentName1-jobName1] " +
                "does not have a job defined with the same identifier! This job resides within the following child contexts " +
                "within the job plan[Context3, Context4, Context5]. Please check the job definition artefact and " +
                "confirm that the identifier in the artefact is correct.\n", e.getContextErrors().get(1).getErrorMessage());

            throw e;
        }
    }

    @Test(expected = InvalidContextTemplateException.class)
    public void test_simple_context_validation_fail_with_bad_job_dependency_identifiers() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/context-bad-job-dependency-identifier.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();

        try {
            validator.validateJobs(contextTemplate, this.createJobs(contextTemplate, false, false));
        }
        catch (InvalidContextTemplateException e) {
            Assert.assertEquals(1, e.getContextErrors().size());
            Assert.assertEquals("Job Dependency Identifier [bad-identifier] defined in the job plan " +
                    "template does not have a job artefact defined with the same identifier!\n"
                , e.getContextErrors().get(0).getErrorMessage());

            throw e;
        }
    }

    @Test(expected = InvalidContextTemplateException.class)
    public void test_job_missing_from_scheduler_jobs_collection() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/context-job-missing-scheduler-job-collection.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();
        try {
            validator.validate(contextTemplate);
        }
        catch (InvalidContextTemplateException e) {
            Assert.assertEquals("The context template is invalid!\nContext[Context4] The following job [agentName11-jobName11] appears in a job dependency, but is not defined in the scheduler job collection.\n" +
                    "Context[Context5] The following job [agentName15-jobName15] appears in a job dependency, but is not defined in the scheduler job collection.\n"
                , e.getMessage());
            logger.info(e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidContextTemplateException.class)
    public void test_job_missing_from_job_dependency() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/context-job-missing-from-job-dependency.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();
        try {
            validator.validate(contextTemplate);
        }
        catch (InvalidContextTemplateException e) {
            Assert.assertEquals("The context template is invalid!\nContext[Context3] The following job [agentName4-jobName4] appears in the scheduler jobs collection, but is not defined in any job dependencies.\n" +
                    "Context[Context3] The following job [agentName6-jobName6] appears in the scheduler jobs collection, but is not defined in any job dependencies.\n"
                , e.getMessage());
            logger.info(e.getMessage());
            throw e;
        }
    }

    @Test
    @Ignore
    public void test_nested_context_validation_success() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/context-builder-nested-context-result.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();

        validator.validate(contextTemplate);
    }

    @Test
    @Ignore
    public void test_context_with_job_locks_validation_success() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/locks/context-with-job-locks-validation.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();
        validator.validate(contextTemplate);
    }

    @Test(expected = InvalidContextTemplateException.class)
    @Ignore
    public void test_context_with_job_locks_validation_fail_bad_job_identifier() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/locks/context-with-job-locks-validation-fail-bad-job-identifier.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();

        try {
            validator.validate(contextTemplate);
        }
        catch (InvalidContextTemplateException e) {
            Assert.assertEquals("Context[Context Template Name] contains jobs locks and and jobs, however there " +
                    "are job identifiers defined in job lock[TEST-LOCK] that do not reference scheduler jobs defined within the context.\n"
                , e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidContextTemplateException.class)
    @Ignore
    public void test_context_with_job_locks_validation_fail_contexts_at_same_level() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/locks/context-with-job-locks-validation-fail-contexts-and-job-locks-at-same-level.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();

        try {
            validator.validate(contextTemplate);
        }
        catch (InvalidContextTemplateException e) {
            Assert.assertEquals("Context[Context Template Name] contains both scheduled jobs and contexts. A context can only contain " +
                    "either scheduled jobs or contexts, but not both.\n" +
                    "Context[Context Template Name] contains both jobs locks and contexts. A context cannot contain contexts and job locks.\n"
                , e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidContextTemplateException.class)
    @Ignore
    public void test_exception_scheduler_jobs_and_contexts_at_parent_level() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/bad-simple-context-chained-jobs-with-context-parameters-jobs-and-context-at-same-level.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();

        try {
            validator.validate(contextTemplate);
        }
        catch (InvalidContextTemplateException e) {
            Assert.assertEquals("Context[Context1] contains both scheduled jobs and contexts. " +
                    "A context can only contain either scheduled jobs or contexts, but not both.\n"
                , e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidContextTemplateException.class)
    @Ignore
    public void test_exception_scheduler_jobs_and_contexts_at_nested_level() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/bad-nested-context-with-context-andjob-at-nested-level.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();

        try {
            validator.validate(contextTemplate);
        }
        catch (InvalidContextTemplateException e) {
            Assert.assertEquals("Context[Context Template 2] contains both scheduled jobs and contexts. " +
                    "A context can only contain either scheduled jobs or contexts, but not both.\n"
                , e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidContextTemplateException.class)
    @Ignore
    public void test_exception_context_parameters_at_nested_level() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/test.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();

        try {
            validator.validate(contextTemplate);
        }
        catch (InvalidContextTemplateException e) {
            Assert.assertEquals("Context[Context Template 2] must not contain any context parameters. " +
                    "Context parameters can only be present in the root context.\n"
                , e.getMessage());
            throw e;
        }
    }

    @Test(expected = InvalidContextTemplateException.class)
    public void test_duplicate_contexts_names() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContextTemplate(loadDataFile("/data/context_duplicate.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();

        try {
            validator.validate(contextTemplate);
        }
        catch (InvalidContextTemplateException e) {
            Assert.assertEquals("The context template is invalid!\nThe context name [Context2] has been repeated [2] times within the template. Context Names needs to be unique.\n"
                , e.getMessage());
            throw e;
        }
    }

    private List<SchedulerJob> createJobs(ContextTemplate contextTemplate, boolean withEmptyJobName, boolean withBadIdentifier) {
        List<SchedulerJob> schedulerJobs = ContextHelper.getAllJobs(contextTemplate);

        if(withEmptyJobName) {
            schedulerJobs = schedulerJobs.stream().map(job -> {
                SchedulerJob schedulerJob = (SchedulerJob) SerializationUtils.clone(job);
                schedulerJob.setJobName("");
                return schedulerJob;
            }).collect(Collectors.toList());
        }

        if(withBadIdentifier) {
            schedulerJobs = schedulerJobs.stream().map(job -> {
                SchedulerJob schedulerJob = (SchedulerJob) SerializationUtils.clone(job);
                schedulerJob.setIdentifier("bad identifier");
                return schedulerJob;
            }).collect(Collectors.toList());
        }

        return schedulerJobs;
    }
}
