package org.ikasan.dashboard.ui.util;

import com.vaadin.flow.component.UI;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class DateTimeUtil
{
    private static ArrayList<TimezonePair> TIMEZONE_PAIRS;

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


    public static final  List<TimezonePair> getAllZoneIdsAndItsOffSet() {

        if(TIMEZONE_PAIRS == null) {
            TIMEZONE_PAIRS = new ArrayList<>();
        }
        else {
            return TIMEZONE_PAIRS;
        }

        LocalDateTime localDateTime = LocalDateTime.now();

        for (String zoneId : ZoneId.getAvailableZoneIds()) {

            ZoneId id = ZoneId.of(zoneId);

            // LocalDateTime -> ZonedDateTime
            ZonedDateTime zonedDateTime = localDateTime.atZone(id);

            // ZonedDateTime -> ZoneOffset
            ZoneOffset zoneOffset = zonedDateTime.getOffset();

            //replace Z to +00:00
            String offset = zoneOffset.getId().replaceAll("Z", "+00:00");

            TIMEZONE_PAIRS.add(new TimezonePair(id.getId(), offset));

        }

        return TIMEZONE_PAIRS;
    }

    public static final TimezonePair getTimezonePairForZoneId(String zoneId) {
        AtomicReference<TimezonePair> timezonePairAtomicReference = new AtomicReference<>();
        TIMEZONE_PAIRS.stream()
            .filter(timezonePair -> timezonePair.zoneId.equals(zoneId))
            .findFirst().ifPresent(timezonePair -> timezonePairAtomicReference.set(timezonePair));

        return timezonePairAtomicReference.get();
    }

    public static class TimezonePair {
        public final String zoneId;
        public final String offset;

        public TimezonePair(String zoneId, String offset) {
            this.zoneId = zoneId;
            this.offset = offset;
        }
    }
}
