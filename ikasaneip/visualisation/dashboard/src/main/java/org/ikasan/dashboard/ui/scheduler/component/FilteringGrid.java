package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import org.ikasan.spec.solr.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;

public abstract class FilteringGrid<DATA, FILTER, RESULTS extends SearchResults> extends Grid<DATA>
{
    private Logger logger = LoggerFactory.getLogger(FilteringGrid.class);

    protected DataProvider<DATA, FILTER> dataProvider;
    protected ConfigurableFilterDataProvider<DATA,Void, FILTER> filteredDataProvider;

    protected FILTER searchFilter;
    protected UI ui;

    private long resultSize = 0;

    /**
     * Constructors
     */
    public FilteringGrid(FILTER searchFilter)
    {
        this.searchFilter = searchFilter;
        if(this.searchFilter ==  null)
        {
            throw new IllegalArgumentException("SearchFilter cannot be null!");
        }

        this.ui = UI.getCurrent();
    }

    /**
     * Add filtering to a column.
     *
     * @param hr
     * @param setFilter
     * @param columnKey
     */
    public void addGridFiltering(HeaderRow hr, Consumer<String> setFilter, String columnKey)
    {
        TextField textField = new TextField();
        textField.setWidthFull();

        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });

        hr.getCell(getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * Add general filtering.
     *
     * @param textField
     * @param setFilter
     */
    public void addGridFiltering(TextField textField, Consumer<String> setFilter)
    {
        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });
    }

    /**
     * Add date and time based grid filtering.
     *
     * @param date
     * @param startTime
     * @param endTime
     * @param startTimeFilter
     * @param endTimeFilter
     */
    public abstract void addGridFiltering(DatePicker date, TimePicker startTime, TimePicker endTime, Consumer<Long> startTimeFilter, Consumer<Long> endTimeFilter);

    public void init()
    {
        dataProvider = DataProvider.fromFilteringCallbacks(query ->
        {
            Optional<FILTER> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            RESULTS results;

            String sortField = null;
            String sortOrder = null;

            if(query.getSortOrders() != null && !query.getSortOrders().isEmpty()) {
                sortField = query.getSortOrders().get(0).getSorted();
                sortOrder = query.getSortOrders().get(0).getDirection().name();
            }

            if(filter.isPresent())
            {
                results = this.getResults(filter.get(), offset, limit, sortField, sortOrder);
            }
            else
            {
                results = this.getResults(null, offset, limit, sortField, sortOrder);
            }

            return results.getResultList().stream();
        }, query ->
        {
            Optional<FILTER> filter = query.getFilter();

            RESULTS results;

            if(filter.isPresent())
            {
                results = this.getResults(filter.get(), 0, 0, null, null);
            }
            else
            {
                results = this.getResults(null, 0, 0, null, null);
            }

            this.resultSize = results.getTotalNumberOfResults();


            return (int) results.getTotalNumberOfResults();
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.setDataProvider(filteredDataProvider);
    }

    /**
     * Refresh the data presented to the grid.
     */
    public void refresh() {
        ui.access(() -> {
            this.dataProvider.refreshAll();
        });
    }

    protected abstract RESULTS getResults(FILTER filter, int offset, int limit, String sortField, String sortOrder);

    public long getResultSize()
    {
        return resultSize;
    }
}
