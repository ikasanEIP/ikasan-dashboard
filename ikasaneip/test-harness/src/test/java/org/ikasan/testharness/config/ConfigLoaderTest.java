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
}
