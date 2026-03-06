package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.AndImpl;
import org.ikasan.job.orchestration.model.context.LogicalGroupingImpl;
import org.ikasan.job.orchestration.model.context.NotImpl;
import org.ikasan.job.orchestration.model.context.OrImpl;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Not;
import org.ikasan.spec.scheduled.context.model.Or;
import org.junit.Assert;
import org.junit.Test;

public class LogicalGroupingBuilderTest {

    @Test
    public void test_build_empty() {
        LogicalGrouping grouping = new LogicalGroupingBuilder().build();

        Assert.assertNotNull(grouping);
        Assert.assertNull(grouping.getAnd());
        Assert.assertNull(grouping.getOr());
        Assert.assertNull(grouping.getNot());
        Assert.assertNull(grouping.getLogicalGrouping());
    }

    @Test
    public void test_build_with_single_and() {
        And and = new AndImpl();
        and.setIdentifier("and1");

        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .addAnd(and)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertNotNull(grouping.getAnd());
        Assert.assertEquals(1, grouping.getAnd().size());
        Assert.assertSame(and, grouping.getAnd().get(0));
    }

    @Test
    public void test_build_with_multiple_ands() {
        And and1 = new AndImpl();
        and1.setIdentifier("and1");
        And and2 = new AndImpl();
        and2.setIdentifier("and2");

        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .addAnd(and1)
            .addAnd(and2)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertNotNull(grouping.getAnd());
        Assert.assertEquals(2, grouping.getAnd().size());
        Assert.assertTrue(grouping.getAnd().contains(and1));
        Assert.assertTrue(grouping.getAnd().contains(and2));
    }

    @Test
    public void test_build_with_single_or() {
        Or or = new OrImpl();
        or.setIdentifier("or1");

        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .addOr(or)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertNotNull(grouping.getOr());
        Assert.assertEquals(1, grouping.getOr().size());
        Assert.assertSame(or, grouping.getOr().get(0));
    }

    @Test
    public void test_build_with_multiple_ors() {
        Or or1 = new OrImpl();
        or1.setIdentifier("or1");
        Or or2 = new OrImpl();
        or2.setIdentifier("or2");

        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .addOr(or1)
            .addOr(or2)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertNotNull(grouping.getOr());
        Assert.assertEquals(2, grouping.getOr().size());
        Assert.assertTrue(grouping.getOr().contains(or1));
        Assert.assertTrue(grouping.getOr().contains(or2));
    }

    @Test
    public void test_build_with_single_not() {
        Not not = new NotImpl();
        not.setIdentifier("not1");

        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .addNot(not)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertNotNull(grouping.getNot());
        Assert.assertEquals(1, grouping.getNot().size());
        Assert.assertSame(not, grouping.getNot().get(0));
    }

    @Test
    public void test_build_with_multiple_nots() {
        Not not1 = new NotImpl();
        not1.setIdentifier("not1");
        Not not2 = new NotImpl();
        not2.setIdentifier("not2");

        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .addNot(not1)
            .addNot(not2)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertNotNull(grouping.getNot());
        Assert.assertEquals(2, grouping.getNot().size());
        Assert.assertTrue(grouping.getNot().contains(not1));
        Assert.assertTrue(grouping.getNot().contains(not2));
    }

    @Test
    public void test_build_with_nested_logical_grouping() {
        LogicalGrouping nestedGrouping = new LogicalGroupingImpl();

        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .withLogicalGrouping(nestedGrouping)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertSame(nestedGrouping, grouping.getLogicalGrouping());
    }

    @Test
    public void test_build_with_all_types() {
        And and = new AndImpl();
        Or or = new OrImpl();
        Not not = new NotImpl();
        LogicalGrouping nestedGrouping = new LogicalGroupingImpl();

        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .addAnd(and)
            .addOr(or)
            .addNot(not)
            .withLogicalGrouping(nestedGrouping)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertEquals(1, grouping.getAnd().size());
        Assert.assertEquals(1, grouping.getOr().size());
        Assert.assertEquals(1, grouping.getNot().size());
        Assert.assertSame(nestedGrouping, grouping.getLogicalGrouping());
    }

    @Test
    public void test_method_chaining() {
        LogicalGroupingBuilder builder = new LogicalGroupingBuilder();

        LogicalGroupingBuilder result1 = builder.addAnd(new AndImpl());
        Assert.assertSame(builder, result1);

        LogicalGroupingBuilder result2 = builder.addOr(new OrImpl());
        Assert.assertSame(builder, result2);

        LogicalGroupingBuilder result3 = builder.addNot(new NotImpl());
        Assert.assertSame(builder, result3);

        LogicalGroupingBuilder result4 = builder.withLogicalGrouping(new LogicalGroupingImpl());
        Assert.assertSame(builder, result4);
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        And and = new AndImpl();
        LogicalGroupingBuilder builder = new LogicalGroupingBuilder()
            .addAnd(and);

        LogicalGrouping grouping1 = builder.build();
        LogicalGrouping grouping2 = builder.build();

        Assert.assertEquals(grouping1.getAnd().size(), grouping2.getAnd().size());
    }

    @Test
    public void test_add_null_and() {
        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .addAnd(null)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertNotNull(grouping.getAnd());
        Assert.assertEquals(1, grouping.getAnd().size());
        Assert.assertNull(grouping.getAnd().get(0));
    }

    @Test
    public void test_add_null_or() {
        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .addOr(null)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertNotNull(grouping.getOr());
        Assert.assertEquals(1, grouping.getOr().size());
        Assert.assertNull(grouping.getOr().get(0));
    }

    @Test
    public void test_add_null_not() {
        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .addNot(null)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertNotNull(grouping.getNot());
        Assert.assertEquals(1, grouping.getNot().size());
        Assert.assertNull(grouping.getNot().get(0));
    }

    @Test
    public void test_with_null_logical_grouping() {
        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .withLogicalGrouping(null)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertNull(grouping.getLogicalGrouping());
    }

    @Test
    public void test_complex_nested_structure() {
        And and1 = new AndImpl();
        And and2 = new AndImpl();
        Or or1 = new OrImpl();
        Not not1 = new NotImpl();
        LogicalGrouping nestedGrouping = new LogicalGroupingImpl();

        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .addAnd(and1)
            .addAnd(and2)
            .addOr(or1)
            .addNot(not1)
            .withLogicalGrouping(nestedGrouping)
            .build();

        Assert.assertNotNull(grouping);
        Assert.assertEquals(2, grouping.getAnd().size());
        Assert.assertEquals(1, grouping.getOr().size());
        Assert.assertEquals(1, grouping.getNot().size());
        Assert.assertSame(nestedGrouping, grouping.getLogicalGrouping());
    }

    @Test
    public void test_overwrite_logical_grouping() {
        LogicalGrouping grouping1 = new LogicalGroupingImpl();
        LogicalGrouping grouping2 = new LogicalGroupingImpl();

        LogicalGrouping grouping = new LogicalGroupingBuilder()
            .withLogicalGrouping(grouping1)
            .withLogicalGrouping(grouping2)
            .build();

        Assert.assertSame(grouping2, grouping.getLogicalGrouping());
    }
}
