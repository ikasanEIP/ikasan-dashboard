package org.ikasan.dashboard.ui.scheduler.component;

import com.cronutils.model.Cron;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CronBuilderDialog extends AbstractCronBuilderDialog {
    private Logger logger = LoggerFactory.getLogger(CronBuilderDialog.class);

    @Override
    protected void setNaturalLanguageDescription(String cronExpression) {
        try {
            Cron quartzCron = parser.parse(cronExpression);
            this.naturalLanguageTf.setValue(descriptor.describe(quartzCron));
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
}