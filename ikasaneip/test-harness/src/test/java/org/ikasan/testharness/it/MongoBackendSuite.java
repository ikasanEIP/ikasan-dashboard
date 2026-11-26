package org.ikasan.testharness.it;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Every end-to-end test (*IT) on Mongo. With {@code -Dharness.env=all} failsafe runs the per-backend suites instead of
 * the test classes (pom.xml, profile all-backends): MongoBackendSuite then SolrBackendSuite, in one JVM, so one
 * Reporting Labs report covers both. Each suite is a separate execution, with its own harness session (baseline, stack,
 * login, scheduler agent) for its environment.
 */
@Suite
@SuiteDisplayName("Mongo")
@SelectPackages("org.ikasan.testharness.it")
@IncludeClassNamePatterns(".*IT")
@ConfigurationParameter(key = "harness.env", value = "compose-mongo")
public class MongoBackendSuite
{
}
