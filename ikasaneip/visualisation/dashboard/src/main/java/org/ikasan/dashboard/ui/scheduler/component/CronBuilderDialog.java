package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.componentfactory.gridlayout.GridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class CronBuilderDialog extends AbstractCloseableResizableDialog {

    private Tab secondsTab;
    private Tab minutesTab;
    private Tab hoursTab;
    private Tab daysTab;
    private Tabs tabs;

    private TextField cronTextField;
    private String secondPart = "*";
    private String minutePart = "*";
    private String hourPart = "*";
    private String dayOfMonthPart = "?";
    private String monthPart = "*";
    private String dayOfWeekPart = "*";
    private String yearPart = "*";

    public CronBuilderDialog() {
        super.showResize(false);
        // todo translation
        super.title.setText("Cron Builder");

        this.cronTextField = new TextField("Cron Expression");
        this.cronTextField.setValue(this.getCronExpression());

        // todo translation
        this.secondsTab = new Tab("Seconds");
        this.secondsTab.setId("secondsTab");
        this.minutesTab = new Tab("Minutes");
        this.minutesTab.setId("minutesTab");
        this.hoursTab = new Tab("Hours");
        this.hoursTab.setId("hoursTab");
        this.daysTab = new Tab("Days");
        this.daysTab.setId("daysTab");
        this.tabs = new Tabs(this.secondsTab, this.minutesTab, this.hoursTab, this.daysTab);

        Component secondsLayout = this.getSecondsLayout();
        Component minutesLayout = this.getMinutesLayout();
        minutesLayout.setVisible(false);
        Component hoursLayout = this.getHoursLayout();
        hoursLayout.setVisible(false);
        Component daysLayout = getDaysLayout();
        daysLayout.setVisible(false);

        Map<Tab, Component> tabsToPages = new HashMap<>();
        tabsToPages.put(this.secondsTab, secondsLayout);
        tabsToPages.put(this.minutesTab, minutesLayout);
        tabsToPages.put(this.hoursTab, hoursLayout);
        tabsToPages.put(this.daysTab, daysLayout);

        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            com.vaadin.flow.component.Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
            selectedPage.setVisible(true);
        });

        Button okButton = new Button("Ok");
        VerticalLayout buttonLayout = new VerticalLayout();
        buttonLayout.setWidth("100%");
        buttonLayout.add(okButton);
        buttonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, okButton);

        super.content.add(this.cronTextField, tabs, secondsLayout, minutesLayout, hoursLayout, daysLayout, buttonLayout);

        this.setWidth("1000px");
        this.setHeight("600px");
    }

    private Component getSecondsLayout() {
        RadioButtonGroup<TimeComponent> radioGroup = new RadioButtonGroup<>();

        TimeComponent everySecond = this.getEverySecond();
        radioGroup.setItems(List.of(everySecond
            , this.getEverySecondStartingAt()
            , this.getSpecificSecond()
            , this.getEverySecondBetween()));
        radioGroup.setValue(everySecond);

        radioGroup.setRenderer(new ComponentRenderer<>(timeComponent -> new Span(timeComponent.getName())));

        VerticalLayout horizontalLayout = new VerticalLayout();
        horizontalLayout.setSizeFull();
        horizontalLayout.add(radioGroup);

        radioGroup.addValueChangeListener(event -> {
            horizontalLayout.removeAll();
            horizontalLayout.add(radioGroup, event.getValue().getComponent());
        });

        return horizontalLayout;
    }

    private TimeComponent getEverySecond() {
        Label label = new Label("Every Second");
        TimeComponent timeComponent = new TimeComponent("Every second", label);

        return timeComponent;
    }

    private TimeComponent getEverySecondStartingAt() {
        Label label = new Label("Every ");
        List<String> seconds = new ArrayList<>();
        IntStream.range(1, 60).forEach(i -> seconds.add(Integer.toString(i)));
        Select<String> secondSelect = new Select<>();
        secondSelect.setEnabled(true);
        secondSelect.setItems(seconds);
        secondSelect.setValue("1");


        List<String> secondsStart = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> secondsStart.add(Integer.toString(i)));
        Select<String> secondStartSelect = new Select<>();
        secondStartSelect.setEnabled(true);
        secondStartSelect.setItems(secondsStart);
        secondStartSelect.setValue("0");

        secondSelect.addValueChangeListener(event -> {
            this.secondPart = event.getValue() + "/" + secondStartSelect.getValue();
            this.cronTextField.setValue(this.getCronExpression());
        });

        secondStartSelect.addValueChangeListener(event -> {
            this.secondPart = secondSelect.getValue() + "/" + event.getValue();
            this.cronTextField.setValue(this.getCronExpression());
        });

        Label label2 = new Label(" seconds starting at ");

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, secondSelect, label2, secondStartSelect);

        TimeComponent timeComponent = new TimeComponent("Every n seconds starting at", layout);
        return timeComponent;
    }

    private TimeComponent getEverySecondBetween() {
        Label label = new Label("Every second between second ");
        List<String> seconds = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> seconds.add(Integer.toString(i)));
        Select<String> secondSelect = new Select<>();
        secondSelect.setEnabled(true);
        secondSelect.setItems(seconds);


        List<String> secondsStart = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> secondsStart.add(Integer.toString(i)));
        Select<String> secondStartSelect = new Select<>();
        secondStartSelect.setEnabled(true);
        secondStartSelect.setItems(seconds);

        Label label2 = new Label(" and second ");

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, secondSelect, label2, secondStartSelect);

        TimeComponent timeComponent = new TimeComponent("Every second between", layout);

        return timeComponent;
    }

    private TimeComponent getSpecificSecond() {

        List<Checkbox> secondsStart = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> secondsStart.add(new Checkbox(Integer.toString(i))));

        GridLayout layout = new GridLayout(12, 5);
        secondsStart.forEach(item -> {
            item.addValueChangeListener(event -> {

            });
            layout.addComponent(item);
        });
        layout.setSizeFull();

        TimeComponent timeComponent = new TimeComponent("Specific second (multiple selectable)", layout);

        return timeComponent;
    }

    private Component getMinutesLayout() {
        RadioButtonGroup<TimeComponent> radioGroup = new RadioButtonGroup<>();

        radioGroup.setItems(List.of(this.getEveryMinute()
            , this.getEveryMinuteStartingAt()
            , this.getSpecificMinute()
            , this.getEveryMinuteBetween()));

        radioGroup.setRenderer(new ComponentRenderer<>(timeComponent -> new Span(timeComponent.getName())));

        VerticalLayout horizontalLayout = new VerticalLayout();
        horizontalLayout.setSizeFull();
        horizontalLayout.add(radioGroup);

        radioGroup.addValueChangeListener(event -> {
            horizontalLayout.removeAll();
            horizontalLayout.add(radioGroup, event.getValue().getComponent());
        });

        return horizontalLayout;
    }

    private TimeComponent getEveryMinute() {
        TimeComponent timeComponent = new TimeComponent("Every minute", new Div());

        return timeComponent;
    }

    private TimeComponent getEveryMinuteStartingAt() {
        Label label = new Label("Every ");
        List<String> seconds = new ArrayList<>();
        IntStream.range(1, 60).forEach(i -> seconds.add(Integer.toString(i)));
        Select<String> secondSelect = new Select<>();
        secondSelect.setEnabled(true);
        secondSelect.setItems(seconds);


        List<String> secondsStart = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> secondsStart.add(Integer.toString(i)));
        Select<String> secondStartSelect = new Select<>();
        secondStartSelect.setEnabled(true);
        secondStartSelect.setItems(seconds);

        Label label2 = new Label(" minutes starting at ");

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, secondSelect, label2, secondStartSelect);

        TimeComponent timeComponent = new TimeComponent("Every n seconds starting at", layout);

        return timeComponent;
    }

    private TimeComponent getEveryMinuteBetween() {
        Label label = new Label("Every minute between minute ");
        List<String> seconds = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> seconds.add(Integer.toString(i)));
        Select<String> secondSelect = new Select<>();
        secondSelect.setEnabled(true);
        secondSelect.setItems(seconds);


        List<String> secondsStart = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> secondsStart.add(Integer.toString(i)));
        Select<String> secondStartSelect = new Select<>();
        secondStartSelect.setEnabled(true);
        secondStartSelect.setItems(seconds);

        Label label2 = new Label(" and minute ");

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, secondSelect, label2, secondStartSelect);

        TimeComponent timeComponent = new TimeComponent("Every minute between", layout);

        return timeComponent;
    }

    private TimeComponent getSpecificMinute() {

        List<Component> secondsStart = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> secondsStart.add(new Checkbox(Integer.toString(i))));

        GridLayout layout = new GridLayout(12, 5);
        secondsStart.forEach(item -> layout.addComponent(item));
        layout.setSizeFull();

        TimeComponent timeComponent = new TimeComponent("Specific minute (multiple selectable)", layout);

        return timeComponent;
    }

    private Component getHoursLayout() {
        RadioButtonGroup<TimeComponent> radioGroup = new RadioButtonGroup<>();

        radioGroup.setItems(List.of(this.getEveryHour()
            , this.getEveryHourStartingAt()
            , this.getSpecificHour()
            , this.getEveryHourBetween()));

        radioGroup.setRenderer(new ComponentRenderer<>(timeComponent -> new Span(timeComponent.getName())));

        VerticalLayout horizontalLayout = new VerticalLayout();
        horizontalLayout.setSizeFull();
        horizontalLayout.add(radioGroup);

        radioGroup.addValueChangeListener(event -> {
            horizontalLayout.removeAll();
            horizontalLayout.add(radioGroup, event.getValue().getComponent());
        });

        return horizontalLayout;
    }

    private TimeComponent getEveryHour() {
        TimeComponent timeComponent = new TimeComponent("Every hour", new Div());

        return timeComponent;
    }

    private String getCronExpression() {
        StringBuffer cronExpression = new StringBuffer();

        cronExpression.append(this.secondPart).append(" ");
        cronExpression.append(this.minutePart).append(" ");
        cronExpression.append(this.hourPart).append(" ");
        cronExpression.append(this.dayOfMonthPart).append(" ");
        cronExpression.append(this.monthPart).append(" ");
        cronExpression.append(this.dayOfWeekPart).append(" ");
        cronExpression.append(this.yearPart);

        return cronExpression.toString();
    }

    private TimeComponent getEveryHourStartingAt() {
        Label label = new Label("Every ");
        List<String> seconds = new ArrayList<>();
        IntStream.range(1, 25).forEach(i -> seconds.add(Integer.toString(i)));
        Select<String> secondSelect = new Select<>();
        secondSelect.setEnabled(true);
        secondSelect.setItems(seconds);


        List<String> secondsStart = new ArrayList<>();
        IntStream.range(0, 24).forEach(i -> secondsStart.add(Integer.toString(i)));
        Select<String> secondStartSelect = new Select<>();
        secondStartSelect.setEnabled(true);
        secondStartSelect.setItems(secondsStart);

        Label label2 = new Label(" hour(s) starting at ");

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, secondSelect, label2, secondStartSelect);

        TimeComponent timeComponent = new TimeComponent("Every n hour(s) starting at", layout);

        return timeComponent;
    }

    private TimeComponent getEveryHourBetween() {
        Label label = new Label("Every hour between hour ");
        List<String> seconds = new ArrayList<>();
        IntStream.range(0, 24).forEach(i -> seconds.add(Integer.toString(i)));
        Select<String> secondSelect = new Select<>();
        secondSelect.setEnabled(true);
        secondSelect.setItems(seconds);


        List<String> secondsStart = new ArrayList<>();
        IntStream.range(0, 24).forEach(i -> secondsStart.add(Integer.toString(i)));
        Select<String> secondStartSelect = new Select<>();
        secondStartSelect.setEnabled(true);
        secondStartSelect.setItems(secondsStart);

        Label label2 = new Label(" and hour ");

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, secondSelect, label2, secondStartSelect);

        TimeComponent timeComponent = new TimeComponent("Every hour between", layout);

        return timeComponent;
    }

    private TimeComponent getSpecificHour() {

        List<Component> secondsStart = new ArrayList<>();
        IntStream.range(0, 24).forEach(i -> secondsStart.add(new Checkbox(Integer.toString(i))));

        GridLayout layout = new GridLayout(12, 5);
        secondsStart.forEach(item -> layout.addComponent(item));
        layout.setSizeFull();

        TimeComponent timeComponent = new TimeComponent("Specific hour (multiple selectable)", layout);

        return timeComponent;
    }

    private Component getDaysLayout() {
        RadioButtonGroup<TimeComponent> radioGroup = new RadioButtonGroup<>();

        radioGroup.setItems(List.of(this.getEveryDay()
            , this.getEveryDayStartingOnDay()
            , this.getEveryDayStartingOnCalendarDay()
            , this.getSpecificDay()
            , this.getSpecificDayOfWeek()
            , this.getLastDayOfMonth()
            , this.getLastWeekOfMonth()
            , this.getLastDaySelectOfMonth()));

        radioGroup.setRenderer(new ComponentRenderer<>(timeComponent -> new Span(timeComponent.getName())));

        VerticalLayout horizontalLayout = new VerticalLayout();
        horizontalLayout.setSizeFull();
        horizontalLayout.add(radioGroup);

        radioGroup.addValueChangeListener(event -> {
            horizontalLayout.removeAll();
            horizontalLayout.add(radioGroup, event.getValue().getComponent());
        });

        return horizontalLayout;
    }

    private TimeComponent getEveryDay() {
        TimeComponent timeComponent = new TimeComponent("Every day", new Div());

        return timeComponent;
    }

    private TimeComponent getEveryDayStartingOnDay() {
        Label label = new Label("Every ");
        List<String> days = new ArrayList<>();
        IntStream.range(1, 8).forEach(i -> days.add(Integer.toString(i)));
        Select<String> secondSelect = new Select<>();
        secondSelect.setEnabled(true);
        secondSelect.setItems(days);


        List<String> dayStart = new ArrayList<>();
        dayStart.add("Monday");
        dayStart.add("Tuesday");
        dayStart.add("Wednesday");
        dayStart.add("Thursday");
        dayStart.add("Friday");
        dayStart.add("Saturday");
        dayStart.add("Sunday");
        Select<String> secondStartSelect = new Select<>();
        secondStartSelect.setEnabled(true);
        secondStartSelect.setItems(dayStart);

        Label label2 = new Label(" days(s) starting on ");

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, secondSelect, label2, secondStartSelect);

        TimeComponent timeComponent = new TimeComponent("Every nth day starting on", layout);

        return timeComponent;
    }

    private TimeComponent getEveryDayStartingOnCalendarDay() {
        Label label = new Label("Every ");
        List<String> days = new ArrayList<>();
        IntStream.range(1, 8).forEach(i -> days.add(Integer.toString(i)));
        Select<String> secondSelect = new Select<>();
        secondSelect.setEnabled(true);
        secondSelect.setItems(days);


        List<String> dayStart = List.of("1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th",
            "11th", "12th", "13th", "14th", "15th", "16th", "17th", "18th", "19th", "20th", "21st", "22nd", "23rd",
            "24th", "25th", "26th", "27th", "28th", "29th", "30th", "31st");
        Select<String> secondStartSelect = new Select<>();
        secondStartSelect.setEnabled(true);
        secondStartSelect.setItems(dayStart);

        Label label2 = new Label(" days(s) starting on the ");

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, secondSelect, label2, secondStartSelect);

        TimeComponent timeComponent = new TimeComponent("Every nth day starting on the", layout);

        return timeComponent;
    }

    private TimeComponent getSpecificDayOfWeek() {

        List<Component> dayStart = new ArrayList<>();
        dayStart.add(new Checkbox("Monday"));
        dayStart.add(new Checkbox("Tuesday"));
        dayStart.add(new Checkbox("Wednesday"));
        dayStart.add(new Checkbox("Thursday"));
        dayStart.add(new Checkbox("Friday"));
        dayStart.add(new Checkbox("Saturday"));
        dayStart.add(new Checkbox("Sunday"));

        GridLayout layout = new GridLayout(7, 1);
        dayStart.forEach(item -> layout.addComponent(item));
        layout.setSizeFull();

        TimeComponent timeComponent = new TimeComponent("Specific day of week (multiple selectable)", layout);

        return timeComponent;
    }

    private TimeComponent getSpecificDay() {

        List<Component> day = new ArrayList<>();
        IntStream.range(1, 32).forEach(i -> day.add(new Checkbox(Integer.toString(i))));

        GridLayout layout = new GridLayout(12, 3);
        day.forEach(item -> layout.addComponent(item));
        layout.setSizeFull();

        TimeComponent timeComponent = new TimeComponent("Specific day of month (multiple selectable)", layout);

        return timeComponent;
    }

    private TimeComponent getLastDayOfMonth() {
        TimeComponent timeComponent = new TimeComponent("Last day of month", new Div());

        return timeComponent;
    }

    private TimeComponent getLastWeekOfMonth() {
        TimeComponent timeComponent = new TimeComponent("Last week of month", new Div());

        return timeComponent;
    }

    private TimeComponent getLastDaySelectOfMonth() {

        List<String> dayStart = new ArrayList<>();
        dayStart.add("Monday");
        dayStart.add("Tuesday");
        dayStart.add("Wednesday");
        dayStart.add("Thursday");
        dayStart.add("Friday");
        dayStart.add("Saturday");
        dayStart.add("Sunday");
        Select<String> dayStartSelect = new Select<>();
        dayStartSelect.setEnabled(true);
        dayStartSelect.setItems(dayStart);

        Label start = new Label("On the last ");
        Label end = new Label(" of the month");

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(start, dayStartSelect, end);

        TimeComponent timeComponent = new TimeComponent("Specific last day of month", layout);

        return timeComponent;
    }

    private class TimeComponent {
        private String name;
        private Component component;

        public TimeComponent(String name, Component component) {
            this.name = name;
            this.component = component;
        }

        public String getName() {
            return name;
        }

        public Component getComponent() {
            return component;
        }
    }
}
