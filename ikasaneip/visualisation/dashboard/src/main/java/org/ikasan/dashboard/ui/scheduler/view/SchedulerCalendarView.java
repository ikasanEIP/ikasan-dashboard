package org.ikasan.dashboard.ui.scheduler.view;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.IronIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.scheduler.component.*;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.solr.model.IkasanSolrDocumentSearchResults;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.solr.SolrGeneralService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vaadin.stefan.fullcalendar.*;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "scheduler", layout = IkasanAppLayout.class)
@UIScope
@Component
@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/hospital-events.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
public class SchedulerCalendarView extends VerticalLayout implements BeforeEnterObserver
{
    Logger logger = LoggerFactory.getLogger(SchedulerCalendarView.class);

    private static DateTimeFormatter MONTH_DATE_FORMATTER =  DateTimeFormatter.ofPattern("MMM YYYY");
    private static DateTimeFormatter YEAR_DATE_FORMATTER =  DateTimeFormatter.ofPattern("YYYY");
    private static DateTimeFormatter DAY_DATE_FORMATTER =  DateTimeFormatter.ofPattern("MMMM dd, YYYY");

    @Resource
    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;

    @Autowired
    private ModuleMetaDataService moduleMetadataService;

    @Resource
    private SolrGeneralService<IkasanSolrDocument, IkasanSolrDocumentSearchResults> solrGeneralService;

    @Resource
    private DateFormatter dateFormatter;

    @Resource
    private ConfigurationService configurationRestService;

    @Resource
    private ScheduledProcessManagementService scheduledProcessManagementService;

    @Resource
    private ModuleControlService moduleControlRestService;

    @Resource
    private MetaDataService metaDataRestService;

    private SchedulerAgentDashboardView schedulerAgentDashboardView;

    private Board scheduleJobsTab;
    private Board scheduleJStatsTab;
    private VerticalLayout calendarView;
    private FullCalendar calendar;
    private LocalDate localDate = LocalDate.now();
    private Span dateString;
    private ComboBox<CalendarView> comboBoxView;


    private boolean initialised = false;

    public SchedulerCalendarView()
    {
        this.setSpacing(false);
        this.setMargin(false);
    }

