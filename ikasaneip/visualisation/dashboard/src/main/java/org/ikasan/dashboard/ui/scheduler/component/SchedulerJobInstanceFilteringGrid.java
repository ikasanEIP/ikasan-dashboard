package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class SchedulerJobInstanceFilteringGrid extends Grid<SchedulerJobInstanceRecord> {
    private Logger logger = LoggerFactory.getLogger(SchedulerJobInstanceFilteringGrid.class);

    private SchedulerJobInstanceService schedulerJobInstanceService;

    private DataProvider<SchedulerJobInstanceRecord, SchedulerJobInstanceSearchFilter> dataProvider;
    private ConfigurableFilterDataProvider<SchedulerJobInstanceRecord, Void, SchedulerJobInstanceSearchFilter> filteredDataProvider;

    private SchedulerJobInstanceSearchFilter searchFilter;

    private long resultSize = 0;

    private String contextNameInstanceId;

    /**
     * Constructor
     *
     * @param schedulerJobInstanceService
     * @param searchFilter
     */
    public SchedulerJobInstanceFilteringGrid(SchedulerJobInstanceService schedulerJobInstanceService,
                                             SchedulerJobInstanceSearchFilter searchFilter) {
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if(this.schedulerJobInstanceService ==  null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.searchFilter = searchFilter;
        if(this.searchFilter ==  null) {
            throw new IllegalArgumentException("searchFilter cannot be null!");
        }
    }

    /**
     * Add filtering to a column.
     *
     * @param hr
     * @param setFilter
     * @param columnKey
     */
    public void addGridFiltering(HeaderRow hr, Consumer<String> setFilter, String columnKey) {
        TextField textField = new TextField();
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setWidthFull();

        textField.addValueChangeListener(ev -> {

            setFilter.accept("*"+ev.getValue()+"*");

            filteredDataProvider.refreshAll();
        });

        hr.getCell(getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * Add filtering to a column.
     *
     * @param hr
     * @param setStartTime
     * @param columnKey
     */
    public void addDateTimeGridFiltering(HeaderRow hr, Consumer<Long> setStartTime, Consumer<Long> setEndTime, String columnKey) {
        DatePicker datePicker = new DatePicker(getTranslation("label.date", UI.getCurrent().getLocale()));
        datePicker.setLocale(Locale.UK);
        datePicker.getElement().getThemeList().add("always-float-label");

        TimePicker startTimePicker = new TimePicker(getTranslation("label.start-time", UI.getCurrent().getLocale()));
        startTimePicker.setStep(Duration.ofMinutes(15));
        startTimePicker.setLocale(Locale.UK);
        startTimePicker.getElement().getThemeList().add("always-float-label");

        TimePicker endTimePicker = new TimePicker(getTranslation("label.end-time", UI.getCurrent().getLocale()));
        endTimePicker.setStep(Duration.ofMinutes(15));
        endTimePicker.setLocale(Locale.UK);
        endTimePicker.getElement().getThemeList().add("always-float-label");

        Label timeLabel = new Label();
        Icon clearFilter = IconDecorator.decorate(VaadinIcon.CLOSE_SMALL.create(), getTranslation("tooltip.clear-filter", UI.getCurrent().getLocale()), "14px", "");
        clearFilter.setSize("14px");
        clearFilter.setVisible(false);

        Dialog dateTimeDialog = new Dialog();
        dateTimeDialog.setWidth("660px");

        datePicker.addValueChangeListener(event -> {
            if(datePicker.getValue() != null && startTimePicker.getValue() != null
                && endTimePicker.getValue() != null) {
                long startOfDayMilli = datePicker.getValue().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

                long startMilli = startTimePicker.getValue().toSecondOfDay() * 1000;
                long endMilli = endTimePicker.getValue().toSecondOfDay() * 1000;

                setStartTime.accept(startOfDayMilli + startMilli);
                setEndTime.accept(startOfDayMilli + endMilli);
                dateTimeDialog.close();
            }
            else {
                setStartTime.accept(-1L);
                setEndTime.accept(-1L);
            }

            if(datePicker.getValue() != null && startTimePicker.getValue() != null
                && endTimePicker.getValue() != null) {
                timeLabel.setText(datePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " "
                    + startTimePicker.getValue() + " " + getTranslation("label.to-lower-case", UI.getCurrent().getLocale())
                    + " " + endTimePicker.getValue());
                clearFilter.setVisible(true);
            }

            filteredDataProvider.refreshAll();
        });

        startTimePicker.addValueChangeListener(event->{
            if(datePicker.getValue() != null && startTimePicker.getValue() != null
                && endTimePicker.getValue() != null) {
                long startOfDayMilli = datePicker.getValue().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

                long startMilli = startTimePicker.getValue().toSecondOfDay() * 1000;
                long endMilli = endTimePicker.getValue().toSecondOfDay() * 1000;

                setStartTime.accept(startOfDayMilli + startMilli);
                setEndTime.accept(startOfDayMilli + endMilli);
                dateTimeDialog.close();
            }
            else {
                setStartTime.accept(-1L);
                setEndTime.accept(-1L);
            }

            if(datePicker.getValue() != null && startTimePicker.getValue() != null
                && endTimePicker.getValue() != null) {
                timeLabel.setText(datePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " "
                    + startTimePicker.getValue() + " " + getTranslation("label.to-lower-case", UI.getCurrent().getLocale())
                    + " " + endTimePicker.getValue());
                clearFilter.setVisible(true);
            }

            filteredDataProvider.refreshAll();
        });

        endTimePicker.addValueChangeListener(event->{
            if(datePicker.getValue() != null && startTimePicker.getValue() != null
                && endTimePicker.getValue() != null) {
                long startOfDayMilli = datePicker.getValue().atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

                long startMilli = startTimePicker.getValue().toSecondOfDay() * 1000;
                long endMilli = endTimePicker.getValue().toSecondOfDay() * 1000;

                setStartTime.accept(startOfDayMilli + startMilli);
                setEndTime.accept(startOfDayMilli + endMilli);
                dateTimeDialog.close();
            }
            else {
                setStartTime.accept(-1L);
                setEndTime.accept(-1L);
            }

            if(datePicker.getValue() != null && startTimePicker.getValue() != null
                && endTimePicker.getValue() != null) {
                timeLabel.setText(datePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " "
                    + startTimePicker.getValue() + " " + getTranslation("label.to-lower-case", UI.getCurrent().getLocale())
                    + " " + endTimePicker.getValue());
                clearFilter.setVisible(true);
            }

            filteredDataProvider.refreshAll();
        });

        HorizontalLayout layout = new HorizontalLayout(datePicker, startTimePicker, endTimePicker);
        layout.setMargin(true);
        layout.setWidthFull();

        dateTimeDialog.add(layout);

        Icon icon = IconDecorator.decorate(VaadinIcon.CALENDAR_CLOCK.create(), getTranslation("tooltip.add-time-filter", UI.getCurrent().getLocale()), "16pt", "");
        icon.addClickListener(event -> {
            dateTimeDialog.open();
        });

        dateTimeDialog.addOpenedChangeListener(event -> {
           if(!event.isOpened()) {
               if(datePicker.getValue() != null && startTimePicker.getValue() != null
                    && endTimePicker.getValue() != null) {
                   timeLabel.setText(datePicker.getValue().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + " "
                       + startTimePicker.getValue() + " " + getTranslation("label.to-lower-case", UI.getCurrent().getLocale())
                       + " " + endTimePicker.getValue());
                   clearFilter.setVisible(true);
               }
           }
        });

        clearFilter.addClickListener(event -> {
            clearFilter.setVisible(false);
            datePicker.setValue(null);
            startTimePicker.setValue(null);
            endTimePicker.setValue(null);
            timeLabel.setText("");
        });

        HorizontalLayout filterLayout = new HorizontalLayout(icon, timeLabel, clearFilter);
        filterLayout.setWidth("300px");
        filterLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, clearFilter);

        hr.getCell(getColumnByKey(columnKey)).setComponent(filterLayout);
    }

    /**
     * Add filtering to a column.
     *
     * @param hr
     * @param setFilter
     * @param columnKey
     */
    public void addSelectGridFiltering(HeaderRow hr, Consumer<String> setFilter, Set<Map.Entry<String, String>> options, String columnKey) {
        Select<Map.Entry<String, String>> select = new Select<>();
        select.setItems(options);
        select.setWidthFull();
        select.setEmptySelectionAllowed(true);
        select.setItemLabelGenerator(entry -> {
            if(entry == null) {
                return "";
            }

            return entry.getKey();
        });

        select.addValueChangeListener(ev-> {

            if(ev.getValue() == null) {
                setFilter.accept(null);
            }
            else {
                setFilter.accept(ev.getValue().getValue());
            }

            filteredDataProvider.refreshAll();
        });

        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");

        HorizontalLayout layout = new HorizontalLayout(select, filterIcon);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, filterIcon);

        hr.getCell(getColumnByKey(columnKey)).setComponent(layout);
    }

    /**
     * Add filtering to a column.
     *
     * @param hr
     * @param setFilter
     * @param columnKey
     */
    public void addSelectGridFiltering(HeaderRow hr, Consumer<String> setFilter, List<String> options, String columnKey) {
        Select<String> select = new Select<>();
        select.setItems(options);
        select.setWidthFull();
        select.setEmptySelectionAllowed(true);
        select.setItemLabelGenerator(entry -> {
            if(entry == null) {
                return "";
            }

            return entry;
        });

        select.addValueChangeListener(ev-> {

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });

        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");

        HorizontalLayout layout = new HorizontalLayout(select, filterIcon);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, filterIcon);

        hr.getCell(getColumnByKey(columnKey)).setComponent(layout);
    }

    /**
     * Add general filter
     *
     * @param textField
     * @param setFilter
     */
    public void addGridFiltering(TextField textField, Consumer<String> setFilter) {
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setSuffixComponent(filterIcon);
        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });
    }

    /**
     * Add filtering to a column.
     *
     * @param hr
     * @param setFilter
     * @param columnKey
     */
    public void addCheckboxGridFiltering(HeaderRow hr, Consumer<Boolean> setFilter, String columnKey) {
        Checkbox checkbox = new Checkbox();
        checkbox.setWidthFull();
        checkbox.getElement().getStyle().set("margin-bottom", "0px");
        checkbox.addValueChangeListener(ev-> {
            if(ev.getValue()) {
                setFilter.accept(ev.getValue());
            }
            else {
                setFilter.accept(null);
            }
            filteredDataProvider.refreshAll();
        });

        HorizontalLayout layout = new HorizontalLayout(checkbox);
        layout.setMargin(false);
        layout.setWidth("100px");
        layout.getElement().getStyle().set("margin-bottom", "0px");

        hr.getCell(getColumnByKey(columnKey)).setComponent(layout);
    }

    /**
     * Initialise the grid.
     */
    public void init() {
        dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<SchedulerJobInstanceSearchFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            SearchResults results;

            if(query.getSortOrders().size() > 0) {
                results = this.getResults(filter.get(), offset, limit, query.getSortOrders().get(0).getSorted(),
                    query.getSortOrders().get(0).getDirection().name());
            }
            else {
                results = this.getResults(filter.get(), offset, limit, null, null);
            }

            return results.getResultList().stream();
        }, query -> {
            Optional<SchedulerJobInstanceSearchFilter> filter = query.getFilter();

            SearchResults results;

            results = this.getResults(filter.get(), 0, 0, null, null);

            this.resultSize = results.getTotalNumberOfResults();

            return (int) this.resultSize;
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.setDataProvider(filteredDataProvider);
    }

    private SearchResults getResults(SchedulerJobInstanceSearchFilter filter, int offset, int limit, String sortColumn, String sortDirection) {
        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        SearchResults results;

        try {
            results = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, limit, offset, sortColumn, sortDirection);
        }
        catch (Exception e) {
            logger.error("An error has occurred querying solr!", e);
            final UI current = UI.getCurrent();
            final I18NProvider i18NProvider = VaadinService.getCurrent().getInstantiator().getI18NProvider();
            NotificationHelper.showErrorNotification(i18NProvider.getTranslation("error.solr-unavailable"
                , current.getLocale()));

            results = new SearchResultsImpl(new ArrayList<>(), 0, 0);
        }

        return results;
    }

    public long getResultSize()
    {
        return resultSize;
    }

    public void setContextInstanceId(String contextInstanceId) {
        this.contextNameInstanceId = contextInstanceId;
        this.searchFilter.setContextInstanceId(contextNameInstanceId);
    }

    public void refreshItem(SchedulerJobInstanceRecord schedulerJobRecord) {
        this.dataProvider.refreshItem(schedulerJobRecord);
        this.filteredDataProvider.refreshItem(schedulerJobRecord);
    }

    public void refreshAll() {
        this.dataProvider.refreshAll();
        this.filteredDataProvider.refreshAll();
    }
}
