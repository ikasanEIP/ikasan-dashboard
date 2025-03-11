package org.ikasan.dashboard.ui.scheduler.component;

import com.cronutils.model.Cron;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.apache.commons.lang.StringUtils;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class JobPlanCronBuilderDialog extends CronBuilderDialog {

    private ContextTemplate contextTemplate;

    /**
     * Constructs a JobPlanCronBuilderDialog with the provided contextTemplate.
     *
     * @param contextTemplate the context template to be used for the dialog
     * @throws IllegalArgumentException if the contextTemplate is null
     */
    public JobPlanCronBuilderDialog(ContextTemplate contextTemplate) {
        this.contextTemplate = contextTemplate;
        if(this.contextTemplate == null) {
            throw new IllegalArgumentException("contextTemplate cannot be null!");
        }
    }

    @Override
    protected void setNaturalLanguageDescription(String cronExpression) {
        try {
            Cron quartzCron = parser.parse(cronExpression);
            if(this.contextTemplate.isCustomWeekDayOfMonth()) {
                String naturalDescription = descriptor.describe(quartzCron);
                String weekday = super.dayOfMonthPart.replace("W","");
                this.naturalLanguageTf.setValue(naturalDescription.replaceFirst
                    ("the nearest weekday to the \\b([1-9]|[12][0-9])\\b of the month"
                        , String.format("the %s week day of the month", weekday)));
            }
            else {
                this.naturalLanguageTf.setValue(descriptor.describe(quartzCron));
            }
        }
        catch (IllegalArgumentException e) {
            // ignore as the user might manually enter a bad cron that can not be converted to natural language.
        }
    }

    @Override
    protected Component getDaysLayout() {
        RadioButtonGroup<TimeComponent> radioGroup = new RadioButtonGroup<>();

        TimeComponent everyDay = this.getEveryDay();
        TimeComponent everyDayStartingOnDay = this.getEveryDayStartingOnDay();
        TimeComponent everyDayStartingOnCalendarDay = this.getEveryDayStartingOnCalendarDay();
        TimeComponent specificDay = this.getSpecificDay();
        TimeComponent specificDayOfWeek = this.getSpecificDayOfWeek();
        TimeComponent lastDayOfMonth = this.getLastDayOfMonth();
        TimeComponent lastWeekDayOfMonth = this.getLastWeekDayOfMonth();
        TimeComponent firstWeekDayOfMonth = this.getFirstWeekDayOfMonth();
        TimeComponent ikasanCustomNthWeekdayOfTheMonth = this.getIkasanCustomNthWeekDayOfMonth();
        TimeComponent closestWeekdayOfTheMonthToDay = this.getClosestToNthWeekDayOfMonth();
        TimeComponent lastDaySelectOfMonth = this.getLastDaySelectOfMonth();
        TimeComponent specificNthDayOfMonth = this.getSpecificNthDayOfMonth();

        radioGroup.setItems(List.of(everyDay
            , everyDayStartingOnDay
            , everyDayStartingOnCalendarDay
            , specificDay
            , specificDayOfWeek
            , specificNthDayOfMonth
            , lastDayOfMonth
            , firstWeekDayOfMonth
            , ikasanCustomNthWeekdayOfTheMonth
            , closestWeekdayOfTheMonthToDay
            , lastWeekDayOfMonth
            , lastDaySelectOfMonth));
        radioGroup.setValue(everyDay);
        radioGroup.setRenderer(new ComponentRenderer<>(timeComponent -> new Span(timeComponent.getName())));

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(radioGroup);

        if(this.dayOfMonthPart.equals("?") && this.dayOfWeekPart.equals("*")) {
            radioGroup.setValue(everyDay);
            layout.add(radioGroup, everyDay.getComponent());
        }
        if(this.dayOfMonthPart.equals("?") && this.dayOfWeekPart.contains("#")) {
            radioGroup.setValue(specificNthDayOfMonth);
            layout.add(radioGroup, specificNthDayOfMonth.getComponent());
        }
        else if(this.dayOfMonthPart.equals("?") && this.dayOfWeekPart.contains("/")) {
            radioGroup.setValue(everyDayStartingOnDay);
            layout.add(radioGroup, everyDayStartingOnDay.getComponent());
        }
        else if(this.dayOfMonthPart.contains("/") && this.dayOfWeekPart.equals("?")) {
            radioGroup.setValue(everyDayStartingOnCalendarDay);
            layout.add(radioGroup, everyDayStartingOnCalendarDay.getComponent());
        }
        else if(this.dayOfMonthPart.equals("?") && (this.dayOfWeekPart.contains("SUN") ||
            this.dayOfWeekPart.contains("MON") || this.dayOfWeekPart.contains("TUE") ||
            this.dayOfWeekPart.contains("WED") ||this.dayOfWeekPart.contains("THU") ||
            this.dayOfWeekPart.contains("FRI") || this.dayOfWeekPart.contains("SAT"))) {
            radioGroup.setValue(specificDayOfWeek);
            layout.add(radioGroup, specificDayOfWeek.getComponent());
        }
        else if((this.dayOfMonthPart.contains(",") || StringUtils.isNumeric(this.dayOfMonthPart))
            && this.dayOfWeekPart.contains("?")) {
            radioGroup.setValue(specificDay);
            layout.add(radioGroup, specificDay.getComponent());
        }
        else if(this.dayOfMonthPart.equals("L") && this.dayOfWeekPart.equals("?")) {
            radioGroup.setValue(lastDayOfMonth);
            layout.add(radioGroup, lastDayOfMonth.getComponent());
        }
        else if(this.dayOfMonthPart.equals("LW") && this.dayOfWeekPart.equals("?")) {
            radioGroup.setValue(lastWeekDayOfMonth);
            layout.add(radioGroup, lastWeekDayOfMonth.getComponent());
        }
        else if(this.dayOfMonthPart.equals("1W") && this.dayOfWeekPart.equals("?")) {
            radioGroup.setValue(firstWeekDayOfMonth);
            layout.add(radioGroup, firstWeekDayOfMonth.getComponent());
        }
        else if(this.dayOfMonthPart.contains("W")
            && !this.dayOfMonthPart.equals("LW")
            && this.dayOfWeekPart.equals("?")
            && this.contextTemplate.isCustomWeekDayOfMonth()) {
            radioGroup.setValue(ikasanCustomNthWeekdayOfTheMonth);
            layout.add(radioGroup, ikasanCustomNthWeekdayOfTheMonth.getComponent());
        }
        else if(this.dayOfMonthPart.contains("W")
            && !this.dayOfMonthPart.equals("LW")
            && this.dayOfWeekPart.equals("?")
            && !this.contextTemplate.isCustomWeekDayOfMonth()) {
            radioGroup.setValue(closestWeekdayOfTheMonthToDay);
            layout.add(radioGroup, closestWeekdayOfTheMonthToDay.getComponent());
        }
        else if(this.dayOfMonthPart.equals("?") && this.dayOfWeekPart.contains("L")) {
            radioGroup.setValue(lastDaySelectOfMonth);
            layout.add(radioGroup, lastDaySelectOfMonth.getComponent());
        }

        radioGroup.addValueChangeListener(event -> {
            if(event.getValue().equals(everyDayStartingOnDay) || event.getValue().equals(specificDayOfWeek)
                || event.getValue().equals(lastDaySelectOfMonth) || event.getValue().equals(specificNthDayOfMonth)) {
                this.dayOfWeekPart = event.getValue().getValue();
                this.dayOfMonthPart = "?";
            }
            else if(event.getValue().equals(everyDay)) {
                this.dayOfWeekPart = "*";
                this.dayOfMonthPart = "?";
            }
            else if(event.getValue().equals(ikasanCustomNthWeekdayOfTheMonth)) {
                this.dayOfMonthPart = event.getValue().getValue();
                this.dayOfWeekPart = "?";
                this.contextTemplate.setCustomWeekDayOfMonth(true);
            }
            else if(event.getValue().equals(closestWeekdayOfTheMonthToDay)) {
                this.dayOfMonthPart = event.getValue().getValue();
                this.dayOfWeekPart = "?";
                this.contextTemplate.setCustomWeekDayOfMonth(false);
            }
            else {
                this.dayOfMonthPart = event.getValue().getValue();
                this.dayOfWeekPart = "?";
            }
            this.cronExpressionTf.setValue(this.getCronExpression());

            layout.removeAll();
            layout.add(radioGroup, event.getValue().getComponent());
        });

        return layout;
    }

    /**
     * Retrieves a custom time component representing the nth week day of the month.
     * This method constructs a layout consisting of labels, select box, help button, and help dialog
     * for selecting the nth week day of the month in a cron schedule.
     *
     * @return TimeComponent representing the custom nth week day of the month
     */
    TimeComponent getIkasanCustomNthWeekDayOfMonth() {
        NativeLabel prefix = new NativeLabel(getTranslation("time-component.custom-week-day-of-month-prefix"
            , UI.getCurrent().getLocale()));
        NativeLabel suffix = new NativeLabel(getTranslation("time-component.custom-week-day-of-month-suffix"
            , UI.getCurrent().getLocale()));

        Icon helpIcon = new Icon(VaadinIcon.QUESTION_CIRCLE);
        helpIcon.setSize("18px");
        Button helpButton = new Button("Help", helpIcon);
        helpButton.setId("helpButton");
        helpButton.setIconAfterText(false);
        helpButton.setHeight("32px");

        helpButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            HelpDialog helpDialog = new HelpDialog(getTranslation("time-component.custom-week-day-of-month-help"
                , UI.getCurrent().getLocale()));
            helpDialog.open();
        });

        List<String> days = new ArrayList<>();
        // The maximum number of weekdays in a month is 23.
        IntStream.range(2, 24).forEach(i -> days.add(Integer.toString(i)));
        Select<String> daySelect = new Select<>();
        daySelect.setEnabled(true);
        daySelect.setItems(days);
        if(this.dayOfMonthPart.contains("W") && !this.dayOfMonthPart.equals("LW")) {
            daySelect.setValue(this.dayOfMonthPart.replace("W", ""));
        }
        else {
            daySelect.setValue("1");
        }

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(prefix, daySelect, suffix, helpButton);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, prefix, suffix, helpButton);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.ikasan-custom-nth-weekday-of-the-month"
            , UI.getCurrent().getLocale()), layout);
        timeComponent.setValue(daySelect.getValue()+"W");

        daySelect.addValueChangeListener(event -> {
            timeComponent.setValue(daySelect.getValue()+"W");
            this.dayOfWeekPart = "?";
            this.dayOfMonthPart = timeComponent.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        return timeComponent;
    }

    /**
     * Retrieves a custom time component representing the nth week day of the month.
     * This method constructs a layout consisting of labels, select box, help button, and help dialog
     * for selecting the nth week day of the month in a cron schedule.
     *
     * @return TimeComponent representing the custom nth week day of the month
     */
    TimeComponent getClosestToNthWeekDayOfMonth() {
        NativeLabel prefix = new NativeLabel(getTranslation("time-component.closest-week-day-of-month-prefix"
            , UI.getCurrent().getLocale()));
        NativeLabel suffix = new NativeLabel(getTranslation("time-component.closest-week-day-of-month-suffix"
            , UI.getCurrent().getLocale()));

        Icon helpIcon = new Icon(VaadinIcon.QUESTION_CIRCLE);
        helpIcon.setSize("18px");
        Button helpButton = new Button("Help", helpIcon);
        helpButton.setId("helpButton");
        helpButton.setIconAfterText(false);
        helpButton.setHeight("32px");

        helpButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            HelpDialog helpDialog = new HelpDialog(getTranslation("time-component.closest-week-day-of-month-help"
                , UI.getCurrent().getLocale()));
            helpDialog.open();
        });

        List<String> days = new ArrayList<>();
        // The maximum number of weekdays in a month is 31.
        IntStream.range(2, 32).forEach(i -> days.add(Integer.toString(i)));
        Select<String> daySelect = new Select<>();
        daySelect.setEnabled(true);
        daySelect.setItems(days);
        if(this.dayOfMonthPart.contains("W") && !this.dayOfMonthPart.equals("LW")) {
            daySelect.setValue(this.dayOfMonthPart.replace("W", ""));
        }
        else {
            daySelect.setValue("1");
        }

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(prefix, daySelect, suffix, helpButton);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, prefix, suffix, helpButton);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.closest-weekday-of-the-month-to-day"
            , UI.getCurrent().getLocale()), layout);
        timeComponent.setValue(daySelect.getValue()+"W");

        daySelect.addValueChangeListener(event -> {
            timeComponent.setValue(daySelect.getValue()+"W");
            this.dayOfWeekPart = "?";
            this.dayOfMonthPart = timeComponent.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        return timeComponent;
    }
}
