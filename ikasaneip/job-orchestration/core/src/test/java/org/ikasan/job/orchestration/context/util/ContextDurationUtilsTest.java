package org.ikasan.job.orchestration.context.util;

import org.junit.Assert;
import org.junit.Test;

public class ContextDurationUtilsTest {

    @Test
    public void test_success_5_hour_ttl() {
        long ttl = 18000000L;

        int days = ContextDurationUtils.getDays(ttl);
        int hours = ContextDurationUtils.getHours(ttl);
        int minutes = ContextDurationUtils.getMinutes(ttl);


        Assert.assertEquals(0, days);
        Assert.assertEquals(5, hours);
        Assert.assertEquals(0, minutes);
    }

    @Test
    public void test_success_5_minutes_ttl() {
        long ttl = 300000L;

        int days = ContextDurationUtils.getDays(ttl);
        int hours = ContextDurationUtils.getHours(ttl);
        int minutes = ContextDurationUtils.getMinutes(ttl);


        Assert.assertEquals(0, days);
        Assert.assertEquals(0, hours);
        Assert.assertEquals(5, minutes);
    }

    @Test
    public void test_success_5_seconds_ttl() {
        long ttl = 5000L;

        int days = ContextDurationUtils.getDays(ttl);
        int hours = ContextDurationUtils.getHours(ttl);
        int minutes = ContextDurationUtils.getMinutes(ttl);


        Assert.assertEquals(0, days);
        Assert.assertEquals(0, hours);
        Assert.assertEquals(0, minutes);
    }

    @Test
    public void test_success_2_days_ttl() {
        long ttl = 172800000L;

        int days = ContextDurationUtils.getDays(ttl);
        int hours = ContextDurationUtils.getHours(ttl);
        int minutes = ContextDurationUtils.getMinutes(ttl);


        Assert.assertEquals(2, days);
        Assert.assertEquals(0, hours);
        Assert.assertEquals(0, minutes);
    }

    @Test
    public void test_success_2_days_5_hours_37_minutes_ttl() {
        long ttl = 193020000L;

        int days = ContextDurationUtils.getDays(ttl);
        int hours = ContextDurationUtils.getHours(ttl);
        int minutes = ContextDurationUtils.getMinutes(ttl);


        Assert.assertEquals(2, days);
        Assert.assertEquals(5, hours);
        Assert.assertEquals(37, minutes);
    }

    @Test
    public void test_success_40_days_ttl() {
        long ttl = 3456000000L;

        int days = ContextDurationUtils.getDays(ttl);
        int hours = ContextDurationUtils.getHours(ttl);
        int minutes = ContextDurationUtils.getMinutes(ttl);


        Assert.assertEquals(40, days);
        Assert.assertEquals(0, hours);
        Assert.assertEquals(0, minutes);
    }

    @Test
    public void test_success_1_year_ttl() {
        long ttl = 31536000000L;

        int days = ContextDurationUtils.getDays(ttl);
        int hours = ContextDurationUtils.getHours(ttl);
        int minutes = ContextDurationUtils.getMinutes(ttl);


        Assert.assertEquals(365, days);
        Assert.assertEquals(0, hours);
        Assert.assertEquals(0, minutes);
    }

    @Test
    public void test_success_0_ttl() {
        long ttl = 0L;

        int days = ContextDurationUtils.getDays(ttl);
        int hours = ContextDurationUtils.getHours(ttl);
        int minutes = ContextDurationUtils.getMinutes(ttl);


        Assert.assertEquals(0, days);
        Assert.assertEquals(0, hours);
        Assert.assertEquals(0, minutes);
    }

    @Test
    public void test_success_2_days_5_hours_37_minutes_ttl_as_milli() {
        long ttl = 193020000L;

        long ttlActual = ContextDurationUtils.getMilliseconds(2, 5, 37);

        Assert.assertEquals(ttl, ttlActual);
    }

    @Test
    public void test_success_1_year_ttl_as_milli() {
        long ttl = 31536000000L;

        long ttlActual = ContextDurationUtils.getMilliseconds(365, 0, 0);

        Assert.assertEquals(ttl, ttlActual);
    }

    @Test
    public void test_success_5_minutes_ttl_as_milli() {
        long ttl = 300000L;

        long ttlActual = ContextDurationUtils.getMilliseconds(0, 0, 5);

        Assert.assertEquals(ttl, ttlActual);
    }

    @Test
    public void test_success_0_ttl_as_milli() {
        long ttl = 0L;

        long ttlActual = ContextDurationUtils.getMilliseconds(0, 0, 0);

        Assert.assertEquals(ttl, ttlActual);
    }
}
