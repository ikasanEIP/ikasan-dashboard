package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SessionAttributeConstants;
import org.ikasan.scheduled.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.model.UpcomingScheduledProcess;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;
import org.vaadin.stefan.fullcalendar.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class SchedulerCalendar extends VerticalLayout implements BeforeEnterObserver {

    private static DateTimeFormatter MONTH_DATE_FORMATTER =  DateTimeFormatter.ofPattern("MMM YYYY");
    private static DateTimeFormatter YEAR_DATE_FORMATTER =  DateTimeFormatter.ofPattern("YYYY");
    private static DateTimeFormatter DAY_DATE_FORMATTER =  DateTimeFormatter.ofPattern("MMMM dd, YYYY");


    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ModuleMetaDataService moduleMetaDataService;

    private FullCalendar calendar;
    private LocalDate localDate = LocalDate.now();
    private Span dateString;
    private ComboBox<CalendarView> comboBoxView;
    private DateFormatter dateTimeFormatter;

    public SchedulerCalendar(ScheduledProcessManagementService scheduledProcessManagementService,
                             ModuleMetaDataService moduleMetaDataService) {
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.dateTimeFormatter = new DateFormatter();
        this.init();
    }

    public void init() {
        calendar = new FullCalendarWithTooltip(12);//FullCalendarBuilder.create().withEntryLimit(12).build();
        calendar.setWeekNumbersVisible(false);

        calendar.setFirstDay(DayOfWeek.MONDAY);
        calendar.setNowIndicatorShown(true);
        calendar.setNumberClickable(true);
        calendar.setTimeslotsSelectable(true);
        calendar.addEntryClickedListener((ComponentEventListener<EntryClickedEvent>) entryClickedEvent -> {
            Object event = entryClickedEvent.getEntry().getExtendedProps().get("event");

            if(event instanceof ScheduledProcessEvent) {
                ScheduledProcessExecutionDialog dialog = new ScheduledProcessExecutionDialog((ScheduledProcessEvent)event,
                    moduleMetaDataService.findById(((ScheduledProcessEvent)event).getAgentName()));
                dialog.open();
            }
            else if(event instanceof UpcomingScheduledProcess) {
                UpcomingJobExecutionDialog dialog = new UpcomingJobExecutionDialog((UpcomingScheduledProcess)event,
                    moduleMetaDataService.findById(((UpcomingScheduledProcess)event).getAgentName()));
                dialog.open();
            }
        });

//        calendar.setOption("rerenderDelay", 3);
//        calendar.setOption("eventRenderWait", 3);
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

//         this following code is an exapmle on how to create a server side dialog showing all entries of the day
//        calendar.setMoreLinkClickAction(FullCalendar.MoreLinkClickAction.);
//          calendar.addMoreLinkClickedListener(event -> {
//            Collection<Entry> entries = event.getEntries();
//            if (!entries.isEmpty()) {
//                Dialog dialog = new Dialog();
//                VerticalLayout dialogLayout = new VerticalLayout();
//                dialogLayout.setSpacing(false);
//                dialogLayout.setPadding(false);
//                dialogLayout.setMargin(false);
//                dialogLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
//
//                dialogLayout.add(new Span("Entries of " + event.getClickedDate()));
//                entries.stream()
//                        .sorted(Comparator.comparing(Entry::getTitle))
//                        .map(entry -> {
//                            NativeButton button = new NativeButton(entry.getTitle(), clickEvent -> {});
//                            Style style = button.getStyle();
//                            style.set("background-color", Optional.ofNullable(entry.getColor()).orElse("rgb(58, 135, 173)"));
//                            style.set("color", "white");
//                            style.set("border", "0 none black");
//                            style.set("border-radius", "3px");
//                            style.set("text-align", "left");
//                            style.set("margin", "1px");
//                            return button;
//                        }).forEach(dialogLayout::add);
//
//                dialog.add(dialogLayout);
//                dialog.open();
//            }
//        });

        this.add(this.createBasicToolbar(), calendar);
        this.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
        this.getStyle().set("flex-grow", "1");

        this.setFlexGrow(1, calendar);

        this.calendar.setHeightAuto();
//        this.calendar.setHeight(2000);

//        this.setSizeFull();
//        calendar.setSizeFull();
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
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, dateString);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, textField);
        return layout;
    }



    private void createRecurringEvents(FullCalendar calendar) {
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

    private void createDayEntry(FullCalendar calendar, String title, String description, LocalDate start, int days, String color) {
        Entry entry = new Entry();
        setValues(calendar, entry, title, description, start.atStartOfDay(), days, ChronoUnit.DAYS, color);

        calendar.addEntry(entry);
    }

    private Entry createTimedEntry(FullCalendar calendar, String title, String description, LocalDateTime start, int minutes, String color, HashMap<String, Object> extendedProps) {
        Entry entry = new Entry();
        setValues(calendar, entry, title, description, start, minutes, ChronoUnit.MINUTES, color, extendedProps);


        return entry;
    }



    private void setValues(FullCalendar calendar, Entry entry, String title, String description, LocalDateTime start, int amountToAdd, ChronoUnit unit, String color) {
        entry.setTitle(title);
        entry.setDescription(description);
        entry.setStart(start, calendar.getTimezone());
        entry.setEnd(entry.getStartUTC().plus(amountToAdd, unit));
        entry.setAllDay(unit == ChronoUnit.DAYS);
        entry.setColor(color);
    }

    private void setValues(FullCalendar calendar, Entry entry, String title, String description, LocalDateTime start, int amountToAdd, ChronoUnit unit, String color, HashMap<String, Object> extendedProps) {
        entry.setTitle(title);
        entry.setDescription(description);
        entry.setStart(start, calendar.getTimezone());
        entry.setEnd(entry.getStartUTC().plus(amountToAdd, unit));
        entry.setAllDay(unit == ChronoUnit.DAYS);
        entry.setColor(color);
        entry.setExtendedProps(extendedProps);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.calendar.removeAllEntries();
        LocalDate firstDay = this.localDate.withDayOfMonth(1);
        LocalDate lastDate = this.localDate.withDayOfMonth(this.localDate.lengthOfMonth());
        lastDate.plusDays(1);
        LocalDate oneMonthPrevious = this.localDate.minusMonths(1).withDayOfMonth(30);
        LocalDate yesterday = LocalDate.now().minusDays(1);


        Instant startDateInstant = firstDay.atStartOfDay(ZoneId.of((String)UI.getCurrent().getSession()
            .getAttribute(SessionAttributeConstants.TIMEZONE_ID))).toInstant();
        long firstDayStartMillis = startDateInstant.toEpochMilli();

        Instant endDateInstant = lastDate.atStartOfDay(ZoneId.of((String)UI.getCurrent().getSession()
            .getAttribute(SessionAttributeConstants.TIMEZONE_ID))).toInstant();
        long lastDayStartMillis = endDateInstant.toEpochMilli();

        Instant oneMonthPreviousInstant = oneMonthPrevious.atStartOfDay(ZoneId.of((String)UI.getCurrent().getSession()
            .getAttribute(SessionAttributeConstants.TIMEZONE_ID))).toInstant();
        long oneMonthPreviousMillis = oneMonthPreviousInstant.toEpochMilli();

        Instant yesterdayInstant = yesterday.atStartOfDay(ZoneId.of((String)UI.getCurrent().getSession()
            .getAttribute(SessionAttributeConstants.TIMEZONE_ID))).toInstant();
        long yesterdayMillis = yesterdayInstant.toEpochMilli();

        ScheduledProcessEventSearchResults<UpcomingScheduledProcess> upComingScheduledProcesses
            = scheduledProcessManagementService.getUpComingScheduledProcesses(firstDayStartMillis, lastDayStartMillis, null);

        ArrayList<Entry> entries = new ArrayList<>();

        upComingScheduledProcesses.getResultList().forEach(upcomingScheduledProcess -> {
            LocalDateTime dateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(upcomingScheduledProcess.getFireTime()), ZoneId.of((String)UI.getCurrent().getSession()
                    .getAttribute(SessionAttributeConstants.TIMEZONE_ID)));
            HashMap<String, Object> extendedProps = new HashMap<>();
            extendedProps.put("event", upcomingScheduledProcess);
            entries.add(createTimedEntry(calendar,  String.format("Job[%s]", upcomingScheduledProcess.getJobName()), String.format("<b>Agent</b> - %s<br/><b>Job Group<b/> - %s<br/><b>Job Name<b/> - %s<br/><b>Description<b/> - %s<br/><b>Execution Time<b/> - %s", upcomingScheduledProcess.getAgentName()
                , upcomingScheduledProcess.getJobGroup(), upcomingScheduledProcess.getJobName(), upcomingScheduledProcess.getJobDescription(), this.dateTimeFormatter.getFormattedDate(upcomingScheduledProcess.getFireTime())),dateTime, 0, this.intToARGB((upcomingScheduledProcess.getAgentName()+
                upcomingScheduledProcess.getJobName()+upcomingScheduledProcess.getJobGroup()).hashCode()), extendedProps));
        });

        ScheduledProcessEventSearchResults<ScheduledProcessEvent>  scheduledProcessEventSearchResults
            = this.scheduledProcessManagementService.getScheduledProcessEvents(oneMonthPreviousMillis, yesterdayMillis
            , null, false, 0, 100);

        scheduledProcessEventSearchResults.getResultList().forEach(scheduledProcessEvent -> {
            LocalDateTime dateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduledProcessEvent.getFireTime()), ZoneId.of((String)UI.getCurrent().getSession()
                    .getAttribute(SessionAttributeConstants.TIMEZONE_ID)));
            HashMap<String, Object> extendedProps = new HashMap<>();
            extendedProps.put("event", scheduledProcessEvent);
            entries.add(createTimedEntry(calendar, String.format("Job[%s]", scheduledProcessEvent.getJobName())
                , String.format("<b>Agent</b> - %s<br/><b>Job Group<b/> - %s<br/><b>Job Name<b/> - %s<br/><b>Description<b/> - %s<br/><b>Execution Time<b/> - %s", scheduledProcessEvent.getAgentName()
                    , scheduledProcessEvent.getJobGroup(), scheduledProcessEvent.getJobName(), scheduledProcessEvent.getJobDescription(), this.dateTimeFormatter.getFormattedDate(scheduledProcessEvent.getFireTime()))
                ,dateTime, 0, scheduledProcessEvent.isSuccessful() ? "green":"red", extendedProps));

        });

        calendar.addEntries(entries);
        this.calendar.setHeightAuto();
    }

    private String intToARGB(int i){
        return "#"+
            Integer.toHexString(((i>>24)&0xFF))+
            Integer.toHexString((i&0xFF))+
            Integer.toHexString(((i>>16)&0xFF))+
            Integer.toHexString(((i>>8)&0xFF));
    }
}
