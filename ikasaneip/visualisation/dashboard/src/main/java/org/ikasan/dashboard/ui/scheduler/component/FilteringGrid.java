package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import org.ikasan.dashboard.ui.scheduler.model.SearchResults;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.function.Consumer;

public abstract class FilteringGrid<DATA, FILTER, RESULTS extends SearchResults> extends Grid<DATA>
{
    private Logger logger = LoggerFactory.getLogger(FilteringGrid.class);

    private DataProvider<DATA, FILTER> dataProvider;
    private ConfigurableFilterDataProvider<DATA,Void, FILTER> filteredDataProvider;

    private FILTER searchFilter;

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

    public void addGridFiltering(TextField textField, Consumer<String> setFilter)
    {
        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });
    }

    public void init()
    {
        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        dataProvider = DataProvider.fromFilteringCallbacks(query ->
        {
            Optional<FILTER> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            RESULTS results;

            if(filter.isPresent())
            {
                results = this.getResults(filter.get(), offset, limit);
            }
            else
            {
                results = this.getResults(null, offset, limit);
            }

            return results.getResultList().stream();
        }, query ->
        {
            Optional<FILTER> filter = query.getFilter();

            RESULTS results;

            if(filter.isPresent())
            {
                results = this.getResults(filter.get(), 0, 0);
            }
            else
            {
                results = this.getResults(null, 0, 0);
            }

            this.resultSize = results.getTotalNumberOfResults();


            return (int) results.getTotalNumberOfResults();
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.setDataProvider(filteredDataProvider);
    }

    protected abstract RESULTS getResults(FILTER filter, int offset, int limit);

    public long getResultSize()
    {
        return resultSize;
    }
}
