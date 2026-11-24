package org.ikasan.testharness.junit;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an end-to-end test class: the baseline is established once per run and each test gets a fresh,
 * logged-in browser page. See {@link HarnessExtension}.
 * <p>
 * Tag classes/methods with {@code @Tag} for selection via {@code run.include}/{@code run.exclude}; tag tests that
 * change data with {@code @Tag("mutating")} so they run after the read-only tests
 * ({@link MutatingLastClassOrderer}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(HarnessExtension.class)
public @interface HarnessTest
{
}
