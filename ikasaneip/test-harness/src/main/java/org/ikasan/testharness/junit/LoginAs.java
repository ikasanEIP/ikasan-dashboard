package org.ikasan.testharness.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The test (or every test in the class) starts logged in as this persona from {@code data/personas.yaml} instead of
 * admin, e.g. {@code @LoginAs("access-reader")}. A method's annotation wins over its class's.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginAs
{
    /** The persona's name (its username). */
    String value();
}
