package org.ikasan.testharness.config;

import org.ikasan.testharness.config.HarnessConfig.Backend;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs against the real config/ files of the module. */
class ConfigLoaderTest
{
    @Test
    void defaultsToComposeMongo()
    {
        HarnessConfig config = ConfigLoader.load(Map.of(), new Properties());
        assertThat(config.env()).isEqualTo("compose-mongo");
        assertThat(config.backend()).isEqualTo(Backend.mongo);
        assertThat(config.compose().enabled()).isTrue();
        assertThat(config.seed().enabled()).isTrue();
        assertThat(config.expectations().paths()).containsExactly("expectations", "expectations/env/compose-mongo");
    }

    /** A per-backend suite names its environment, which wins over HARNESS_ENV / -Dharness.env (e.g. "all"). */
    @Test
    void anExplicitEnvironmentWinsOverTheProcessEnvironment()
    {
        Properties props = new Properties();
        props.setProperty("harness.env", "all");

        HarnessConfig solr = ConfigLoader.load("compose-solr", Map.of("HARNESS_ENV", "all"), props);
        HarnessConfig mongo = ConfigLoader.load("compose-mongo", Map.of("HARNESS_ENV", "all"), props);

        assertThat(solr.env()).isEqualTo("compose-solr");
        assertThat(solr.backend()).isEqualTo(HarnessConfig.Backend.solr);
        assertThat(mongo.env()).isEqualTo("compose-mongo");
        assertThat(mongo.backend()).isEqualTo(HarnessConfig.Backend.mongo);
    }

    @Test
    void environmentFileIsLayeredOverTheDefaults()
    {
        HarnessConfig config = ConfigLoader.load(Map.of("HARNESS_ENV", "compose-solr"), new Properties());
        assertThat(config.backend()).isEqualTo(Backend.solr);

        HarnessConfig external = ConfigLoader.load(Map.of("HARNESS_ENV", "external"), new Properties());
        assertThat(external.compose().enabled()).isFalse();
        assertThat(external.seed().enabled()).isFalse();
        assertThat(external.seed().preserveTypes()).contains("securityUser", "dashboardPlatformSetup");
    }

    @Test
    void environmentVariablesOverrideWithTypeCoercion()
    {
        HarnessConfig config = ConfigLoader.load(Map.of(
            "HARNESS_DASHBOARD_URL", "http://example:1234",
            "HARNESS_BACKEND", "solr",
            "HARNESS_RUN_EXPECT_TIMEOUT_SECONDS", "42",
            "HARNESS_RUN_HEADLESS", "false",
            "HARNESS_RUN_INCLUDE", "smoke, search",
            "HARNESS_SEED_PRESERVETYPES", ""), new Properties());
        assertThat(config.dashboard().url()).isEqualTo("http://example:1234");
        assertThat(config.backend()).isEqualTo(Backend.solr);
        assertThat(config.run().expectTimeoutSeconds()).isEqualTo(42);
        assertThat(config.run().headless()).isFalse();
        assertThat(config.run().include()).isEqualTo(List.of("smoke", "search"));
        assertThat(config.seed().preserveTypes()).isEmpty();
    }

    @Test
    void systemPropertiesOverrideAndEnvironmentVariablesWin()
    {
        Properties props = new Properties();
        props.setProperty("harness.env", "compose-solr");
        props.setProperty("harness.run.timeoutSeconds", "7");
        props.setProperty("harness.dashboard.url", "http://from-property");
        HarnessConfig config = ConfigLoader.load(Map.of("HARNESS_DASHBOARD_URL", "http://from-env"), props);
        assertThat(config.env()).isEqualTo("compose-solr");
        assertThat(config.run().timeoutSeconds()).isEqualTo(7);
        assertThat(config.dashboard().url()).isEqualTo("http://from-env");
    }

    @Test
    void unknownEnvironmentFails()
    {
        assertThatThrownBy(() -> ConfigLoader.load(Map.of("HARNESS_ENV", "nope"), new Properties()))
            .hasMessageContaining("Unknown harness environment 'nope'");
    }

    /** -Dharness.env=all -Dharness.stack=local: the per-backend suites run on the docker-free environments. */
    @Test
    void aStackRenamesTheComposeEnvironments()
    {
        assertThat(ConfigLoader.onStack("compose-solr", "local")).isEqualTo("local-solr");
        assertThat(ConfigLoader.onStack("compose-mongo", "local")).isEqualTo("local-mongo");
        assertThat(ConfigLoader.onStack("compose-solr", "")).isEqualTo("compose-solr");
        assertThat(ConfigLoader.onStack("external", "local")).isEqualTo("external");
    }

    @Test
    void localEnvironmentsStartNoContainers()
    {
        for (String env : java.util.List.of("local-mongo", "local-solr"))
        {
            HarnessConfig config = ConfigLoader.load(env, java.util.Map.of(), new java.util.Properties());
            assertThat(config.compose().enabled()).as(env + " compose").isFalse();
            assertThat(config.dashboardProcess().enabled()).as(env + " dashboard process").isTrue();
            assertThat(config.scheduler().fakeAgentHost()).as(env + " fake agent host").isEqualTo("localhost");
        }
    }
}
