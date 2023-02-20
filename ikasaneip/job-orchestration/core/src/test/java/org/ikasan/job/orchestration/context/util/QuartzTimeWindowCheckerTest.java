package org.ikasan.job.orchestration.context.util;

import org.junit.Ignore;
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

    /**
     * A custom test to put in adhoc cron expression to see what withinOperatingWindow will return.
     */
    @Test
    public void test_custom_withinOperatingWindow() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(),
            "27 2 18 8 FEB ? *", "0 30 6 8 FEB ? 2024", myDate.parse("2023-02-08T20:34:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(),
            "0 30 11 8 * ? *", "0 59 7 ? * * 2024", myDate.parse("2023-02-08T14:34:00")), is(false));
    }

    @Test
    public void is_within_an_operating_window_of_5_minutes() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, 300000L, myDate.parse("2022-05-18T14:34:00")), is(false));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, 300000L, myDate.parse("2022-05-18T14:35:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, 300000L, myDate.parse("2022-05-18T14:36:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, 300000L, myDate.parse("2022-05-19T14:29:00")), is(false));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, 300000L, myDate.parse("2022-05-19T14:35:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, 300000L, myDate.parse("2022-05-19T14:30:01")), is(false));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, 300000L, myDate.parse("2022-05-19T14:31:00")), is(false));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, 300000L, myDate.parse("2022-05-19T14:34:00")), is(false));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, 300000L, myDate.parse("2022-05-19T14:35:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, 300000L, myDate.parse("2022-05-19T14:36:00")), is(true));
    }

    @Test
    public void is_within_an_operating_window_of_exceeding_1_day() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_35_00, 93600000L, myDate.parse("2022-05-18T15:34:00")), is(true));


    }

    @Test
    public void is_within_operating_window_very_small_window() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, 120000L, myDate.parse("2022-05-18T14:29:59")), is(false));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, 120000L, myDate.parse("2022-05-18T14:30:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, 120000L, myDate.parse("2022-05-18T14:31:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, 120000L, myDate.parse("2022-05-18T14:32:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, 120000L, myDate.parse("2022-05-18T14:32:01")), is(false));
    }

    @Test
    public void is_within_operating_window_very_small_window_check_winter_dates() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, 120000L, myDate.parse("2022-12-18T14:29:59")), is(false));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, 120000L, myDate.parse("2022-12-18T14:30:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, 120000L, myDate.parse("2022-12-18T14:31:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, 120000L, myDate.parse("2022-12-18T14:32:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_14_30_00, 120000L, myDate.parse("2022-12-18T14:32:01")), is(false));
    }

    @Test
    public void is_within_operating_window_very_small_window_with_timezone() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(AUSTRALIA.toString(), AT_14_30_00, 120000L, myDate.parse("2022-05-18T14:29:59")), is(false));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(AUSTRALIA.toString(), AT_14_30_00, 120000L, myDate.parse("2022-05-18T14:30:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(AUSTRALIA.toString(), AT_14_30_00, 120000L, myDate.parse("2022-05-18T14:31:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(AUSTRALIA.toString(), AT_14_30_00, 120000L, myDate.parse("2022-05-18T14:32:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(AUSTRALIA.toString(), AT_14_30_00, 120000L, myDate.parse("2022-05-18T14:32:01")), is(false));
    }

    @Test
    public void is_within_operating_window_never_except_midnight_exactly() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 0 ? * * *", 0L, myDate.parse("2022-05-18T11:59:59")), is(false));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 0 ? * * *", 0L, myDate.parse("2022-05-18T00:00:00")), is(true));
    }

    @Test
    public void is_within_operating_window_every_second_all_day() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "* * * ? * * *", 1000L, myDate.parse("2022-05-18T23:59:59")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "* * * ? * * *", 1000L, myDate.parse("2022-05-18T00:00:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "* * * ? * * *", 1000L, myDate.parse("2022-05-18T12:12:12")), is(true));
    }

    @Test
    public void is_within_operating_window_every_second() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_EVERY_SECOND_FORMAT1, 1000L, myDate.parse("2022-05-18T05:59:59")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), AT_EVERY_SECOND_FORMAT2, 1000L, myDate.parse("2022-05-18T05:59:59")), is(true));
    }

    @Test
    public void is_within_operating_window_every_hour_on_the_hour() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 * ? * * *", 3500000, myDate.parse("2022-05-18T05:59:59")), is(false));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 * ? * * *", 3500000, myDate.parse("2022-05-18T06:00:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 * * * ? *", 3500000, myDate.parse("2022-05-18T07:59:59")), is(false));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 * * * ? *", 3500000, myDate.parse("2022-05-18T07:00:00")), is(true));
    }

    @Test
    public void is_within_operating_window_every_hour_on_the_hour_between_3am_and_6am() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 3 ? * * *", 10800000, myDate.parse("2022-05-18T05:59:59")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 3 ? * * *", 10800000, myDate.parse("2022-05-22T06:00:00")), is(true));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 3 ? * * *", 10800000, myDate.parse("2022-05-01T06:00:01")), is(false));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "0 0 3 ? * * *", 10800000, myDate.parse("2022-05-18T06:01:00")), is(false));
    }

    @Test(expected = RuntimeException.class)
    public void throws_parse_exception_invalid_expression_start() throws ParseException {
        SimpleDateFormat myDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDate.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        assertThat(QuartzTimeWindowChecker.withinOperatingWindow(LONDON.toString(), "* * ? * * *", 10000L, myDate.parse("1970-01-18T05:59:59")), is(true));
    }

    private static final String AT_12_MIDDAY = "0 0 12 * * ? *";
    private static final String AT_EVERY_SECOND_FORMAT1 = "* * * * * ? *";
    private static final String AT_EVERY_SECOND_FORMAT2 = "* * * ? * * *";
    @Test
    public void test_cron_blackout_window_any_valid_cron_blackout_in_list_returns_true() throws ParseException {
        SimpleDateFormat myDateLondon = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDateLondon.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        SimpleDateFormat myDateSydney = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDateSydney.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));

        List<String> cronExpressions = Arrays.asList(AT_12_MIDDAY, AT_EVERY_SECOND_FORMAT1);
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, LONDON.toString(), myDateLondon.parse("1970-01-18T14:30:00")), is(true));
        cronExpressions = Arrays.asList(AT_EVERY_SECOND_FORMAT2, AT_12_MIDDAY);
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, AUSTRALIA.toString(), myDateSydney.parse("1970-01-18T14:30:00")), is(true));
        cronExpressions = Arrays.asList(AT_EVERY_SECOND_FORMAT1);
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, LONDON.toString(), myDateLondon.parse("1970-01-18T14:30:00")), is(true));
        cronExpressions = Arrays.asList(AT_14_30_00);
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, LONDON.toString(), myDateLondon.parse("1970-01-18T14:30:00")), is(true));
    }

    @Test
    public void test_cron_blackout_window_all_cron_blackouts_in_list_returns_must_be_invalid_for_false_return() throws ParseException {
        SimpleDateFormat myDateLondon = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDateLondon.setTimeZone(TimeZone.getTimeZone("Europe/London"));

        SimpleDateFormat myDateSydney = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        myDateSydney.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));

        List<String> cronExpressions = Arrays.asList(AT_12_MIDDAY);
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, AUSTRALIA.toString(), myDateSydney.parse("1970-01-18T14:30:00")), is(false));
        cronExpressions = new ArrayList<>();
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, LONDON.toString(), myDateLondon.parse("1970-01-18T14:30:00")), is(false));
        cronExpressions = Arrays.asList(EVERYDAY_AT_6_AM, AT_12_MIDDAY);
        assertThat(QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(cronExpressions, LONDON.toString(), myDateLondon.parse("1970-01-18T14:30:00")), is(false));
    }

    private static final Long FIRST_SECOND_OF_2099 = 4070908801000L;
    private static final Long FIRST_SECOND_OF_2024 = 1704067201000L;
    private static final Long FIRST_SECOND_OF_2023 = 1672531201000L;
    private static final Long FIRST_SECOND_OF_2022 = 1640995201000L;
    @Test
    public void test_blackout_ranges_any_within_window_returns_true() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.of(2022, 1, 1, 0, 1, 0, 0));
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
        Timestamp now = Timestamp.valueOf(LocalDateTime.of(2022, 1, 1, 0, 1, 0, 0));
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(new HashMap<>(), new Date(now.getTime())), is(false));
        Map <Long, Long> blackoutRanges = Map.of(FIRST_SECOND_OF_2022, FIRST_SECOND_OF_2023, 0L,1L);
        assertThat(QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(blackoutRanges, new Date(now.getTime())), is(true));
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