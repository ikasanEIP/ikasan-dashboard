package org.ikasan.job.orchestration.context.util;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

import org.junit.Test;

public class QuartzTimeWindowCheckerTest {

    // note: * * 6 means every day at 6 am
    private static final String START_TIME_WINDOW = "0 0 6 ? * * *";
    // note: * * 15 means every second during 15:00 hour on every day
    private static final String END_TIME_WINDOW = "* * 15 ? * * *";

    private Timestamp now;

    @Test
    public void is_within_operating_window_very_small_window() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 16, 27, 59, 0));
        assertFalse(QuartzTimeWindowChecker.withinOperatingWindow("0 28 16 ? * * *", "0 30 16 ? * * *", new Date(now.getTime())));
        assertTrue(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 28 16 ? * * *", "0 30 16 ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 16, 28, 0, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("0 28 16 ? * * *", "0 30 16 ? * * *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 28 16 ? * * *", "0 30 16 ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 16, 29, 0, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("0 28 16 ? * * *", "0 30 16 ? * * *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 28 16 ? * * *", "0 30 16 ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 16, 30, 0, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("0 28 16 ? * * *", "0 30 16 ? * * *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 28 16 ? * * *", "0 30 16 ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 16, 30, 0, 1));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("0 28 16 ? * * *", "0 30 16 ? * * *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 28 16 ? * * *", "0 30 16 ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 16, 30, 1, 0));
        assertFalse(QuartzTimeWindowChecker.withinOperatingWindow("0 28 16 ? * * *", "0 30 16 ? * * *", new Date(now.getTime())));
        assertTrue(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 28 16 ? * * *", "0 30 16 ? * * *", new Date(now.getTime())));
    }

    @Test
    public void is_within_operating_window_never_except_midnight_exactly() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 11, 59, 59, 0));
        assertFalse(QuartzTimeWindowChecker.withinOperatingWindow("0 0 0 ? * * *", "0 0 0 ? * * *", new Date(now.getTime())));
        assertTrue(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 0 0 ? * * *", "0 0 0 ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 0, 0, 0, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("0 0 0 ? * * *", "0 0 0 ? * * *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 0 0 ? * * *", "0 0 0 ? * * *", new Date(now.getTime())));
    }

    @Test
    public void is_within_operating_window_every_second_all_day() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 23, 59, 59, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("* * 0 ? * * *", "* * 23 ? * * *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("* * 0 ? * * *", "* * 23 ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 0, 0, 0, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("* * 0 ? * * *", "* * 23 ? * * *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("* * 0 ? * * *", "* * 23 ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 12, 12, 12, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("* * 0 ? * * *", "* * 23 ? * * *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("* * 0 ? * * *", "* * 23 ? * * *", new Date(now.getTime())));
    }

    @Test
    public void is_within_operating_window_every_second() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("* * * ? * * *", "* * * ? * * *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("* * * ? * * *", "* * * ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("* * * * * ? *", "* * * * * ? *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("* * * ? * * *", "* * * ? * * *", new Date(now.getTime())));
    }

    @Test
    public void is_within_operating_window_every_hour_on_the_hour() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 0));
        assertFalse(QuartzTimeWindowChecker.withinOperatingWindow("0 0 * ? * * *", "0 0 * ? * * *", new Date(now.getTime())));
        assertTrue(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 0 * ? * * *", "0 0 * ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 22, 6, 0, 0, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("0 0 * ? * * *", "0 0 * ? * * *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 0 * ? * * *", "0 0 * ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 0));
        assertFalse(QuartzTimeWindowChecker.withinOperatingWindow("0 0 * * * ? *", "0 0 * * * ? *", new Date(now.getTime())));
        assertTrue(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 0 * * * ? *", "0 0 * * * ? *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 11, 7, 0, 0, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("0 0 * * * ? *", "0 0 * * * ? *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 0 * * * ? *", "0 0 * * * ? *", new Date(now.getTime())));
    }

    @Test
    public void is_within_operating_window_every_hour_on_the_hour_between_3am_and_6am() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("0 0 3 ? * * *", "0 0 6 ? * * *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 0 3 ? * * *", "0 0 6 ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 22, 6, 0, 0, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("0 0 3 ? * * *", "0 0 6 ? * * *", new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 0 3 ? * * *", "0 0 6 ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 1, 6, 0, 1, 0));
        assertFalse(QuartzTimeWindowChecker.withinOperatingWindow("0 0 3 ? * * *", "0 0 6 ? * * *", new Date(now.getTime())));
        assertTrue(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 0 3 ? * * *", "0 0 6 ? * * *", new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 1, 6, 1, 0, 0));
        assertFalse(QuartzTimeWindowChecker.withinOperatingWindow("0 0 3 ? * * *", "0 0 6 ? * * *", new Date(now.getTime())));
        assertTrue(QuartzTimeWindowChecker.outsideOfOperatingWindow("0 0 3 ? * * *", "0 0 6 ? * * *", new Date(now.getTime())));
    }

    @Test
    public void is_within_operating_window_6am_to_all_of_3pm() {
        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 0));
        assertFalse(QuartzTimeWindowChecker.withinOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));
        assertTrue(QuartzTimeWindowChecker.outsideOfOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 5, 59, 59, 999));
        assertFalse(QuartzTimeWindowChecker.withinOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));
        assertTrue(QuartzTimeWindowChecker.outsideOfOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 6, 0, 0, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 6, 0, 1, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 6, 0, 0, 1));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 14, 59, 59, 999));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 15, 0, 0, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 15, 0, 0, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 15, 0, 1, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 15, 0, 59, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));
        assertFalse(QuartzTimeWindowChecker.outsideOfOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));

        now = Timestamp.valueOf(LocalDateTime.of(2022, 5, 18, 16, 0, 0, 0));
        assertFalse(QuartzTimeWindowChecker.withinOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));
        assertTrue(QuartzTimeWindowChecker.outsideOfOperatingWindow(START_TIME_WINDOW, END_TIME_WINDOW, new Date(now.getTime())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throws_parse_exception_invalid_expression_start() {
        now = Timestamp.valueOf(LocalDateTime.of(1970, 1, 18, 5, 59, 59, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("* * ? * * *", "* * * ? * * *", new Date(now.getTime())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throws_parse_exception_invalid_expression_end() {
        now = Timestamp.valueOf(LocalDateTime.of(1970, 5, 22, 5, 59, 59, 0));
        assertTrue(QuartzTimeWindowChecker.withinOperatingWindow("* * * ? * * *", "* * * * * * *", new Date(now.getTime())));
    }
}