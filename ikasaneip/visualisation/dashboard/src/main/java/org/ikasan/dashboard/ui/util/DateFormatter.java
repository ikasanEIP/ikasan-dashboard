package org.ikasan.dashboard.ui.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Component
public class DateFormatter
{
    public static final String DATE_FORMAT_TABLE_VIEWS = "dd/MM/yyyy HH:mm:ss.SSS";
    public static final DateTimeFormatter DATE_FORMAT_WITH_TIMEZONE = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    @Value("${deployment.timezone.id}")
    public String zoneId;

    private static SimpleDateFormat tableFormatter;

    public DateFormatter() {
        tableFormatter = new SimpleDateFormat(DATE_FORMAT_TABLE_VIEWS);
    }

    public String getFormattedDate(long timestamp)
    {
        if(timestamp == 0)
        {
            return "N/A";
        }

        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
            ZoneId.of(zoneId));

        return getFormattedDateWithTimezone(zdt);
    }

    public String getFormattedDateWithTimezone(ZonedDateTime dateTime)
    {
        return DATE_FORMAT_WITH_TIMEZONE.format(dateTime);
    }
}
