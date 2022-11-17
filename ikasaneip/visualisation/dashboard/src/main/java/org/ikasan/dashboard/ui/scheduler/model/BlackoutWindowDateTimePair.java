package org.ikasan.dashboard.ui.scheduler.model;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;

import java.time.*;
import java.util.Locale;

public class BlackoutWindowDateTimePair {
    private final DateTimePicker blackoutWindowStartTime;
    private final DateTimePicker blackoutWindowEndTime;

    public BlackoutWindowDateTimePair() {
        this.blackoutWindowStartTime = new DateTimePicker();
        this.blackoutWindowStartTime.setStep(Duration.ofMinutes(15));
        this.blackoutWindowStartTime.setLocale(new Locale("en", "GB"));
        this.blackoutWindowEndTime = new DateTimePicker();
        this.blackoutWindowEndTime.setStep(Duration.ofMinutes(15));
        this.blackoutWindowEndTime.setLocale(new Locale("en", "GB"));
    }

    public BlackoutWindowDateTimePair(long start, long end, String zoneId) {
        LocalDateTime now = LocalDateTime.now();
        ZoneId zone = ZoneId.of(zoneId);
        ZoneOffset zoneOffSet = zone.getRules().getOffset(now);

        this.blackoutWindowStartTime = new DateTimePicker();
        this.blackoutWindowStartTime.setStep(Duration.ofMinutes(15));
        this.blackoutWindowStartTime.setValue(LocalDateTime.ofInstant(Instant.ofEpochMilli(start), zoneOffSet));
        this.blackoutWindowStartTime.setLocale(new Locale("en", "GB"));
        this.blackoutWindowEndTime = new DateTimePicker();
        this.blackoutWindowEndTime.setStep(Duration.ofMinutes(15));
        this.blackoutWindowEndTime.setValue(LocalDateTime.ofInstant(Instant.ofEpochMilli(end), zoneOffSet));
        this.blackoutWindowEndTime.setLocale(new Locale("en", "GB"));
    }

    public DateTimePicker getBlackoutWindowStartTime() {
        return blackoutWindowStartTime;
    }

    public DateTimePicker getBlackoutWindowEndTime() {
        return blackoutWindowEndTime;
    }
}
