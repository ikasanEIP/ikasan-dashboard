package org.ikasan.job.orchestration.context.util;

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import static com.cronutils.model.field.expression.FieldExpressionFactory.on;
import static com.cronutils.model.field.expression.FieldExpressionFactory.questionMark;

public class CronUtils {

    /**
     * Helper method to take a start time in millis from epoch, add an offset to it and a zone context,
     * and return a relevant quartz cron expressions that represents the offset time.
     *
     * @param startTime
     * @param offset
     * @param zoneId
     * @return
     */
    public static String buildCronFromOriginalWithMillisecondOffset(long startTime, long offset, String zoneId) {
        Instant instant = Instant.ofEpochMilli(startTime);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId != null && !zoneId.isEmpty() ? ZoneId.of(zoneId) : ZoneId.systemDefault());
        zonedDateTime = zonedDateTime.plus(offset, ChronoUnit.MILLIS);

        Cron cron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
            .withSecond(on(zonedDateTime.get(ChronoField.SECOND_OF_MINUTE)))
            .withMinute(on(zonedDateTime.get(ChronoField.MINUTE_OF_HOUR)))
            .withHour(on(zonedDateTime.get(ChronoField.HOUR_OF_DAY)))
            .withDoW(questionMark())
            .withDoM(on(zonedDateTime.get(ChronoField.DAY_OF_MONTH)))
            .withMonth(on(zonedDateTime.get(ChronoField.MONTH_OF_YEAR)))
            .withYear(on(zonedDateTime.get(ChronoField.YEAR)))
            .instance();

        return cron.asString();
    }

    /**
     * Helper method to take a start time in millis from epoch create cron expression.
     *
     * @param startTime
     * @param zoneId
     * @return
     */
    public static String buildCronFromOriginal(long startTime, String zoneId) {
        Instant instant = Instant.ofEpochMilli(startTime);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId != null && !zoneId.isEmpty() ? ZoneId.of(zoneId) : ZoneId.systemDefault());

        Cron cron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
            .withSecond(on(zonedDateTime.get(ChronoField.SECOND_OF_MINUTE)))
            .withMinute(on(zonedDateTime.get(ChronoField.MINUTE_OF_HOUR)))
            .withHour(on(zonedDateTime.get(ChronoField.HOUR_OF_DAY)))
            .withDoW(questionMark())
            .withDoM(on(zonedDateTime.get(ChronoField.DAY_OF_MONTH)))
            .withMonth(on(zonedDateTime.get(ChronoField.MONTH_OF_YEAR)))
            .withYear(on(zonedDateTime.get(ChronoField.YEAR)))
            .instance();

        return cron.asString();
    }
}
