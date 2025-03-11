package org.ikasan.dashboard.ui.scheduler.component;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.vaadin.componentfactory.gridlayout.GridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
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
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;

import static com.cronutils.model.CronType.QUARTZ;

public abstract class AbstractCronBuilderDialog extends AbstractCloseableResizableDialog {

    private Logger logger = LoggerFactory.getLogger(AbstractCronBuilderDialog.class);
    protected Tab secondsTab;
    protected Tab minutesTab;
    protected Tab hoursTab;
    protected Tab daysTab;
    protected Tab monthsTab;
    protected Tab yearsTab;
    protected Tabs tabs;

    protected TextField cronExpressionTf;
    protected TextField naturalLanguageTf;
    protected String secondPart = "0";
    protected String minutePart = "0";
    protected String hourPart = "0";
    protected String dayOfMonthPart = "?";
    protected String monthPart = "*";
    protected String dayOfWeekPart = "*";
    protected String yearPart = "*";

    protected String cronExpression;
    protected boolean isSaveClose = false;

    protected CronParser parser;
    protected CronDescriptor descriptor;

    /**
     * Constructor for AbstractCronBuilderDialog class.
     * Initializes the dialog with required components such as tabs, text fields, and title.
     */
    public AbstractCronBuilderDialog() {
        super.showResize(false);

        parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));
        descriptor = CronDescriptor.instance(Locale.UK);

        super.title.setText(getTranslation("header.cronBuilder", UI.getCurrent().getLocale()));

        naturalLanguageTf = new TextField(getTranslation("text-field.description", UI.getCurrent().getLocale()));

        this.cronExpressionTf = new TextField(getTranslation("text-field.cronExpression", UI.getCurrent().getLocale()));
        this.cronExpressionTf.setEnabled(false);

        this.secondsTab = new Tab(getTranslation("tab.seconds", UI.getCurrent().getLocale()));
        this.secondsTab.setId("secondsTab");
        this.minutesTab = new Tab(getTranslation("tab.minutes", UI.getCurrent().getLocale()));
        this.minutesTab.setId("minutesTab");
        this.hoursTab = new Tab(getTranslation("tab.hours", UI.getCurrent().getLocale()));
        this.hoursTab.setId("hoursTab");
        this.daysTab = new Tab(getTranslation("tab.days", UI.getCurrent().getLocale()));
        this.daysTab.setId("daysTab");
        this.monthsTab = new Tab(getTranslation("tab.months", UI.getCurrent().getLocale()));
        this.monthsTab.setId("monthsTab");
        this.yearsTab = new Tab(getTranslation("tab.years", UI.getCurrent().getLocale()));
        this.yearsTab.setId("yearsTab");
        this.tabs = new Tabs(this.secondsTab, this.minutesTab, this.hoursTab, this.daysTab, this.monthsTab, this.yearsTab);
    }

    /**
     * Initialise the builder with a pre-existing cron expression or a null or empty
     * expression which defaults to * * * ? * * *
     *
     * @param cronExpression
     */
    public void init(String cronExpression) {
        if(cronExpression == null || cronExpression.isEmpty()) {
            this.cronExpression = "0 0 0 ? * * *";
        }
        else {
            this.cronExpression = cronExpression;
        }

        this.initialiseParts();
        this.setNaturalLanguageDescription(this.cronExpression);
        this.cronExpressionTf.setValue(this.cronExpression);

        Component secondsLayout = this.getSecondsLayout();
        Component minutesLayout = this.getMinutesLayout();
        minutesLayout.setVisible(false);
        Component hoursLayout = this.getHoursLayout();
        hoursLayout.setVisible(false);
        Component daysLayout = getDaysLayout();
        daysLayout.setVisible(false);
        Component monthsLayout = this.getMonthLayout();
        monthsLayout.setVisible(false);
        Component yearsLayout = this.getYearLayout();
        yearsLayout.setVisible(false);

        Map<Tab, Component> tabsToPages = new HashMap<>();
        tabsToPages.put(this.secondsTab, secondsLayout);
        tabsToPages.put(this.minutesTab, minutesLayout);
        tabsToPages.put(this.hoursTab, hoursLayout);
        tabsToPages.put(this.daysTab, daysLayout);
        tabsToPages.put(this.monthsTab, monthsLayout);
        tabsToPages.put(this.yearsTab, yearsLayout);

        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
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

        super.content.add(this.cronExpressionTf, this.naturalLanguageTf, tabs, secondsLayout, minutesLayout, hoursLayout, daysLayout, monthsLayout, yearsLayout, buttonLayout);


        this.setWidth("1200px");
        this.setHeight("550px");
    }

    /**
     * Retrieves the component layout for the days tab in the cron builder dialog.
     *
     * @return the component representing the layout for the days tab
     */
    protected abstract Component getDaysLayout();

    /**
     * Retrieves the value of the isSaveClose boolean flag.
     *
     * @return true if the dialog should be saved and closed, otherwise false
     */
    public boolean isSaveClose() {
        return isSaveClose;
    }


    /**
     * Retrieves the component layout for the seconds tab in the cron builder dialog.
     * This method builds a layout that allows the user to select different options for the seconds part of the cron expression.
     *
     * @return the component representing the layout for the seconds tab
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
            this.cronExpressionTf.setValue(this.getCronExpression());
            layout.removeAll();
            layout.add(radioGroup, event.getValue().getComponent());
        });

        return layout;
    }


    /**
     * Retrieves the TimeComponent representing the selection of every second in a cron expression.
     *
     * @return the TimeComponent for every second selection
     */
    private TimeComponent getEverySecond() {
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-second"
            , UI.getCurrent().getLocale()), new Div());
        timeComponent.setValue("*");

        return timeComponent;
    }


    /**
     * Retrieves the TimeComponent representing the selection of every second starting at a given value in a cron expression.
     *
     * @return the TimeComponent for selecting every second starting at a specific value
     */
    private TimeComponent getEverySecondStartingAt() {
        NativeLabel label = new NativeLabel(getTranslation("time-component.every"
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
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        secondStartSelect.addValueChangeListener(event -> {
            this.secondPart = event.getValue() + "/" + secondSelect.getValue();
            timeComponent.setValue(this.secondPart);
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        NativeLabel label2 = new NativeLabel(getTranslation("time-component.seconds-starting-at"
            , UI.getCurrent().getLocale()));

        layout.add(label, secondSelect, label2, secondStartSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        return timeComponent;
    }


    /**
     * Retrieves the TimeComponent representing the selection of every second between two specified values in a cron expression.
     *
     * @return the TimeComponent for selecting every second between two specific values
     */
    private TimeComponent getEverySecondBetween() {
        NativeLabel label = new NativeLabel(getTranslation("time-component.every-second-between-second"
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
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        secondsEndSelect.addValueChangeListener(event -> {
            this.secondPart = secondsStartSelect.getValue() + "-" + event.getValue();
            timeComponent.setValue(this.secondPart);
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        NativeLabel label2 = new NativeLabel(getTranslation("time-component.and-second"
            , UI.getCurrent().getLocale()));

        layout.add(label, secondsStartSelect, label2, secondsEndSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        return timeComponent;
    }


    /**
     * Retrieves the TimeComponent representing the selection of a specific second in a cron expression.
     *
     * @return the TimeComponent for selecting a specific second
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
                this.cronExpressionTf.setValue(this.getCronExpression());
            });
            layout.addComponent(item);
        });
        layout.setSizeFull();

        return timeComponent;
    }

    /**
     * Retrieves the layout component for selecting minute options in a cron expression.
     *
     * @return The layout component containing minute selection options
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
            this.cronExpressionTf.setValue(this.getCronExpression());
            layout.removeAll();
            layout.add(radioGroup, event.getValue().getComponent());
        });

        return layout;
    }

    /**
     * Retrieves the TimeComponent representing the selection of every minute in a cron expression.
     *
     * @return the TimeComponent for every minute selection
     */
    private TimeComponent getEveryMinute() {
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-minute"
            , UI.getCurrent().getLocale()), new Div());
        timeComponent.setValue("*");

        return timeComponent;
    }

    /**
     * Retrieves the TimeComponent representing the selection of every minute starting at a specific value in a cron expression.
     * This method creates a TimeComponent allowing the user to choose every minute starting at a specific value.
     *
     * @return the TimeComponent for selecting every minute starting at a specific value
     */
    private TimeComponent getEveryMinuteStartingAt() {
        NativeLabel label = new NativeLabel(getTranslation("time-component.every"
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

        NativeLabel label2 = new NativeLabel(getTranslation("time-component.minutes-starting-at"
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
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        minuteStartSelect.addValueChangeListener(event -> {
            this.minutePart = event.getValue() + "/" + minuteSelect.getValue();
            timeComponent.setValue(this.minutePart);
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        return timeComponent;
    }

    /**
     * Retrieves the TimeComponent representing the selection of every minute between two specified values in a cron expression.
     *
     * @return the TimeComponent for selecting every minute between two specific values
     */
    private TimeComponent getEveryMinuteBetween() {
        NativeLabel label = new NativeLabel(getTranslation("time-component.every-minute-between-minute"
            , UI.getCurrent().getLocale()));
        List<String> minutesStart = new ArrayList<>();

        NativeLabel label2 = new NativeLabel(getTranslation("time-component.and-minute"
            , UI.getCurrent().getLocale()));

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
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        minutesEndSelect.addValueChangeListener(event -> {
            this.minutePart = minutesStartSelect.getValue() + "-" + event.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        layout.add(label, minutesStartSelect, label2, minutesEndSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        return timeComponent;
    }

    /**
     * Retrieves the TimeComponent representing the selection of a specific minute in a cron expression.
     *
     * @return the TimeComponent for selecting a specific minute
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
                this.cronExpressionTf.setValue(this.getCronExpression());
            });
        });
        layout.setSizeFull();


        return timeComponent;
    }

    /**
     * Retrieves the component layout for selecting different hour options in a cron expression.
     *
     * @return The Component layout representing the options for selecting hours in a cron expression
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
            this.cronExpressionTf.setValue(this.getCronExpression());
            layout.removeAll();
            layout.add(radioGroup, event.getValue().getComponent());
        });

        return layout;
    }

    /**
     * Retrieves the TimeComponent representing the selection of every hour in a cron expression.
     *
     * @return the TimeComponent representing every hour selection
     */
    private TimeComponent getEveryHour() {
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-hour"
            , UI.getCurrent().getLocale()), new Div());
        timeComponent.setValue("*");

        return timeComponent;
    }

    /**
     * Get the TimeComponent that represents selecting every hour starting at a specific value in a cron expression.
     *
     * @return the TimeComponent for selecting every hour starting at a specific value
     */
    private TimeComponent getEveryHourStartingAt() {
        NativeLabel label = new NativeLabel(getTranslation("time-component.every"
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

        NativeLabel label2 = new NativeLabel(getTranslation("time-component.hours-starting-at"
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
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        hoursStartSelect.addValueChangeListener(event -> {
            this.hourPart = event.getValue() + "/" + hoursSelect.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });


        return timeComponent;
    }

    /**
     * Retrieves the TimeComponent representing the selection of every hour between two specified values in a cron expression.
     * This method allows the user to choose every hour between a start and end value. The selected hours are displayed in a layout
     * containing Select components and labels for user interaction.
     *
     * @return TimeComponent representing the selection of every hour between specified values
     */
    private TimeComponent getEveryHourBetween() {
        NativeLabel label = new NativeLabel(getTranslation("time-component.every-hour-between-hour"
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

        NativeLabel label2 = new NativeLabel(getTranslation("time-component.and-hour"
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
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        hoursEndSelect.addValueChangeListener(event -> {
            this.hourPart = hoursStartSelect.getValue() + "-" + event.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        return timeComponent;
    }

    /**
     * Retrieves a TimeComponent representing the selection of specific hour options in a cron expression.
     *
     * @return TimeComponent representing the selection of specific hour options
     */
     TimeComponent getSpecificHour() {

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
                this.cronExpressionTf.setValue(this.getCronExpression());
            });
        });
        layout.setSizeFull();

        return timeComponent;
    }

    /**
     * Retrieves a TimeComponent representing the selection of every day in a cron expression.
     *
     * @return TimeComponent representing the selection of every day
     */
    TimeComponent getEveryDay() {
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-day"
            , UI.getCurrent().getLocale()), new Div());
        timeComponent.setValue("*");

        return timeComponent;
    }

    /**
     * Retrieves a TimeComponent representing the selection of every day starting on a specific day of the week in a cron expression.
     * The method constructs a layout with dropdowns for selecting the day of the week and the starting day.
     * Users can choose the frequency of every day and the day of the week to start on.
     *
     * @return TimeComponent representing the selection of every day starting on a specific day of the week
     */
    TimeComponent getEveryDayStartingOnDay() {
        NativeLabel label = new NativeLabel(getTranslation("time-component.every"
            , UI.getCurrent().getLocale()));
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

        NativeLabel label2 = new NativeLabel(getTranslation("time-component.days-starting-on"
            , UI.getCurrent().getLocale()));

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, daySelect, label2, dayStartSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-nth-day-starting-on"
            , UI.getCurrent().getLocale()), layout);
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
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        daySelect.addValueChangeListener(event -> {
            this.dayOfWeekPart = this.dayOfWeek(dayStartSelect.getValue()) + "/" + event.getValue();
            this.dayOfMonthPart = "?";
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        return timeComponent;
    }

    /**
     * Retrieves a TimeComponent object representing a user interface component allowing the user
     * to select the frequency of repetition for every day starting on a specific calendar day.
     *
     * @return TimeComponent object with the necessary UI components and functionality for selecting
     * the frequency of repetition for every day starting on a specific calendar day.
     */
    TimeComponent getEveryDayStartingOnCalendarDay() {
        NativeLabel label = new NativeLabel(getTranslation("time-component.every"
            , UI.getCurrent().getLocale()));
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

        NativeLabel label2 = new NativeLabel(getTranslation("time-component.days-starting-on-the"
            , UI.getCurrent().getLocale()));

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, daySelect, label2, dayStartSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-nth-day-starting-on-the"
            , UI.getCurrent().getLocale()), layout);
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
            this.cronExpressionTf.setValue(this.getCronExpression());
            timeComponent.setValue(this.dayOfMonthPart);
        });

        daySelect.addValueChangeListener(event -> {
            this.dayOfMonthPart = dayStartSelect.getValue().substring(0, dayStartSelect.getValue().length()-2) + "/" + event.getValue();
            this.dayOfWeekPart = "?";
            this.cronExpressionTf.setValue(this.getCronExpression());
            timeComponent.setValue(this.dayOfMonthPart);
        });

        return timeComponent;
    }

    /**
     * Retrieves the TimeComponent for selecting a specific day of the week.
     *
     * @return TimeComponent representing the selection of a specific day of the week
     */
    TimeComponent getSpecificDayOfWeek() {

        List<Checkbox> dayStart = new ArrayList<>();
        dayStart.add(new Checkbox("Monday"));
        dayStart.add(new Checkbox("Tuesday"));
        dayStart.add(new Checkbox("Wednesday"));
        dayStart.add(new Checkbox("Thursday"));
        dayStart.add(new Checkbox("Friday"));
        dayStart.add(new Checkbox("Saturday"));
        dayStart.add(new Checkbox("Sunday"));

        GridLayout layout = new GridLayout(7, 1);
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.specific-day-of-the-week"
            , UI.getCurrent().getLocale()), layout);
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
                this.cronExpressionTf.setValue(this.getCronExpression());
            });
        });
        layout.setSizeFull();

        return timeComponent;
    }

    /**
     * Constructs and returns a TimeComponent representing a specific nth instance of a day in the month.
     * The TimeComponent includes a layout for UI display, combo boxes for selecting the instance number and day of the week,
     * and radio buttons for setting the day of the week. Updates the TimeComponent value and cron expression text field
     * based on user selections.
     *
     * @return TimeComponent representing the specific nth day of the week in the month
     */
    TimeComponent getSpecificNthDayOfMonth() {

        GridLayout layout = new GridLayout(7, 1);
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.specific-nth-instance-of-day-in-month"
            , UI.getCurrent().getLocale()), layout);

        if(this.dayOfWeekPart.contains("#")) {
            timeComponent.setValue(this.dayOfWeekPart);
        }
        else {
            timeComponent.setValue("2#1");
        }

        String[] tokens = timeComponent.getValue().split("#");

        ComboBox<String> instanceNumCb = new ComboBox<>();
        instanceNumCb.setItems("1st", "2nd", "3rd", "4th", "5th");
        instanceNumCb.setValue(this.instanceTense(tokens[1]));

        layout.addComponent(instanceNumCb);

        RadioButtonGroup<String> radioGroup = new RadioButtonGroup<>();
        radioGroup.setItems("Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday", "Sunday");
        radioGroup.setValue(this.numToDayOfWeek(tokens[0]));

        layout.addComponent(radioGroup);
        layout.addComponent(new NativeLabel(" of the month."));



        instanceNumCb.addValueChangeListener(event -> {
            this.dayOfMonthPart = "?";
            dayOfWeekPart = this.dayOfWeek(radioGroup.getValue())+"#"+this.instance(instanceNumCb.getValue());
            timeComponent.setValue(this.dayOfWeekPart);
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        radioGroup.addValueChangeListener(event -> {
            this.dayOfMonthPart = "?";
            dayOfWeekPart = this.dayOfWeek(radioGroup.getValue())+"#"+this.instance(instanceNumCb.getValue());
            timeComponent.setValue(this.dayOfWeekPart);
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        layout.setSizeFull();

        return timeComponent;
    }

    /**
     * Creates a TimeComponent displaying checkboxes for each day of the month.
     *
     * @return A TimeComponent representing specific day of the month with checkboxes for selection
     */
    TimeComponent getSpecificDay() {
        List<Checkbox> day = new ArrayList<>();
        IntStream.range(1, 32).forEach(i -> day.add(new Checkbox(Integer.toString(i))));

        GridLayout layout = new GridLayout(12, 3);
        layout.setSizeFull();

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.specific-day-of-the-month"
            , UI.getCurrent().getLocale()), layout);

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
                this.cronExpressionTf.setValue(this.getCronExpression());
            });
        });

        return timeComponent;
    }

    /**
     * Retrieves the TimeComponent representing the last day of the month selection in a cron expression.
     *
     * @return TimeComponent representing the last day of the month selection
     */
    TimeComponent getLastDayOfMonth() {
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.last-day-of-the-month"
            , UI.getCurrent().getLocale()), new Div());
        timeComponent.setValue("L");

        return timeComponent;
    }

    /**
     * Retrieves a TimeComponent representing the last weekday of the month selection in a cron expression.
     *
     * @return TimeComponent representing the last weekday of the month selection
     */
    TimeComponent getLastWeekDayOfMonth() {
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.last-weekday-of-the-month"
            , UI.getCurrent().getLocale()), new Div());
        timeComponent.setValue("LW");

        return timeComponent;
    }


    /**
     * Retrieves the TimeComponent representing the first weekday of the month selection.
     *
     * @return TimeComponent representing the first weekday of the month selection
     */
    TimeComponent getFirstWeekDayOfMonth() {
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.first-weekday-of-the-month"
            , UI.getCurrent().getLocale()), new Div());
        timeComponent.setValue("1W");

        return timeComponent;
    }

    /**
     * Retrieves the time component for selecting the last day of the month.
     * The component includes a drop-down list to select the day of the week.
     *
     * @return the time component for selecting the last day of the month
     */
    TimeComponent getLastDaySelectOfMonth() {

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

        NativeLabel start = new NativeLabel(getTranslation("time-component.on-the-last"
            , UI.getCurrent().getLocale()));
        NativeLabel end = new NativeLabel(getTranslation("time-component.of-the-month"
            , UI.getCurrent().getLocale()));

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(start, daySelect, end);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, start, end);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.specific-last-day-of-month"
            , UI.getCurrent().getLocale()), layout);
        timeComponent.setValue("1L");

        if(this.dayOfWeekPart.contains("L")) {
            daySelect.setValue(this.numToDayOfWeek(this.dayOfWeekPart.substring(0,1)));
            timeComponent.setValue(this.dayOfWeekPart);
        }

        daySelect.addValueChangeListener(event -> {
            this.dayOfMonthPart = "?";
            this.dayOfWeekPart = this.dayOfWeek(event.getValue()) + "L";
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        return timeComponent;
    }

    /**
     * Generates a layout that displays options for selecting a specific month or range of months
     *
     * @return a VerticalLayout containing a RadioButtonGroup with options based on the selected month configuration
     */
    private Component getMonthLayout() {
        RadioButtonGroup<TimeComponent> radioGroup = new RadioButtonGroup<>();

        TimeComponent everyMonth = this.getEveryMonth();
        TimeComponent everyMonthStartingFrom = this.getEveryMonthStartingFrom();
        TimeComponent specificMonths = this.getSpecificMonth();
        TimeComponent everyMonthBetween = this.getEveryMonthBetween();

        radioGroup.setItems(List.of(everyMonth
            , everyMonthStartingFrom
            , specificMonths
            , everyMonthBetween));
        radioGroup.setValue(everyMonth);

        radioGroup.setRenderer(new ComponentRenderer<>(timeComponent -> new Span(timeComponent.getName())));

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(radioGroup);

        if(this.monthPart.equals("*")) {
            radioGroup.setValue(everyMonth);
            layout.add(radioGroup, everyMonth.getComponent());
        }
        else if(this.monthPart.contains("/")) {
            radioGroup.setValue(everyMonthStartingFrom);
            layout.add(radioGroup, everyMonthStartingFrom.getComponent());
        }
        else if(this.monthPart.contains(",") || StringUtils.isNumeric(this.monthPart)) {
            radioGroup.setValue(specificMonths);
            layout.add(radioGroup, specificMonths.getComponent());
        }
        else {
            radioGroup.setValue(everyMonthBetween);
            layout.add(radioGroup, everyMonthBetween.getComponent());
        }

        radioGroup.addValueChangeListener(event -> {
            this.monthPart = event.getValue().getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
            layout.removeAll();
            layout.add(radioGroup, event.getValue().getComponent());
        });

        return layout;
    }

    /**
     * Retrieves the TimeComponent for every month.
     *
     * @return TimeComponent object representing "every month"
     */
    private TimeComponent getEveryMonth() {
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-month"
            , UI.getCurrent().getLocale()), new Div());
        timeComponent.setValue("*");

        return timeComponent;
    }

    /**
     * Generates a TimeComponent for selecting every month starting from a specified month and day of the month.
     *
     * @return TimeComponent for selecting every month starting from a specified month and day of the month
     */
    private TimeComponent getEveryMonthStartingFrom() {
        NativeLabel label = new NativeLabel(getTranslation("time-component.every"
            , UI.getCurrent().getLocale()));
        List<String> monthsNum = new ArrayList<>();
        IntStream.range(1, 13).forEach(i -> monthsNum.add(Integer.toString(i)));
        Select<String> monthsNumSelect = new Select<>();
        monthsNumSelect.setEnabled(true);
        monthsNumSelect.setItems(monthsNum);
        monthsNumSelect.setValue("1");

        List<String> monthStart = List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");
        Select<String> monthStartSelect = new Select<>();
        monthStartSelect.setEnabled(true);
        monthStartSelect.setItems(monthStart);
        monthStartSelect.setValue("JAN");

        NativeLabel label2 = new NativeLabel(getTranslation("time-component.month-starting-from"
            , UI.getCurrent().getLocale()));

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, monthsNumSelect, label2, monthStartSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-n-months-starting-from"
            , UI.getCurrent().getLocale()), layout);
        timeComponent.setValue("JAN/1");

        if(this.monthPart.contains("/")) {
            StringTokenizer st = new StringTokenizer(this.monthPart, "/");
            monthStartSelect.setValue(st.nextToken());
            monthsNumSelect.setValue(st.nextToken());
            timeComponent.setValue(this.monthPart);
        }

        monthsNumSelect.addValueChangeListener(event -> {
            this.monthPart = monthStartSelect.getValue() + "/" + event.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        monthStartSelect.addValueChangeListener(event -> {
            this.monthPart = event.getValue() + "/" + monthsNumSelect.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });


        return timeComponent;
    }

    /**
     * Returns a TimeComponent representing a specific month selection with checkboxes for each month.
     * The TimeComponent allows the user to select one or more months.
     *
     * @return a TimeComponent with checkboxes for each month selection
     */
    private TimeComponent getSpecificMonth() {

        List<Checkbox> months = new ArrayList<>();
        List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC").forEach(i -> months.add(new Checkbox(i)));

        GridLayout layout = new GridLayout(12, 1);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.specific-month"
            , UI.getCurrent().getLocale()), layout);
        if(this.monthPart.contains(",")) {
            timeComponent.setValue(this.monthPart);
        }
        else {
            timeComponent.setValue("JAN");
        }

        List<String> selectedMonths = Arrays.asList(this.monthPart.split("\\s*,\\s*"));

        months.forEach(item -> {
            layout.addComponent(item);
            if(selectedMonths.contains(item.getLabel())) {
                item.setValue(true);
            }

            item.addValueChangeListener(event -> {
                StringBuffer value = new StringBuffer();
                months.forEach(checkbox -> {
                    if(checkbox.getValue()) {
                        value.append(checkbox.getLabel()).append(",");
                    }
                });
                this.monthPart = value.substring(0, value.length()-1);
                timeComponent.setValue(this.monthPart);
                this.cronExpressionTf.setValue(this.getCronExpression());
            });
        });
        layout.setSizeFull();

        return timeComponent;
    }

    /**
     * Retrieves a TimeComponent that allows the user to select a range of months.
     * The TimeComponent includes two Select inputs for choosing the start and end months of the range.
     * By default, both Select inputs are set to "JAN".
     * If the monthPart variable contains a range of months separated by "-",
     * the Select inputs and TimeComponent's value will be updated accordingly.
     * Listens to changes in the Select inputs and updates the monthPart value and cronExpressionTf accordingly.
     *
     * @return a TimeComponent for selecting a range of months
     */
    private TimeComponent getEveryMonthBetween() {
        NativeLabel label = new NativeLabel(getTranslation("time-component.every-month-between"
            , UI.getCurrent().getLocale()));
        List<String> months = List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");
        Select<String> monthStartSelect = new Select<>();
        monthStartSelect.setEnabled(true);
        monthStartSelect.setItems(months);
        monthStartSelect.setValue("JAN");


        Select<String> monthEndSelect = new Select<>();
        monthEndSelect.setEnabled(true);
        monthEndSelect.setItems(months);
        monthEndSelect.setValue("JAN");

        NativeLabel label2 = new NativeLabel(getTranslation("time-component.and-month"
            , UI.getCurrent().getLocale()));

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, monthStartSelect, label2, monthEndSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-month-between"
            , UI.getCurrent().getLocale()), layout);
        timeComponent.setValue("JAN-JAN");

        if(this.monthPart.contains("-")) {
            StringTokenizer st = new StringTokenizer(this.monthPart, "-");
            monthStartSelect.setValue(st.nextToken());
            monthEndSelect.setValue(st.nextToken());
            timeComponent.setValue(this.monthPart);
        }

        monthStartSelect.addValueChangeListener(event -> {
            this.monthPart = event.getValue() + "-" + monthEndSelect.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        monthEndSelect.addValueChangeListener(event -> {
            this.monthPart = monthStartSelect.getValue() + "-" + event.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        return timeComponent;
    }

    /**
     * Retrieves a layout containing radio buttons for selecting various yearly time components.
     *
     * @return a VerticalLayout component displaying radio buttons for selecting yearly time components
     */
    private Component getYearLayout() {
        RadioButtonGroup<TimeComponent> radioGroup = new RadioButtonGroup<>();

        TimeComponent everyYear = this.getEveryYear();
        TimeComponent everyYearStartingFrom = this.getEveryYearStartingFrom();
        TimeComponent specificYears = this.getSpecificYear();
        TimeComponent everyYearBetween = this.getEveryYearBetween();

        radioGroup.setItems(List.of(everyYear
            , everyYearStartingFrom
            , specificYears
            , everyYearBetween));
        radioGroup.setValue(everyYear);

        radioGroup.setRenderer(new ComponentRenderer<>(timeComponent -> new Span(timeComponent.getName())));

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(radioGroup);

        if(this.yearPart.equals("*")) {
            radioGroup.setValue(everyYear);
            layout.add(radioGroup, everyYear.getComponent());
        }
        else if(this.yearPart.contains("/")) {
            radioGroup.setValue(everyYearStartingFrom);
            layout.add(radioGroup, everyYearStartingFrom.getComponent());
        }
        else if(this.yearPart.contains(",") || StringUtils.isNumeric(this.yearPart)) {
            radioGroup.setValue(specificYears);
            layout.add(radioGroup, specificYears.getComponent());
        }
        else {
            radioGroup.setValue(everyYearBetween);
            layout.add(radioGroup, everyYearBetween.getComponent());
        }

        radioGroup.addValueChangeListener(event -> {
            this.yearPart = event.getValue().getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
            layout.removeAll();
            layout.add(radioGroup, event.getValue().getComponent());
        });

        return layout;
    }

    /**
     * Retrieve a TimeComponent representing "every year".
     *
     * @return TimeComponent representing "every year"
     */
    private TimeComponent getEveryYear() {
        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-year"
            , UI.getCurrent().getLocale()), new Div());
        timeComponent.setValue("*");

        return timeComponent;
    }

    /**
     * Retrieves a TimeComponent representing a selection for every n years starting from a specified year.
     *
     * @return TimeComponent representing the selection for every n years starting from a specified year
     */
    private TimeComponent getEveryYearStartingFrom() {

        // Get the current year
        int currentYear = new DateTime().getYear();

        NativeLabel label = new NativeLabel(getTranslation("time-component.every"
            , UI.getCurrent().getLocale()));
        List<String> yearNum = new ArrayList<>();
        IntStream.range(1, 11).forEach(i -> yearNum.add(Integer.toString(i)));
        Select<String> yearsNumSelect = new Select<>();
        yearsNumSelect.setEnabled(true);
        yearsNumSelect.setItems(yearNum);
        yearsNumSelect.setValue("1");

        List<String> yearStart = new ArrayList<>();
        IntStream.range(currentYear, currentYear + 100).forEach(i -> yearStart.add(Integer.toString(i)));
        Select<String> yearStartSelect = new Select<>();
        yearStartSelect.setEnabled(true);
        yearStartSelect.setItems(yearStart);
        yearStartSelect.setValue(currentYear + "");

        NativeLabel label2 = new NativeLabel(getTranslation("time-component.year-starting-from"
            , UI.getCurrent().getLocale()));

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, yearsNumSelect, label2, yearStartSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-n-years-starting-from"
            , UI.getCurrent().getLocale()), layout);
        timeComponent.setValue(currentYear + "/1");

        if(this.yearPart.contains("/")) {
            StringTokenizer st = new StringTokenizer(this.yearPart, "/");
            yearStartSelect.setValue(st.nextToken());
            yearsNumSelect.setValue(st.nextToken());
            timeComponent.setValue(this.yearPart);
        }

        yearsNumSelect.addValueChangeListener(event -> {
            this.yearPart = yearStartSelect.getValue() + "/" + event.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        yearStartSelect.addValueChangeListener(event -> {
            this.yearPart = event.getValue() + "/" + yearsNumSelect.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });


        return timeComponent;
    }

    /**
     * Retrieves a list of checkboxes for selecting a specific year.
     *
     * @return TimeComponent containing checkboxes for each year starting from the current year up to 2099
     */
    private TimeComponent getSpecificYear() {

        // Get the current year
        int currentYear = new DateTime().getYear();

        List<Checkbox> year = new ArrayList<>();
        // Quartz only supports years up until 2099
        IntStream.range(currentYear, 2100).forEach(i -> year.add(new Checkbox(Integer.toString(i))));

        GridLayout layout = new GridLayout(10, 10);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.specific-year"
            , UI.getCurrent().getLocale()), layout);
        if(this.yearPart.contains(",")) {
            timeComponent.setValue(this.yearPart);
        }
        else {
            timeComponent.setValue(currentYear + "");
        }

        List<String> selectedYear = Arrays.asList(this.yearPart.split("\\s*,\\s*"));

        year.forEach(item -> {
            layout.addComponent(item);
            if(selectedYear.contains(item.getLabel())) {
                item.setValue(true);
            }

            item.addValueChangeListener(event -> {
                StringBuffer value = new StringBuffer();
                year.forEach(checkbox -> {
                    if(checkbox.getValue()) {
                        value.append(checkbox.getLabel()).append(",");
                    }
                });
                this.yearPart = value.substring(0, value.length()-1);
                timeComponent.setValue(this.yearPart);
                this.cronExpressionTf.setValue(this.getCronExpression());
            });
        });
        layout.setSizeFull();

        return timeComponent;
    }

    /**
     * Retrieves a TimeComponent with options to select a range of years between the current year and the next 100 years.
     * Allows users to select a start year and an end year using dropdown menus.
     * The years selection defaults to the current year.
     *
     * @return a TimeComponent with options to select a range of years
     */
    private TimeComponent getEveryYearBetween() {

        // Get the current year
        int currentYear = new DateTime().getYear();

        NativeLabel label = new NativeLabel(getTranslation("time-component.every-year-between"
            , UI.getCurrent().getLocale()));
        List<String> year = new ArrayList<>();
        IntStream.range(currentYear, currentYear + 100).forEach(i -> year.add(Integer.toString(i)));
        Select<String> yearStartSelect = new Select<>();
        yearStartSelect.setEnabled(true);
        yearStartSelect.setItems(year);
        yearStartSelect.setValue(currentYear + "");


        Select<String> yearEndSelect = new Select<>();
        yearEndSelect.setEnabled(true);
        yearEndSelect.setItems(year);
        yearEndSelect.setValue(currentYear + "");

        NativeLabel label2 = new NativeLabel(getTranslation("time-component.and-year"
            , UI.getCurrent().getLocale()));

        HorizontalLayout layout = new HorizontalLayout();
        layout.add(label, yearStartSelect, label2, yearEndSelect);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, label, label2);

        TimeComponent timeComponent = new TimeComponent(getTranslation("time-component.every-year-between"
            , UI.getCurrent().getLocale()), layout);
        timeComponent.setValue(currentYear + "-" + currentYear);

        if(this.yearPart.contains("-")) {
            StringTokenizer st = new StringTokenizer(this.yearPart, "-");
            yearStartSelect.setValue(st.nextToken());
            yearEndSelect.setValue(st.nextToken());
            timeComponent.setValue(this.yearPart);
        }

        yearStartSelect.addValueChangeListener(event -> {
            this.yearPart = event.getValue() + "-" + yearEndSelect.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        yearEndSelect.addValueChangeListener(event -> {
            this.yearPart = yearStartSelect.getValue() + "-" + event.getValue();
            this.cronExpressionTf.setValue(this.getCronExpression());
        });

        return timeComponent;
    }

    /**
     * Concatenates the individual parts of a cron expression to form a complete cron expression.
     * Also sets a natural language description of the cron expression.
     *
     * @return The complete cron expression formed by concatenating the individual parts
     */
    public String getCronExpression() {
        StringBuffer cronExpression = new StringBuffer();

        cronExpression.append(this.secondPart).append(" ");
        cronExpression.append(this.minutePart).append(" ");
        cronExpression.append(this.hourPart).append(" ");
        cronExpression.append(this.dayOfMonthPart).append(" ");
        cronExpression.append(this.monthPart).append(" ");
        cronExpression.append(this.dayOfWeekPart).append(" ");
        cronExpression.append(this.yearPart);

        this.setNaturalLanguageDescription(cronExpression.toString());

        return cronExpression.toString();
    }

    /**
     * Sets the natural language description for the given cron expression.
     *
     * @param cronExpression the cron expression for which the natural language description is to be set
     */
    protected abstract void setNaturalLanguageDescription(String cronExpression);

    /**
     * Returns the numerical representation of the day of the week based on the given text representation.
     *
     * @param textDay the text representation of the day of the week (e.g. "Sunday", "Monday", etc.)
     * @return the numerical representation of the day of the week (1 for Sunday, 2 for Monday, ..., 7 for Saturday), or an empty string if the input is not a valid day
     */
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

    /**
     * Maps a given instance string to its equivalent numerical representation.
     *
     * @param instance the instance string to be mapped ("1st", "2nd", "3rd", "4th", "5th")
     * @return the corresponding numerical representation as a string, or an empty string if no match found
     */
    private String instance(String instance) {
        switch (instance){
            case "1st" : return "1";
            case "2nd" : return "2";
            case "3rd" : return "3";
            case "4th" : return "4";
            case "5th" : return "5";
            default: return "";
        }
    }

    /**
     * Returns the ordinal indicator for a given instance number.
     *
     * @param instance the instance number as a string (e.g. "1", "2", "3", etc.)
     * @return the ordinal indicator corresponding to the given instance number,
     *         empty string if the instance number is not within the specified range
     */
    private String instanceTense(String instance) {
        switch (instance){
            case "1" : return "1st";
            case "2" : return "2nd";
            case "3" : return "3rd";
            case "4" : return "4th";
            case "5" : return "5th";
            default: return "";
        }
    }

    /**
     * Converts a numeric representation of a day to its corresponding day of the week.
     *
     * @param numDay a String representing a numeric value for the day (e.g., "1" for Sunday, "2" for Monday, etc.)
     * @return the day of the week as a String, or an empty String if the input is invalid
     */
    private String numToDayOfWeek(String numDay) {
        switch (numDay){
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

    /**
     * Converts the full name of a day of the week to its corresponding abbreviation.
     *
     * @param textDay the full name of the day of the week to abbreviate
     * @return the abbreviation of the provided day of the week, or an empty string if the input is invalid
     */
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

    /**
     * This method is used to initialise the parts of a cron expression by parsing the given cron expression.
     * It extracts the individual parts (second, minute, hour, day of month, month, day of week, and optional year) from the provided cron expression and assigns them to the corresponding
     * instance variables.
     * If the cron expression does not contain at least 6 tokens, a log message is generated indicating that the internal state of the cron dialog cannot be initialised.
     */
    private void initialiseParts() {
        StringTokenizer stringTokenizer = new StringTokenizer(this.cronExpression, " ");
        if(stringTokenizer.countTokens() < 6) {
            logger.info("Cannot initialise the cron dialog internal state. The cron expression must contain at least 6 tokens");
        }
        else {
            this.secondPart = stringTokenizer.nextToken();
            this.minutePart = stringTokenizer.nextToken();
            this.hourPart = stringTokenizer.nextToken();
            this.dayOfMonthPart = stringTokenizer.nextToken();
            this.monthPart = stringTokenizer.nextToken();
            this.dayOfWeekPart = stringTokenizer.nextToken();
            if(stringTokenizer.countTokens() == 7) {
                this.yearPart = stringTokenizer.nextToken();
            }
        }
    }

    /**
     * Inner class that represents a time component within a cron schedule.
     */
    protected class TimeComponent {
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