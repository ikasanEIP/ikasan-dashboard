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
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SessionAttributeConstants;
import org.ikasan.scheduled.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.model.UpcomingScheduledProcess;
import org.ikasan.scheduled.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;
import org.ikasan.spec.solr.BatchInsertEvent;
import org.ikasan.spec.solr.BatchInsertListener;
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

    private UI ui;

    public SchedulerCalendar(ScheduledProcessManagementService scheduledProcessManagementService,
                             ModuleMetaDataService moduleMetaDataService) {
        this.scheduledProcessManagementService = scheduledProcessManagementService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.dateTimeFormatter = new DateFormatter();
        this.init();
    }

    public void init() {
        Header testHeader = new Header();
        HeaderFooterPart headerCenter = testHeader.getCenter();
        headerCenter.addItem(HeaderFooterItem.TITLE);
        calendar = new FullCalendarWithTooltip(30);
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
            System.out.println("dates rendered: " + event.getStart() + " " + event.getEnd());
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

        comboBoxView = new ComboBox<>("", List.of(CalendarViewImpl.DAY_GRID_DAY,
            CalendarViewImpl.DAY_GRID_WEEK, CalendarViewImpl.LIST_DAY,
            CalendarViewImpl.LIST_WEEK));
        comboBoxView.setRenderer(new ComponentRenderer<>(item -> {
            Div text = new Div();
            text.setText(item.getName());

            if(item.getName().equals(CalendarViewImpl.DAY_GRID_DAY.getName())){
                text.setText("Grid Day");
            }
            else if(item.getName().equals(CalendarViewImpl.DAY_GRID_WEEK.getName())){
                text.setText("Grid Week");
            }
            else if(item.getName().equals(CalendarViewImpl.LIST_DAY.getName())){
                text.setText("List Day");
            }
            else if(item.getName().equals(CalendarViewImpl.LIST_WEEK.getName())){
                text.setText("List Week");
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
            else if(item.getName().equals(CalendarViewImpl.LIST_DAY.getName())){
                return "List Day";
            }
            else if(item.getName().equals(CalendarViewImpl.LIST_WEEK.getName())){
                return "List Week";
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


    private Entry createTimedEntry(String title, String description, LocalDateTime start, int minutes, String color, HashMap<String, Object> extendedProps) {
        Entry entry = new Entry();
        setValues(entry, title, description, start, minutes, ChronoUnit.MINUTES, color, extendedProps);

        return entry;
    }


    private void setValues(Entry entry, String title, String description, LocalDateTime start, int amountToAdd, ChronoUnit unit, String color, HashMap<String, Object> extendedProps) {
        entry.setTitle(title);
        entry.setDescription(description);
        entry.setStart(start);
        entry.setEnd(entry.getStartUTC().plus(amountToAdd, unit));
        entry.setAllDay(unit == ChronoUnit.DAYS);
        entry.setColor(color);
        entry.setExtendedProps(extendedProps);
    }

    public void renderCalendar() {
        this.calendar.render();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
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

    private void updateCalendarContents(LocalDate firstDay, LocalDate lastDate, String filter) {
        Instant startDateInstant = firstDay.atStartOfDay(ZoneId.of((String)UI.getCurrent().getSession()
            .getAttribute(SessionAttributeConstants.TIMEZONE_ID))).toInstant();
        long firstDayStartMillis = startDateInstant.toEpochMilli();

        Instant endDateInstant = lastDate.atStartOfDay(ZoneId.of((String)UI.getCurrent().getSession()
            .getAttribute(SessionAttributeConstants.TIMEZONE_ID))).toInstant();
        long lastDayStartMillis = endDateInstant.toEpochMilli();

        ScheduledProcessEventSearchResults<UpcomingScheduledProcess> upComingScheduledProcesses
            = scheduledProcessManagementService.getUpComingScheduledProcesses(firstDayStartMillis, lastDayStartMillis, filter);

        ArrayList<Entry> entries = new ArrayList<>();

        upComingScheduledProcesses.getResultList().forEach(upcomingScheduledProcess -> {
            LocalDateTime dateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(upcomingScheduledProcess.getFireTime()), ZoneId.of((String)UI.getCurrent().getSession()
                    .getAttribute(SessionAttributeConstants.TIMEZONE_ID)));
            HashMap<String, Object> extendedProps = new HashMap<>();
            extendedProps.put("event", upcomingScheduledProcess);
            entries.add(createTimedEntry(String.format("Job[%s]", upcomingScheduledProcess.getJobName()), String.format("<b>Agent</b> - %s<br/><b>Job Group<b/> - %s<br/><b>Job Name<b/> - %s<br/><b>Description<b/> - %s<br/><b>Execution Time<b/> - %s %s %s", upcomingScheduledProcess.getAgentName()
                , upcomingScheduledProcess.getJobGroup(), upcomingScheduledProcess.getJobName(), upcomingScheduledProcess.getJobDescription(), this.dateTimeFormatter.getFormattedDate(upcomingScheduledProcess.getFireTime()), dateTime, upcomingScheduledProcess.toString().substring(upcomingScheduledProcess.toString().lastIndexOf("."))), dateTime, 1, this.intToARGB((upcomingScheduledProcess.getAgentName()+
                upcomingScheduledProcess.getJobName()+upcomingScheduledProcess.getJobGroup()).hashCode()), extendedProps));
        });

        ScheduledProcessEventSearchResults<ScheduledProcessEvent>  scheduledProcessEventSearchResults
            = this.scheduledProcessManagementService.getScheduledProcessEvents(firstDayStartMillis, System.currentTimeMillis()
            , filter, false, 0, 100);

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

    private String intToARGB(int i){
        return "#default";
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
