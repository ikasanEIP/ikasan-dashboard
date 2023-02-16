package org.ikasan.job.orchestration.context.util;

import org.junit.Assert;
import org.junit.Test;

public class CronUtilsTest {

    @Test
    public void test_cron_convert_success_5_hour_ttl() {
        long secondOfMarch1973 = 99878400000L;
        long ttl5Hours = 18000000L;

        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(secondOfMarch1973, ttl5Hours, "Europe/London");

        Assert.assertEquals("0 0 5 2 3 ? 1973", cron);
    }

    @Test
    public void test_cron_convert_success_5_minutes_ttl() {
        long secondOfMarch1973 = 99878400000L;
        long ttl5Hours = 300000L;

        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(secondOfMarch1973, ttl5Hours, "Europe/London");

        Assert.assertEquals("0 5 0 2 3 ? 1973", cron);
    }

    @Test
    public void test_cron_convert_success_5_seconds_ttl() {
        long secondOfMarch1973 = 99878400000L;
        long ttl5Hours = 5000L;

        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(secondOfMarch1973, ttl5Hours, "Europe/London");

        Assert.assertEquals("5 0 0 2 3 ? 1973", cron);
    }

    @Test
    public void test_cron_convert_success_2_days_ttl() {
        long secondOfMarch1973 = 99878400000L;
        long ttl5Hours = 172800000L;

        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(secondOfMarch1973, ttl5Hours, "Europe/London");

        Assert.assertEquals("0 0 0 4 3 ? 1973", cron);
    }

    @Test
    public void test_cron_convert_success_40_days_ttl() {
        long secondOfMarch1973 = 99878400000L;
        long ttl5Hours = 3456000000L;

        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(secondOfMarch1973, ttl5Hours, "Europe/London");

        Assert.assertEquals("0 0 1 11 4 ? 1973", cron);
    }

    @Test
    public void test_cron_convert_success_1_year_ttl() {
        long secondOfMarch1973 = 99878400000L;
        long ttl5Hours = 31536000000L;

        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(secondOfMarch1973, ttl5Hours, "Europe/London");

        Assert.assertEquals("0 0 0 2 3 ? 1974", cron);
    }
}
