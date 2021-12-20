package org.ikasan.dashboard.ui.scheduler.component;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.vaadin.componentfactory.gridlayout.GridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
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
import org.apache.commons.lang.StringUtils;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;

import java.util.*;
import java.util.stream.IntStream;

import static com.cronutils.model.CronType.QUARTZ;

// todo translation for entire class...
public class CronBuilderDialog extends AbstractCloseableResizableDialog {

    private Tab secondsTab;
    private Tab minutesTab;
    private Tab hoursTab;
    private Tab daysTab;
    private Tabs tabs;

    private TextField cronTextField;
    private TextField naturalLanguageLabel;
    private String secondPart = "*";
    private String minutePart = "*";
    private String hourPart = "*";
    private String dayOfMonthPart = "?";
    private String monthPart = "*";
    private String dayOfWeekPart = "*";
    private String yearPart = "*";

    private String cronExpression;
    private boolean isSaveClose = false;

    public CronBuilderDialog() {
        super.showResize(false);
        // todo translation
        super.title.setText("Cron Builder");

        naturalLanguageLabel = new TextField(getTranslation("text-field.description", UI.getCurrent().getLocale()));

        this.cronTextField = new TextField(getTranslation("text-field.cronExpression", UI.getCurrent().getLocale()));
        this.cronTextField.setValue(this.getCronExpression());
        this.cronTextField.setEnabled(false);

        // todo translation
        this.secondsTab = new Tab(getTranslation("tab.seconds", UI.getCurrent().getLocale()));
        this.secondsTab.setId("secondsTab");
        this.minutesTab = new Tab(getTranslation("tab.minutes", UI.getCurrent().getLocale()));
        this.minutesTab.setId("minutesTab");
        this.hoursTab = new Tab(getTranslation("tab.hours", UI.getCurrent().getLocale()));
        this.hoursTab.setId("hoursTab");
        this.daysTab = new Tab(getTranslation("tab.days", UI.getCurrent().getLocale()));
        this.daysTab.setId("daysTab");
        this.tabs = new Tabs(this.secondsTab, this.minutesTab, this.hoursTab, this.daysTab);
    }

    /**
     * Initialise the builder with a pre-existing cron expression or a null or empty
     * expression which defaults to * * * ? * * *
     *
     * @param cronExpression
     */
    public void init(String cronExpression) {
        if(cronExpression == null || cronExpression.isEmpty()) {
            this.cronExpression = "* * * ? * * *";
        }
        else {
            this.cronExpression = cronExpression;
        }

        this.cronTextField.setValue(this.cronExpression);
        this.initialiseParts();

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

        Button okButton = new Button(getTranslation("button.ok", UI.getCurrent().getLocale()));
        okButton.addClickListener(event -> {
            this.isSaveClose = true;
            this.close();
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(event -> this.close());

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.add(okButton, cancelButton);

        VerticalLayout buttonLayout = new VerticalLayout();
        buttonLayout.setWidth("100%");
        buttonLayout.add(buttons);
        buttonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttons);

        super.content.add(this.cronTextField, this.naturalLanguageLabel, tabs, secondsLayout, minutesLayout, hoursLayout, daysLayout, buttonLayout);

        this.setWidth("1000px");
        this.setHeight("550px");
    }

    public boolean isSaveClose() {
        return isSaveClose;
    }

    /**
     * Create the seconds layout with radio button choices.
     *
     * @return
     */
    private Component getSecondsLayout() {
        RadioButtonGroup<TimeComponent> radioGroup = new RadioButtonGroup<>();

        TimeComponent everySecond = this.getEverySecond();
        TimeComponent everySecondStartingAt = this.getEverySecondStartingAt();
        TimeComponent specificSecond = this.getSpecificSecond();
        TimeComponent everySecondBetween = getEverySecondBetween();
        radioGroup.setItems(List.of(everySecond
            , everySecondStartingAt
            , specificSecond
            , everySecondBetween));

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(radioGroup);

        if(this.secondPart.equals("*")) {
            radioGroup.setValue(everySecond);
            layout.add(radioGroup, everySecond.getComponent());
        }
        else if(this.secondPart.contains("/")) {
            radioGroup.setValue(everySecondStartingAt);
            layout.add(radioGroup, everySecondStartingAt.getComponent());
        }
        else if(this.secondPart.contains(",") || StringUtils.isNumeric(this.secondPart)) {
            radioGroup.setValue(specificSecond);
            layout.add(radioGroup, specificSecond.getComponent());
        }
        else {
            radioGroup.setValue(everySecondBetween);
            layout.add(radioGroup, everySecondBetween.getComponent());
        }

        radioGroup.setRenderer(new ComponentRenderer<>(timeComponent -> new Span(timeComponent.getName())));

        radioGroup.addValueChangeListener(event -> {
            this.secondPart = event.getValue().getValue();
            this.cronTextField.setValue(this.getCronExpression());
            layout.removeAll();
            layout.add(radioGroup, event.getValue().getComponent());
        });

        return layout;
    }

    /**
     * Get the Every Second TimeComponent
     *
     * @return
     */
    private TimeComponent getEverySecond() {
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-second"
            , UI.getCurrent().getLocale()), new Div());
        timeComponent.setValue("*");

