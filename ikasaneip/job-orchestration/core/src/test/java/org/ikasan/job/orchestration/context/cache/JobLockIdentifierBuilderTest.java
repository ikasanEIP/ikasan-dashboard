package org.ikasan.job.orchestration.context.cache;

import org.ikasan.job.orchestration.model.job.SchedulerJobLockParticipantImpl;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for JobLockIdentifierBuilder.
 */
public class JobLockIdentifierBuilderTest {

    @Test
    public void test_getIdentifier_with_all_parameters() {
        String result = JobLockIdentifierBuilder.getIdentifier(
            "job123",
            "parentCtx",
            "childCtx",
            "instance456"
        );

        String expected = "job123:parent-context-name:parentCtx:context-name:childCtx:context-id:instance456";
        assertEquals(expected, result);
    }

    @Test
    public void test_getIdentifier_without_instance_id() {
        String result = JobLockIdentifierBuilder.getIdentifier(
            "job456",
            "parent2",
            "child2"
        );

        String expected = "job456:parent-context-name:parent2:context-name:child2";
        assertEquals(expected, result);
    }

    @Test
    public void test_getIdentifier_from_job_participant() {
        SchedulerJobLockParticipant job = new SchedulerJobLockParticipantImpl();
        job.setIdentifier("jobABC");
        job.setContextName("parentContext");
        job.setChildContextName("childContext");

        String result = JobLockIdentifierBuilder.getIdentifier(job);

        String expected = "jobABC:parent-context-name:parentContext:context-name:childContext";
        assertEquals(expected, result);
    }

    @Test
    public void test_getJobIdentifier_from_full_identifier() {
        String uberIdentifier = "job123:parent-context-name:parentCtx:context-name:childCtx:context-id:instance456";

        String result = JobLockIdentifierBuilder.getJobIdentifier(uberIdentifier);

        assertEquals("job123", result);
    }

    @Test
    public void test_getJobIdentifier_from_partial_identifier() {
        String uberIdentifier = "job456:parent-context-name:parent2:context-name:child2";

        String result = JobLockIdentifierBuilder.getJobIdentifier(uberIdentifier);

        assertEquals("job456", result);
    }

    @Test
    public void test_getJobIdentifier_with_null() {
        String result = JobLockIdentifierBuilder.getJobIdentifier(null);

        assertNull(result);
    }

    @Test
    public void test_getJobIdentifier_without_context_markers() {
        String result = JobLockIdentifierBuilder.getJobIdentifier("simpleJobId");

        assertEquals("simpleJobId", result);
    }

    @Test
    public void test_getParentContextName_from_full_identifier() {
        String uberIdentifier = "job123:parent-context-name:parentCtx:context-name:childCtx:context-id:instance456";

        String result = JobLockIdentifierBuilder.getParentContextName(uberIdentifier);

        assertEquals("parentCtx", result);
    }

    @Test
    public void test_getParentContextName_from_partial_identifier() {
        String uberIdentifier = "job456:parent-context-name:parent2:context-name:child2";

        String result = JobLockIdentifierBuilder.getParentContextName(uberIdentifier);

        assertEquals("parent2", result);
    }

    @Test
    public void test_getParentContextName_with_null() {
        String result = JobLockIdentifierBuilder.getParentContextName(null);

        assertNull(result);
    }

    @Test
    public void test_getParentContextName_without_marker() {
        String result = JobLockIdentifierBuilder.getParentContextName("simpleJobId");

        assertNull(result);
    }

    @Test
    public void test_getParentContextName_empty_value() {
        String uberIdentifier = "job123:parent-context-name::context-name:childCtx";

        String result = JobLockIdentifierBuilder.getParentContextName(uberIdentifier);

        assertEquals("", result);
    }

    @Test
    public void test_getContextName_from_full_identifier() {
        String uberIdentifier = "job123:parent-context-name:parentCtx:context-name:childCtx:context-id:instance456";

        String result = JobLockIdentifierBuilder.getContextName(uberIdentifier);

        assertEquals("childCtx", result);
    }

    @Test
    public void test_getContextName_from_partial_identifier() {
        String uberIdentifier = "job456:parent-context-name:parent2:context-name:child2";

        String result = JobLockIdentifierBuilder.getContextName(uberIdentifier);

        assertEquals("child2", result);
    }

