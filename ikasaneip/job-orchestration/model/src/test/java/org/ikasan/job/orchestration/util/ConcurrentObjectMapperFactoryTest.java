package org.ikasan.job.orchestration.util;

import org.ikasan.job.orchestration.model.context.*;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedSchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.instance.*;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.model.profile.ContextProfileImpl;
import org.ikasan.job.orchestration.model.profile.ContextProfileRecordImpl;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.junit.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.Assert.*;

/**
 * Unit test for ConcurrentObjectMapperFactory focusing on:
 * - NON_NULL serialization configuration
 * - NON_EMPTY serialization configuration
 * - Abstract type mappings for concurrent collections
 * - All abstract type mappings
 */
public class ConcurrentObjectMapperFactoryTest {

    /**
     * Test that JsonMapper instance is created successfully
     */
    @Test
    public void test_newInstance_creates_JsonMapper() {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();
        assertNotNull("JsonMapper should not be null", JsonMapper);
    }

    /**
     * Test NON_NULL configuration - null values should not be serialized
     */
    @Test
    public void test_nonNull_configuration() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        TestObject obj = new TestObject();
        obj.setNonNullField("value");
        obj.setNullField(null);

        String json = JsonMapper.writeValueAsString(obj);

        assertFalse("JSON should not contain null field", json.contains("nullField"));
        assertTrue("JSON should contain non-null field", json.contains("nonNullField"));
    }

    /**
     * Test NON_EMPTY configuration - empty collections should not be serialized
     */
    @Test
    public void test_nonEmpty_configuration_with_emptyList() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        TestObjectWithCollections obj = new TestObjectWithCollections();
        obj.setEmptyList(new CopyOnWriteArrayList<>());
        obj.setPopulatedList(new CopyOnWriteArrayList<>(Arrays.asList("item1", "item2")));

        String json = JsonMapper.writeValueAsString(obj);

        assertFalse("JSON should not contain empty list", json.contains("emptyList"));
        assertTrue("JSON should contain populated list", json.contains("populatedList"));
    }

    /**
     * Test NON_EMPTY configuration - empty maps should not be serialized
     */
    @Test
    public void test_nonEmpty_configuration_with_emptyMap() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        TestObjectWithCollections obj = new TestObjectWithCollections();
        obj.setEmptyMap(new ConcurrentHashMap<>());
        obj.setPopulatedMap(new ConcurrentHashMap<>(Map.of("key1", "value1")));

        String json = JsonMapper.writeValueAsString(obj);

        assertFalse("JSON should not contain empty map", json.contains("emptyMap"));
        assertTrue("JSON should contain populated map", json.contains("populatedMap"));
    }

    /**
     * Test NON_EMPTY configuration - empty sets should not be serialized
     */
    @Test
    public void test_nonEmpty_configuration_with_emptySet() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        TestObjectWithCollections obj = new TestObjectWithCollections();
        obj.setEmptySet(new CopyOnWriteArraySet<>());
        obj.setPopulatedSet(new CopyOnWriteArraySet<>(Arrays.asList("item1")));

        String json = JsonMapper.writeValueAsString(obj);

        assertFalse("JSON should not contain empty set", json.contains("emptySet"));
        assertTrue("JSON should contain populated set", json.contains("populatedSet"));
    }

    /**
     * Test NON_EMPTY configuration - empty strings should not be serialized
     */
    @Test
    public void test_nonEmpty_configuration_with_emptyString() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        TestObject obj = new TestObject();
        obj.setEmptyString("");
        obj.setNonNullField("value");

        String json = JsonMapper.writeValueAsString(obj);

        assertFalse("JSON should not contain empty string field", json.contains("emptyString"));
        assertTrue("JSON should contain non-empty field", json.contains("nonNullField"));
    }

    /**
     * Test FAIL_ON_UNKNOWN_PROPERTIES is disabled
     */
    @Test
    public void test_failOnUnknownProperties_disabled() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String jsonWithUnknownProperty = "{\"nonNullField\":\"value\",\"unknownProperty\":\"unknown\"}";

        // Should not throw exception despite unknown property
        TestObject obj = JsonMapper.readValue(jsonWithUnknownProperty, TestObject.class);
        assertNotNull("Object should be deserialized", obj);
        assertEquals("Known field should be deserialized", "value", obj.getNonNullField());
    }

    /**
     * Test List abstract type mapping to CopyOnWriteArrayList
     */
    @Test
    public void test_list_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "[\"item1\",\"item2\",\"item3\"]";
        List<?> list = JsonMapper.readValue(json, List.class);

        assertTrue("List should be deserialized as CopyOnWriteArrayList",
                   list instanceof CopyOnWriteArrayList);
        assertEquals("List should contain 3 items", 3, list.size());
    }

    /**
     * Test Map abstract type mapping to ConcurrentHashMap
     */
    @Test
    public void test_map_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        Map<?, ?> map = JsonMapper.readValue(json, Map.class);

        assertTrue("Map should be deserialized as ConcurrentHashMap",
                   map instanceof ConcurrentHashMap);
        assertEquals("Map should contain 2 entries", 2, map.size());
    }

    /**
     * Test Set abstract type mapping to CopyOnWriteArraySet
     */
    @Test
    public void test_set_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "[\"item1\",\"item2\",\"item3\"]";
        Set<?> set = JsonMapper.readValue(json, Set.class);

        assertTrue("Set should be deserialized as CopyOnWriteArraySet",
                   set instanceof CopyOnWriteArraySet);
        assertEquals("Set should contain 3 items", 3, set.size());
    }

    /**
     * Test And abstract type mapping
     */
    @Test
    public void test_and_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"logicalOperators\":[]}";
        And and = JsonMapper.readValue(json, And.class);

        assertTrue("And should be deserialized as AndImpl", and instanceof AndImpl);
    }

    /**
     * Test Or abstract type mapping
     */
    @Test
    public void test_or_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"logicalOperators\":[]}";
        Or or = JsonMapper.readValue(json, Or.class);

        assertTrue("Or should be deserialized as OrImpl", or instanceof OrImpl);
    }

    /**
     * Test Not abstract type mapping
     */
    @Test
    public void test_not_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"logicalOperator\":null}";
        Not not = JsonMapper.readValue(json, Not.class);

        assertTrue("Not should be deserialized as NotImpl", not instanceof NotImpl);
    }

    /**
     * Test ContextTemplate abstract type mapping
     */
    @Test
    public void test_contextTemplate_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"name\":\"test\"}";
        ContextTemplate contextTemplate = JsonMapper.readValue(json, ContextTemplate.class);

        assertTrue("ContextTemplate should be deserialized as ContextTemplateImpl",
                   contextTemplate instanceof ContextTemplateImpl);
    }

    /**
     * Test ContextParameter abstract type mapping
     */
    @Test
    public void test_contextParameter_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"name\":\"test\"}";
        ContextParameter contextParameter = JsonMapper.readValue(json, ContextParameter.class);

        assertTrue("ContextParameter should be deserialized as ContextParameterImpl",
                   contextParameter instanceof ContextParameterImpl);
    }

    /**
     * Test SchedulerJob abstract type mapping
     */
    @Test
    public void test_schedulerJob_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"agentName\":\"test\",\"jobName\":\"testJob\"}";
        SchedulerJob schedulerJob = JsonMapper.readValue(json, SchedulerJob.class);

        assertTrue("SchedulerJob should be deserialized as SchedulerJobImpl",
                   schedulerJob instanceof SchedulerJobImpl);
    }

    /**
     * Test SchedulerJobLockParticipant abstract type mapping
     */
    @Test
    public void test_schedulerJobLockParticipant_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"jobName\":\"test\"}";
        SchedulerJobLockParticipant participant = JsonMapper.readValue(json, SchedulerJobLockParticipant.class);

        assertTrue("SchedulerJobLockParticipant should be deserialized as SchedulerJobLockParticipantImpl",
                   participant instanceof SchedulerJobLockParticipantImpl);
    }

    /**
     * Test JobDependency abstract type mapping
     */
    @Test
    public void test_jobDependency_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"jobName\":\"test\"}";
        JobDependency jobDependency = JsonMapper.readValue(json, JobDependency.class);

        assertTrue("JobDependency should be deserialized as JobDependencyImpl",
                   jobDependency instanceof JobDependencyImpl);
    }

    /**
     * Test ContextDependency abstract type mapping
     */
    @Test
    public void test_contextDependency_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"contextName\":\"test\"}";
        ContextDependency contextDependency = JsonMapper.readValue(json, ContextDependency.class);

        assertTrue("ContextDependency should be deserialized as ContextDependencyImpl",
                   contextDependency instanceof ContextDependencyImpl);
    }

    /**
     * Test LogicalGrouping abstract type mapping
     */
    @Test
    public void test_logicalGrouping_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"name\":\"test\"}";
        LogicalGrouping logicalGrouping = JsonMapper.readValue(json, LogicalGrouping.class);

        assertTrue("LogicalGrouping should be deserialized as LogicalGroupingImpl",
                   logicalGrouping instanceof LogicalGroupingImpl);
    }

    /**
     * Test ContextInstance abstract type mapping
     */
    @Test
    public void test_contextInstance_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"id\":\"test-id\"}";
        ContextInstance contextInstance = JsonMapper.readValue(json, ContextInstance.class);

        assertTrue("ContextInstance should be deserialized as ContextInstanceImpl",
                   contextInstance instanceof ContextInstanceImpl);
    }

    /**
     * Test SchedulerJobInstance abstract type mapping
     */
    @Test
    public void test_schedulerJobInstance_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"id\":\"test-id\"}";
        SchedulerJobInstance jobInstance = JsonMapper.readValue(json, SchedulerJobInstance.class);

        assertTrue("SchedulerJobInstance should be deserialized as SchedulerJobInstanceImpl",
                   jobInstance instanceof SchedulerJobInstanceImpl);
    }

    /**
     * Test ContextParameterInstance abstract type mapping
     */
    @Test
    public void test_contextParameterInstance_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"name\":\"test\"}";
        ContextParameterInstance paramInstance = JsonMapper.readValue(json, ContextParameterInstance.class);

        assertTrue("ContextParameterInstance should be deserialized as ContextParameterInstanceImpl",
                   paramInstance instanceof ContextParameterInstanceImpl);
    }

    /**
     * Test ScheduledProcessEvent abstract type mapping
     */
    @Test
    public void test_scheduledProcessEvent_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"moduleName\":\"test\"}";
        ScheduledProcessEvent event = JsonMapper.readValue(json, ScheduledProcessEvent.class);

        assertTrue("ScheduledProcessEvent should be deserialized as ContextualisedScheduledProcessEventImpl",
                   event instanceof ContextualisedScheduledProcessEventImpl);
    }

    /**
     * Test ContextualisedScheduledProcessEvent abstract type mapping
     */
    @Test
    public void test_contextualisedScheduledProcessEvent_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"moduleName\":\"test\"}";
        ContextualisedScheduledProcessEvent event = JsonMapper.readValue(json, ContextualisedScheduledProcessEvent.class);

        assertTrue("ContextualisedScheduledProcessEvent should be deserialized as ContextualisedScheduledProcessEventImpl",
                   event instanceof ContextualisedScheduledProcessEventImpl);
    }

    /**
     * Test ContextualisedSchedulerJobInitiationEvent abstract type mapping
     */
    @Test
    public void test_contextualisedSchedulerJobInitiationEvent_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"moduleName\":\"test\"}";
        ContextualisedSchedulerJobInitiationEvent event = JsonMapper.readValue(json, ContextualisedSchedulerJobInitiationEvent.class);

        assertTrue("ContextualisedSchedulerJobInitiationEvent should be deserialized as ContextualisedSchedulerJobInitiationEventImpl",
                   event instanceof ContextualisedSchedulerJobInitiationEventImpl);
    }

    /**
     * Test SchedulerJobInitiationEvent abstract type mapping
     */
    @Test
    public void test_schedulerJobInitiationEvent_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"moduleName\":\"test\"}";
        SchedulerJobInitiationEvent event = JsonMapper.readValue(json, SchedulerJobInitiationEvent.class);

        assertTrue("SchedulerJobInitiationEvent should be deserialized as SchedulerJobInitiationEventImpl",
                   event instanceof SchedulerJobInitiationEventImpl);
    }

    /**
     * Test InternalEventDrivenJob abstract type mapping
     */
    @Test
    public void test_internalEventDrivenJob_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"agentName\":\"test\",\"jobName\":\"testJob\"}";
        InternalEventDrivenJob job = JsonMapper.readValue(json, InternalEventDrivenJob.class);

        assertTrue("InternalEventDrivenJob should be deserialized as InternalEventDrivenJobImpl",
                   job instanceof InternalEventDrivenJobImpl);
    }

    /**
     * Test InternalEventDrivenJobInstance abstract type mapping
     */
    @Test
    public void test_internalEventDrivenJobInstance_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"id\":\"test-id\"}";
        InternalEventDrivenJobInstance instance = JsonMapper.readValue(json, InternalEventDrivenJobInstance.class);

        assertTrue("InternalEventDrivenJobInstance should be deserialized as InternalEventDrivenJobInstanceImpl",
                   instance instanceof InternalEventDrivenJobInstanceImpl);
    }

    /**
     * Test QuartzScheduleDrivenJob abstract type mapping
     */
    @Test
    public void test_quartzScheduleDrivenJob_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"agentName\":\"test\",\"jobName\":\"testJob\"}";
        QuartzScheduleDrivenJob job = JsonMapper.readValue(json, QuartzScheduleDrivenJob.class);

        assertTrue("QuartzScheduleDrivenJob should be deserialized as QuartzScheduleDrivenJobImpl",
                   job instanceof QuartzScheduleDrivenJobImpl);
    }

    /**
     * Test FileEventDrivenJob abstract type mapping
     */
    @Test
    public void test_fileEventDrivenJob_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"agentName\":\"test\",\"jobName\":\"testJob\"}";
        FileEventDrivenJob job = JsonMapper.readValue(json, FileEventDrivenJob.class);

        assertTrue("FileEventDrivenJob should be deserialized as FileEventDrivenJobImpl",
                   job instanceof FileEventDrivenJobImpl);
    }

    /**
     * Test GlobalEventJob abstract type mapping
     */
    @Test
    public void test_globalEventJob_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"agentName\":\"test\",\"jobName\":\"testJob\"}";
        GlobalEventJob job = JsonMapper.readValue(json, GlobalEventJob.class);

        assertTrue("GlobalEventJob should be deserialized as GlobalEventJobImpl",
                   job instanceof GlobalEventJobImpl);
    }

    /**
     * Test ContextProfileRecord abstract type mapping
     */
    @Test
    public void test_contextProfileRecord_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"contextName\":\"test\"}";
        ContextProfileRecord record = JsonMapper.readValue(json, ContextProfileRecord.class);

        assertTrue("ContextProfileRecord should be deserialized as ContextProfileRecordImpl",
                   record instanceof ContextProfileRecordImpl);
    }

    /**
     * Test ContextProfile abstract type mapping
     */
    @Test
    public void test_contextProfile_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"contextName\":\"test\"}";
        ContextProfile profile = JsonMapper.readValue(json, ContextProfile.class);

        assertTrue("ContextProfile should be deserialized as ContextProfileImpl",
                   profile instanceof ContextProfileImpl);
    }

    /**
     * Test JobLock abstract type mapping
     */
    @Test
    public void test_jobLock_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"lockName\":\"test\"}";
        JobLock jobLock = JsonMapper.readValue(json, JobLock.class);

        assertTrue("JobLock should be deserialized as JobLockImpl",
                   jobLock instanceof JobLockImpl);
    }

    /**
     * Test JobLockInstance abstract type mapping
     */
    @Test
    public void test_jobLockInstance_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"id\":\"test-id\"}";
        JobLockInstance jobLockInstance = JsonMapper.readValue(json, JobLockInstance.class);

        assertTrue("JobLockInstance should be deserialized as JobLockInstanceImpl",
                   jobLockInstance instanceof JobLockInstanceImpl);
    }

    /**
     * Test JobLockHolder abstract type mapping
     */
    @Test
    public void test_jobLockHolder_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"contextInstanceId\":\"test-id\"}";
        JobLockHolder holder = JsonMapper.readValue(json, JobLockHolder.class);

        assertTrue("JobLockHolder should be deserialized as JobLockHolderImpl",
                   holder instanceof JobLockHolderImpl);
    }

    /**
     * Test ReplacementPair abstract type mapping
     */
    @Test
    public void test_replacementPair_abstractTypeMapping() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        String json = "{\"search\":\"test\",\"replace\":\"value\"}";
        ReplacementPair pair = JsonMapper.readValue(json, ReplacementPair.class);

        assertTrue("ReplacementPair should be deserialized as ReplacementPairImpl",
                   pair instanceof ReplacementPairImpl);
    }

    /**
     * Test combined NON_NULL and NON_EMPTY behavior
     */
    @Test
    public void test_combined_nonNull_nonEmpty_behavior() throws JacksonException {
        JsonMapper JsonMapper = ConcurrentObjectMapperFactory.newInstance();

        TestObjectWithCollections obj = new TestObjectWithCollections();
        obj.setNullValue(null);
        obj.setEmptyList(new CopyOnWriteArrayList<>());
        obj.setEmptyMap(new ConcurrentHashMap<>());
        obj.setEmptySet(new CopyOnWriteArraySet<>());
        obj.setPopulatedList(new CopyOnWriteArrayList<>(Arrays.asList("item")));

        String json = JsonMapper.writeValueAsString(obj);

        assertFalse("JSON should not contain null value", json.contains("nullValue"));
        assertFalse("JSON should not contain empty list", json.contains("emptyList"));
        assertFalse("JSON should not contain empty map", json.contains("emptyMap"));
        assertFalse("JSON should not contain empty set", json.contains("emptySet"));
        assertTrue("JSON should contain populated list", json.contains("populatedList"));
    }

    // Helper test classes
    public static class TestObject {
        private String nonNullField;
        private String nullField;
        private String emptyString;

        public String getNonNullField() {
            return nonNullField;
        }

        public void setNonNullField(String nonNullField) {
            this.nonNullField = nonNullField;
        }

        public String getNullField() {
            return nullField;
        }

        public void setNullField(String nullField) {
            this.nullField = nullField;
        }

        public String getEmptyString() {
            return emptyString;
        }

        public void setEmptyString(String emptyString) {
            this.emptyString = emptyString;
        }
    }

    public static class TestObjectWithCollections {
        private String nullValue;
        private List<String> emptyList;
        private List<String> populatedList;
        private Map<String, String> emptyMap;
        private Map<String, String> populatedMap;
        private Set<String> emptySet;
        private Set<String> populatedSet;

        public String getNullValue() {
            return nullValue;
        }

        public void setNullValue(String nullValue) {
            this.nullValue = nullValue;
        }

        public List<String> getEmptyList() {
            return emptyList;
        }

        public void setEmptyList(List<String> emptyList) {
            this.emptyList = emptyList;
        }

        public List<String> getPopulatedList() {
            return populatedList;
        }

        public void setPopulatedList(List<String> populatedList) {
            this.populatedList = populatedList;
        }

        public Map<String, String> getEmptyMap() {
            return emptyMap;
        }

        public void setEmptyMap(Map<String, String> emptyMap) {
            this.emptyMap = emptyMap;
        }

        public Map<String, String> getPopulatedMap() {
            return populatedMap;
        }

        public void setPopulatedMap(Map<String, String> populatedMap) {
            this.populatedMap = populatedMap;
        }

        public Set<String> getEmptySet() {
            return emptySet;
        }

        public void setEmptySet(Set<String> emptySet) {
            this.emptySet = emptySet;
        }

        public Set<String> getPopulatedSet() {
            return populatedSet;
        }

        public void setPopulatedSet(Set<String> populatedSet) {
            this.populatedSet = populatedSet;
        }
    }
}
