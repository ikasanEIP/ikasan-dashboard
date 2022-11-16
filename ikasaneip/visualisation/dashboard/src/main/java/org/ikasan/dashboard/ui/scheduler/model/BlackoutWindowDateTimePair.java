package org.ikasan.dashboard.ui.scheduler.model;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

public class BlackoutWindowDateTimePair {
    private DateTimePicker blackoutWindowStartTime;
    private DateTimePicker blackoutWindowEndTime;

    public BlackoutWindowDateTimePair() {
        this.blackoutWindowStartTime = new DateTimePicker();
        this.blackoutWindowStartTime.setStep(Duration.ofMinutes(15));
        this.blackoutWindowStartTime.setLocale(new Locale("en", "GB"));
        this.blackoutWindowEndTime = new DateTimePicker();
        this.blackoutWindowEndTime.setStep(Duration.ofMinutes(15));
        this.blackoutWindowEndTime.setLocale(new Locale("en", "GB"));
    }

    public BlackoutWindowDateTimePair(long start, long end) {
        this.blackoutWindowStartTime = new DateTimePicker();
        this.blackoutWindowStartTime.setStep(Duration.ofMinutes(15));
        this.blackoutWindowStartTime.setValue(LocalDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneOffset.UTC));
        this.blackoutWindowStartTime.setLocale(new Locale("en", "GB"));
        this.blackoutWindowEndTime = new DateTimePicker();
        this.blackoutWindowEndTime.setStep(Duration.ofMinutes(15));
        this.blackoutWindowEndTime.setValue(LocalDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneOffset.UTC));
        this.blackoutWindowEndTime.setLocale(new Locale("en", "GB"));
    }

    public DateTimePicker getBlackoutWindowStartTime() {
        return blackoutWindowStartTime;
    }

    public DateTimePicker getBlackoutWindowEndTime() {
        return blackoutWindowEndTime;
    }
}
