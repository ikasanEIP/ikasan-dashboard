package org.ikasan.dashboard.ui.util;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateFormatter
{
    public static final String DATE_FORMAT_TABLE_VIEWS = "dd/MM/yyyy HH:mm:ss.SSS";
    public static final DateTimeFormatter DATE_FORMAT_WITH_TIMEZONE = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    private static SimpleDateFormat tableFormatter;

    static
    {
        tableFormatter = new SimpleDateFormat(DATE_FORMAT_TABLE_VIEWS);
    }

    public static String getFormattedDate(long timestamp)
    {
        if(timestamp == 0)
        {
            return "N/A";
        }

        Date date = new Date(timestamp);

        return tableFormatter.format(date);
    }

    public static String getFormattedDateWithTimezone(ZonedDateTime dateTime)
    {
        return DATE_FORMAT_WITH_TIMEZONE.format(dateTime);
    }
}
