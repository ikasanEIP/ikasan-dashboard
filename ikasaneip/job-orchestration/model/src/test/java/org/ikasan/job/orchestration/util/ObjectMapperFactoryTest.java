package org.ikasan.job.orchestration.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit test for ObjectMapperFactory focusing on:
 * - NON_NULL serialization configuration
 * - NON_EMPTY serialization configuration
 * - Abstract type mappings for standard collections (ArrayList, HashMap, HashSet)
 * - All abstract type mappings
 */
public class ObjectMapperFactoryTest {

    /**
     * Test that ObjectMapper instance is created successfully
     */
    @Test
    public void test_newInstance_creates_objectMapper() {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
        assertNotNull("ObjectMapper should not be null", objectMapper);
    }

    /**
     * Test NON_NULL configuration - null values should not be serialized
     */
    @Test
    public void test_nonNull_configuration() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        TestObject obj = new TestObject();
        obj.setNonNullField("value");
        obj.setNullField(null);

        String json = objectMapper.writeValueAsString(obj);

        assertFalse("JSON should not contain null field", json.contains("nullField"));
        assertTrue("JSON should contain non-null field", json.contains("nonNullField"));
    }

    /**
     * Test NON_EMPTY configuration - empty collections should not be serialized
     */
    @Test
    public void test_nonEmpty_configuration_with_emptyList() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        TestObjectWithCollections obj = new TestObjectWithCollections();
        obj.setEmptyList(new ArrayList<>());
        obj.setPopulatedList(new ArrayList<>(Arrays.asList("item1", "item2")));

        String json = objectMapper.writeValueAsString(obj);

        assertFalse("JSON should not contain empty list", json.contains("emptyList"));
        assertTrue("JSON should contain populated list", json.contains("populatedList"));
    }

    /**
     * Test NON_EMPTY configuration - empty maps should not be serialized
     */
    @Test
    public void test_nonEmpty_configuration_with_emptyMap() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        TestObjectWithCollections obj = new TestObjectWithCollections();
        obj.setEmptyMap(new HashMap<>());
        obj.setPopulatedMap(new HashMap<>(Map.of("key1", "value1")));

        String json = objectMapper.writeValueAsString(obj);

        assertFalse("JSON should not contain empty map", json.contains("emptyMap"));
        assertTrue("JSON should contain populated map", json.contains("populatedMap"));
    }

    /**
     * Test NON_EMPTY configuration - empty sets should not be serialized
     */
    @Test
    public void test_nonEmpty_configuration_with_emptySet() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        TestObjectWithCollections obj = new TestObjectWithCollections();
        obj.setEmptySet(new HashSet<>());
        obj.setPopulatedSet(new HashSet<>(Arrays.asList("item1")));

        String json = objectMapper.writeValueAsString(obj);

        assertFalse("JSON should not contain empty set", json.contains("emptySet"));
        assertTrue("JSON should contain populated set", json.contains("populatedSet"));
    }

    /**
     * Test NON_EMPTY configuration - empty strings should not be serialized
     */
    @Test
    public void test_nonEmpty_configuration_with_emptyString() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        TestObject obj = new TestObject();
        obj.setEmptyString("");
        obj.setNonNullField("value");

        String json = objectMapper.writeValueAsString(obj);

        assertFalse("JSON should not contain empty string field", json.contains("emptyString"));
        assertTrue("JSON should contain non-empty field", json.contains("nonNullField"));
    }

    /**
     * Test FAIL_ON_UNKNOWN_PROPERTIES is disabled
     */
    @Test
    public void test_failOnUnknownProperties_disabled() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String jsonWithUnknownProperty = "{\"nonNullField\":\"value\",\"unknownProperty\":\"unknown\"}";

        // Should not throw exception despite unknown property
        TestObject obj = objectMapper.readValue(jsonWithUnknownProperty, TestObject.class);
        assertNotNull("Object should be deserialized", obj);
        assertEquals("Known field should be deserialized", "value", obj.getNonNullField());
    }

    /**
     * Test List abstract type mapping to ArrayList
     */
    @Test
    public void test_list_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "[\"item1\",\"item2\",\"item3\"]";
        List<?> list = objectMapper.readValue(json, List.class);

        assertTrue("List should be deserialized as ArrayList",
                   list instanceof ArrayList);
        assertEquals("List should contain 3 items", 3, list.size());
    }

    /**
     * Test Map abstract type mapping to HashMap
     */
    @Test
    public void test_map_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        Map<?, ?> map = objectMapper.readValue(json, Map.class);

        assertTrue("Map should be deserialized as HashMap",
                   map instanceof HashMap);
        assertEquals("Map should contain 2 entries", 2, map.size());
    }

    /**
     * Test Set abstract type mapping to HashSet
     */
    @Test
    public void test_set_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "[\"item1\",\"item2\",\"item3\"]";
        Set<?> set = objectMapper.readValue(json, Set.class);

        assertTrue("Set should be deserialized as HashSet",
                   set instanceof HashSet);
        assertEquals("Set should contain 3 items", 3, set.size());
    }

    /**
     * Test And abstract type mapping
     */
    @Test
    public void test_and_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"logicalOperators\":[]}";
        And and = objectMapper.readValue(json, And.class);

        assertTrue("And should be deserialized as AndImpl", and instanceof AndImpl);
    }

    /**
     * Test Or abstract type mapping
     */
    @Test
    public void test_or_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"logicalOperators\":[]}";
        Or or = objectMapper.readValue(json, Or.class);

        assertTrue("Or should be deserialized as OrImpl", or instanceof OrImpl);
    }

    /**
     * Test Not abstract type mapping
     */
    @Test
    public void test_not_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"logicalOperator\":null}";
        Not not = objectMapper.readValue(json, Not.class);

        assertTrue("Not should be deserialized as NotImpl", not instanceof NotImpl);
    }

    /**
     * Test ContextTemplate abstract type mapping
     */
    @Test
    public void test_contextTemplate_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"name\":\"test\"}";
        ContextTemplate contextTemplate = objectMapper.readValue(json, ContextTemplate.class);

        assertTrue("ContextTemplate should be deserialized as ContextTemplateImpl",
                   contextTemplate instanceof ContextTemplateImpl);
    }

    /**
     * Test ContextParameter abstract type mapping
     */
    @Test
    public void test_contextParameter_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"name\":\"test\"}";
        ContextParameter contextParameter = objectMapper.readValue(json, ContextParameter.class);

        assertTrue("ContextParameter should be deserialized as ContextParameterImpl",
                   contextParameter instanceof ContextParameterImpl);
    }

    /**
     * Test SchedulerJob abstract type mapping
     */
    @Test
    public void test_schedulerJob_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"agentName\":\"test\",\"jobName\":\"testJob\"}";
        SchedulerJob schedulerJob = objectMapper.readValue(json, SchedulerJob.class);

        assertTrue("SchedulerJob should be deserialized as SchedulerJobImpl",
                   schedulerJob instanceof SchedulerJobImpl);
    }

    /**
     * Test SchedulerJobLockParticipant abstract type mapping
     */
    @Test
    public void test_schedulerJobLockParticipant_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"jobName\":\"test\"}";
        SchedulerJobLockParticipant participant = objectMapper.readValue(json, SchedulerJobLockParticipant.class);

        assertTrue("SchedulerJobLockParticipant should be deserialized as SchedulerJobLockParticipantImpl",
                   participant instanceof SchedulerJobLockParticipantImpl);
    }

    /**
     * Test JobDependency abstract type mapping
     */
    @Test
    public void test_jobDependency_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"jobName\":\"test\"}";
        JobDependency jobDependency = objectMapper.readValue(json, JobDependency.class);

        assertTrue("JobDependency should be deserialized as JobDependencyImpl",
                   jobDependency instanceof JobDependencyImpl);
    }

    /**
     * Test ContextDependency abstract type mapping
     */
    @Test
    public void test_contextDependency_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"contextName\":\"test\"}";
        ContextDependency contextDependency = objectMapper.readValue(json, ContextDependency.class);

        assertTrue("ContextDependency should be deserialized as ContextDependencyImpl",
                   contextDependency instanceof ContextDependencyImpl);
    }

    /**
     * Test LogicalGrouping abstract type mapping
     */
    @Test
    public void test_logicalGrouping_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"name\":\"test\"}";
        LogicalGrouping logicalGrouping = objectMapper.readValue(json, LogicalGrouping.class);

        assertTrue("LogicalGrouping should be deserialized as LogicalGroupingImpl",
                   logicalGrouping instanceof LogicalGroupingImpl);
    }

    /**
     * Test ContextInstance abstract type mapping
     */
    @Test
    public void test_contextInstance_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"id\":\"test-id\"}";
        ContextInstance contextInstance = objectMapper.readValue(json, ContextInstance.class);

        assertTrue("ContextInstance should be deserialized as ContextInstanceImpl",
                   contextInstance instanceof ContextInstanceImpl);
    }

    /**
     * Test SchedulerJobInstance abstract type mapping
     */
    @Test
    public void test_schedulerJobInstance_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"id\":\"test-id\"}";
        SchedulerJobInstance jobInstance = objectMapper.readValue(json, SchedulerJobInstance.class);

        assertTrue("SchedulerJobInstance should be deserialized as SchedulerJobInstanceImpl",
                   jobInstance instanceof SchedulerJobInstanceImpl);
    }

    /**
     * Test ContextParameterInstance abstract type mapping
     */
    @Test
    public void test_contextParameterInstance_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"name\":\"test\"}";
        ContextParameterInstance paramInstance = objectMapper.readValue(json, ContextParameterInstance.class);

        assertTrue("ContextParameterInstance should be deserialized as ContextParameterInstanceImpl",
                   paramInstance instanceof ContextParameterInstanceImpl);
    }

    /**
     * Test ScheduledProcessEvent abstract type mapping
     */
    @Test
    public void test_scheduledProcessEvent_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"moduleName\":\"test\"}";
        ScheduledProcessEvent event = objectMapper.readValue(json, ScheduledProcessEvent.class);

        assertTrue("ScheduledProcessEvent should be deserialized as ContextualisedScheduledProcessEventImpl",
                   event instanceof ContextualisedScheduledProcessEventImpl);
    }

    /**
     * Test ContextualisedScheduledProcessEvent abstract type mapping
     */
    @Test
    public void test_contextualisedScheduledProcessEvent_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"moduleName\":\"test\"}";
        ContextualisedScheduledProcessEvent event = objectMapper.readValue(json, ContextualisedScheduledProcessEvent.class);

        assertTrue("ContextualisedScheduledProcessEvent should be deserialized as ContextualisedScheduledProcessEventImpl",
                   event instanceof ContextualisedScheduledProcessEventImpl);
    }

    /**
     * Test ContextualisedSchedulerJobInitiationEvent abstract type mapping
     */
    @Test
    public void test_contextualisedSchedulerJobInitiationEvent_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"moduleName\":\"test\"}";
        ContextualisedSchedulerJobInitiationEvent event = objectMapper.readValue(json, ContextualisedSchedulerJobInitiationEvent.class);

        assertTrue("ContextualisedSchedulerJobInitiationEvent should be deserialized as ContextualisedSchedulerJobInitiationEventImpl",
                   event instanceof ContextualisedSchedulerJobInitiationEventImpl);
    }

    /**
     * Test SchedulerJobInitiationEvent abstract type mapping
     */
    @Test
    public void test_schedulerJobInitiationEvent_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"moduleName\":\"test\"}";
        SchedulerJobInitiationEvent event = objectMapper.readValue(json, SchedulerJobInitiationEvent.class);

        assertTrue("SchedulerJobInitiationEvent should be deserialized as SchedulerJobInitiationEventImpl",
                   event instanceof SchedulerJobInitiationEventImpl);
    }

    /**
     * Test InternalEventDrivenJob abstract type mapping
     */
    @Test
    public void test_internalEventDrivenJob_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"agentName\":\"test\",\"jobName\":\"testJob\"}";
        InternalEventDrivenJob job = objectMapper.readValue(json, InternalEventDrivenJob.class);

        assertTrue("InternalEventDrivenJob should be deserialized as InternalEventDrivenJobImpl",
                   job instanceof InternalEventDrivenJobImpl);
    }

    /**
     * Test InternalEventDrivenJobInstance abstract type mapping
     */
    @Test
    public void test_internalEventDrivenJobInstance_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"id\":\"test-id\"}";
        InternalEventDrivenJobInstance instance = objectMapper.readValue(json, InternalEventDrivenJobInstance.class);

        assertTrue("InternalEventDrivenJobInstance should be deserialized as InternalEventDrivenJobInstanceImpl",
                   instance instanceof InternalEventDrivenJobInstanceImpl);
    }

    /**
     * Test QuartzScheduleDrivenJob abstract type mapping
     */
    @Test
    public void test_quartzScheduleDrivenJob_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"agentName\":\"test\",\"jobName\":\"testJob\"}";
        QuartzScheduleDrivenJob job = objectMapper.readValue(json, QuartzScheduleDrivenJob.class);

        assertTrue("QuartzScheduleDrivenJob should be deserialized as QuartzScheduleDrivenJobImpl",
                   job instanceof QuartzScheduleDrivenJobImpl);
    }

    /**
     * Test FileEventDrivenJob abstract type mapping
     */
    @Test
    public void test_fileEventDrivenJob_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"agentName\":\"test\",\"jobName\":\"testJob\"}";
        FileEventDrivenJob job = objectMapper.readValue(json, FileEventDrivenJob.class);

        assertTrue("FileEventDrivenJob should be deserialized as FileEventDrivenJobImpl",
                   job instanceof FileEventDrivenJobImpl);
    }

    /**
     * Test GlobalEventJob abstract type mapping
     */
    @Test
    public void test_globalEventJob_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"agentName\":\"test\",\"jobName\":\"testJob\"}";
        GlobalEventJob job = objectMapper.readValue(json, GlobalEventJob.class);

        assertTrue("GlobalEventJob should be deserialized as GlobalEventJobImpl",
                   job instanceof GlobalEventJobImpl);
    }

    /**
     * Test ContextProfileRecord abstract type mapping
     */
    @Test
    public void test_contextProfileRecord_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"contextName\":\"test\"}";
        ContextProfileRecord record = objectMapper.readValue(json, ContextProfileRecord.class);

        assertTrue("ContextProfileRecord should be deserialized as ContextProfileRecordImpl",
                   record instanceof ContextProfileRecordImpl);
    }

    /**
     * Test ContextProfile abstract type mapping
     */
    @Test
    public void test_contextProfile_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"contextName\":\"test\"}";
        ContextProfile profile = objectMapper.readValue(json, ContextProfile.class);

        assertTrue("ContextProfile should be deserialized as ContextProfileImpl",
                   profile instanceof ContextProfileImpl);
    }

    /**
     * Test JobLock abstract type mapping
     */
    @Test
    public void test_jobLock_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"lockName\":\"test\"}";
        JobLock jobLock = objectMapper.readValue(json, JobLock.class);

        assertTrue("JobLock should be deserialized as JobLockImpl",
                   jobLock instanceof JobLockImpl);
    }

    /**
     * Test JobLockInstance abstract type mapping
     */
    @Test
    public void test_jobLockInstance_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"id\":\"test-id\"}";
        JobLockInstance jobLockInstance = objectMapper.readValue(json, JobLockInstance.class);

        assertTrue("JobLockInstance should be deserialized as JobLockInstanceImpl",
                   jobLockInstance instanceof JobLockInstanceImpl);
    }

    /**
     * Test JobLockHolder abstract type mapping
     */
    @Test
    public void test_jobLockHolder_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"contextInstanceId\":\"test-id\"}";
        JobLockHolder holder = objectMapper.readValue(json, JobLockHolder.class);

        assertTrue("JobLockHolder should be deserialized as JobLockHolderImpl",
                   holder instanceof JobLockHolderImpl);
    }

    /**
     * Test ReplacementPair abstract type mapping
     */
    @Test
    public void test_replacementPair_abstractTypeMapping() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String json = "{\"search\":\"test\",\"replace\":\"value\"}";
        ReplacementPair pair = objectMapper.readValue(json, ReplacementPair.class);

        assertTrue("ReplacementPair should be deserialized as ReplacementPairImpl",
                   pair instanceof ReplacementPairImpl);
    }

    /**
     * Test combined NON_NULL and NON_EMPTY behavior
     */
    @Test
    public void test_combined_nonNull_nonEmpty_behavior() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        TestObjectWithCollections obj = new TestObjectWithCollections();
        obj.setNullValue(null);
        obj.setEmptyList(new ArrayList<>());
        obj.setEmptyMap(new HashMap<>());
        obj.setEmptySet(new HashSet<>());
        obj.setPopulatedList(new ArrayList<>(Arrays.asList("item")));

        String json = objectMapper.writeValueAsString(obj);

        assertFalse("JSON should not contain null value", json.contains("nullValue"));
        assertFalse("JSON should not contain empty list", json.contains("emptyList"));
        assertFalse("JSON should not contain empty map", json.contains("emptyMap"));
        assertFalse("JSON should not contain empty set", json.contains("emptySet"));
        assertTrue("JSON should contain populated list", json.contains("populatedList"));
    }

    /**
     * Test that standard collections (ArrayList, HashMap, HashSet) are used instead of concurrent ones
     */
    @Test
    public void test_standard_collections_not_concurrent() throws JsonProcessingException {
        ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

        String listJson = "[\"item1\"]";
        String mapJson = "{\"key\":\"value\"}";
        String setJson = "[\"item1\"]";

        List<?> list = objectMapper.readValue(listJson, List.class);
        Map<?, ?> map = objectMapper.readValue(mapJson, Map.class);
        Set<?> set = objectMapper.readValue(setJson, Set.class);

        assertTrue("List should be ArrayList, not CopyOnWriteArrayList", list instanceof ArrayList);
        assertTrue("Map should be HashMap, not ConcurrentHashMap", map instanceof HashMap);
        assertTrue("Set should be HashSet, not CopyOnWriteArraySet", set instanceof HashSet);
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
