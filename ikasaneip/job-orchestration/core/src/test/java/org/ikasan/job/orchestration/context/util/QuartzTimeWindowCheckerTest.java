package org.ikasan.job.orchestration.context.util;

import org.junit.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class QuartzTimeWindowCheckerTest {

    // note: * * 6 means every day at 6 am
    private static final String EVERYDAY_AT_6_AM = "0 0 6 ? * * *";
    // note: * * 15 means every second during 15:00 hour on every day
    private static final String EVERY_SECOND_DURING_THE_HOUR_OF_15 = "* * 15 ? * * *";
    private static final String AT_14_30_00 = "0 30 14 ? * * *";
    private static final String AT_14_32_00 = "0 32 14 ? * * *";
    private static final String AT_14_35_00 = "0 35 14 ? * * *";

    private final ZoneId LONDON = ZoneId.of("Europe/London");
    private final ZoneId AUSTRALIA = ZoneId.of("Australia/Sydney");
    private Timestamp now;

    @Test
    public void is_within_an_operating_window_of_5_minutes() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 34, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, AT_14_30_00, new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 35, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, AT_14_30_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 36, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, AT_14_30_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 19, 14, 29, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, AT_14_30_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 19, 14, 30, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, AT_14_30_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 19, 14, 30, 1, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, AT_14_30_00, new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 19, 14, 31, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, AT_14_30_00, new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 19, 14, 34, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, AT_14_30_00, new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 19, 14, 35, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, AT_14_30_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 19, 14, 36, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, AT_14_30_00, new Date(now.getTime())), is(true));
    }

    @Test
    public void is_within_operating_window_very_small_window() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 29, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, AT_14_32_00, new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 30, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, AT_14_32_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 31, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, AT_14_32_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 32, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, AT_14_32_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 32, 0, 1));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, AT_14_32_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 32, 1, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, AT_14_32_00, new Date(now.getTime())), is(false));
    }

    @Test
    public void is_within_operating_window_very_small_window_check_winter_dates() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 12, 18, 14, 29, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, AT_14_32_00, new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 12, 18, 14, 30, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, AT_14_32_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 12, 18, 14, 31, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, AT_14_32_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 12, 18, 14, 32, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, AT_14_32_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 12, 18, 14, 32, 0, 1));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, AT_14_32_00, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 12, 18, 14, 32, 1, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, AT_14_32_00, new Date(now.getTime())), is(false));
    }

    @Test
    public void is_within_operating_window_very_small_window_with_timezone() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 29, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(AUSTRALIA.toString(), AT_14_30_00, AT_14_32_00, myDate.parse("2022-05-18T14:29:59")), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 30, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(AUSTRALIA.toString(), AT_14_30_00, AT_14_32_00, myDate.parse("2022-05-18T14:30:00")), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 31, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(AUSTRALIA.toString(), AT_14_30_00, AT_14_32_00, myDate.parse("2022-05-18T14:31:00")), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 32, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(AUSTRALIA.toString(), AT_14_30_00, AT_14_32_00, myDate.parse("2022-05-18T14:32:00")), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 32, 0, 1));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(AUSTRALIA.toString(), AT_14_30_00, AT_14_32_00, myDate.parse("2022-05-18T14:32:01")), is(false));
    }

    @Test
    public void is_within_operating_window_never_except_midnight_exactly() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 11, 59, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 0 ? * * *", "0 0 0 ? * * *", new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 0, 0, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 0 ? * * *", "0 0 0 ? * * *", new Date(now.getTime())), is(true));
    }

    @Test
    public void is_within_operating_window_every_second_all_day() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 23, 59, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "* * 0 ? * * *", "* * 23 ? * * *", new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 0, 0, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "* * 0 ? * * *", "* * 23 ? * * *", new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 12, 12, 12, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "* * 0 ? * * *", "* * 23 ? * * *", new Date(now.getTime())), is(true));
    }

    @Test
    public void is_within_operating_window_every_second() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_EVERY_SECOND_FORMAT1, AT_EVERY_SECOND_FORMAT1, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_EVERY_SECOND_FORMAT2, AT_EVERY_SECOND_FORMAT2, new Date(now.getTime())), is(true));
    }

    @Test
    public void is_within_operating_window_every_hour_on_the_hour() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 * ? * * *", "0 0 * ? * * *", new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 22, 6, 0, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 * ? * * *", "0 0 * ? * * *", new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 * * * ? *", "0 0 * * * ? *", new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 11, 7, 0, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 * * * ? *", "0 0 * * * ? *", new Date(now.getTime())), is(true));
    }

    @Test
    public void is_within_operating_window_every_hour_on_the_hour_between_3am_and_6am() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 3 ? * * *", "0 0 6 ? * * *", new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 22, 6, 0, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 3 ? * * *", "0 0 6 ? * * *", new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 1, 6, 0, 1, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 3 ? * * *", "0 0 6 ? * * *", new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 1, 6, 1, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 3 ? * * *", "0 0 6 ? * * *", new Date(now.getTime())), is(false));
    }

    @Test
    public void is_within_operating_window_6am_to_all_of_3pm() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), EVERYDAY_AT_6_AM, EVERY_SECOND_DURING_THE_HOUR_OF_15, new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 999));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), EVERYDAY_AT_6_AM, EVERY_SECOND_DURING_THE_HOUR_OF_15, new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 6, 0, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), EVERYDAY_AT_6_AM, EVERY_SECOND_DURING_THE_HOUR_OF_15, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 6, 0, 1, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), EVERYDAY_AT_6_AM, EVERY_SECOND_DURING_THE_HOUR_OF_15, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 6, 0, 0, 1));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), EVERYDAY_AT_6_AM, EVERY_SECOND_DURING_THE_HOUR_OF_15, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 59, 59, 999));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), EVERYDAY_AT_6_AM, EVERY_SECOND_DURING_THE_HOUR_OF_15, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 15, 0, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), EVERYDAY_AT_6_AM, EVERY_SECOND_DURING_THE_HOUR_OF_15, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 15, 0, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), EVERYDAY_AT_6_AM, EVERY_SECOND_DURING_THE_HOUR_OF_15, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 15, 0, 1, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), EVERYDAY_AT_6_AM, EVERY_SECOND_DURING_THE_HOUR_OF_15, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 15, 0, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), EVERYDAY_AT_6_AM, EVERY_SECOND_DURING_THE_HOUR_OF_15, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 16, 0, 0, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), EVERYDAY_AT_6_AM, EVERY_SECOND_DURING_THE_HOUR_OF_15, new Date(now.getTime())), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throws_parse_exception_invalid_expression_start() {
        now = Timestamp.valueOf(LocalDateTime.of(1970, 1, 18, 5, 59, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "* * ? * * *", "* * * ? * * *", new Date(now.getTime())), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throws_parse_exception_invalid_expression_end() {
        now = Timestamp.valueOf(LocalDateTime.of(1970, 5, 22, 5, 59, 59, 0));
        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "* * * ? * * *", "* * * * * * *", new Date(now.getTime())), is(true));
    }

    private static final String AT_12_MIDDAY = "0 0 12 * * ? *";
    private static final String AT_EVERY_SECOND_FORMAT1 = "* * * * * ? *";
    private static final String AT_EVERY_SECOND_FORMAT2 = "* * * ? * * *";
    @Test
    public void test_cron_blackout_window_any_valid_cron_blackout_in_list_returns_true() {
        now = Timestamp.valueOf(LocalDateTime.of(1970, 1, 18, 14, 30, 0, 0));
        List<String> cronExpressions = Arrays.asList(AT_12_MIDDAY, AT_EVERY_SECOND_FORMAT1);
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, LONDON.toString(), new Date(now.getTime())), is(true));
        cronExpressions = Arrays.asList(AT_EVERY_SECOND_FORMAT2, AT_12_MIDDAY);
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, AUSTRALIA.toString(), new Date(now.getTime())), is(true));
        cronExpressions = Arrays.asList(AT_EVERY_SECOND_FORMAT1);
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, LONDON.toString(), new Date(now.getTime())), is(true));
        cronExpressions = Arrays.asList(AT_14_30_00);
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, LONDON.toString(), new Date(now.getTime())), is(true));
    }

    @Test
    public void test_cron_blackout_window_all_cron_blackouts_in_list_returns_must_be_invalid_for_false_return() {
        now = Timestamp.valueOf(LocalDateTime.of(1970, 1, 18, 14, 30, 0, 0));
        List<String> cronExpressions = Arrays.asList(AT_12_MIDDAY);
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, AUSTRALIA.toString(), new Date(now.getTime())), is(false));
        cronExpressions = new ArrayList<>();
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, LONDON.toString(), new Date(now.getTime())), is(false));
        cronExpressions = Arrays.asList(EVERYDAY_AT_6_AM, AT_12_MIDDAY);
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, LONDON.toString(), new Date(now.getTime())), is(false));
    }

    private static final Long FIRST_SECOND_OF_2099 = 4070908801000L;
    private static final Long FIRST_SECOND_OF_2024 = 1704067201000L;
    private static final Long FIRST_SECOND_OF_2023 = 1672531201000L;
    private static final Long FIRST_SECOND_OF_2022 = 1640995201000L;
    @Test
    public void test_blackout_ranges_any_within_window_returns_true() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 1, 1, 0, 1, 0, 0));
        Map <Long, Long> blackoutRanges = Map.of(FIRST_SECOND_OF_2022, FIRST_SECOND_OF_2023, 0L,1L);
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(blackoutRanges, new Date(now.getTime())), is(true));
        blackoutRanges = Map.of(0L,1L, FIRST_SECOND_OF_2022, FIRST_SECOND_OF_2023);
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(blackoutRanges, new Date(now.getTime())), is(true));
        blackoutRanges = Map.of(FIRST_SECOND_OF_2022, FIRST_SECOND_OF_2023);
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(blackoutRanges, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 1, 1, 0, 0, 1, 0));
        blackoutRanges = Map.of(FIRST_SECOND_OF_2022, FIRST_SECOND_OF_2023);
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(blackoutRanges, new Date(now.getTime())), is(true));

        now = Timestamp.valueOf(LocalDateTime.of(2023, 1, 1, 0, 0, 1, 0));
        blackoutRanges = Map.of(FIRST_SECOND_OF_2022, FIRST_SECOND_OF_2023);
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(blackoutRanges, new Date(now.getTime())), is(true));
    }

    @Test
    public void test_blackout_ranges_onlt_all_outside_window_returns_false() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 1, 1, 0, 1, 0, 0));
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(new HashMap<>(), new Date(now.getTime())), is(false));
        Map <Long, Long> blackoutRanges = Map.of(FIRST_SECOND_OF_2022, FIRST_SECOND_OF_2023, 0L,1L);
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(blackoutRanges, new Date(now.getTime())), is(false));
        blackoutRanges = Map.of(0L,1L, FIRST_SECOND_OF_2023, FIRST_SECOND_OF_2024, FIRST_SECOND_OF_2024, FIRST_SECOND_OF_2099);
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(blackoutRanges, new Date(now.getTime())), is(false));
        blackoutRanges = Map.of(FIRST_SECOND_OF_2099, FIRST_SECOND_OF_2022);
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(blackoutRanges, new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 12, 31, 23, 59, 59, 0));
        blackoutRanges = Map.of(FIRST_SECOND_OF_2023, FIRST_SECOND_OF_2024);
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(blackoutRanges, new Date(now.getTime())), is(false));

        now = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 1, 1, 2, 0));
        blackoutRanges = Map.of(FIRST_SECOND_OF_2023, FIRST_SECOND_OF_2024);
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(blackoutRanges, new Date(now.getTime())), is(false));
    }
}