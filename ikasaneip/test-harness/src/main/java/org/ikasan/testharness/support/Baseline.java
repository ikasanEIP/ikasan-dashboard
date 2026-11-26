package org.ikasan.testharness.support;

import org.ikasan.testharness.config.HarnessConfig;
import org.ikasan.testharness.seed.Clock;
import org.ikasan.testharness.seed.Dataset;
import org.ikasan.testharness.seed.Seeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Brings the environment to the baseline state:
 * <ol>
 *     <li>start the datastore (compose)</li>
 *     <li>wipe it, load the dataset and verify the count per type</li>
 *     <li>(re)start the dashboard so its in-memory caches - and the security baseline it creates when the
 *     {@code dashboardPlatformSetup} marker is missing - load from the fresh store</li>
 *     <li>wait for {@code /actuator/health}</li>
 * </ol>
 */
public final class Baseline
{
    private static final Logger logger = LoggerFactory.getLogger(Baseline.class);

    /**
     * @param clock  the clock the dataset's time tokens were resolved with; expectations must use the same
     * @param seeded documents seeded per type (empty when seeding is disabled)
     */
    public record Result(Clock clock, Map<String, Integer> seeded) {}

    /** Where the clock anchor of the last seed is kept, so a later run against the same data can reuse it. */
    public static final String ANCHOR_FILE = "target/harness-clock-anchor";

    private Baseline() {}

    /**
     * The clock the current data was seeded with: the anchor recorded by the last seed if there is one,
     * else the configured anchor (or now). Used when the baseline is skipped, so exact timestamps still line up.
     */
    public static Clock seededClock(HarnessConfig config)
    {
        java.nio.file.Path file = org.ikasan.testharness.config.ConfigLoader.resolve(ANCHOR_FILE);
        if (config.seed().clockAnchor().isBlank() && java.nio.file.Files.exists(file))
        {
            try
            {
                return Clock.anchoredAt(java.nio.file.Files.readString(file).trim(), config.run().zone());
            }
            catch (java.io.IOException e)
            {
                logger.warn("cannot read {}, using now", file, e);
            }
        }
        return Clock.anchoredAt(config.seed().clockAnchor(), config.run().zone());
    }

    public static Result establish(HarnessConfig config)
    {
        Clock clock = Clock.anchoredAt(config.seed().clockAnchor(), config.run().zone());
        Compose compose = new Compose(config);
        Map<String, Integer> seeded = Map.of();

        if (config.compose().enabled())
        {
            logger.info("starting {} datastore", config.backend());
            compose.upStore();
        }
        if (DashboardProcess.enabled(config))
        {
            // the datastore is already running; the dashboard caches it, so it is (re)started after seeding
            DashboardProcess.stop();
        }
        if (config.seed().enabled())
        {
            Dataset dataset = Dataset.load(config.seed().dataset(), clock);
            logger.info("seeding dataset '{}' ({} documents) into {}, clock anchor {}", dataset.name(),
                dataset.documents().size(), config.backend(), Instant.ofEpochMilli(clock.anchor()));
            try (Seeder seeder = Seeder.create(config))
            {
                seeded = Seeder.seedAndVerify(seeder, dataset, config.seed().preserveTypes());
            }
            logger.info("seeded {}", seeded);
            try
            {
                java.nio.file.Path file = org.ikasan.testharness.config.ConfigLoader.resolve(ANCHOR_FILE);
                java.nio.file.Files.createDirectories(file.getParent());
                java.nio.file.Files.writeString(file, Instant.ofEpochMilli(clock.anchor()).toString());
            }
            catch (java.io.IOException e)
            {
                logger.warn("cannot record the clock anchor in {}", ANCHOR_FILE, e);
            }
        }
        if (config.compose().enabled())
        {
            logger.info("starting dashboard");
            compose.restartDashboard();
        }
        else if (DashboardProcess.enabled(config))
        {
            DashboardProcess.start(config);
        }
        logger.info("waiting for dashboard at {}", config.dashboard().url());
        try
        {
            DashboardApi.waitForDashboard(config);
        }
        catch (RuntimeException e)
        {
            if (DashboardProcess.enabled(config) && !config.compose().enabled())
            {
                logger.error("dashboard log tail:\n{}", DashboardProcess.logTail(150));
            }
            else if (config.compose().enabled())
            {
                try
                {
                    logger.error("dashboard log tail:\n{}", compose.logs("dashboard-" + config.backend(), 150));
                }
                catch (RuntimeException ignored)
                {
                    // best effort
                }
            }
            throw e;
        }
        logger.info("dashboard is up");
        return new Result(clock, seeded);
    }
}
