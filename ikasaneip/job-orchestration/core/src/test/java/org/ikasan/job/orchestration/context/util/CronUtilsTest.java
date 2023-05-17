package org.ikasan.job.orchestration.context.util;

import org.junit.Assert;
import org.junit.Test;

public class CronUtilsTest {

    @Test
    public void test_cron_convert_success_5_hour_ttl() {
        long secondOfMarch1973 = 99878400000L;
        long ttl = 18000000L;

        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(secondOfMarch1973, ttl, "Europe/London");

        Assert.assertEquals("0 0 5 2 3 ? 1973", cron);
    }

    @Test
    public void test_cron_convert_success_5_minutes_ttl() {
        long secondOfMarch1973 = 99878400000L;
        long ttl = 300000L;

        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(secondOfMarch1973, ttl, "Europe/London");

        Assert.assertEquals("0 5 0 2 3 ? 1973", cron);
    }

    @Test
    public void test_cron_convert_success_5_seconds_ttl() {
        long secondOfMarch1973 = 99878400000L;
        long ttl = 5000L;

        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(secondOfMarch1973, ttl, "Europe/London");

        Assert.assertEquals("5 0 0 2 3 ? 1973", cron);
    }

    @Test
    public void test_cron_convert_success_2_days_ttl() {
        long secondOfMarch1973 = 99878400000L;
        long ttl = 172800000L;

        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(secondOfMarch1973, ttl, "Europe/London");

        Assert.assertEquals("0 0 0 4 3 ? 1973", cron);
    }

    @Test
    public void test_cron_convert_success_40_days_ttl() {
        long secondOfMarch1973 = 99878400000L;
        long ttl = 3456000000L;

        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(secondOfMarch1973, ttl, "Europe/London");

        Assert.assertEquals("0 0 1 11 4 ? 1973", cron);
    }

    @Test
    public void test_cron_convert_success_1_year_ttl() {
        long secondOfMarch1973 = 99878400000L;
        long ttl = 31536000000L;

        String cron = CronUtils.buildCronFromOriginalWithMillisecondOffset(secondOfMarch1973, ttl, "Europe/London");

        Assert.assertEquals("0 0 0 2 3 ? 1974", cron);
    }

    @Test
    public void test_duration_against_fire_time() {
        long twentyThreeHours = 23 * 60 * 60 * 1000;
        long threeHours = 3 * 60 * 60 * 1000;
        long twoMinutes = 2 * 60 * 1000;
        long oneMinute = 1 * 60 * 1000;

        Assert.assertFalse(CronUtils.isDurationGreaterThanNextFireTime
            ("* * * ? * MON,TUE,WED,THU,FRI *", oneMinute, 1));
        Assert.assertFalse(CronUtils.isDurationGreaterThanNextFireTime
            ("* * * ? * MON,TUE,WED,THU,FRI *", oneMinute, 2));
        Assert.assertFalse(CronUtils.isDurationGreaterThanNextFireTime
            ("* * * ? * MON,TUE,WED,THU,FRI *", oneMinute, 3));

        Assert.assertFalse(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0/2 * ? * MON,TUE,WED,THU,FRI *", twoMinutes, 1));
        Assert.assertTrue(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0/2 * ? * MON,TUE,WED,THU,FRI *", twoMinutes, 2));
        Assert.assertTrue(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0/2 * ? * MON,TUE,WED,THU,FRI *", twoMinutes, 3));

        Assert.assertTrue(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0/3 * ? * MON,TUE,WED,THU,FRI *", twoMinutes, 1));
        Assert.assertTrue(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0/3 * ? * MON,TUE,WED,THU,FRI *", twoMinutes, 2));
        Assert.assertTrue(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0/3 * ? * MON,TUE,WED,THU,FRI *", twoMinutes, 3));

        Assert.assertFalse(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0/3 * ? * MON,TUE,WED,THU,FRI *", twentyThreeHours, 1));
        Assert.assertFalse(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0/3 * ? * MON,TUE,WED,THU,FRI *", twentyThreeHours, 2));
        Assert.assertFalse(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0/3 * ? * MON,TUE,WED,THU,FRI *", twentyThreeHours, 3));

        Assert.assertTrue(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0 1 ? * * *", twentyThreeHours, 1));
        Assert.assertTrue(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0 1 ? * * *", twentyThreeHours, 2));
        Assert.assertTrue(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0 1 ? * * *", twentyThreeHours, 3));

        Assert.assertTrue(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0 1,7,11 ? * * *", threeHours, 1));
        Assert.assertTrue(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0 1,7,11 ? * * *", threeHours, 2));
        Assert.assertTrue(CronUtils.isDurationGreaterThanNextFireTime
            ("0 0 1,7,11 ? * * *", threeHours, 3));
    }
}
