package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.LogicalGroupingImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Or;
import org.junit.Assert;
import org.junit.Test;

public class ContextOrBuilderTest {

    @Test
    public void test_build_with_identifier() {
        Or or = new ContextOrBuilder()
            .withIdentifier("context1")
            .build();

        Assert.assertNotNull(or);
        Assert.assertEquals("context1", or.getIdentifier());
    }

    @Test
    public void test_build_with_logical_grouping() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        Or or = new ContextOrBuilder()
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(or);
        Assert.assertSame(grouping, or.getLogicalGrouping());
    }

    @Test
    public void test_build_minimal() {
        Or or = new ContextOrBuilder().build();

        Assert.assertNotNull(or);
        Assert.assertNull(or.getIdentifier());
        Assert.assertNull(or.getLogicalGrouping());
    }

    @Test
    public void test_build_with_null_identifier() {
        Or or = new ContextOrBuilder()
            .withIdentifier(null)
            .build();

        Assert.assertNotNull(or);
        Assert.assertNull(or.getIdentifier());
    }

    @Test
    public void test_build_with_null_logical_grouping() {
        Or or = new ContextOrBuilder()
            .withLogicalGrouping(null)
            .build();

        Assert.assertNotNull(or);
        Assert.assertNull(or.getLogicalGrouping());
    }

    @Test
    public void test_method_chaining() {
        ContextOrBuilder builder = new ContextOrBuilder();

        ContextOrBuilder result1 = builder.withIdentifier("context1");
        Assert.assertSame(builder, result1);

        ContextOrBuilder result2 = builder.withLogicalGrouping(new LogicalGroupingImpl());
        Assert.assertSame(builder, result2);
    }

    @Test
    public void test_build_complete_configuration() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        Or or = new ContextOrBuilder()
            .withIdentifier("context1")
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(or);
        Assert.assertEquals("context1", or.getIdentifier());
        Assert.assertSame(grouping, or.getLogicalGrouping());
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        ContextOrBuilder builder = new ContextOrBuilder()
            .withIdentifier("context1");

        Or or1 = builder.build();
        Or or2 = builder.build();

        Assert.assertEquals(or1.getIdentifier(), or2.getIdentifier());
    }

    @Test
    public void test_overwrite_identifier() {
        Or or = new ContextOrBuilder()
            .withIdentifier("context1")
            .withIdentifier("context2")
            .build();

        Assert.assertEquals("context2", or.getIdentifier());
    }

    @Test
    public void test_overwrite_logical_grouping() {
        LogicalGrouping grouping1 = new LogicalGroupingImpl();
        LogicalGrouping grouping2 = new LogicalGroupingImpl();

        Or or = new ContextOrBuilder()
            .withLogicalGrouping(grouping1)
            .withLogicalGrouping(grouping2)
            .build();

        Assert.assertSame(grouping2, or.getLogicalGrouping());
    }

    @Test
    public void test_with_empty_identifier() {
        Or or = new ContextOrBuilder()
            .withIdentifier("")
            .build();

        Assert.assertEquals("", or.getIdentifier());
    }

    @Test
    public void test_with_special_characters_in_identifier() {
        Or or = new ContextOrBuilder()
            .withIdentifier("context@#$%^")
            .build();

        Assert.assertEquals("context@#$%^", or.getIdentifier());
    }

    @Test
    public void test_with_long_identifier() {
        String longId = "a".repeat(1000);
        Or or = new ContextOrBuilder()
            .withIdentifier(longId)
            .build();

        Assert.assertEquals(longId, or.getIdentifier());
    }
}
