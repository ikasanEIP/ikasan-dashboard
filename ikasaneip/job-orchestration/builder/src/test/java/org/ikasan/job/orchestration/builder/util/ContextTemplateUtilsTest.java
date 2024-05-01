package org.ikasan.job.orchestration.builder.util;

import org.ikasan.job.orchestration.builder.context.ContextTemplateBuilder;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContextTemplateUtilsTest {

    @Test
    public void test_ordinals_set_when_none_set() {
        List<ContextTemplate> testContextTemplates = new ArrayList<>();
        testContextTemplates.add(new ContextTemplateBuilder().withName("Context Template 1").build());
        testContextTemplates.add(new ContextTemplateBuilder().withName("Context Template 2").build());
        testContextTemplates.add(new ContextTemplateBuilder().withName("Context Template 3").build());

        List<ContextTemplate> actualSortedContextTemplates = ContextTemplateUtils.setOrdinalsInContextTemplates(testContextTemplates);

        Assert.assertEquals(testContextTemplates.size(), actualSortedContextTemplates.size());
        Assert.assertEquals("[0, 1, 2]", actualSortedContextTemplates.stream().map(ContextTemplate::getOrdinal).collect(Collectors.toUnmodifiableList()).toString());
    }

    @Test
    public void test_ordinals_set_when_some_are_set() {
        List<ContextTemplate> testContextTemplates = new ArrayList<>();
        testContextTemplates.add(new ContextTemplateBuilder().withName("Context Template 1").withOrdinal(10).build());
        testContextTemplates.add(new ContextTemplateBuilder().withName("Context Template 2").build());
        testContextTemplates.add(new ContextTemplateBuilder().withName("Context Template 3").withOrdinal(5).build());

        List<ContextTemplate> actualSortedContextTemplates = ContextTemplateUtils.setOrdinalsInContextTemplates(testContextTemplates);

        Assert.assertEquals(testContextTemplates.size(), actualSortedContextTemplates.size());
        Assert.assertEquals("[5, 10, 11]", actualSortedContextTemplates.stream().map(ContextTemplate::getOrdinal).collect(Collectors.toUnmodifiableList()).toString());
        Assert.assertEquals("[Context Template 3, Context Template 1, Context Template 2]", actualSortedContextTemplates.stream().map(ContextTemplate::getName).collect(Collectors.toUnmodifiableList()).toString());
    }
}