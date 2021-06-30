package org.ikasan.dashboard.ui.util;

import com.vaadin.flow.component.UI;

import java.time.*;
import java.util.*;

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

    public static final List<String> getOrderedZoneIdsWithOffset() {
        Map<String, String> sortedMap = new LinkedHashMap<>();

        List<TimezonePair> allZoneIdsAndItsOffSet = getAllZoneIdsAndItsOffSet();

        allZoneIdsAndItsOffSet.sort((o1, o2) -> o1.zoneId.compareTo(o2.zoneId));

        List<String> results = new ArrayList();

        // print map
        allZoneIdsAndItsOffSet.forEach(timezonePair ->
            results.add(String.format("%35s (UTC%s) %n", timezonePair.zoneId, timezonePair.offset).trim()));

        return results;
    }

    public static final  List<TimezonePair> getAllZoneIdsAndItsOffSet() {

        List<TimezonePair> result = new ArrayList<>();

        LocalDateTime localDateTime = LocalDateTime.now();

        for (String zoneId : ZoneId.getAvailableZoneIds()) {

            ZoneId id = ZoneId.of(zoneId);

            // LocalDateTime -> ZonedDateTime
            ZonedDateTime zonedDateTime = localDateTime.atZone(id);

            // ZonedDateTime -> ZoneOffset
            ZoneOffset zoneOffset = zonedDateTime.getOffset();

            //replace Z to +00:00
            String offset = zoneOffset.getId().replaceAll("Z", "+00:00");

            result.add(new TimezonePair(id.getId(), offset));

        }

        return result;
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
