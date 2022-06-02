package org.ikasan.job.orchestration.context.util;

import static com.cronutils.model.CronType.QUARTZ;

import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.quartz.CronExpression;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

public class QuartzTimeWindowChecker {

    public static boolean outsideOfOperatingWindow(String startTimeWindow, String endTimeWindow, Date dateTime) {
        return !withinOperatingWindow(startTimeWindow, endTimeWindow, dateTime);
    }

    public static boolean withinOperatingWindow(String startTimeWindow, String endTimeWindow, Date dateTime) {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(dateTime.toInstant(), ZoneId.systemDefault());

        ZonedDateTime nextExecutionStart = getNextExecution(startTimeWindow, zonedDateTime);
        ZonedDateTime previousExecutionStart = getPreviousExecution(startTimeWindow, zonedDateTime);
        ZonedDateTime executionEnd = getNextExecution(endTimeWindow, zonedDateTime);

        ZonedDateTime startExecution = nextExecutionStart.isAfter(executionEnd) ? previousExecutionStart : nextExecutionStart;

        if (isOnFireTime(startTimeWindow, endTimeWindow, dateTime)) {
            return true;
        } else {
            return zonedDateTime.isAfter(startExecution) && zonedDateTime.isBefore(executionEnd);
        }
    }

    private static boolean isOnFireTime(String startTimeWindow, String endTimeWindow, Date dateTime) {
        // we are in the operating window if isSatisfiedBy returns true - used for when its exactly in the fire time
        try {
            CronExpression cronExpressionStart = new CronExpression(startTimeWindow);
            CronExpression cronExpressionEnd = new CronExpression(endTimeWindow);
            return cronExpressionStart.isSatisfiedBy(dateTime) || cronExpressionEnd.isSatisfiedBy(dateTime);
        } catch (ParseException e) {
            throw new RuntimeException("Can not parse quartz expression " + startTimeWindow + " or " + endTimeWindow);
        }
    }

    private static ZonedDateTime getPreviousExecution(String endTimeWindow, ZonedDateTime now) {
        ExecutionTime executionTime = getExecutionTime(endTimeWindow);
        return executionTime.lastExecution(now).orElse(null);
    }

    private static ZonedDateTime getNextExecution(String startTimeWindow, ZonedDateTime now) {
        ExecutionTime executionTime = getExecutionTime(startTimeWindow);
        return executionTime.nextExecution(now).orElse(null);
    }

    private static ExecutionTime getExecutionTime(String cronExpression) {
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
        CronParser cronParser = new CronParser(cronDefinition);
        Cron parse = cronParser.parse(cronExpression);
        return ExecutionTime.forCron(parse);
    }
}
