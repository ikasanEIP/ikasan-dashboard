package org.ikasan.testharness.seed;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClockTest
{
    private static final long ANCHOR = Instant.parse("2026-03-10T12:34:56Z").toEpochMilli();
    private final Clock clock = new Clock(ANCHOR, ZoneOffset.UTC);

    @Test
    void wholeTokensResolveToEpochMillis()
    {
        assertThat(clock.resolveString("{{now}}")).isEqualTo(ANCHOR);
        assertThat(clock.resolveString("{{now-5m}}")).isEqualTo(ANCHOR - 300_000L);
        assertThat(clock.resolveString(" {{ now + 2d }} ")).isEqualTo(ANCHOR + 2 * 86_400_000L);
        assertThat(clock.resolveString("{{now-1w}}")).isEqualTo(ANCHOR - 604_800_000L);
    }

    @Test
    void todayUsesTheAnchorsDateAtTheGivenTime()
    {
        assertThat(clock.resolveString("{{today@09:30|iso}}")).isEqualTo("2026-03-10T09:30:00Z");
        assertThat(clock.resolveString("{{today}}")).isEqualTo(Instant.parse("2026-03-10T00:00:00Z").toEpochMilli());
    }

    @Test
    void formats()
    {
        assertThat(clock.resolveString("{{now|s}}")).isEqualTo(ANCHOR / 1000);
        assertThat(clock.resolveString("{{now-1d|date}}")).isEqualTo("2026-03-09");
        assertThat(clock.resolveString("{{now+1h|time}}")).isEqualTo("13:34");
    }

    @Test
    void tableFormatIsTheDashboardGridFormat()
    {
        assertThat(new Clock(ANCHOR, ZoneId.of("Europe/London")).resolveString("{{now-5m|table}}"))
            .isEqualTo("10/03/2026 12:29:56.000 [Europe/London - GMT]");
    }

    @Test
    void embeddedTokensAreSubstitutedAsText()
    {
        assertThat(clock.resolveString("from {{now-1d|date}} to {{now|date}}")).isEqualTo("from 2026-03-09 to 2026-03-10");
        assertThat(clock.resolveString("no tokens")).isEqualTo("no tokens");
    }

    @Test
    void resolvesNestedStructures()
    {
        Map<String, Object> resolved = clock.resolve(Map.of("a", List.of("{{now}}", 1), "b", Map.of("c", "{{now+1s}}")));
        assertThat(resolved).containsEntry("a", List.of(ANCHOR, 1)).containsEntry("b", Map.of("c", ANCHOR + 1000));
    }

    @Test
    void anchoredAtParsesIsoAndDefaultsToNow()
    {
        assertThat(Clock.anchoredAt("2026-03-10T12:34:56Z").anchor()).isEqualTo(ANCHOR);
        long before = System.currentTimeMillis();
        assertThat(Clock.anchoredAt("").anchor()).isBetween(before, System.currentTimeMillis());
    }

    @Test
    void millisRejectsNonTokens()
    {
        assertThat(new Clock(ANCHOR, ZoneId.of("UTC")).millis("{{now-1m}}")).isEqualTo(ANCHOR - 60_000);
        assertThatThrownBy(() -> clock.millis("yesterday")).isInstanceOf(IllegalArgumentException.class);
    }
}
