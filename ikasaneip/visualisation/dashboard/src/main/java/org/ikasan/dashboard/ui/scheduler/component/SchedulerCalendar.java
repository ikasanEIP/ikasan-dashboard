package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.scheduler.model.CalendarConfiguration;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SessionAttributeConstants;
import org.ikasan.scheduled.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.model.UpcomingScheduledProcess;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;
import org.ikasan.spec.solr.BatchInsertEvent;
import org.ikasan.spec.solr.BatchInsertListener;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.stefan.fullcalendar.*;
import org.vaadin.stefan.fullcalendar.model.Header;
import org.vaadin.stefan.fullcalendar.model.HeaderFooterItem;
import org.vaadin.stefan.fullcalendar.model.HeaderFooterPart;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SchedulerCalendar extends VerticalLayout implements BeforeEnterObserver, BatchInsertListener<ScheduledProcessEvent> {

    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ModuleMetaDataService moduleMetaDataService;

    private FullCalendar calendar;
    private ComboBox<CalendarView> comboBoxView;
    private DateFormatter dateTimeFormatter;
    private TextField filterField = new TextField("");

    private LocalDate firstDay = null;
    private LocalDate lastDate = null;

    private CalendarConfiguration calendarConfiguration;

    private UI ui;
    private IkasanAuthentication authentication;


    /**
     * Constructor
     *
     * @param scheduledProcessManagementService
     * @param moduleMetaDataService
     * @param calendarConfiguration
     */
    public SchedulerCalendar(ScheduledProcessManagementService scheduledProcessManagementService,
                             ModuleMetaDataService moduleMetaDataService, CalendarConfiguration calendarConfiguration) {
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.calendarConfiguration = calendarConfiguration;
        this.dateTimeFormatter = new DateFormatter();
        this.init();
    }

    /**
     * Initialise the internals of the class.
     */
    public void init() {
        Header testHeader = new Header();
        HeaderFooterPart headerCenter = testHeader.getCenter();
        headerCenter.addItem(HeaderFooterItem.TITLE);
        calendar = new FullCalendarWithTooltip(this.calendarConfiguration.getNumberOfEventsPerDay());
        calendar.setWeekNumbersVisible(false);

        calendar.setHeaderToolbar(testHeader);

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

        calendar.addDatesRenderedListener(event -> {
            this.firstDay = event.getStart();
            this.lastDate = event.getEnd();
            this.calendar.removeAllEntries();
            this.updateCalendarContents(event.getStart(), event.getEnd()
                , filterField.getValue() != null ? filterField.getValue():null);
        });

        this.add(this.createBasicToolbar(), calendar);
        this.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
        this.getStyle().set("flex-grow", "1");

        this.setFlexGrow(1, calendar);

        this.calendar.setHeightAuto();
        this.ui = UI.getCurrent();
    }

    /**
     * Create the controls toolbar.
     *
     * @return
     */
    private HorizontalLayout createBasicToolbar() {
        Button buttonToday = new Button("Today", VaadinIcon.HOME.create(), e -> {
            this.calendar.removeAllEntries();
            calendar.today();
        });
        Button buttonPrevious = new Button("Previous", VaadinIcon.ANGLE_LEFT.create(),  e -> {
            this.calendar.removeAllEntries();
            calendar.previous();
        });
        Button buttonNext = new Button("Next", VaadinIcon.ANGLE_RIGHT.create(), e -> {
            this.calendar.removeAllEntries();
            calendar.next();
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

        comboBoxView = new ComboBox<>("", getCalendarViews());
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

        comboBoxView.setValue(CalendarViewImpl.DAY_GRID_WEEK);
        calendar.changeView(CalendarViewImpl.DAY_GRID_WEEK);
        comboBoxView.addValueChangeListener(e -> {
            CalendarView value = e.getValue();
            calendar.removeAllEntries();
            calendar.changeView(value == null ? CalendarViewImpl.DAY_GRID_WEEK : value);
        });

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        filterField.setSuffixComponent(icon);
        filterField.setWidth("300px");
        filterField.getElement().getStyle().set("margin-left", "auto");

        filterField.addValueChangeListener((HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TextField, String>>)
            textFieldStringComponentValueChangeEvent -> {
                this.calendar.removeAllEntries();
                this.updateCalendarContents(this.firstDay, this.lastDate, filterField.getValue());
            });

        HorizontalLayout layout = new HorizontalLayout(buttonToday, buttonPrevious, buttonNext, buttonDatePicker
            , gotoDate, comboBoxView, filterField);
        layout.setWidthFull();

        layout.setVerticalComponentAlignment(Alignment.END, filterField);
        return layout;
    }


    /**
     * Create a timed entry to add to the calendar.
     *
     * @param title
     * @param description
     * @param start
     * @param minutes
     * @param color
     * @param extendedProps
     * @return
     */
    private Entry createTimedEntry(String title, String description, LocalDateTime start, int minutes
        , String color, HashMap<String, Object> extendedProps) {
        Entry entry = new Entry();

        entry.setTitle(title);
        entry.setDescription(description);
        entry.setStart(start);
        entry.setEnd(entry.getStartUTC().plus(minutes, ChronoUnit.MINUTES));
        entry.setAllDay(ChronoUnit.MINUTES == ChronoUnit.DAYS);
        if(color != null) entry.setColor(color);
        entry.setExtendedProps(extendedProps);

        return entry;
    }

    /**
     * Force calendar to re-render.
     */
    public void renderCalendar() {
        this.calendar.render();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.calendar.changeView(this.comboBoxView.getValue() == null ? CalendarViewImpl.DAY_GRID_WEEK : this.comboBoxView.getValue());
        this.calendar.setHeightAuto();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.scheduledProcessManagementService.addBatchInsertListener(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.scheduledProcessManagementService.removeBatchInsertListener(this);
    }

    /**
     * This method assists with pagination within the calendar so that only
     * data of interest is loaded.
     *
     * @param firstDay
     * @param lastDate
     * @param filter
     */
    private void updateCalendarContents(LocalDate firstDay, LocalDate lastDate, String filter) {
        Instant startDateInstant = firstDay.atStartOfDay(ZoneId.of((String)UI.getCurrent().getSession()
            .getAttribute(SessionAttributeConstants.TIMEZONE_ID))).toInstant();
        long firstDayStartMillis = startDateInstant.toEpochMilli();

        Instant endDateInstant = lastDate.atStartOfDay(ZoneId.of((String)UI.getCurrent().getSession()
            .getAttribute(SessionAttributeConstants.TIMEZONE_ID))).toInstant();
        long lastDayStartMillis = endDateInstant.toEpochMilli();


        ScheduledProcessEventSearchResults<UpcomingScheduledProcess> upComingScheduledProcesses = null;
        if(authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY) || this.authentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ADMIN)) {
            upComingScheduledProcesses =  this.scheduledProcessManagementService.getUpComingScheduledProcesses(null, firstDayStartMillis
                , lastDayStartMillis, filter);
        }
        else {
            upComingScheduledProcesses =  this.scheduledProcessManagementService.getUpComingScheduledProcesses(new ArrayList<>(SecurityUtils.getAccessibleModules(authentication)), firstDayStartMillis
                , lastDayStartMillis, filter);
        }

        ArrayList<Entry> entries = new ArrayList<>();

        upComingScheduledProcesses.getResultList().forEach(upcomingScheduledProcess -> {
            LocalDateTime dateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(upcomingScheduledProcess.getFireTime()), ZoneId.of((String)UI.getCurrent().getSession()
                    .getAttribute(SessionAttributeConstants.TIMEZONE_ID)));
            HashMap<String, Object> extendedProps = new HashMap<>();
            extendedProps.put("event", upcomingScheduledProcess);
            entries.add(createTimedEntry(String.format("Job[%s]", upcomingScheduledProcess.getJobName()), String.format("<b>Agent</b> - %s<br/><b>Job Group<b/> - %s<br/><b>Job Name<b/> - %s<br/><b>Description<b/> - %s<br/><b>Execution Time<b/> - %s %s %s", upcomingScheduledProcess.getAgentName()
                , upcomingScheduledProcess.getJobGroup(), upcomingScheduledProcess.getJobName(), upcomingScheduledProcess.getJobDescription(), this.dateTimeFormatter.getFormattedDate(upcomingScheduledProcess.getFireTime()), dateTime, upcomingScheduledProcess.toString().substring(upcomingScheduledProcess.toString().lastIndexOf(".")))
                , dateTime, 1, null, extendedProps));
        });

        ScheduledProcessEventSearchResults<ScheduledProcessEvent>  scheduledProcessEventSearchResults;

        if(authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY) || this.authentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ADMIN)) {
            scheduledProcessEventSearchResults =  this.scheduledProcessManagementService.getScheduledProcessEvents(null,
                firstDayStartMillis, System.currentTimeMillis(), filter, false, 0, 1000, "desc");
        }
        else {
            scheduledProcessEventSearchResults =  this.scheduledProcessManagementService.getScheduledProcessEvents(new ArrayList<>(SecurityUtils.getAccessibleModules(authentication)),
                firstDayStartMillis, System.currentTimeMillis(), filter, false, 0, 1000, "desc");
        }

        scheduledProcessEventSearchResults.getResultList().forEach(scheduledProcessEvent -> {
            LocalDateTime dateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduledProcessEvent.getFireTime()), ZoneId.of((String)UI.getCurrent().getSession()
                    .getAttribute(SessionAttributeConstants.TIMEZONE_ID)));
            HashMap<String, Object> extendedProps = new HashMap<>();
            extendedProps.put("event", scheduledProcessEvent);
            entries.add(createTimedEntry(String.format("Job[%s]", scheduledProcessEvent.getJobName())
                , String.format("<b>Agent</b> - %s<br/><b>Job Group<b/> - %s<br/><b>Job Name<b/> - %s<br/><b>Description<b/> - %s<br/><b>Execution Time<b/> - %s", scheduledProcessEvent.getAgentName()
                    , scheduledProcessEvent.getJobGroup(), scheduledProcessEvent.getJobName(), scheduledProcessEvent.getJobDescription(), this.dateTimeFormatter.getFormattedDate(scheduledProcessEvent.getFireTime()))
                ,dateTime, 1, scheduledProcessEvent.isSuccessful() ? "#66bb6a":"#ef5350", extendedProps));

        });
        calendar.addEntries(entries);

    }

    /**
     * Get a list of possible calendar views.
     *
     * @return
     */
    private List<CalendarView> getCalendarViews() {
        ArrayList<CalendarView> calenderViews = new ArrayList<>();

        calenderViews.add(CalendarViewImpl.DAY_GRID_WEEK);

        if(this.calendarConfiguration.isShowDayGrid()) {
            calenderViews.add(CalendarViewImpl.DAY_GRID_DAY);
        }

        if(this.calendarConfiguration.isShowMonthGrid()) {
            calenderViews.add(CalendarViewImpl.DAY_GRID_MONTH);
        }

        if(this.calendarConfiguration.isShowDayList()) {
            calenderViews.add(CalendarViewImpl.LIST_DAY);
        }
        if(this.calendarConfiguration.isShowWeekList()) {
            calenderViews.add(CalendarViewImpl.LIST_WEEK);
        }

        if(this.calendarConfiguration.isShowMonthList()) {
            calenderViews.add(CalendarViewImpl.LIST_MONTH);
        }

        if(this.calendarConfiguration.isShowYearList()) {
            calenderViews.add(CalendarViewImpl.LIST_YEAR);
        }

        return calenderViews;
    }

    @Override
    public void onBatchInsert(BatchInsertEvent<ScheduledProcessEvent> batchInsertEvent) {
        ui.access(() ->{
            if(this.firstDay != null && this.lastDate != null && filterField.getValue() != null) {
                this.calendar.removeAllEntries();
                this.updateCalendarContents(this.firstDay, this.lastDate, filterField.getValue());
                this.calendar.render();
            }
        });
    }
}
