package org.ikasan.testharness.config;

import java.util.List;

/**
 * Typed view of the harness configuration (config/harness.default.yaml, layered by {@link ConfigLoader}).
 *
 * @param env          name of the active environment (config/environments/&lt;env&gt;.yaml)
 * @param dashboard    the dashboard under test
 * @param backend      datastore the dashboard runs on: {@code mongo} or {@code solr}
 * @param mongo        Mongo connection, used by the seeder
 * @param solr         Solr connection, used by the seeder
 * @param compose      the docker compose stack the harness can run itself
 * @param seed         baseline dataset settings
 * @param expectations where externalised expectations are read from
 * @param run          browser and test selection settings
 */
public record HarnessConfig(
    String env,
    Dashboard dashboard,
    Backend backend,
    Mongo mongo,
    Solr solr,
    Compose compose,
    Seed seed,
    Scheduler scheduler,
    Expectations expectations,
    Run run)
{
    public enum Backend
    {
        mongo, solr;

        public Backend other()
        {
            return this == mongo ? solr : mongo;
        }
    }

    public record Dashboard(String url, String username, String password, String restUsername, String restPassword,
                            int startupTimeoutSeconds) {}

    public record Mongo(String uri, String database, String collection, boolean applyIndexes, boolean atlasSearch) {}

    public record Solr(String url, String core, String username, String password) {}

    public record Compose(boolean enabled, String file, String project, boolean teardown, String dashboardJar) {}

    /**
     * @param enabled       wipe the datastore and load the dataset before the run
     * @param dataset       dataset name from data/baseline/manifest.yaml
     * @param clockAnchor   ISO-8601 instant time tokens resolve against; empty means the run start
     * @param preserveTypes document types (or ids) the wipe leaves alone
     */
    public record Seed(boolean enabled, String dataset, String clockAnchor, List<String> preserveTypes) {}

    /**
     * @param agent         fake | real | none - who runs the command jobs of job plan instances
     * @param agentName     the scheduler agent's name (job plans refer to it)
     * @param fakeAgentPort port of the in-JVM fake agent
     * @param fakeAgentHost host name the dashboard container uses to reach the harness JVM
     * @param realAgentUrl  url the dashboard calls the real agent on (the compose service, in real mode)
     * @param realAgentJar  the scheduler-agent Spring Boot jar the real agent container runs
     */
    public record Scheduler(String agent, String agentName, int fakeAgentPort, String fakeAgentHost,
                            String realAgentUrl, String realAgentJar)
    {
        public boolean fake()
        {
            return "fake".equalsIgnoreCase(agent);
        }

        public boolean real()
        {
            return "real".equalsIgnoreCase(agent);
        }

        /** The url the dashboard calls the fake agent on. */
        public String fakeAgentUrl()
        {
            return "http://" + fakeAgentHost + ":" + fakeAgentPort + "/scheduler-agent";
        }
    }

    public record Expectations(List<String> paths) {}

    /**
     * @param browser               chromium | firefox | webkit
     * @param headless              run the browser headless
     * @param slowMoMillis          slow every Playwright action down (handy when watching a headed run)
     * @param timeoutSeconds        default Playwright action/navigation timeout
     * @param expectTimeoutSeconds  default timeout for web-first assertions
     * @param include               only run tests/scenarios with at least one of these tags (empty = all)
     * @param exclude               skip tests/scenarios with any of these tags
     * @param traceOnFailure        keep a Playwright trace + screenshot for failed tests
     * @param timezone              browser and time-token time zone; empty = the system default
     */
    public record Run(String browser, boolean headless, int slowMoMillis, int timeoutSeconds, int expectTimeoutSeconds,
                      List<String> include, List<String> exclude, boolean traceOnFailure, String timezone)
    {
        /** The configured time zone, or the system default. */
        public java.time.ZoneId zone()
        {
            return timezone == null || timezone.isBlank() ? java.time.ZoneId.systemDefault() : java.time.ZoneId.of(timezone);
        }
    }
}
