package org.ikasan.testharness.support;

import org.ikasan.testharness.config.ConfigLoader;

import java.time.Duration;

/**
 * Every timeout and wait in the harness goes through here, scaled by {@code run.timeoutScale} so the same tests are
 * reliable on a slow machine (a CI server) without being slow to fail locally. A scale of 0 (the default) means
 * automatic: {@value #CI_SCALE} when the environment variable {@code CI} is true (Travis and most CI servers set it),
 * otherwise 1.
 *
 * <p>Only waits are scaled, never data: time windows (e.g. "the last day") and the time stamps of seeded events stay
 * as they are.</p>
 */
public final class Timeouts
{
    static final double CI_SCALE = 3.0;

    private static volatile Double scale;

    private Timeouts() {}

    /** The factor every timeout is multiplied by. */
    public static double scale()
    {
        if (scale == null)
        {
            scale = resolve(ConfigLoader.get().run().timeoutScale(), System.getenv("CI"));
        }
        return scale;
    }

    static double resolve(double configured, String ciVariable)
    {
        if (configured > 0)
        {
            return configured;
        }
        return "true".equalsIgnoreCase(ciVariable) || "1".equals(ciVariable) ? CI_SCALE : 1.0;
    }

    /** The timeout, scaled. */
    public static Duration of(Duration timeout)
    {
        return Duration.ofMillis(Math.round(timeout.toMillis() * scale()));
    }

    public static Duration seconds(long seconds)
    {
        return of(Duration.ofSeconds(seconds));
    }

    /** A timeout in milliseconds, scaled (Playwright's options take milliseconds as a double). */
    public static double millis(double millis)
    {
        return millis * scale();
    }
}
