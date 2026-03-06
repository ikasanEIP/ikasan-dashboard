package org.ikasan.job.orchestration.builder.context;

import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.junit.Assert;
import org.junit.Test;

public class ContextParameterBuilderTest {

    @Test
    public void test_build_with_name_and_default_value() {
        ContextParameter contextParameter = new ContextParameterBuilder()
            .withName("testParam")
            .withDefaultValue("testValue")
            .build();

        Assert.assertNotNull(contextParameter);
        Assert.assertEquals("testParam", contextParameter.getName());
        Assert.assertEquals("testValue", contextParameter.getDefaultValue());
    }

    @Test
    public void test_build_with_name_only() {
        ContextParameter contextParameter = new ContextParameterBuilder()
            .withName("testParam")
            .build();

        Assert.assertNotNull(contextParameter);
        Assert.assertEquals("testParam", contextParameter.getName());
        Assert.assertNull(contextParameter.getDefaultValue());
    }

    @Test
    public void test_build_with_default_value_only() {
        ContextParameter contextParameter = new ContextParameterBuilder()
            .withDefaultValue("testValue")
            .build();

        Assert.assertNotNull(contextParameter);
        Assert.assertNull(contextParameter.getName());
        Assert.assertEquals("testValue", contextParameter.getDefaultValue());
    }

    @Test
    public void test_build_with_no_parameters() {
        ContextParameter contextParameter = new ContextParameterBuilder()
            .build();

        Assert.assertNotNull(contextParameter);
        Assert.assertNull(contextParameter.getName());
        Assert.assertNull(contextParameter.getDefaultValue());
    }

    @Test
    public void test_build_with_null_name() {
        ContextParameter contextParameter = new ContextParameterBuilder()
            .withName(null)
            .withDefaultValue("testValue")
            .build();

        Assert.assertNotNull(contextParameter);
        Assert.assertNull(contextParameter.getName());
        Assert.assertEquals("testValue", contextParameter.getDefaultValue());
    }

    @Test
    public void test_build_with_null_default_value() {
        ContextParameter contextParameter = new ContextParameterBuilder()
            .withName("testParam")
            .withDefaultValue(null)
            .build();

        Assert.assertNotNull(contextParameter);
        Assert.assertEquals("testParam", contextParameter.getName());
        Assert.assertNull(contextParameter.getDefaultValue());
    }

    @Test
    public void test_build_with_empty_strings() {
        ContextParameter contextParameter = new ContextParameterBuilder()
            .withName("")
            .withDefaultValue("")
            .build();

        Assert.assertNotNull(contextParameter);
        Assert.assertEquals("", contextParameter.getName());
        Assert.assertEquals("", contextParameter.getDefaultValue());
    }

    @Test
    public void test_builder_method_chaining() {
        ContextParameterBuilder builder = new ContextParameterBuilder();

        ContextParameterBuilder result1 = builder.withName("testParam");
        Assert.assertSame(builder, result1);

        ContextParameterBuilder result2 = builder.withDefaultValue("testValue");
        Assert.assertSame(builder, result2);
    }

    @Test
    public void test_multiple_builds_from_same_builder() {
        ContextParameterBuilder builder = new ContextParameterBuilder()
            .withName("testParam")
            .withDefaultValue("testValue");

        ContextParameter param1 = builder.build();
        ContextParameter param2 = builder.build();

        // Each build should create a new instance
        Assert.assertNotSame(param1, param2);
        Assert.assertEquals(param1.getName(), param2.getName());
        Assert.assertEquals(param1.getDefaultValue(), param2.getDefaultValue());
    }

    @Test
    public void test_overwrite_values() {
        ContextParameterBuilder builder = new ContextParameterBuilder()
            .withName("firstParam")
            .withDefaultValue("firstValue")
            .withName("secondParam")
            .withDefaultValue("secondValue");

        ContextParameter contextParameter = builder.build();

        Assert.assertEquals("secondParam", contextParameter.getName());
        Assert.assertEquals("secondValue", contextParameter.getDefaultValue());
    }

    @Test
    public void test_build_with_special_characters() {
        ContextParameter contextParameter = new ContextParameterBuilder()
            .withName("test-param_name.123")
            .withDefaultValue("value!@#$%^&*()")
            .build();

        Assert.assertNotNull(contextParameter);
        Assert.assertEquals("test-param_name.123", contextParameter.getName());
        Assert.assertEquals("value!@#$%^&*()", contextParameter.getDefaultValue());
    }

    @Test
    public void test_build_with_long_strings() {
        String longName = "a".repeat(1000);
        String longValue = "b".repeat(1000);

        ContextParameter contextParameter = new ContextParameterBuilder()
            .withName(longName)
            .withDefaultValue(longValue)
            .build();

        Assert.assertNotNull(contextParameter);
        Assert.assertEquals(longName, contextParameter.getName());
        Assert.assertEquals(longValue, contextParameter.getDefaultValue());
    }
}
