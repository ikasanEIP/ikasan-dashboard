package org.ikasan.dashboard.ui.scheduler;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.Assert;
import org.junit.Test;

public class DurationFormatUtilsTest {

    @Test
    public void test_format() {
        Assert.assertEquals("00h:01m:35.000s", DurationFormatUtils.formatDuration(95000, "HH'h':mm'm':ss.SSS's'", true));
        Assert.assertEquals("00h:15m:50.000s", DurationFormatUtils.formatDuration(950000, "HH'h':mm'm':ss.SSS's'", true));
        Assert.assertEquals("02h:38m:23.744s", DurationFormatUtils.formatDuration(9503744, "HH'h':mm'm':ss.SSS's'", true));
    }
}
