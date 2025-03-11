package org.ikasan.job.orchestration.context.util;

import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.junit.Assert;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public class CustomWeekdayOfMonthHelperTest {

    @Test
    public void test_get_projected_cron_expression_success() {
        ContextTemplateImpl c = new ContextTemplateImpl();
        c.setCustomWeekDayOfMonth(true);
        c.setTimeWindowStart("0 0 2 3W * ? *");
        String newCron = CustomWeekdayOfMonthHelper.determineContextStartCron
            (c, LocalDateTime.of(2025, 2, 1, 4, 4, 4));

        Assert.assertEquals("0 0 2 5 2 ? 2025", newCron);

        newCron = CustomWeekdayOfMonthHelper.determineContextStartCron
            (c, LocalDateTime.of(2025, 2, 15, 4, 4, 4));

        Assert.assertEquals("0 0 2 5 3 ? 2025", newCron);

        c.setTimeWindowStart("0 30 16 7W * ? *");

        newCron = CustomWeekdayOfMonthHelper.determineContextStartCron
            (c, LocalDateTime.of(2025, 2, 15, 4, 4, 4));

        Assert.assertEquals("0 30 16 11 3 ? 2025", newCron);

        // 31 is the highest value that weekday of the month can be set
        c.setTimeWindowStart("0 30 16 31W * ? *");

        newCron = CustomWeekdayOfMonthHelper.determineContextStartCron
            (c, LocalDateTime.of(2025, 2, 15, 4, 4, 4));

        Assert.assertEquals("0 30 16 28 2 ? 2025", newCron);
    }

    @Test
    public void test_has_nth_day_of_month() {
        LocalDateTime nthWeekdayOfMonth = CustomWeekdayOfMonthHelper.getNthWeekDayOfMonth
            (2, 4, 4, 4, LocalDateTime.of(2025, 2, 15, 4, 4, 4));

        Assert.assertEquals(DayOfWeek.TUESDAY, nthWeekdayOfMonth.getDayOfWeek());

        nthWeekdayOfMonth = CustomWeekdayOfMonthHelper.getNthWeekDayOfMonth
            (8, 4, 4, 4, LocalDateTime.of(2025, 2, 15, 4, 4, 4));

        Assert.assertEquals(DayOfWeek.WEDNESDAY, nthWeekdayOfMonth.getDayOfWeek());
    }

    @Test
    public void test_has_nth_day_already_fallen() {
        boolean alreadyFallen = CustomWeekdayOfMonthHelper
            .hasTheTimeAlreadyFallenOfThisMonthAlreadyFallen(2, 4, 4,4
                , LocalDateTime.of(2025, 2, 15, 4, 4, 4));

        Assert.assertEquals(true, alreadyFallen);
    }
}
