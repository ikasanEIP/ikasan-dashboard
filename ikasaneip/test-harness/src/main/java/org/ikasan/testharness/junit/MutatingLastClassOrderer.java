package org.ikasan.testharness.junit;

import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.api.Tag;

import java.util.Comparator;

/**
 * Runs test classes tagged {@code mutating} after all others, so tests that change data cannot affect the
 * read-only tests' view of the baseline. Registered in src/test/resources/junit-platform.properties.
 */
public class MutatingLastClassOrderer implements ClassOrderer
{
    @Override
    public void orderClasses(ClassOrdererContext context)
    {
        context.getClassDescriptors().sort(Comparator
            .comparing((ClassDescriptor d) -> d.findRepeatableAnnotations(Tag.class).stream()
                .anyMatch(t -> t.value().equalsIgnoreCase("mutating")))
            .thenComparing(d -> d.getTestClass().getName()));
    }
}