    private void init() {
        this.schedulerAgentDashboardView = new SchedulerAgentDashboardView(this.moduleMetadataService
            , this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService, this.metaDataRestService);
        this.schedulerAgentDashboardView.addClassName("styled");
        this.schedulerAgentDashboardView.setSizeFull();
        this.schedulerAgentDashboardView.setVisible(true);

        scheduleJobsTab = new Board();
        scheduleJobsTab.addClassName("styled");
        scheduleJobsTab.setSizeFull();
        scheduleJobsTab.setVisible(false);

        scheduleJStatsTab = new Board();
        scheduleJStatsTab.addClassName("styled");
        scheduleJStatsTab.setSizeFull();
        scheduleJStatsTab.setVisible(false);

        this.calendarView = calendarView();
        this.calendarView.addClassName("styled");
        this.calendarView.setVisible(false);

//        jobManagementTab = new Board();
//        jobManagementTab.addClassName("styled");
//        jobManagementTab.setSizeFull();
//        jobManagementTab.setVisible(false);

        Tab schedulerDashboardTab = new Tab("Scheduler Dashboard");
        Tab schedulerJobTab = new Tab("Scheduled Jobs");
//        Tab schedulerStatusTab = new Tab("Scheduler Statistics");
        Tab calendarTab = new Tab("Scheduled Jobs Calendar");
//        Tab schedulerJobManagementTab = new Tab("Scheduler Status");
        Tabs tabs = new Tabs(schedulerDashboardTab, schedulerJobTab, calendarTab);

        Map<Tab, com.vaadin.flow.component.Component> tabsToPages = new HashMap<>();
        tabsToPages.put(schedulerDashboardTab, this.schedulerAgentDashboardView);
        tabsToPages.put(schedulerJobTab, scheduleJobsTab);
//        tabsToPages.put(schedulerStatusTab, scheduleJStatsTab);
        tabsToPages.put(calendarTab, this.calendarView);
//        tabsToPages.put(schedulerJobManagementTab, jobManagementTab);

        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            com.vaadin.flow.component.Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
            selectedPage.setVisible(true);
        });

        Button addButton = new Button();
        addButton.getStyle().set("position", "absolute");
        addButton.getStyle().set("top", "70px");
        addButton.getStyle().set("right", "30px");

        addButton.addClickListener(buttonClickEvent -> {
            ScheduledJobDialog scheduledJobDialog = new ScheduledJobDialog(null, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService);
            scheduledJobDialog.open();
        });

        IronIcon addIcon = IronIcons.ADD.create();
        addIcon.setSize("16pt");

        addButton.getElement().appendChild(addIcon.getElement());

        this.add(tabs, addButton, this.schedulerAgentDashboardView, scheduleJobsTab, scheduleJStatsTab, this.calendarView);
    }

    private VerticalLayout calendarView() {
        VerticalLayout container = new VerticalLayout();
        calendar = FullCalendarBuilder.create().withAutoBrowserTimezone().build();
        calendar.setWeekNumbersVisible(false);
        calendar.addEntryClickedListener((ComponentEventListener<EntryClickedEvent>) entryClickedEvent -> {
//            ScheduledProcessExecutionDialog scheduledProcessExecutionDialog = new ScheduledProcessExecutionDialog();
//            scheduledProcessExecutionDialog.open();
        });
//        Header header = new Header();
//        header.
//        calendar.setHeader(new Header());
//        calendar.setEntryRenderCallback("" +
//            "function(info) {" +
//            "   console.log(info.event.title + 'X');" +
//            "   info.el.style.color = 'red';" +
//            "   info.el. = 'red';" +
//            "   return info.el; " +
//            "}"
//        );
        container.add(this.createBasicToolbar(), calendar);
        container.setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
        container.getStyle().set("flex-grow", "1");

        container.setFlexGrow(1, calendar);

        return container;
    }

    private HorizontalLayout createBasicToolbar() {
        Button buttonToday = new Button("Today", VaadinIcon.HOME.create(), e -> {
            calendar.today();
            if(this.comboBoxView.getValue().equals(CalendarViewImpl.LIST_DAY) || this.comboBoxView.getValue().equals(CalendarViewImpl.DAY_GRID_DAY)) {
                this.localDate = LocalDate.now();
                dateString.setText(DAY_DATE_FORMATTER.format(localDate));
            }
            else if(this.comboBoxView.getValue().equals(CalendarViewImpl.DAY_GRID_MONTH) || this.comboBoxView.getValue().equals(CalendarViewImpl.LIST_MONTH) ||
                this.comboBoxView.getValue().equals(CalendarViewImpl.DAY_GRID_WEEK) || this.comboBoxView.getValue().equals(CalendarViewImpl.LIST_WEEK)) {
                this.localDate = LocalDate.now();
                dateString.setText(MONTH_DATE_FORMATTER.format(localDate));
            }
            else if(this.comboBoxView.getValue().equals(CalendarViewImpl.LIST_YEAR)) {
                this.localDate = LocalDate.now();
                dateString.setText(YEAR_DATE_FORMATTER.format(localDate));
            }
        });
        Button buttonPrevious = new Button("Previous", VaadinIcon.ANGLE_LEFT.create(),  e -> {
            calendar.previous();
            if(this.comboBoxView.getValue().equals(CalendarViewImpl.LIST_DAY) || this.comboBoxView.getValue().equals(CalendarViewImpl.DAY_GRID_DAY)) {
                this.localDate = this.localDate.minusDays(1);
                dateString.setText(DAY_DATE_FORMATTER.format(localDate));
            }
            else if(this.comboBoxView.getValue().equals(CalendarViewImpl.DAY_GRID_WEEK) || this.comboBoxView.getValue().equals(CalendarViewImpl.LIST_WEEK)) {
                this.localDate = this.localDate.minusWeeks(1);
                dateString.setText(MONTH_DATE_FORMATTER.format(localDate));
            }
            else if(this.comboBoxView.getValue().equals(CalendarViewImpl.DAY_GRID_MONTH) || this.comboBoxView.getValue().equals(CalendarViewImpl.LIST_MONTH)) {
                this.localDate = this.localDate.minusMonths(1);
                dateString.setText(MONTH_DATE_FORMATTER.format(localDate));
            }
            else if(this.comboBoxView.getValue().equals(CalendarViewImpl.LIST_YEAR)) {
                this.localDate = this.localDate.minusYears(1);
                dateString.setText(YEAR_DATE_FORMATTER.format(localDate));
            }
        });
        Button buttonNext = new Button("Next", VaadinIcon.ANGLE_RIGHT.create(), e -> {
            calendar.next();
            if(this.comboBoxView.getValue().equals(CalendarViewImpl.LIST_DAY) || this.comboBoxView.getValue().equals(CalendarViewImpl.DAY_GRID_DAY)) {
                this.localDate = this.localDate.plusDays(1);
                dateString.setText(DAY_DATE_FORMATTER.format(localDate));
            }
            else if(this.comboBoxView.getValue().equals(CalendarViewImpl.DAY_GRID_WEEK) || this.comboBoxView.getValue().equals(CalendarViewImpl.LIST_WEEK)) {
                this.localDate = this.localDate.plusWeeks(1);
                dateString.setText(MONTH_DATE_FORMATTER.format(localDate));
            }
            else if(this.comboBoxView.getValue().equals(CalendarViewImpl.DAY_GRID_MONTH) || this.comboBoxView.getValue().equals(CalendarViewImpl.LIST_MONTH)) {
                this.localDate = this.localDate.plusMonths(1);
                dateString.setText(MONTH_DATE_FORMATTER.format(localDate));
            }
            else if(this.comboBoxView.getValue().equals(CalendarViewImpl.LIST_YEAR)) {
                this.localDate = this.localDate.plusYears(1);
                dateString.setText(YEAR_DATE_FORMATTER.format(localDate));
            }
        });
        buttonNext.setIconAfterText(true);

        // simulate the date picker light that we can use in polymer
        DatePicker gotoDate = new DatePicker();
        gotoDate.addValueChangeListener(event1 -> calendar.gotoDate(event1.getValue()));
        gotoDate.getElement().getStyle().set("visibility", "hidden");
        gotoDate.getElement().getStyle().set("position", "fixed");
        gotoDate.setWidth("0px");
        gotoDate.setHeight("0px");
        gotoDate.setWeekNumbersVisible(true);
        Button buttonDatePicker = new Button(VaadinIcon.CALENDAR.create());
        buttonDatePicker.getElement().appendChild(gotoDate.getElement());
        buttonDatePicker.addClickListener(event -> gotoDate.open());

        dateString = new Span(MONTH_DATE_FORMATTER.format(this.localDate));
        dateString.getStyle().set("font-size", "20pt");


        comboBoxView = new ComboBox<>("", List.of(CalendarViewImpl.DAY_GRID_DAY,
            CalendarViewImpl.DAY_GRID_WEEK, CalendarViewImpl.DAY_GRID_MONTH, CalendarViewImpl.LIST_DAY,
            CalendarViewImpl.LIST_WEEK, CalendarViewImpl.LIST_MONTH, CalendarViewImpl.LIST_YEAR));
        comboBoxView.setRenderer(new ComponentRenderer<>(item -> {
            Div text = new Div();
            text.setText(item.getName());

            if(item.getName().equals(CalendarViewImpl.DAY_GRID_DAY.getName())){
                text.setText("Grid Day");
            }
            else if(item.getName().equals(CalendarViewImpl.DAY_GRID_WEEK.getName())){
                text.setText("Grid Week");
            }
            else if(item.getName().equals(CalendarViewImpl.DAY_GRID_MONTH.getName())){
                text.setText("Grid Month");
            }
            else if(item.getName().equals(CalendarViewImpl.LIST_DAY.getName())){
                text.setText("List Day");
            }
            else if(item.getName().equals(CalendarViewImpl.LIST_WEEK.getName())){
                text.setText("List Week");
            }
            else if(item.getName().equals(CalendarViewImpl.LIST_MONTH.getName())){
                text.setText("List Month");
            }
            else if(item.getName().equals(CalendarViewImpl.LIST_YEAR.getName())){
                text.setText("List Year");
            }

            return text;
        }));
        comboBoxView.setItemLabelGenerator(item -> {
            if(item.getName().equals(CalendarViewImpl.DAY_GRID_DAY.getName())){
                return "Grid Day";
            }
            else if(item.getName().equals(CalendarViewImpl.DAY_GRID_WEEK.getName())){
                return "Grid Week";
            }
            else if(item.getName().equals(CalendarViewImpl.DAY_GRID_MONTH.getName())){
                return "Grid Month";
            }
            else if(item.getName().equals(CalendarViewImpl.LIST_DAY.getName())){
                return "List Day";
            }
            else if(item.getName().equals(CalendarViewImpl.LIST_WEEK.getName())){
                return "List Week";
            }
            else if(item.getName().equals(CalendarViewImpl.LIST_MONTH.getName())){
                return "List Month";
            }
            else if(item.getName().equals(CalendarViewImpl.LIST_YEAR.getName())){
                return "List Year";
            }

            return "";
        });

        comboBoxView.setValue(CalendarViewImpl.DAY_GRID_MONTH);
        comboBoxView.addValueChangeListener(e -> {
            if(e.getValue().equals(CalendarViewImpl.LIST_DAY) || e.getValue().equals(CalendarViewImpl.DAY_GRID_DAY)) {
                dateString.setText(DAY_DATE_FORMATTER.format(localDate));
            }
            else if(e.getValue().equals(CalendarViewImpl.DAY_GRID_MONTH) || e.getValue().equals(CalendarViewImpl.LIST_MONTH) ||
                e.getValue().equals(CalendarViewImpl.DAY_GRID_WEEK) || e.getValue().equals(CalendarViewImpl.LIST_WEEK)) {
                dateString.setText(MONTH_DATE_FORMATTER.format(localDate));
            }
            else if(e.getValue().equals(CalendarViewImpl.LIST_YEAR)) {
                dateString.setText(YEAR_DATE_FORMATTER.format(localDate));
            }
            CalendarView value = e.getValue();
            calendar.changeView(value == null ? CalendarViewImpl.DAY_GRID_MONTH : value);
            calendar.render();
        });

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        TextField textField = new TextField("");

        textField.setSuffixComponent(icon);
        textField.setWidth("300px");

        HorizontalLayout layout = new HorizontalLayout(buttonToday, buttonPrevious, buttonNext, buttonDatePicker
            , gotoDate, dateString, comboBoxView, textField);
        layout.setVerticalComponentAlignment(Alignment.CENTER, dateString);
        layout.setVerticalComponentAlignment(Alignment.START, textField);
        return layout;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!initialised) {
            this.init();
            this.schedulerAgentDashboardView.beforeEnter(beforeEnterEvent);
            scheduleJobsTab.addRow(new UpcomingJobExecutionsWidget(this.scheduledProcessManagementService, this.dateFormatter, this.configurationRestService,
                this.moduleControlRestService, this.metaDataRestService, this.moduleMetadataService));
            scheduleJobsTab.addRow(new RunningAndRecentlyCompletedJobExecutionsWidget(this.scheduledProcessManagementService, this.dateFormatter,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.moduleMetadataService));

            this.scheduleJStatsTab.addRow(new DurationWidget());
            this.scheduleJStatsTab.addRow(new StartAndEndTimeWidget());
            initialised = true;
        }

        SchedulerCalendarView.createTestEntries(calendar);
    }

    private static void createTestEntries(FullCalendar calendar) {
        LocalDate now = LocalDate.now();


        HashMap<String, Object> extendedProps = new HashMap<String, Object>();
        HashMap<String, Object> cursors = new HashMap<String, Object>();
        cursors.put("enabled", "pointer");
        cursors.put("disabled", "not-allowed");
        extendedProps.put("cursors", cursors);


        createTimedEntry(calendar, "Agent[Agent 1]\n Job Group[Job Group 1]\n Job[Job 1]", now.withDayOfMonth(3).atTime(10, 0), 0, null);
        createTimedBackgroundEntry(calendar, now.withDayOfMonth(3).atTime(10, 0), 120, null);
        createTimedEntry(calendar, "Agent[Agent 1]\n Job Group[Job Group 1]\n Job[Job 1]", now.withDayOfMonth(7).atTime(11, 30), 0, "mediumseagreen");
        createTimedEntry(calendar, "Agent[Agent 2]\n Job Group[Job Group 1]\n Job[Job 1]", now.withDayOfMonth(7).atTime(11, 30), 0, "black");
        createTimedEntry(calendar, "Agent[Agent 3]\n Job Group[Job Group 1]\n Job[Job 1]", now.withDayOfMonth(7).atTime(11, 30), 0, "pink");
        createTimedEntry(calendar, "Agent[Agent 1]\n Job Group[Job Group 1]\n Job[Job 1]", now.withDayOfMonth(12).atTime(9, 0), 0, "mediumseagreen");
        createTimedEntry(calendar, "Agent[Agent 1]\n Job Group[Job Group 1]\n Job[Job 1]", now.withDayOfMonth(13).atTime(10, 0), 0, "mediumseagreen");
        createTimedEntry(calendar, "Agent[Agent 1]\n Job Group[Job Group 1]\n Job[Job 1]", now.withDayOfMonth(17).atTime(11, 30), 0, "mediumseagreen");
        createTimedEntry(calendar, "Agent[Agent 1]\n Job Group[Job Group 1]\n Job[Job 1]", now.withDayOfMonth(22).atTime(9, 0), 0, "mediumseagreen");

        createTimedEntry(calendar, "Agent[Agent 1]\n Job Group[Job Group 1]\n Job[Job 1]", now.withDayOfMonth(7).atTime(17, 30), 0, "violet");
        createTimedEntry(calendar, "Agent[Agent 1]\n Job Group[Job Group 1]\n Job[Job 1]", now.withDayOfMonth(20).atTime(11, 30), 0, "violet");
        createTimedEntry(calendar, "Agent[Agent 1]\n Job Group[Job Group 1]\n Job[Job 1]", now.withDayOfMonth(10).atTime(20, 30), 0, "dodgerblue");
//        createDayEntry(calendar, "Short trip", now.withDayOfMonth(17), 2, "dodgerblue");
//        createDayEntry(calendar, "John's Birthday", now.withDayOfMonth(23), 1, "gray");
//        createDayEntry(calendar, "This special holiday", now.withDayOfMonth(4), 1, "gray");
//
//        createDayEntry(calendar, "Multi 1", now.withDayOfMonth(12), 2, "tomato");
//        createDayEntry(calendar, "Multi 2", now.withDayOfMonth(12), 2, "tomato");
//        createDayEntry(calendar, "Multi 3", now.withDayOfMonth(12), 2, "tomato");
//        createDayEntry(calendar, "Multi 4", now.withDayOfMonth(12), 2, "tomato");
//        createDayEntry(calendar, "Multi 5", now.withDayOfMonth(12), 2, "tomato");
//        createDayEntry(calendar, "Multi 6", now.withDayOfMonth(12), 2, "tomato");
//        createDayEntry(calendar, "Multi 7", now.withDayOfMonth(12), 2, "tomato");
//        createDayEntry(calendar, "Multi 8", now.withDayOfMonth(12), 2, "tomato");
//        createDayEntry(calendar, "Multi 9", now.withDayOfMonth(12), 2, "tomato");
//        createDayEntry(calendar, "Multi 10", now.withDayOfMonth(12), 2, "tomato");
//
//        createDayBackgroundEntry(calendar, now.withDayOfMonth(4), 6, "#B9FFC3");
//        createDayBackgroundEntry(calendar, now.withDayOfMonth(19), 2, "#CEE3FF");
        createTimedBackgroundEntry(calendar, now.withDayOfMonth(20).atTime(11, 0), 5, "#FBC8FF");

        createRecurringEvents(calendar);
    }

    static void createRecurringEvents(FullCalendar calendar) {
        LocalDate now = LocalDate.now();

        Entry recurring = new Entry();
        recurring.setRecurring(true);
        recurring.setTitle("Agent[Agent 1]\n Job Group[Job Group 1]\n Job[Job 1]");
        recurring.setColor("lightgray");
        recurring.setRecurringDaysOfWeeks(Collections.singleton(DayOfWeek.SUNDAY));

        recurring.setRecurringStartDate(now.with(TemporalAdjusters.firstDayOfYear()), calendar.getTimezone());
        recurring.setRecurringEndDate(now.with(TemporalAdjusters.lastDayOfYear()), calendar.getTimezone());
        recurring.setRecurringStartTime(LocalTime.of(14, 0));
        recurring.setRecurringEndTime(LocalTime.of(14, 0));

        calendar.addEntry(recurring);
    }

    static void createDayEntry(FullCalendar calendar, String title, LocalDate start, int days, String color) {
        Entry entry = new Entry();
        setValues(calendar, entry, title, start.atStartOfDay(), days, ChronoUnit.DAYS, color);

        calendar.addEntry(entry);
    }

    static void createTimedEntry(FullCalendar calendar, String title, LocalDateTime start, int minutes, String color) {
        Entry entry = new Entry();
        setValues(calendar, entry, title, start, minutes, ChronoUnit.MINUTES, color);

        calendar.addEntry(entry);
    }

    static void createDayBackgroundEntry(FullCalendar calendar, LocalDate start, int days, String color) {
        Entry entry = new Entry();
        setValues(calendar, entry, "BG", start.atStartOfDay(), days, ChronoUnit.DAYS, color);

        entry.setRenderingMode(Entry.RenderingMode.BACKGROUND);

        calendar.addEntry(entry);
    }

    static void createTimedBackgroundEntry(FullCalendar calendar, LocalDateTime start, int minutes, String color) {
        Entry entry = new Entry();
        setValues(calendar, entry, "BG", start, minutes, ChronoUnit.MINUTES, color);
        entry.setRenderingMode(Entry.RenderingMode.BACKGROUND);

        calendar.addEntry(entry);
    }

    static void setValues(FullCalendar calendar, Entry entry, String title, LocalDateTime start, int amountToAdd, ChronoUnit unit, String color) {
        entry.setTitle(title);
        entry.setStart(start, calendar.getTimezone());
        entry.setEnd(entry.getStartUTC().plus(amountToAdd, unit));
        entry.setAllDay(unit == ChronoUnit.DAYS);
        entry.setColor(color);
    }

    static void setValues(FullCalendar calendar, Entry entry, String title, LocalDateTime start, int amountToAdd, ChronoUnit unit, String color, HashMap<String, Object> extendedProps) {
        entry.setTitle(title);
        entry.setStart(start, calendar.getTimezone());
        entry.setEnd(entry.getStartUTC().plus(amountToAdd, unit));
        entry.setAllDay(unit == ChronoUnit.DAYS);
        entry.setColor(color);
        entry.setExtendedProps(extendedProps);
    }
}

