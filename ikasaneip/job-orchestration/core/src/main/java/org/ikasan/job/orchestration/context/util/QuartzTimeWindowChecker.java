package org.ikasan.job.orchestration.context.util;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.cronutils.model.CronType.QUARTZ;


public class QuartzTimeWindowChecker {

    public static final long TWENTY_FOUR_HOURS_MILLISECONDS = 24 * 60 * 60 * 1000;

    /**
     * Determine if the reference dateTime is within the start and end cron expressions.
     * @param timezone to use to adjust referenceDateTime
     * @param startTimeCronExpression to check
     * @param contextTtl to check
     * @param referenceDateTime to which we are comparing, note that date is not always UTC, just depends on how it is created.
     *                          This method does not require the date/time to be UTC, it will be adjusted according to timezone
     * @return true if the reference date / time is within the range, false otherwise
     */
    public static boolean withinOperatingWindow(String timezone, String startTimeCronExpression, long contextTtl, Date referenceDateTime) {
        ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
        ZonedDateTime referenceZDateTime = ZonedDateTime.ofInstant(referenceDateTime.toInstant(), zoneId);

        ZonedDateTime nextExecutionStart = getNextExecution(startTimeCronExpression, referenceZDateTime);
        ZonedDateTime previousExecutionStart = getPreviousExecution(startTimeCronExpression, referenceZDateTime);

        String endTimeCronExpressionFromPrevious = CronUtils.buildCronFromOriginalWithMillisecondOffset(previousExecutionStart.toEpochSecond()*1000
            , contextTtl, timezone);
        String endTimeCronExpressionFromNext = CronUtils.buildCronFromOriginalWithMillisecondOffset(nextExecutionStart.toEpochSecond()*1000
            , contextTtl, timezone);


        ZonedDateTime executionEndFromPrevious = getNextExecution(endTimeCronExpressionFromPrevious, referenceZDateTime);
        if(executionEndFromPrevious == null) {
            executionEndFromPrevious = getPreviousExecution(endTimeCronExpressionFromPrevious, referenceZDateTime);
        }
        ZonedDateTime executionEndFromNext = getNextExecution(endTimeCronExpressionFromNext, referenceZDateTime);

        if (isOnFireTime(zoneId, startTimeCronExpression, endTimeCronExpressionFromPrevious, referenceDateTime) ||
            isOnFireTime(zoneId, startTimeCronExpression, endTimeCronExpressionFromNext, referenceDateTime)) {
            return true;
        } else {
            return (executionEndFromPrevious != null && referenceZDateTime.isAfter(previousExecutionStart) && referenceZDateTime.isBefore(executionEndFromPrevious))
                || (referenceZDateTime.isAfter(nextExecutionStart) && referenceZDateTime.isBefore(executionEndFromNext));
        }
    }


