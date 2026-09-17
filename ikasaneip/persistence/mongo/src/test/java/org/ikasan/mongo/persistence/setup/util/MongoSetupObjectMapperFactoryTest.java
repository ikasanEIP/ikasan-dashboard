package org.ikasan.mongo.persistence.setup.util;

import org.ikasan.mongo.persistence.setup.model.MongoDashboardSetupItemImpl;
import org.ikasan.spec.persistence.model.DashboardSetupItem;
import org.junit.Assert;
import org.junit.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Comprehensive unit test for MongoSetupObjectMapperFactory
 */
public class MongoSetupObjectMapperFactoryTest {

    @Test
    public void test_newInstance_returns_non_null() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();
        Assert.assertNotNull(mapper);
    }

    @Test
    public void test_newInstance_returns_configured_mapper() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();
        Assert.assertNotNull(mapper);
        // Verify it's a properly configured JsonMapper
        Assert.assertTrue(mapper instanceof JsonMapper);
    }

    @Test
    public void test_serialize_dashboard_setup_item() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl("test-task", "COMPLETE", 12345L);

        String json = mapper.writeValueAsString(item);

        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("test-task"));
        Assert.assertTrue(json.contains("COMPLETE"));
        Assert.assertTrue(json.contains("12345"));
    }

    @Test
    public void test_deserialize_dashboard_setup_item() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        String json = "{\"name\":\"test-task\",\"status\":\"COMPLETE\",\"executionTimestamp\":12345}";

        DashboardSetupItem item = mapper.readValue(json, DashboardSetupItem.class);

        Assert.assertNotNull(item);
        Assert.assertTrue(item instanceof MongoDashboardSetupItemImpl);
        Assert.assertEquals("test-task", item.getName());
        Assert.assertEquals("COMPLETE", item.getStatus());
        Assert.assertEquals(12345L, item.getExecutionTimestamp());
    }

    @Test
    public void test_serialize_list_of_items() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("item1", "COMPLETE", 1000L));
        items.add(new MongoDashboardSetupItemImpl("item2", "IN_PROGRESS", 2000L));
        items.add(new MongoDashboardSetupItemImpl("item3", "PENDING", 3000L));

        String json = mapper.writeValueAsString(items);

        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("item1"));
        Assert.assertTrue(json.contains("item2"));
        Assert.assertTrue(json.contains("item3"));
        Assert.assertTrue(json.contains("COMPLETE"));
        Assert.assertTrue(json.contains("IN_PROGRESS"));
        Assert.assertTrue(json.contains("PENDING"));
    }

    @Test
    public void test_deserialize_list_of_items() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        String json = "[" +
            "{\"name\":\"item1\",\"status\":\"COMPLETE\",\"executionTimestamp\":1000}," +
            "{\"name\":\"item2\",\"status\":\"IN_PROGRESS\",\"executionTimestamp\":2000}," +
            "{\"name\":\"item3\",\"status\":\"PENDING\",\"executionTimestamp\":3000}" +
            "]";

        List<DashboardSetupItem> items = mapper.readValue(json, new TypeReference<List<DashboardSetupItem>>() {});

        Assert.assertNotNull(items);
        Assert.assertEquals(3, items.size());
        Assert.assertTrue(items instanceof CopyOnWriteArrayList);
        Assert.assertEquals("item1", items.get(0).getName());
        Assert.assertEquals("COMPLETE", items.get(0).getStatus());
        Assert.assertEquals(1000L, items.get(0).getExecutionTimestamp());
    }

    @Test
    public void test_null_field_handling() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        item.setName("test");
        // status and timestamp are null/0

        String json = mapper.writeValueAsString(item);

        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("test"));
        // Null fields should not be included due to JsonInclude.Include.NON_NULL
        Assert.assertFalse(json.contains("\"status\":null"));
    }

    @Test
    public void test_unknown_properties_ignored() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        // JSON with unknown property
        String json = "{\"name\":\"test\",\"status\":\"COMPLETE\",\"executionTimestamp\":12345,\"unknownField\":\"value\"}";

        // Should not fail due to unknown property
        DashboardSetupItem item = mapper.readValue(json, DashboardSetupItem.class);

        Assert.assertNotNull(item);
        Assert.assertEquals("test", item.getName());
        Assert.assertEquals("COMPLETE", item.getStatus());
    }

    @Test
    public void test_list_type_mapping() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        String json = "[{\"name\":\"test\",\"status\":\"COMPLETE\",\"executionTimestamp\":123}]";

        List<DashboardSetupItem> items = mapper.readValue(json, new TypeReference<List<DashboardSetupItem>>() {});

        Assert.assertNotNull(items);
        Assert.assertTrue(items instanceof CopyOnWriteArrayList);
    }

    @Test
    public void test_map_type_mapping() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

        Map<String, String> map = mapper.readValue(json, new TypeReference<Map<String, String>>() {});

        Assert.assertNotNull(map);
        Assert.assertTrue(map instanceof ConcurrentHashMap);
        Assert.assertEquals("value1", map.get("key1"));
        Assert.assertEquals("value2", map.get("key2"));
    }

    @Test
    public void test_set_type_mapping() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        String json = "[\"value1\",\"value2\",\"value3\"]";

        Set<String> set = mapper.readValue(json, new TypeReference<Set<String>>() {});

        Assert.assertNotNull(set);
        Assert.assertTrue(set instanceof CopyOnWriteArraySet);
        Assert.assertTrue(set.contains("value1"));
        Assert.assertTrue(set.contains("value2"));
        Assert.assertTrue(set.contains("value3"));
    }

    @Test
    public void test_empty_list_serialization() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        List<DashboardSetupItem> emptyList = new ArrayList<>();
        String json = mapper.writeValueAsString(emptyList);

        Assert.assertNotNull(json);
        Assert.assertEquals("[]", json);
    }

    @Test
    public void test_empty_list_deserialization() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        String json = "[]";
        List<DashboardSetupItem> items = mapper.readValue(json, new TypeReference<List<DashboardSetupItem>>() {});

        Assert.assertNotNull(items);
        Assert.assertEquals(0, items.size());
    }

    @Test
    public void test_special_characters_in_fields() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl(
            "task-with-special_chars.test",
            "STATUS_WITH-HYPHENS",
            12345L
        );

        String json = mapper.writeValueAsString(item);
        DashboardSetupItem deserialized = mapper.readValue(json, DashboardSetupItem.class);

        Assert.assertEquals(item.getName(), deserialized.getName());
        Assert.assertEquals(item.getStatus(), deserialized.getStatus());
        Assert.assertEquals(item.getExecutionTimestamp(), deserialized.getExecutionTimestamp());
    }

    @Test
    public void test_unicode_characters() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl("タスク", "完了", 12345L);

        String json = mapper.writeValueAsString(item);
        DashboardSetupItem deserialized = mapper.readValue(json, DashboardSetupItem.class);

        Assert.assertEquals("タスク", deserialized.getName());
        Assert.assertEquals("完了", deserialized.getStatus());
    }

    @Test
    public void test_long_values() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl(
            "test",
            "COMPLETE",
            Long.MAX_VALUE
        );

        String json = mapper.writeValueAsString(item);
        DashboardSetupItem deserialized = mapper.readValue(json, DashboardSetupItem.class);

        Assert.assertEquals(Long.MAX_VALUE, deserialized.getExecutionTimestamp());
    }

    @Test
    public void test_multiple_mapper_instances_are_independent() {
        JsonMapper mapper1 = MongoSetupObjectMapperFactory.newInstance();
        JsonMapper mapper2 = MongoSetupObjectMapperFactory.newInstance();

        Assert.assertNotNull(mapper1);
        Assert.assertNotNull(mapper2);
        // They should be different instances
        Assert.assertNotSame(mapper1, mapper2);
    }

    @Test
    public void test_roundtrip_serialization() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        MongoDashboardSetupItemImpl original = new MongoDashboardSetupItemImpl(
            "roundtrip-test",
            "COMPLETE",
            System.currentTimeMillis()
        );

        String json = mapper.writeValueAsString(original);
        DashboardSetupItem deserialized = mapper.readValue(json, DashboardSetupItem.class);

        Assert.assertEquals(original.getName(), deserialized.getName());
        Assert.assertEquals(original.getStatus(), deserialized.getStatus());
        Assert.assertEquals(original.getExecutionTimestamp(), deserialized.getExecutionTimestamp());
    }

    @Test
    public void test_roundtrip_with_list() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        List<DashboardSetupItem> originalList = new ArrayList<>();
        originalList.add(new MongoDashboardSetupItemImpl("item1", "COMPLETE", 1000L));
        originalList.add(new MongoDashboardSetupItemImpl("item2", "IN_PROGRESS", 2000L));
        originalList.add(new MongoDashboardSetupItemImpl("item3", "PENDING", 3000L));

        String json = mapper.writeValueAsString(originalList);
        List<DashboardSetupItem> deserializedList = mapper.readValue(json, new TypeReference<List<DashboardSetupItem>>() {});

        Assert.assertEquals(originalList.size(), deserializedList.size());
        for (int i = 0; i < originalList.size(); i++) {
            Assert.assertEquals(originalList.get(i).getName(), deserializedList.get(i).getName());
            Assert.assertEquals(originalList.get(i).getStatus(), deserializedList.get(i).getStatus());
            Assert.assertEquals(originalList.get(i).getExecutionTimestamp(), deserializedList.get(i).getExecutionTimestamp());
        }
    }

    @Test
    public void test_null_value_in_list() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        // Note: This tests current behavior - nulls in arrays are typically preserved
        String json = "[{\"name\":\"item1\",\"status\":\"COMPLETE\",\"executionTimestamp\":1000},null]";

        List<DashboardSetupItem> items = mapper.readValue(json, new TypeReference<List<DashboardSetupItem>>() {});

        Assert.assertNotNull(items);
        Assert.assertEquals(2, items.size());
        Assert.assertNotNull(items.get(0));
        Assert.assertNull(items.get(1));
    }

    @Test
    public void test_empty_string_fields() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        String json = "{\"name\":\"\",\"status\":\"\",\"executionTimestamp\":0}";

        DashboardSetupItem item = mapper.readValue(json, DashboardSetupItem.class);

        Assert.assertNotNull(item);
        Assert.assertEquals("", item.getName());
        Assert.assertEquals("", item.getStatus());
        Assert.assertEquals(0L, item.getExecutionTimestamp());
    }

    @Test
    public void test_zero_timestamp() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl("test", "COMPLETE", 0L);

        String json = mapper.writeValueAsString(item);
        DashboardSetupItem deserialized = mapper.readValue(json, DashboardSetupItem.class);

        Assert.assertEquals(0L, deserialized.getExecutionTimestamp());
    }

    @Test
    public void test_negative_timestamp() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl("test", "COMPLETE", -1L);

        String json = mapper.writeValueAsString(item);
        DashboardSetupItem deserialized = mapper.readValue(json, DashboardSetupItem.class);

        Assert.assertEquals(-1L, deserialized.getExecutionTimestamp());
    }

    @Test
    public void test_large_list_performance() {
        JsonMapper mapper = MongoSetupObjectMapperFactory.newInstance();

        List<DashboardSetupItem> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add(new MongoDashboardSetupItemImpl("item-" + i, "COMPLETE", System.currentTimeMillis()));
        }

        // Should handle large lists without issue
        String json = mapper.writeValueAsString(largeList);
        Assert.assertNotNull(json);

        List<DashboardSetupItem> deserialized = mapper.readValue(json, new TypeReference<List<DashboardSetupItem>>() {});
        Assert.assertEquals(1000, deserialized.size());
    }
}
