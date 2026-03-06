package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.LogicalGroupingImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Not;
import org.junit.Assert;
import org.junit.Test;

public class ContextNotBuilderTest {

    @Test
    public void test_build_with_identifier() {
        Not not = new ContextNotBuilder()
            .withIdentifier("context1")
            .build();

        Assert.assertNotNull(not);
        Assert.assertEquals("context1", not.getIdentifier());
    }

    @Test
    public void test_build_with_logical_grouping() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        Not not = new ContextNotBuilder()
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(not);
        Assert.assertSame(grouping, not.getLogicalGrouping());
    }

    @Test
    public void test_build_minimal() {
        Not not = new ContextNotBuilder().build();

        Assert.assertNotNull(not);
        Assert.assertNull(not.getIdentifier());
        Assert.assertNull(not.getLogicalGrouping());
    }

    @Test
    public void test_build_with_null_identifier() {
        Not not = new ContextNotBuilder()
            .withIdentifier(null)
            .build();

        Assert.assertNotNull(not);
        Assert.assertNull(not.getIdentifier());
    }

    @Test
    public void test_build_with_null_logical_grouping() {
        Not not = new ContextNotBuilder()
            .withLogicalGrouping(null)
            .build();

        Assert.assertNotNull(not);
        Assert.assertNull(not.getLogicalGrouping());
    }

    @Test
    public void test_method_chaining() {
        ContextNotBuilder builder = new ContextNotBuilder();

        ContextNotBuilder result1 = builder.withIdentifier("context1");
        Assert.assertSame(builder, result1);

        ContextNotBuilder result2 = builder.withLogicalGrouping(new LogicalGroupingImpl());
        Assert.assertSame(builder, result2);
    }

    @Test
    public void test_build_complete_configuration() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        Not not = new ContextNotBuilder()
            .withIdentifier("context1")
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(not);
        Assert.assertEquals("context1", not.getIdentifier());
        Assert.assertSame(grouping, not.getLogicalGrouping());
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        ContextNotBuilder builder = new ContextNotBuilder()
            .withIdentifier("context1");

        Not not1 = builder.build();
        Not not2 = builder.build();

        Assert.assertEquals(not1.getIdentifier(), not2.getIdentifier());
    }

    @Test
    public void test_overwrite_identifier() {
        Not not = new ContextNotBuilder()
            .withIdentifier("context1")
            .withIdentifier("context2")
            .build();

        Assert.assertEquals("context2", not.getIdentifier());
    }

    @Test
    public void test_overwrite_logical_grouping() {
        LogicalGrouping grouping1 = new LogicalGroupingImpl();
        LogicalGrouping grouping2 = new LogicalGroupingImpl();

        Not not = new ContextNotBuilder()
            .withLogicalGrouping(grouping1)
            .withLogicalGrouping(grouping2)
            .build();

        Assert.assertSame(grouping2, not.getLogicalGrouping());
    }

    @Test
    public void test_with_empty_identifier() {
        Not not = new ContextNotBuilder()
            .withIdentifier("")
            .build();

        Assert.assertEquals("", not.getIdentifier());
    }

    @Test
    public void test_with_special_characters_in_identifier() {
        Not not = new ContextNotBuilder()
            .withIdentifier("context@#$%^")
            .build();

        Assert.assertEquals("context@#$%^", not.getIdentifier());
    }

    @Test
    public void test_with_long_identifier() {
        String longId = "a".repeat(1000);
        Not not = new ContextNotBuilder()
            .withIdentifier(longId)
            .build();

        Assert.assertEquals(longId, not.getIdentifier());
    }
}
