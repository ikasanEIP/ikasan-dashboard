package org.ikasan.scheduler.context.validation;

import org.ikasan.scheduler.core.AbstractTest;
import org.ikasan.scheduler.core.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ContextTemplateValidatorTest extends AbstractTest {

    @Test
    public void test_simple_context_validation_success() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContext(loadDataFile("/data/simple-context-chained-jobs-with-context-parameters.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();
        validator.validate(contextTemplate);
    }

    @Test
    public void test_nested_context_validation_success() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContext(loadDataFile("/data/context-builder-nested-context-result.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();
        validator.validate(contextTemplate);
    }

    @Test(expected = InvalidContextTemplateException.class)
    public void test_exception_scheduler_jobs_and_contexts_at_parent_level() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContext(loadDataFile("/data/bad-simple-context-chained-jobs-with-context-parameters-jobs-and-context-at-same-level.json"));
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
    public void test_exception_scheduler_jobs_and_contexts_at_nested_level() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContext(loadDataFile("/data/bad-nested-context-with-context-andjob-at-nested-level.json"));
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
    public void test_exception_context_parameters_at_nested_level() throws IOException, InvalidContextTemplateException {
        ContextService contextService = new ContextService();

        ContextTemplate contextTemplate = contextService
            .getContext(loadDataFile("/data/test.json"));
        ContextTemplateValidator validator = new ContextTemplateValidator();

        try {
            validator.validate(contextTemplate);
        }
        catch (InvalidContextTemplateException e) {
            System.out.println(e.getMessage());
            Assert.assertEquals("Context[Context Template 2] must not contain any context parameters. " +
                    "Context parameters can only be present in the root context.\n"
                , e.getMessage());
            throw e;
        }
    }
}
