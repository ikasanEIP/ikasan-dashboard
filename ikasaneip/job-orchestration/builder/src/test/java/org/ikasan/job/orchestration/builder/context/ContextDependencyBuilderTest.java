package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.LogicalGroupingImpl;
import org.ikasan.spec.scheduled.context.model.ContextDependency;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.junit.Assert;
import org.junit.Test;

public class ContextDependencyBuilderTest {

    @Test
    public void test_build_with_context_identifier() {
        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withContextIdentifier("context1")
            .build();

        Assert.assertNotNull(contextDependency);
        Assert.assertEquals("context1", contextDependency.getContextIdentifier());
    }

    @Test
    public void test_build_with_context_dependency_name() {
        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withContextDependencyName("dependency1")
            .build();

        Assert.assertNotNull(contextDependency);
        Assert.assertEquals("dependency1", contextDependency.getContextDependencyName());
    }

    @Test
    public void test_build_with_logical_grouping() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(contextDependency);
        Assert.assertSame(grouping, contextDependency.getLogicalGrouping());
    }

    @Test
    public void test_build_complete_configuration() {
        LogicalGrouping grouping = new LogicalGroupingImpl();

        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withContextIdentifier("context1")
            .withContextDependencyName("dependency1")
            .withLogicalGrouping(grouping)
            .build();

        Assert.assertNotNull(contextDependency);
        Assert.assertEquals("context1", contextDependency.getContextIdentifier());
        Assert.assertEquals("dependency1", contextDependency.getContextDependencyName());
        Assert.assertSame(grouping, contextDependency.getLogicalGrouping());
    }

    @Test
    public void test_build_minimal() {
        ContextDependency contextDependency = new ContextDependencyBuilder().build();

        Assert.assertNotNull(contextDependency);
        Assert.assertNull(contextDependency.getContextIdentifier());
        Assert.assertNull(contextDependency.getContextDependencyName());
        Assert.assertNull(contextDependency.getLogicalGrouping());
    }

    @Test
    public void test_build_with_null_context_identifier() {
        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withContextIdentifier(null)
            .build();

        Assert.assertNotNull(contextDependency);
        Assert.assertNull(contextDependency.getContextIdentifier());
    }

    @Test
    public void test_build_with_null_dependency_name() {
        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withContextDependencyName(null)
            .build();

        Assert.assertNotNull(contextDependency);
        Assert.assertNull(contextDependency.getContextDependencyName());
    }

    @Test
    public void test_build_with_null_logical_grouping() {
        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withLogicalGrouping(null)
            .build();

        Assert.assertNotNull(contextDependency);
        Assert.assertNull(contextDependency.getLogicalGrouping());
    }

    @Test
    public void test_method_chaining() {
        ContextDependencyBuilder builder = new ContextDependencyBuilder();

        ContextDependencyBuilder result1 = builder.withContextIdentifier("context1");
        Assert.assertSame(builder, result1);

        ContextDependencyBuilder result2 = builder.withContextDependencyName("dependency1");
        Assert.assertSame(builder, result2);

        ContextDependencyBuilder result3 = builder.withLogicalGrouping(new LogicalGroupingImpl());
        Assert.assertSame(builder, result3);
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        ContextDependencyBuilder builder = new ContextDependencyBuilder()
            .withContextIdentifier("context1")
            .withContextDependencyName("dependency1");

        ContextDependency dependency1 = builder.build();
        ContextDependency dependency2 = builder.build();

        Assert.assertEquals(dependency1.getContextIdentifier(), dependency2.getContextIdentifier());
        Assert.assertEquals(dependency1.getContextDependencyName(), dependency2.getContextDependencyName());
    }

    @Test
    public void test_overwrite_context_identifier() {
        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withContextIdentifier("context1")
            .withContextIdentifier("context2")
            .build();

        Assert.assertEquals("context2", contextDependency.getContextIdentifier());
    }

    @Test
    public void test_overwrite_dependency_name() {
        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withContextDependencyName("dependency1")
            .withContextDependencyName("dependency2")
            .build();

        Assert.assertEquals("dependency2", contextDependency.getContextDependencyName());
    }

    @Test
    public void test_overwrite_logical_grouping() {
        LogicalGrouping grouping1 = new LogicalGroupingImpl();
        LogicalGrouping grouping2 = new LogicalGroupingImpl();

        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withLogicalGrouping(grouping1)
            .withLogicalGrouping(grouping2)
            .build();

        Assert.assertSame(grouping2, contextDependency.getLogicalGrouping());
    }

    @Test
    public void test_with_empty_context_identifier() {
        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withContextIdentifier("")
            .build();

        Assert.assertEquals("", contextDependency.getContextIdentifier());
    }

    @Test
    public void test_with_empty_dependency_name() {
        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withContextDependencyName("")
            .build();

        Assert.assertEquals("", contextDependency.getContextDependencyName());
    }

    @Test
    public void test_with_special_characters() {
        ContextDependency contextDependency = new ContextDependencyBuilder()
            .withContextIdentifier("context@#$")
            .withContextDependencyName("dependency!%^")
            .build();

        Assert.assertEquals("context@#$", contextDependency.getContextIdentifier());
        Assert.assertEquals("dependency!%^", contextDependency.getContextDependencyName());
    }
}
