package org.ikasan.job.orchestration.context.util;

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import com.cronutils.model.field.expression.On;
import com.cronutils.parser.CronParser;
import org.ikasan.spec.scheduled.context.model.Context;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import static com.cronutils.model.CronType.QUARTZ;
import static com.cronutils.model.field.expression.FieldExpressionFactory.*;

public class CustomWeekdayOfMonthHelper {

    private static Set<DayOfWeek> WEEKDAYS = new HashSet<>();

    static {
        WEEKDAYS.add(DayOfWeek.MONDAY);
        WEEKDAYS.add(DayOfWeek.TUESDAY);
        WEEKDAYS.add(DayOfWeek.WEDNESDAY);
        WEEKDAYS.add(DayOfWeek.THURSDAY);
        WEEKDAYS.add(DayOfWeek.FRIDAY);
    }

    /**
     * Determines the cron expression for the start time based on the context and current date.
     *
     * @param context The context object containing information about the time window start and custom settings.
     * @param currentDate The current date to be used in calculations.
     * @return The cron expression for the start time based on the context and current date.
     */
    public static String determineContextStartCron(Context context, LocalDateTime currentDate) {
        if(!context.isCustomWeekDayOfMonth()) {
            return context.getTimeWindowStart();
        }
        else {
            if(context.getTimeWindowStart() != null) {
                CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));
                Cron cron = cronParser.parse(context.getTimeWindowStart());

                CronField dayOfMonth = cron.retrieve(CronFieldName.DAY_OF_MONTH);
                CronField hour = cron.retrieve(CronFieldName.HOUR);
                CronField minute = cron.retrieve(CronFieldName.MINUTE);
                CronField second = cron.retrieve(CronFieldName.SECOND);

                if(dayOfMonth.getExpression() instanceof On
                    && ((On)dayOfMonth.getExpression()).getSpecialChar().getValue().toString().equals("W")
                    && ((On)dayOfMonth.getExpression()).getTime().getValue() > 1
                    && hour.getExpression() instanceof On
                    && minute.getExpression() instanceof On
                    && second.getExpression() instanceof On) {

                    int h = ((On)hour.getExpression()).getTime().getValue();
                    int m = ((On)minute.getExpression()).getTime().getValue();
                    int s = ((On)second.getExpression()).getTime().getValue();

                    if(hasTheTimeAlreadyFallenOfThisMonthAlreadyFallen
                        (((On)dayOfMonth.getExpression()).getTime().getValue(), h, m, s, currentDate)) {
                        currentDate = currentDate.plus(1, ChronoUnit.MONTHS);
                    }

                    LocalDateTime nthWeekDayOfMonth = getNthWeekDayOfMonth
                        ((((On)dayOfMonth.getExpression()).getTime().getValue()), h, m, s, currentDate);

                    Cron finalCron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
                        .withYear(on(nthWeekDayOfMonth.getYear()))
                        .withDoM(on(nthWeekDayOfMonth.getDayOfMonth()))
                        .withMonth(on(nthWeekDayOfMonth.getMonthValue()))
                        .withDoW(questionMark())
                        .withHour(cron.retrieve(CronFieldName.HOUR).getExpression())
                        .withMinute(cron.retrieve(CronFieldName.MINUTE).getExpression())
                        .withSecond(cron.retrieve(CronFieldName.SECOND).getExpression())
                        .instance();

                    return finalCron.asString();
                }
            }
        }

        return context.getTimeWindowStart();
    }

    /**
     * Checks if the given context represents a custom weekday of the month.
     *
     * @param context The context object containing information about the time window start and custom settings.
     * @return true if the context represents a custom weekday of the month that has a specific rule definition, false otherwise.
     */
    public static boolean isCustomWeekdayOfMonth(Context context) {
        if(!context.isCustomWeekDayOfMonth()) return false;

        CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));
        Cron cron = cronParser.parse(context.getTimeWindowStart());

        CronField dayOfMonth = cron.retrieve(CronFieldName.DAY_OF_MONTH);

        return (dayOfMonth.getExpression() instanceof On
            && ((On)dayOfMonth.getExpression()).getSpecialChar().getValue().toString().equals("W")
            && ((On)dayOfMonth.getExpression()).getTime().getValue() > 1);
    }

    /**
     * Checks if the nth weekday of this month has already occurred before the given date.
     *
     * @param weekdayOfMonth The position of the weekday in the month (e.g., 2 for the second Tuesday).
     * @param currentDate The date to compare against.
     * @return true if the nth weekday of this month has already occurred before the given date, false otherwise.
     */
    protected static boolean hasTheTimeAlreadyFallenOfThisMonthAlreadyFallen(int weekdayOfMonth, int hour
        , int minute, int second, LocalDateTime currentDate) {
        LocalDateTime nthWeekdayOfMonth = getNthWeekDayOfMonth(weekdayOfMonth, hour, minute, second, currentDate);

        return nthWeekdayOfMonth.isBefore(currentDate) || nthWeekdayOfMonth.equals(currentDate);
    }

    /**
     * Returns the nth weekday of the month based on the specified position and current date.
     *
     * @param weekdayOfMonth The position of the weekday in the month (e.g., 2 for the second Tuesday).
     * @param currentDate The current date used as reference for the calculation.
     * @return The LocalDate representing the nth weekday of the month.
     */
    protected static LocalDateTime getNthWeekDayOfMonth(int weekdayOfMonth, int hour
        , int minute, int second, LocalDateTime currentDate) {
        YearMonth ym = YearMonth.of(currentDate.getYear(), currentDate.getMonth());
        int daysInMonth = ym.lengthOfMonth();

        int weekdayCounter = 0;
        LocalDateTime nthDayOfWeek = null;
        for (int i=1; i<daysInMonth + 1; i++) {
            nthDayOfWeek = ym.atDay(i).atStartOfDay();
            if(WEEKDAYS.contains(nthDayOfWeek.getDayOfWeek())) {
                weekdayCounter++;
            }
            if(weekdayCounter == weekdayOfMonth) {
                break;
            }
        }

        nthDayOfWeek = nthDayOfWeek.withHour(hour);
        nthDayOfWeek = nthDayOfWeek.withMinute(minute);
        nthDayOfWeek = nthDayOfWeek.withSecond(second);
        return nthDayOfWeek;
    }
}
