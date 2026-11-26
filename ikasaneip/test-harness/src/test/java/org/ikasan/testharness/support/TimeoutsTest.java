package org.ikasan.testharness.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeoutsTest
{
    @Test
    void aConfiguredScaleWins()
    {
        assertThat(Timeouts.resolve(4.0, "true")).isEqualTo(4.0);
        assertThat(Timeouts.resolve(1.0, "true")).isEqualTo(1.0);
    }

    @Test
    void unsetIsAutomaticOnCiServers()
    {
        assertThat(Timeouts.resolve(0, "true")).isEqualTo(Timeouts.CI_SCALE);
        assertThat(Timeouts.resolve(0, null)).isEqualTo(1.0);
        assertThat(Timeouts.resolve(0, "false")).isEqualTo(1.0);
    }
}
