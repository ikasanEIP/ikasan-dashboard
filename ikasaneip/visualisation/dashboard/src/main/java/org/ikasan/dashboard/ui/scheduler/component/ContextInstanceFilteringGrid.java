package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ContextInstanceFilteringGrid extends Grid<ScheduledContextInstanceRecord> {
    private Logger logger = LoggerFactory.getLogger(ContextInstanceFilteringGrid.class);

    private ScheduledContextInstanceService scheduledContextInstanceService;

    private DataProvider<ScheduledContextInstanceRecord, ContextInstanceSearchFilter> dataProvider;
    private ConfigurableFilterDataProvider<ScheduledContextInstanceRecord, Void, ContextInstanceSearchFilter> filteredDataProvider;

    private ContextInstanceSearchFilter searchFilter;

    private long resultSize = 0;

    private String contextName;

    /**
     * Constructor
     *
     * @param scheduledContextInstanceService
     * @param searchFilter
     */
    public ContextInstanceFilteringGrid(ScheduledContextInstanceService scheduledContextInstanceService,
                                        ContextInstanceSearchFilter searchFilter) {
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if(this.scheduledContextInstanceService ==  null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
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
        textField.setWidthFull();

        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);

        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });

        hr.getCell(getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * Add filtering to a column.
     *
     * @param hr
     * @param setFilter
     * @param columnKey
     */
    public void addDateGridFiltering(HeaderRow hr, Consumer<Long> setFilter, String columnKey) {
        DatePicker datePicker = new DatePicker();
        datePicker.setWidthFull();

        datePicker.addValueChangeListener(ev->{
            if(ev.getValue() == null) {
                setFilter.accept(-1L);
            }
            else {
                setFilter.accept(ev.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
            }

            filteredDataProvider.refreshAll();
        });

        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");

        HorizontalLayout layout = new HorizontalLayout(datePicker, filterIcon);
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
        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });
    }

    /**
     * Initialise the grid.
     */
    public void init() {
        dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<ContextInstanceSearchFilter> filter = query.getFilter();

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
            Optional<ContextInstanceSearchFilter> filter = query.getFilter();

            SearchResults results;

            results = this.getResults(filter.get(), 0, 0, null, null);

            this.resultSize = results.getTotalNumberOfResults();

            return (int) this.resultSize;
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.setDataProvider(filteredDataProvider);
    }

    private SearchResults getResults(ContextInstanceSearchFilter filter, int offset, int limit, String sortField, String sortDirection) {
        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        SearchResults results = null;

        try {
            results = this.scheduledContextInstanceService.getScheduledContextInstancesByFilter(filter, limit, offset, sortField, sortDirection);
        }
        catch (Exception e) {
            e.printStackTrace();
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

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }
}