    @Test
    public void test_getContextName_with_null() {
        String result = JobLockIdentifierBuilder.getContextName(null);

        assertNull(result);
    }

    @Test
    public void test_getContextName_without_marker() {
        String result = JobLockIdentifierBuilder.getContextName("simpleJobId");

        assertNull(result);
    }

    @Test
    public void test_getContextName_empty_value() {
        String uberIdentifier = "job123:parent-context-name:parentCtx:context-name::context-id:instance456";

        String result = JobLockIdentifierBuilder.getContextName(uberIdentifier);

        assertEquals("", result);
    }

    @Test
    public void test_getContextInstanceId_from_full_identifier() {
        String uberIdentifier = "job123:parent-context-name:parentCtx:context-name:childCtx:context-id:instance456";

        String result = JobLockIdentifierBuilder.getContextInstanceId(uberIdentifier);

        assertEquals("instance456", result);
    }

    @Test
    public void test_getContextInstanceId_from_partial_identifier() {
        String uberIdentifier = "job456:parent-context-name:parent2:context-name:child2";

        String result = JobLockIdentifierBuilder.getContextInstanceId(uberIdentifier);

        assertNull(result);
    }

    @Test
    public void test_getContextInstanceId_with_null() {
        String result = JobLockIdentifierBuilder.getContextInstanceId(null);

        assertNull(result);
    }

    @Test
    public void test_getContextInstanceId_without_marker() {
        String result = JobLockIdentifierBuilder.getContextInstanceId("simpleJobId");

        assertNull(result);
    }

    @Test
    public void test_getContextInstanceId_empty_value() {
        String uberIdentifier = "job123:parent-context-name:parentCtx:context-name:childCtx:context-id:";

        String result = JobLockIdentifierBuilder.getContextInstanceId(uberIdentifier);

        assertEquals("", result);
    }

    @Test
    public void test_roundtrip_full_identifier() {
        // Build identifier
        String jobId = "myJob";
        String parent = "parentContext";
        String child = "childContext";
        String instance = "instance123";

        String uberIdentifier = JobLockIdentifierBuilder.getIdentifier(jobId, parent, child, instance);

        // Extract components
        assertEquals(jobId, JobLockIdentifierBuilder.getJobIdentifier(uberIdentifier));
        assertEquals(parent, JobLockIdentifierBuilder.getParentContextName(uberIdentifier));
        assertEquals(child, JobLockIdentifierBuilder.getContextName(uberIdentifier));
        assertEquals(instance, JobLockIdentifierBuilder.getContextInstanceId(uberIdentifier));
    }

    @Test
    public void test_roundtrip_partial_identifier() {
        // Build identifier
        String jobId = "anotherJob";
        String parent = "parentCtx2";
        String child = "childCtx2";

        String uberIdentifier = JobLockIdentifierBuilder.getIdentifier(jobId, parent, child);

        // Extract components
        assertEquals(jobId, JobLockIdentifierBuilder.getJobIdentifier(uberIdentifier));
        assertEquals(parent, JobLockIdentifierBuilder.getParentContextName(uberIdentifier));
        assertEquals(child, JobLockIdentifierBuilder.getContextName(uberIdentifier));
        assertNull(JobLockIdentifierBuilder.getContextInstanceId(uberIdentifier));
    }

    @Test
    public void test_special_characters_in_components() {
        String jobId = "job-with-dashes_and_underscores";
        String parent = "parent.context.with.dots";
        String child = "child/context/with/slashes";
        String instance = "instance:with:colons";

        String uberIdentifier = JobLockIdentifierBuilder.getIdentifier(jobId, parent, child, instance);

        assertEquals(jobId, JobLockIdentifierBuilder.getJobIdentifier(uberIdentifier));
        assertEquals(parent, JobLockIdentifierBuilder.getParentContextName(uberIdentifier));
        assertEquals(child, JobLockIdentifierBuilder.getContextName(uberIdentifier));
        assertEquals(instance, JobLockIdentifierBuilder.getContextInstanceId(uberIdentifier));
    }

    @Test
    public void test_constants_are_correct() {
        assertEquals(":parent-context-name:", JobLockIdentifierBuilder.PARENT_CONTEXT_NAME);
        assertEquals(":context-name:", JobLockIdentifierBuilder.CONTEXT_NAME);
        assertEquals(":context-id:", JobLockIdentifierBuilder.CONTEXT_ID);
    }
}
