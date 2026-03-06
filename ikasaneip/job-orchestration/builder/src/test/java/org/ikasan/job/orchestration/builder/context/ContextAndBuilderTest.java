package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.LogicalGroupingImpl;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.junit.Assert;
import org.junit.Test;

public class ContextAndBuilderTest {

    @Test
    public void test_build_with_identifier() {
        And and = new ContextAndBuilder()
            .withIdentifier("context1")
            .build();

        Assert.assertNotNull(and);
        Assert.assertEquals("context1", and.getIdentifier());
    }

    @Test
    public void test_build_with_logical_grouping() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        And and = new ContextAndBuilder()
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(and);
        Assert.assertSame(grouping, and.getLogicalGrouping());
    }

    @Test
    public void test_build_minimal() {
        And and = new ContextAndBuilder().build();

        Assert.assertNotNull(and);
        Assert.assertNull(and.getIdentifier());
        Assert.assertNull(and.getLogicalGrouping());
    }

    @Test
    public void test_build_with_null_identifier() {
        And and = new ContextAndBuilder()
            .withIdentifier(null)
            .build();

        Assert.assertNotNull(and);
        Assert.assertNull(and.getIdentifier());
    }

    @Test
    public void test_build_with_null_logical_grouping() {
        And and = new ContextAndBuilder()
            .withLogicalGrouping(null)
            .build();

        Assert.assertNotNull(and);
        Assert.assertNull(and.getLogicalGrouping());
    }

    @Test
    public void test_method_chaining() {
        ContextAndBuilder builder = new ContextAndBuilder();

        ContextAndBuilder result1 = builder.withIdentifier("context1");
        Assert.assertSame(builder, result1);

        ContextAndBuilder result2 = builder.withLogicalGrouping(new LogicalGroupingImpl());
        Assert.assertSame(builder, result2);
    }

    @Test
    public void test_build_complete_configuration() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        And and = new ContextAndBuilder()
            .withIdentifier("context1")
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(and);
        Assert.assertEquals("context1", and.getIdentifier());
        Assert.assertSame(grouping, and.getLogicalGrouping());
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        ContextAndBuilder builder = new ContextAndBuilder()
            .withIdentifier("context1");

        And and1 = builder.build();
        And and2 = builder.build();

        Assert.assertEquals(and1.getIdentifier(), and2.getIdentifier());
    }

    @Test
    public void test_overwrite_identifier() {
        And and = new ContextAndBuilder()
            .withIdentifier("context1")
            .withIdentifier("context2")
            .build();

        Assert.assertEquals("context2", and.getIdentifier());
    }

    @Test
    public void test_overwrite_logical_grouping() {
        LogicalGrouping grouping1 = new LogicalGroupingImpl();
        LogicalGrouping grouping2 = new LogicalGroupingImpl();

        And and = new ContextAndBuilder()
            .withLogicalGrouping(grouping1)
            .withLogicalGrouping(grouping2)
            .build();

        Assert.assertSame(grouping2, and.getLogicalGrouping());
    }

    @Test
    public void test_with_empty_identifier() {
        And and = new ContextAndBuilder()
            .withIdentifier("")
            .build();

        Assert.assertEquals("", and.getIdentifier());
    }

    @Test
    public void test_with_special_characters_in_identifier() {
        And and = new ContextAndBuilder()
            .withIdentifier("context@#$%^")
            .build();

        Assert.assertEquals("context@#$%^", and.getIdentifier());
    }

    @Test
    public void test_with_long_identifier() {
        String longId = "a".repeat(1000);
        And and = new ContextAndBuilder()
            .withIdentifier(longId)
            .build();

        Assert.assertEquals(longId, and.getIdentifier());
    }
}
