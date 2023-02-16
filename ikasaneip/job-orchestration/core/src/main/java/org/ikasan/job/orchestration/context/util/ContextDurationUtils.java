package org.ikasan.job.orchestration.context.util;

public class ContextDurationUtils {
    private static long MILLI_IN_SECOND = 1000;
    private static long MILLI_IN_MINUTE = 60 * MILLI_IN_SECOND;
    private static long MILLI_IN_HOUR = 60 * MILLI_IN_MINUTE;
    private static long MILLI_IN_DAY = 24 * MILLI_IN_HOUR;

    /**
     * Get the day component from a duration in milliseconds
     *
     * @param durationMilli
     * @return
     */
    public static int getDays(long durationMilli) {
        if(durationMilli <= 0) {
            return 0;
        }

        return (int) (durationMilli / MILLI_IN_DAY);
    }

    /**
     * Get the hour component from the duration in milliseconds
     *
     * @param durationMilli
     * @return
     */
    public static int getHours(long durationMilli) {
        if(durationMilli <= 0) {
            return 0;
        }

        return (int) ((durationMilli % MILLI_IN_DAY) / MILLI_IN_HOUR);
    }

    /**
     * Get the minutes duration from a duration in milliseconds.
     *
     * @param durationMilli
     * @return
     */
    public static int getMinutes(long durationMilli) {
        if(durationMilli <= 0) {
            return 0;
        }

        return (int) (((durationMilli % MILLI_IN_DAY) % MILLI_IN_HOUR) / MILLI_IN_MINUTE);
    }

    /**
     * Get a millisecond value for the days, hours and minutes provided.
     *
     * @param days
     * @param hours
     * @param minutes
     * @return
     */
    public static long getMilliseconds(int days, int hours, int minutes) {
        return (days * MILLI_IN_DAY) + (hours * MILLI_IN_HOUR) + (minutes * MILLI_IN_MINUTE);
    }
}
