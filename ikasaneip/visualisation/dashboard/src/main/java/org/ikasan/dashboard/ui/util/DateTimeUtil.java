package org.ikasan.dashboard.ui.util;

import com.vaadin.flow.component.UI;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class DateTimeUtil
{
    /**
     * Helper method to get the hour and minute milliseconds from a local time.
     * @param localTime
     * @return
     */
    public static long getMilliFromTime(LocalTime localTime)
    {
        long milli = 0;
        if(localTime.getMinute() > 0)
        {
            milli += localTime.getMinute()  * 60 * 1000;
        }
        if(localTime.getHour() > 0)
        {
            milli += localTime.getHour() * 60  * 60 * 1000;
        }

        return milli;
    }

    /**
     * Get the zone id for the currently logged in session.
     *
      * @return
     */
    public static final ZoneId getZoneId() {
        if(UI.getCurrent() == null || UI.getCurrent().getSession() == null
            || UI.getCurrent().getSession().getAttribute(SessionAttributeConstants.TIMEZONE_ID) == null) {
            return ZoneId.of("UTC");
        }

        return ZoneId.of((String) UI.getCurrent().getSession()
            .getAttribute(SessionAttributeConstants.TIMEZONE_ID));
    }

    /**
     * Get the zone id for the currently logged in session.
     *
     * @return
     */
    public static final ZoneOffset getZoneOffset() {
        if(UI.getCurrent() == null || UI.getCurrent().getSession() == null
            || UI.getCurrent().getSession().getAttribute(SessionAttributeConstants.TIMEZONE_ID) == null) {
            return ZoneId.of("UTC").getRules().getOffset(Instant.now());
        }

        return ZoneId.of((String) UI.getCurrent().getSession()
            .getAttribute(SessionAttributeConstants.TIMEZONE_ID)).getRules().getOffset(Instant.now());
    }
}
