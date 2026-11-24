package org.ikasan.testharness.junit;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.ikasan.testharness.config.ConfigLoader;
import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.pages.LoginPage;
import org.ikasan.testharness.scheduler.FakeSchedulerAgent;
import org.ikasan.testharness.scheduler.SchedulerApi;
import org.ikasan.testharness.support.DashboardApi;
import org.ikasan.testharness.seed.Clock;
import org.ikasan.testharness.support.Baseline;
import org.ikasan.testharness.support.Compose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * State shared by every test in the run: the configuration, the baseline (established once), the Playwright
 * browser and the logged-in session that tests reuse. Closed by JUnit when the run ends.
 * <p>
 * {@code HARNESS_SKIP_BASELINE=true} (or {@code -Dharness.skipBaseline=true}) reuses whatever is running, which
 * is handy while writing tests.
 */
public final class HarnessSession implements AutoCloseable
{
    private static final Logger logger = LoggerFactory.getLogger(HarnessSession.class);
    private static volatile HarnessSession current;

    private final HarnessConfig config;
    private final Clock clock;
    private final Map<String, Integer> seeded;
    private final Playwright playwright;
    private final Browser browser;
    private final String authenticatedState;
    private final SchedulerApi schedulerApi;
    private final FakeSchedulerAgent fakeAgent;

    HarnessSession()
    {
        this.config = ConfigLoader.get();
        logger.info("harness env '{}', backend {}, dashboard {}", config.env(), config.backend(), config.dashboard().url());

        boolean skipBaseline = "true".equalsIgnoreCase(System.getenv("HARNESS_SKIP_BASELINE"))
            || Boolean.getBoolean("harness.skipBaseline");
        if (skipBaseline)
        {
            logger.info("HARNESS_SKIP_BASELINE set: using the running environment as is");
            // reuse the anchor the running data was seeded with, so exact timestamps in expectations still match
            this.clock = Baseline.seededClock(config);
            logger.info("time tokens anchored at {}", java.time.Instant.ofEpochMilli(clock.anchor()));
            this.seeded = Map.of();
        }
        else
        {
            Baseline.Result result = Baseline.establish(config);
            this.clock = result.clock();
            this.seeded = result.seeded();
        }

        // Scheduler agent: it must be registered before any job plan instance exists, because an instance captures
        // the agents' urls when it is built.
        this.schedulerApi = new SchedulerApi(new DashboardApi(config));
        HarnessConfig.Scheduler scheduler = config.scheduler();
        if (!scheduler.real() && config.compose().enabled())
        {
            new Compose(config).removeAgent();
        }
        if (scheduler.fake())
        {
            this.fakeAgent = new FakeSchedulerAgent(scheduler.agentName(), scheduler.fakeAgentPort(),
                config.dashboard().restUsername(), config.dashboard().restPassword(), new DashboardApi(config)).start();
            schedulerApi.registerAgent(scheduler.agentName(), scheduler.fakeAgentUrl(), "Test harness fake scheduler agent");
            logger.info("fake scheduler agent '{}' registered at {}", scheduler.agentName(), scheduler.fakeAgentUrl());
        }
        else
        {
            this.fakeAgent = null;
            if (scheduler.real())
            {
                if (config.compose().enabled() && !skipBaseline)
                {
                    new Compose(config).upAgent();
                }
                // the agent registers itself (with its flows) on start; wait for that rather than overwrite it
                schedulerApi.awaitAgentRegistered(scheduler.agentName(), scheduler.realAgentUrl(), java.time.Duration.ofMinutes(2));
                logger.info("real scheduler agent '{}' registered at {}", scheduler.agentName(), scheduler.realAgentUrl());
            }
        }

        PlaywrightAssertions.setDefaultAssertionTimeout(config.run().expectTimeoutSeconds() * 1000.0);
        this.playwright = Playwright.create();
        BrowserType type = switch (config.run().browser())
        {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
        this.browser = type.launch(new BrowserType.LaunchOptions()
            .setHeadless(config.run().headless())
            .setSlowMo(config.run().slowMoMillis()));
        this.authenticatedState = login();
        current = this;
    }

    /**
     * The session of this run. Available once the first {@link HarnessTest} class has started, which includes
     * static {@code @MethodSource} factories (JUnit calls them after the extension's beforeAll).
     */
    public static HarnessSession current()
    {
        if (current == null)
        {
            throw new IllegalStateException("No harness session yet - is the test class annotated with @HarnessTest?");
        }
        return current;
    }

    /** Log in through the UI once; tests start from this session instead of logging in themselves. */
    private String login()
    {
        try (BrowserContext context = newContext(null))
        {
            Page page = context.newPage();
            LoginPage loginPage = new LoginPage(page).open();
            loginPage.login(config.dashboard().username(), config.dashboard().password());
            page.waitForURL(url -> !url.contains("/login"));
            return context.storageState();
        }
    }

    /**
     * A new browser context with the harness settings (base url, viewport, timeouts). Pass null for a context
     * that is not logged in. The caller closes it.
     */
    public BrowserContext newContext(String storageState)
    {
        Browser.NewContextOptions options = new Browser.NewContextOptions()
            .setBaseURL(config.dashboard().url())
            .setViewportSize(1600, 1000)
            .setAcceptDownloads(true)
            // the dashboard shows times in the browser's zone (captured at login); pin it so expectations are exact
            .setTimezoneId(config.run().zone().getId());
        if (storageState != null)
        {
            options.setStorageState(storageState);
        }
        BrowserContext context = browser.newContext(options);
        context.setDefaultTimeout(config.run().timeoutSeconds() * 1000.0);
        context.setDefaultNavigationTimeout(config.run().timeoutSeconds() * 1000.0);
        return context;
    }

    public HarnessConfig config()
    {
        return config;
    }

    /** The clock the dataset was seeded with: resolve expectation time tokens with it. */
    public Clock clock()
    {
        return clock;
    }

    public Map<String, Integer> seeded()
    {
        return seeded;
    }

    String authenticatedState()
    {
        return authenticatedState;
    }

    /** The dashboard's scheduler REST api. */
    public SchedulerApi schedulerApi()
    {
        return schedulerApi;
    }

    /** The in-JVM fake scheduler agent, when {@code scheduler.agent} is {@code fake}. */
    public java.util.Optional<FakeSchedulerAgent> fakeAgent()
    {
        return java.util.Optional.ofNullable(fakeAgent);
    }

    @Override
    public void close()
    {
        current = null;
        try
        {
            if (fakeAgent != null)
            {
                fakeAgent.close();
            }
            browser.close();
            playwright.close();
        }
        finally
        {
            if (config.compose().enabled() && config.compose().teardown())
            {
                logger.info("tearing down the compose stack");
                new Compose(config).down();
            }
        }
    }
}
