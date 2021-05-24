package org.ikasan.dashboard.ui.util;

import com.vaadin.flow.component.UI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateFormatter
{
    public static final String DATE_FORMAT_TABLE_VIEWS = "dd/MM/yyyy HH:mm:ss.SSS '['VV '-' z']'";
    public static final DateTimeFormatter DATE_FORMAT_WITH_TIMEZONE = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    private DateTimeFormatter tableFormatter;

    public DateFormatter() {
        tableFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_TABLE_VIEWS);
    }

    public String getFormattedDate(long timestamp)
    {
        if(timestamp == 0)
        {
            return "N/A";
        }

        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
            DateTimeUtil.getZoneId());

        return this.tableFormatter.format(zdt);
    }

    public String getFormattedDate(ZonedDateTime dateTime)
    {
        return DATE_FORMAT_WITH_TIMEZONE.format(dateTime);
    }
}