    /**
     * If any of the cronExpressions in the blackoutWindowCronExpressions are satisfied by the referenceDate return true
     * The net effect is any cron expression in the list that matches 'now' will cause a blackout.
     *
     * @param blackoutWindowCronExpressions list to test
     * @param timezone to use
     * @param referenceDateTime to use
     * @return true if any cronExpression is satisfied by the reference date time, false otherwise
     */
    public static boolean fallsWithinCronBlackoutWindows(List<String> blackoutWindowCronExpressions, String timezone, Date referenceDateTime) {
        if (blackoutWindowCronExpressions != null && !blackoutWindowCronExpressions.isEmpty()) {
            ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
            for (String cronExpression : blackoutWindowCronExpressions) {
                if (isSatisfiedBy(zoneId, cronExpression, referenceDateTime)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSatisfiedBy(ZoneId zoneId, String cronExpression, Date referenceDateTime) {
        try {
            CronExpression cronExpressionStart = new CronExpression(cronExpression);
            cronExpressionStart.setTimeZone(TimeZone.getTimeZone(zoneId));
            return cronExpressionStart.isSatisfiedBy(referenceDateTime);
        } catch (ParseException e) {
            throw new RuntimeException("Can not parse quartz expression " + cronExpression);
        }
    }

    /**
     * The supplied blackoutDateTimeRanges represents a series of blackout windows i.e.
     * BlackoutStart(UTC) -> BlackoutEnd(UTC)
     * BlackoutStart2(UTC) -> BlackoutEnd2(UTC)
     * If the supplied reference date time is within any of the windows, we are in blackout so return true.
     * Note that we don't need the timezone because the Blackout start/end are saved in UTC relative to the timezone they relate to.
     *
     * @param blackoutDateTimeRanges map to test
     * @param referenceDateTime to test, this will be converted to UTC, assumed to be in TZ relating to ranges.
     * @return true if the reference date time is within any of the supplied blackout windows.
     */
    public static boolean fallsWithinDateTimeBlackoutRanges(Map<Long, Long> blackoutDateTimeRanges, Date referenceDateTime) {
        if(blackoutDateTimeRanges != null && !blackoutDateTimeRanges.isEmpty())
        {
            ZoneId zoneId = ZoneId.of("UTC");
            ZonedDateTime utcReferenceZDateTime = ZonedDateTime.ofInstant(referenceDateTime.toInstant(), zoneId);

            for(Map.Entry<Long,Long> dateRangeEntry : blackoutDateTimeRanges.entrySet()) {
                long utcFrom = dateRangeEntry.getKey();
                long utcTo = dateRangeEntry.getValue();
                long fireTime = utcReferenceZDateTime.toInstant().toEpochMilli();
                if(fireTime >= utcFrom && fireTime <= utcTo) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determine if the supplied reference date time is on the fire time (exactly) of either the cron start or end expressions
     *
     * @param zoneId used to adjust reference date time to mate the time zone associated with the cron expressions
     * @param startTimeCronExpression to check
     * @param endTimeCronExpression to check
     * @param referenceDateTime to which we are comparing, note that date is not always UTC, just depends on how it is created.
     *                          This method does not require the date/time to be UTC, it will be adjusted according to timezone
     * @return true if the supplied date/time is on the start or end cron expressions, false otherwise.
     */
    protected static boolean isOnFireTime(ZoneId zoneId, String startTimeCronExpression, String endTimeCronExpression, Date referenceDateTime) {
        // we are in the operating window if isSatisfiedBy returns true - used for when its exactly in the fire time
        return isSatisfiedBy(zoneId, startTimeCronExpression, referenceDateTime) || isSatisfiedBy(zoneId, endTimeCronExpression, referenceDateTime);
    }

    /**
     * Given the cron expression and a time zone reference date/time, provide the most recent matching previous date time to the referenceZonedDateTime
     *
     * @param cronExpression to evaluate
     * @param referenceZonedDateTime to use
     * @return a ZonedDateTime or null
     */
    protected static ZonedDateTime getPreviousExecution(String cronExpression, ZonedDateTime referenceZonedDateTime) {
        ExecutionTime executionTime = getExecutionTime(cronExpression);
        return executionTime.lastExecution(referenceZonedDateTime).orElse(null);
    }

    /**
     * Given the cron expression and a time zone reference date/time, provide the most recent matching next date time to the referenceZonedDateTime
     * @param cronExpression
     * @param referenceZonedDateTime
     * @return
     */
    protected static ZonedDateTime getNextExecution(String cronExpression, ZonedDateTime referenceZonedDateTime) {
        ExecutionTime executionTime = getExecutionTime(cronExpression);
        return executionTime.nextExecution(referenceZonedDateTime).orElse(null);
    }

    /**
     * Get the actual execution time for a given cron expression.
     *
     * @param cronExpression
     * @return
     */
    private static ExecutionTime getExecutionTime(String cronExpression) {
        try {
            CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
            CronParser cronParser = new CronParser(cronDefinition);
            Cron parse = cronParser.parse(cronExpression);

            return ExecutionTime.forCron(parse);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Can not parse quartz expression " + cronExpression, e);
        }
    }
}