        return timeComponent;
    }

    /**
     * Get the TimeComponent that represents seconds interval with a start offset.
     *
     * @return
     */
    private TimeComponent getEverySecondStartingAt() {
        Label label = new Label(getTranslation("time-component.every"
            , UI.getCurrent().getLocale()));
        List<String> seconds = new ArrayList<>();
        IntStream.range(1, 60).forEach(i -> seconds.add(Integer.toString(i)));
        Select<String> secondSelect = new Select<>();
        secondSelect.setEnabled(true);
        secondSelect.setItems(seconds);

        List<String> secondsStart = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> secondsStart.add(Integer.toString(i)));
        Select<String> secondStartSelect = new Select<>();
        secondStartSelect.setEnabled(true);
        secondStartSelect.setItems(secondsStart);

        HorizontalLayout layout = new HorizontalLayout();
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-n-seconds-starting-at"
            , UI.getCurrent().getLocale()), layout);
        timeComponent.setValue("0/1");

        if(this.secondPart.contains("/")) {
            StringTokenizer st = new StringTokenizer(this.secondPart, "/");
            secondStartSelect.setValue(st.nextToken());
            secondSelect.setValue(st.nextToken());
            timeComponent.setValue(this.secondPart);
        }

        secondSelect.addValueChangeListener(event -> {
            this.secondPart = secondStartSelect.getValue() + "/" + event.getValue();
            timeComponent.setValue(this.secondPart);
            this.cronTextField.setValue(this.getCronExpression());
        });

        secondStartSelect.addValueChangeListener(event -> {
            this.secondPart = event.getValue() + "/" + secondSelect.getValue();
            timeComponent.setValue(this.secondPart);
            this.cronTextField.setValue(this.getCronExpression());
        });

        Label label2 = new Label(getTranslation("time-component.seconds-starting-at"
            , UI.getCurrent().getLocale()));

        layout.add(label, secondSelect, label2, secondStartSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        return timeComponent;
    }

    /**
     * Get the TimeComponent that represents the seconds component to execute between
     * an interval.
     *
     * @return
     */
    private TimeComponent getEverySecondBetween() {
        Label label = new Label(getTranslation("time-component.every-second-between-second"
            , UI.getCurrent().getLocale()));
        List<String> secondsStart = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> secondsStart.add(Integer.toString(i)));
        Select<String> secondsStartSelect = new Select<>();
        secondsStartSelect.setEnabled(true);
        secondsStartSelect.setItems(secondsStart);


        List<String> secondsEnd = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> secondsEnd.add(Integer.toString(i)));
        Select<String> secondsEndSelect = new Select<>();
        secondsEndSelect.setEnabled(true);
        secondsEndSelect.setItems(secondsStart);

        HorizontalLayout layout = new HorizontalLayout();

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-second-between"
            , UI.getCurrent().getLocale()), layout);

        if(this.secondPart.contains("-")) {
            StringTokenizer st = new StringTokenizer(this.secondPart, "-");
            secondsStartSelect.setValue(st.nextToken());
            secondsEndSelect.setValue(st.nextToken());
            timeComponent.setValue(this.secondPart);
        }
        else {
            timeComponent.setValue("0-0");
        }

        secondsStartSelect.addValueChangeListener(event -> {
            this.secondPart = event.getValue() + "-" + secondsEndSelect.getValue();
            timeComponent.setValue(this.secondPart);
            this.cronTextField.setValue(this.getCronExpression());
        });

        secondsEndSelect.addValueChangeListener(event -> {
            this.secondPart = secondsStartSelect.getValue() + "-" + event.getValue();
            timeComponent.setValue(this.secondPart);
            this.cronTextField.setValue(this.getCronExpression());
        });

        Label label2 = new Label(getTranslation("time-component.and-second"
            , UI.getCurrent().getLocale()));

        layout.add(label, secondsStartSelect, label2, secondsEndSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        return timeComponent;
    }

    /**
     * Get the TimeComponent for every specific second that the schedule must fire.
     *
     * @return
     */
    private TimeComponent getSpecificSecond() {
        List<Checkbox> secondsStart = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> secondsStart.add(new Checkbox(Integer.toString(i))));

        GridLayout layout = new GridLayout(12, 5);
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.specific-second"
            , UI.getCurrent().getLocale()), layout);
        if(this.secondPart.contains(",") || StringUtils.isNumeric(this.secondPart)) {
            timeComponent.setValue(this.secondPart);
        }
        else {
            timeComponent.setValue("0");
        }

        List<String> selectedSeconds = Arrays.asList(this.secondPart.split("\\s*,\\s*"));

        secondsStart.forEach(item -> {
            if(selectedSeconds.contains(item.getLabel())) {
                item.setValue(true);
            }
            item.addValueChangeListener(event -> {
                StringBuffer value = new StringBuffer();
                secondsStart.forEach(checkbox -> {
                    if(checkbox.getValue()) {
                        value.append(checkbox.getLabel()).append(",");
                    }
                });
                this.secondPart = value.substring(0, value.length()-1);
                timeComponent.setValue(this.secondPart);
                this.cronTextField.setValue(this.getCronExpression());
            });
            layout.addComponent(item);
        });
        layout.setSizeFull();

        return timeComponent;
    }

    /**
     * Helper to build the minutes layout.
     *
     * @return
     */
    private Component getMinutesLayout() {
        RadioButtonGroup<TimeComponent> radioGroup = new RadioButtonGroup<>();

        TimeComponent everyMinute = this.getEveryMinute();
        TimeComponent everyMinuteStartingAt = this.getEveryMinuteStartingAt();
        TimeComponent specificMinute = this.getSpecificMinute();
        TimeComponent everyMinuteBetween = this.getEveryMinuteBetween();

        radioGroup.setItems(List.of(everyMinute
            , everyMinuteStartingAt
            , specificMinute
            , everyMinuteBetween));
        radioGroup.setValue(everyMinute);
        radioGroup.setRenderer(new ComponentRenderer<>(timeComponent -> new Span(timeComponent.getName())));

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(radioGroup);

        if(this.minutePart.equals("*")) {
            radioGroup.setValue(everyMinute);
            layout.add(radioGroup, everyMinute.getComponent());
        }
        else if(this.minutePart.contains("/")) {
            radioGroup.setValue(everyMinuteStartingAt);
            layout.add(radioGroup, everyMinuteStartingAt.getComponent());
        }
        else if(this.minutePart.contains(",") || StringUtils.isNumeric(this.minutePart)) {
            radioGroup.setValue(specificMinute);
            layout.add(radioGroup, specificMinute.getComponent());
        }
        else {
            radioGroup.setValue(everyMinuteBetween);
            layout.add(radioGroup, everyMinuteBetween.getComponent());
        }

        radioGroup.addValueChangeListener(event -> {
            this.minutePart = event.getValue().getValue();
            this.cronTextField.setValue(this.getCronExpression());
            layout.removeAll();
            layout.add(radioGroup, event.getValue().getComponent());
        });

        return layout;
    }

    /**
     * Time component to represent a schedule that fires every minute.
     *
     * @return
     */
    private TimeComponent getEveryMinute() {
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-minute"
            , UI.getCurrent().getLocale()), new Div());
        timeComponent.setValue("*");

        return timeComponent;
    }

    /**
     * The TimeComponent to represent which minutes to fire with start offset.
     *
     * @return
     */
    private TimeComponent getEveryMinuteStartingAt() {
        Label label = new Label(getTranslation("time-component.every"
            , UI.getCurrent().getLocale()));
        List<String> minutes = new ArrayList<>();
        IntStream.range(1, 60).forEach(i -> minutes.add(Integer.toString(i)));
        Select<String> minuteSelect = new Select<>();
        minuteSelect.setEnabled(true);
        minuteSelect.setItems(minutes);

        List<String> minutesStart = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> minutesStart.add(Integer.toString(i)));
        Select<String> minuteStartSelect = new Select<>();
        minuteStartSelect.setEnabled(true);
        minuteStartSelect.setItems(minutesStart);

        Label label2 = new Label(getTranslation("time-component.minutes-starting-at"
            , UI.getCurrent().getLocale()));

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, minuteSelect, label2, minuteStartSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-n-minutes-starting-at"
            , UI.getCurrent().getLocale()), layout);
        timeComponent.setValue("0/1");

        if(this.minutePart.contains("/")) {
            StringTokenizer st = new StringTokenizer(this.minutePart, "/");
            minuteStartSelect.setValue(st.nextToken());
            minuteSelect.setValue(st.nextToken());
            timeComponent.setValue(this.minutePart);
        }

        minuteSelect.addValueChangeListener(event -> {
            this.minutePart = minuteStartSelect.getValue() + "/" + event.getValue();
            timeComponent.setValue(this.minutePart);
            this.cronTextField.setValue(this.getCronExpression());
        });

        minuteStartSelect.addValueChangeListener(event -> {
            this.minutePart = event.getValue() + "/" + minuteSelect.getValue();
            timeComponent.setValue(this.minutePart);
            this.cronTextField.setValue(this.getCronExpression());
        });

        return timeComponent;
    }

    /**
     * The TimeComponent to define the interval in which the schedule will fire on each minute.
     *
     * @return
     */
    private TimeComponent getEveryMinuteBetween() {
        Label label = new Label(getTranslation("time-component.every-minute-between-minute"
            , UI.getCurrent().getLocale()));
        List<String> minutesStart = new ArrayList<>();

        Label label2 = new Label(" and minute ");

        HorizontalLayout layout = new HorizontalLayout();

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-minute-between"
            , UI.getCurrent().getLocale()), layout);

        IntStream.range(0, 60).forEach(i -> minutesStart.add(Integer.toString(i)));
        Select<String> minutesStartSelect = new Select<>();
        minutesStartSelect.setEnabled(true);
        minutesStartSelect.setItems(minutesStart);
        minutesStartSelect.setValue("0");


        List<String> minutesEnd = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> minutesEnd.add(Integer.toString(i)));
        Select<String> minutesEndSelect = new Select<>();
        minutesEndSelect.setEnabled(true);
        minutesEndSelect.setItems(minutesStart);
        minutesEndSelect.setValue("0");

        if(this.minutePart.contains("-")) {
            StringTokenizer st = new StringTokenizer(this.minutePart, "-");
            minutesStartSelect.setValue(st.nextToken());
            minutesEndSelect.setValue(st.nextToken());
            timeComponent.setValue(this.minutePart);
        }
        else {
            timeComponent.setValue("0-0");
        }

        minutesStartSelect.addValueChangeListener(event -> {
            this.minutePart = event.getValue() + "-" + minutesEndSelect.getValue();
            this.cronTextField.setValue(this.getCronExpression());
        });

        minutesEndSelect.addValueChangeListener(event -> {
            this.minutePart = minutesStartSelect.getValue() + "-" + event.getValue();
            this.cronTextField.setValue(this.getCronExpression());
        });

        layout.add(label, minutesStartSelect, label2, minutesEndSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        return timeComponent;
    }

    /**
     * The TimeComponent which defines the specific minutes that the schedule will fire.
     *
     * @return
     */
    private TimeComponent getSpecificMinute() {

        List<Checkbox> minutes = new ArrayList<>();
        IntStream.range(0, 60).forEach(i -> minutes.add(new Checkbox(Integer.toString(i))));

        GridLayout layout = new GridLayout(12, 5);
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.specific-minute"
            , UI.getCurrent().getLocale()), layout);
        if(this.minutePart.contains(",") || StringUtils.isNumeric(this.minutePart)) {
            timeComponent.setValue(this.minutePart);
        }
        else {
            timeComponent.setValue("0");
        }

        List<String> selectedMinutes = Arrays.asList(this.minutePart.split("\\s*,\\s*"));

        minutes.forEach(item -> {
            layout.addComponent(item);
            if(selectedMinutes.contains(item.getLabel())) {
                item.setValue(true);
            }
            item.addValueChangeListener(event -> {
                StringBuffer value = new StringBuffer();
                minutes.forEach(checkbox -> {
                    if(checkbox.getValue()) {
                        value.append(checkbox.getLabel()).append(",");
                    }
                });
                this.minutePart = value.substring(0, value.length()-1);
                timeComponent.setValue(this.minutePart);
                this.cronTextField.setValue(this.getCronExpression());
            });
        });
        layout.setSizeFull();


        return timeComponent;
    }

    /**
     * Helper to build the hours layout.
     *
     * @return
     */
    private Component getHoursLayout() {
        RadioButtonGroup<TimeComponent> radioGroup = new RadioButtonGroup<>();

        TimeComponent everyHour = this.getEveryHour();
        TimeComponent everyHourStartingAt = this.getEveryHourStartingAt();
        TimeComponent specificHour = this.getSpecificHour();
        TimeComponent everyHourBetween = this.getEveryHourBetween();

        radioGroup.setItems(List.of(everyHour
            , everyHourStartingAt
            , specificHour
            , everyHourBetween));
        radioGroup.setValue(everyHour);

        radioGroup.setRenderer(new ComponentRenderer<>(timeComponent -> new Span(timeComponent.getName())));

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(radioGroup);

        if(this.hourPart.equals("*")) {
            radioGroup.setValue(everyHour);
            layout.add(radioGroup, everyHour.getComponent());
        }
        else if(this.hourPart.contains("/")) {
            radioGroup.setValue(everyHourStartingAt);
            layout.add(radioGroup, everyHourStartingAt.getComponent());
        }
        else if(this.hourPart.contains(",") || StringUtils.isNumeric(this.hourPart)) {
            radioGroup.setValue(specificHour);
            layout.add(radioGroup, specificHour.getComponent());
        }
        else {
            radioGroup.setValue(everyHourBetween);
            layout.add(radioGroup, everyHourBetween.getComponent());
        }

        radioGroup.addValueChangeListener(event -> {
            this.hourPart = event.getValue().getValue();
            this.cronTextField.setValue(this.getCronExpression());
            layout.removeAll();
            layout.add(radioGroup, event.getValue().getComponent());
        });

        return layout;
    }

    /**
     * Get the TimeComponent that defines that the schedule will fire every hour.
     *
     * @return
     */
    private TimeComponent getEveryHour() {
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-hour"
            , UI.getCurrent().getLocale()), new Div());
        timeComponent.setValue("*");

        return timeComponent;
    }

    /**
     * The TimeComponent that represents the hourly interval to fire with an offset.
     *
     * @return
     */
    private TimeComponent getEveryHourStartingAt() {
        Label label = new Label(getTranslation("time-component.every"
            , UI.getCurrent().getLocale()));
        List<String> hours = new ArrayList<>();
        IntStream.range(1, 25).forEach(i -> hours.add(Integer.toString(i)));
        Select<String> hoursSelect = new Select<>();
        hoursSelect.setEnabled(true);
        hoursSelect.setItems(hours);
        hoursSelect.setValue("1");


        List<String> hoursStart = new ArrayList<>();
        IntStream.range(0, 24).forEach(i -> hoursStart.add(Integer.toString(i)));
        Select<String> hoursStartSelect = new Select<>();
        hoursStartSelect.setEnabled(true);
        hoursStartSelect.setItems(hoursStart);
        hoursStartSelect.setValue("0");

        Label label2 = new Label(getTranslation("time-component.hours-starting-at"
            , UI.getCurrent().getLocale()));

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, hoursSelect, label2, hoursStartSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.eveny-n-hours-starting-at"
            , UI.getCurrent().getLocale()), layout);
        timeComponent.setValue("0/1");

        if(this.hourPart.contains("/")) {
            StringTokenizer st = new StringTokenizer(this.hourPart, "/");
            hoursStartSelect.setValue(st.nextToken());
            hoursSelect.setValue(st.nextToken());
            timeComponent.setValue(this.hourPart);
        }

        hoursSelect.addValueChangeListener(event -> {
            this.hourPart = hoursStartSelect.getValue() + "/" + event.getValue();
            this.cronTextField.setValue(this.getCronExpression());
        });

        hoursStartSelect.addValueChangeListener(event -> {
            this.hourPart = event.getValue() + "/" + hoursSelect.getValue();
            this.cronTextField.setValue(this.getCronExpression());
        });


        return timeComponent;
    }

    /**
     * Get the TimeComponent that represents the interval within which the schedule
     * will run every hour.
     *
     * @return
     */
    private TimeComponent getEveryHourBetween() {
        Label label = new Label(getTranslation("time-component.every-hour-between-hour"
            , UI.getCurrent().getLocale()));
        List<String> hours = new ArrayList<>();
        IntStream.range(0, 24).forEach(i -> hours.add(Integer.toString(i)));
        Select<String> hoursStartSelect = new Select<>();
        hoursStartSelect.setEnabled(true);
        hoursStartSelect.setItems(hours);
        hoursStartSelect.setValue("0");


        Select<String> hoursEndSelect = new Select<>();
        hoursEndSelect.setEnabled(true);
        hoursEndSelect.setItems(hours);
        hoursEndSelect.setValue("0");

        Label label2 = new Label(getTranslation("time-component.and-hour"
            , UI.getCurrent().getLocale()));

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, hoursStartSelect, label2, hoursEndSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-hour-between"
            , UI.getCurrent().getLocale()), layout);
        timeComponent.setValue("0-0");

        if(this.hourPart.contains("-")) {
            StringTokenizer st = new StringTokenizer(this.hourPart, "-");
            hoursStartSelect.setValue(st.nextToken());
            hoursEndSelect.setValue(st.nextToken());
            timeComponent.setValue(this.minutePart);
        }

        hoursStartSelect.addValueChangeListener(event -> {
            this.hourPart = event.getValue() + "-" + hoursEndSelect.getValue();
            this.cronTextField.setValue(this.getCronExpression());
        });

        hoursEndSelect.addValueChangeListener(event -> {
            this.hourPart = hoursStartSelect.getValue() + "-" + event.getValue();
            this.cronTextField.setValue(this.getCronExpression());
        });

        return timeComponent;
    }

    private TimeComponent getSpecificHour() {

        List<Checkbox> hours = new ArrayList<>();
        IntStream.range(0, 24).forEach(i -> hours.add(new Checkbox(Integer.toString(i))));

        GridLayout layout = new GridLayout(12, 5);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.specific-hour"
            , UI.getCurrent().getLocale()), layout);
        if(this.hourPart.contains(",") || StringUtils.isNumeric(this.hourPart)) {
            timeComponent.setValue(this.hourPart);
        }
        else {
            timeComponent.setValue("0");
        }

        List<String> selectedHours = Arrays.asList(this.hourPart.split("\\s*,\\s*"));

        hours.forEach(item -> {
            layout.addComponent(item);
            if(selectedHours.contains(item.getLabel())) {
                item.setValue(true);
            }

            item.addValueChangeListener(event -> {
                StringBuffer value = new StringBuffer();
                hours.forEach(checkbox -> {
                    if(checkbox.getValue()) {
                        value.append(checkbox.getLabel()).append(",");
                    }
                });
                this.hourPart = value.substring(0, value.length()-1);
                timeComponent.setValue(this.hourPart);
                this.cronTextField.setValue(this.getCronExpression());
            });
        });
        layout.setSizeFull();

        return timeComponent;
    }

    private Component getDaysLayout() {
        RadioButtonGroup<TimeComponent> radioGroup = new RadioButtonGroup<>();

        TimeComponent everyDay = this.getEveryDay();
        TimeComponent everyDayStartingOnDay = this.getEveryDayStartingOnDay();
        TimeComponent everyDayStartingOnCalendarDay = this.getEveryDayStartingOnCalendarDay();
        TimeComponent specificDay = this.getSpecificDay();
        TimeComponent specificDayOfWeek = this.getSpecificDayOfWeek();
        TimeComponent lastDayOfMonth = this.getLastDayOfMonth();
        TimeComponent lastWeekDayOfMonth = this.getLastWeekOfMonth();
        TimeComponent lastDaySelectOfMonth = this.getLastDaySelectOfMonth();

        radioGroup.setItems(List.of(everyDay
            , everyDayStartingOnDay
            , everyDayStartingOnCalendarDay
            , specificDay
            , specificDayOfWeek
            , lastDayOfMonth
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
        else if(this.dayOfMonthPart.equals("?") && this.dayOfWeekPart.contains("L")) {
            radioGroup.setValue(lastDaySelectOfMonth);
            layout.add(radioGroup, lastDaySelectOfMonth.getComponent());
        }

        radioGroup.addValueChangeListener(event -> {
            if(event.getValue().equals(everyDayStartingOnDay) || event.getValue().equals(specificDayOfWeek) || event.getValue().equals(lastDaySelectOfMonth)) {
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
            this.cronTextField.setValue(this.getCronExpression());

            layout.removeAll();
            layout.add(radioGroup, event.getValue().getComponent());
        });

        return layout;
    }

    private TimeComponent getEveryDay() {
        TimeComponent timeComponent = new TimeComponent("Every day", new Div());
        timeComponent.setValue("*");

        return timeComponent;
    }

    private TimeComponent getEveryDayStartingOnDay() {
        Label label = new Label("Every ");
        List<String> days = new ArrayList<>();
        IntStream.range(1, 8).forEach(i -> days.add(Integer.toString(i)));
        Select<String> daySelect = new Select<>();
        daySelect.setEnabled(true);
        daySelect.setItems(days);
        daySelect.setValue("1");


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
        dayStartSelect.setValue("Sunday");

        Label label2 = new Label(" days(s) starting on ");

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, daySelect, label2, dayStartSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        TimeComponent timeComponent = new TimeComponent("Every nth day starting on", layout);
        timeComponent.setValue("1/1");

        if(this.dayOfWeekPart.contains("/")) {
            StringTokenizer st = new StringTokenizer(this.dayOfWeekPart, "/");
            dayStartSelect.setValue(this.numToDayOfWeek(st.nextToken()));
            daySelect.setValue(st.nextToken());
            timeComponent.setValue(this.dayOfWeekPart);
        }

        dayStartSelect.addValueChangeListener(event -> {
            this.dayOfWeekPart = this.dayOfWeek(event.getValue()) + "/" + daySelect.getValue();
            this.dayOfMonthPart = "?";
            this.cronTextField.setValue(this.getCronExpression());
        });

        daySelect.addValueChangeListener(event -> {
            this.dayOfWeekPart = this.dayOfWeek(dayStartSelect.getValue()) + "/" + event.getValue();
            this.dayOfMonthPart = "?";
            this.cronTextField.setValue(this.getCronExpression());
        });

        return timeComponent;
    }

    private TimeComponent getEveryDayStartingOnCalendarDay() {
        Label label = new Label("Every ");
        List<String> days = new ArrayList<>();
        IntStream.range(1, 8).forEach(i -> days.add(Integer.toString(i)));
        Select<String> daySelect = new Select<>();
        daySelect.setEnabled(true);
        daySelect.setItems(days);
        daySelect.setValue("1");


        List<String> dayStart = List.of("1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th",
            "11th", "12th", "13th", "14th", "15th", "16th", "17th", "18th", "19th", "20th", "21st", "22nd", "23rd",
            "24th", "25th", "26th", "27th", "28th", "29th", "30th", "31st");
        Select<String> dayStartSelect = new Select<>();
        dayStartSelect.setEnabled(true);
        dayStartSelect.setItems(dayStart);
        dayStartSelect.setValue("1st");

        Label label2 = new Label(" days(s) starting on the ");

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, daySelect, label2, dayStartSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        TimeComponent timeComponent = new TimeComponent("Every nth day starting on the", layout);
        timeComponent.setValue("1/1");

        if(this.dayOfMonthPart.contains("/")) {
            StringTokenizer st = new StringTokenizer(this.dayOfMonthPart, "/");
            String numValue = st.nextToken();
            daySelect.setValue(st.nextToken());
            if(numValue.equals("1")) {
                dayStartSelect.setValue(numValue+"st");
            }
            else if(numValue.equals("11") || numValue.equals("12") || numValue.equals("13")) {
                dayStartSelect.setValue(numValue+"th");
            }
            else if(numValue.endsWith("2")) {
                dayStartSelect.setValue(numValue+"nd");
            }
            else if(numValue.endsWith("3")) {
                dayStartSelect.setValue(numValue+"rd");
            }
            else {
                dayStartSelect.setValue(numValue+"th");
            }

            timeComponent.setValue(this.dayOfMonthPart);
        }

        dayStartSelect.addValueChangeListener(event -> {
            this.dayOfMonthPart = event.getValue().substring(0, event.getValue().length()-2) + "/" + daySelect.getValue();
            this.dayOfWeekPart = "?";
            this.cronTextField.setValue(this.getCronExpression());
            timeComponent.setValue(this.dayOfMonthPart);
        });

        daySelect.addValueChangeListener(event -> {
            this.dayOfMonthPart = dayStartSelect.getValue().substring(0, dayStartSelect.getValue().length()-2) + "/" + event.getValue();
            this.dayOfWeekPart = "?";
            this.cronTextField.setValue(this.getCronExpression());
            timeComponent.setValue(this.dayOfMonthPart);
        });

        return timeComponent;
    }

    private TimeComponent getSpecificDayOfWeek() {

        List<Checkbox> dayStart = new ArrayList<>();
        dayStart.add(new Checkbox("Monday"));
        dayStart.add(new Checkbox("Tuesday"));
        dayStart.add(new Checkbox("Wednesday"));
        dayStart.add(new Checkbox("Thursday"));
        dayStart.add(new Checkbox("Friday"));
        dayStart.add(new Checkbox("Saturday"));
        dayStart.add(new Checkbox("Sunday"));

        GridLayout layout = new GridLayout(7, 1);
        TimeComponent timeComponent = new TimeComponent("Specific day of week", layout);
        timeComponent.setValue("SUN");

        dayStart.forEach(item -> {
            layout.addComponent(item);

            item.addValueChangeListener(event -> {
                StringBuffer value = new StringBuffer();
                dayStart.forEach(checkbox -> {
                    if(checkbox.getValue()) {
                        value.append(this.dayOfWeekLong(checkbox.getLabel())).append(",");
                    }
                });
                this.dayOfWeekPart = value.substring(0, value.length()-1);
                timeComponent.setValue(this.dayOfWeekPart);
                dayOfMonthPart = "?";
                this.cronTextField.setValue(this.getCronExpression());
            });
        });
        layout.setSizeFull();

        return timeComponent;
    }

    private TimeComponent getSpecificDay() {

        List<Checkbox> day = new ArrayList<>();
        IntStream.range(1, 32).forEach(i -> day.add(new Checkbox(Integer.toString(i))));

        GridLayout layout = new GridLayout(12, 3);
        layout.setSizeFull();

        TimeComponent timeComponent = new TimeComponent("Specific day of month", layout);

        if(this.dayOfMonthPart.contains(",") || StringUtils.isNumeric(this.dayOfMonthPart)) {
            timeComponent.setValue(this.dayOfMonthPart);
        }
        else {
            timeComponent.setValue("1");
        }

        List<String> selectedDays = Arrays.asList(this.dayOfMonthPart.split("\\s*,\\s*"));

        day.forEach(item -> {
            layout.addComponent(item);
            if(selectedDays.contains(item.getLabel())) {
                item.setValue(true);
            }

            item.addValueChangeListener(event -> {
                StringBuffer value = new StringBuffer();
                day.forEach(checkbox -> {
                    if(checkbox.getValue()) {
                        value.append(checkbox.getLabel()).append(",");
                    }
                });
                this.dayOfMonthPart = value.substring(0, value.length()-1);
                timeComponent.setValue(this.dayOfMonthPart);
                dayOfWeekPart = "?";
                this.cronTextField.setValue(this.getCronExpression());
            });
        });

        return timeComponent;
    }

    private TimeComponent getLastDayOfMonth() {
        TimeComponent timeComponent = new TimeComponent("Last day of month", new Div());
        timeComponent.setValue("L");

        return timeComponent;
    }

    private TimeComponent getLastWeekOfMonth() {
        TimeComponent timeComponent = new TimeComponent("Last week day of month", new Div());
        timeComponent.setValue("LW");

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
        Select<String> daySelect = new Select<>();
        daySelect.setEnabled(true);
        daySelect.setItems(dayStart);

        Label start = new Label("On the last ");
        Label end = new Label(" of the month");

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(start, daySelect, end);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, start, end);

        TimeComponent timeComponent = new TimeComponent("Specific last day of month", layout);
        timeComponent.setValue("1L");

        if(this.dayOfWeekPart.contains("L")) {
            daySelect.setValue(this.numToDayOfWeek(this.dayOfWeekPart.substring(0,1)));
            timeComponent.setValue(this.dayOfWeekPart);
        }

        daySelect.addValueChangeListener(event -> {
            this.dayOfMonthPart = "?";
            this.dayOfWeekPart = this.dayOfWeek(event.getValue()) + "L";
            this.cronTextField.setValue(this.getCronExpression());
        });

        return timeComponent;
    }

    public String getCronExpression() {
        StringBuffer cronExpression = new StringBuffer();

        cronExpression.append(this.secondPart).append(" ");
        cronExpression.append(this.minutePart).append(" ");
        cronExpression.append(this.hourPart).append(" ");
        cronExpression.append(this.dayOfMonthPart).append(" ");
        cronExpression.append(this.monthPart).append(" ");
        cronExpression.append(this.dayOfWeekPart).append(" ");
        cronExpression.append(this.yearPart);

        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));
        Cron quartzCron = parser.parse(cronExpression.toString());
        CronDescriptor descriptor = CronDescriptor.instance(Locale.UK);
        this.naturalLanguageLabel.setValue(descriptor.describe(quartzCron));

        return cronExpression.toString();
    }

    private String dayOfWeek(String textDay) {
        switch (textDay){
            case "Sunday" : return "1";
            case "Monday" : return "2";
            case "Tuesday" : return "3";
            case "Wednesday" : return "4";
            case "Thursday" : return "5";
            case "Friday" : return "6";
            case "Saturday" : return "7";
            default: return "";
        }
    }

    private String numToDayOfWeek(String textDay) {
        switch (textDay){
            case "1" : return "Sunday";
            case "2" : return "Monday";
            case "3" : return "Tuesday";
            case "4" : return "Wednesday";
            case "5" : return "Thursday";
            case "6" : return "Friday";
            case "7" : return "Saturday";
            default: return "";
        }
    }

    private String dayOfWeekLong(String textDay) {
        switch (textDay){
            case "Sunday" : return "SUN";
            case "Monday" : return "MON";
            case "Tuesday" : return "TUE";
            case "Wednesday" : return "WED";
            case "Thursday" : return "THU";
            case "Friday" : return "FRI";
            case "Saturday" : return "SAT";
            default: return "";
        }
    }

    private void initialiseParts() {
        StringTokenizer stringTokenizer = new StringTokenizer(this.cronExpression, " ");
        if(stringTokenizer.countTokens() != 7) {
            throw new RuntimeException("cron expression does not contain 7 tokens");
        }
        else {
            this.secondPart = stringTokenizer.nextToken();
            this.minutePart = stringTokenizer.nextToken();
            this.hourPart = stringTokenizer.nextToken();
            this.dayOfMonthPart = stringTokenizer.nextToken();
            this.monthPart = stringTokenizer.nextToken();
            this.dayOfWeekPart = stringTokenizer.nextToken();
            this.yearPart = stringTokenizer.nextToken();
        }
    }

    /**
     * Inner class that represents a time component within a cron schedule.
     */
    private class TimeComponent {
        private String name;
        private Component component;
        private String value;

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

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
