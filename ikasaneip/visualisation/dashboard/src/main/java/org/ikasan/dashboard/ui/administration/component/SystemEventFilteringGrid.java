package org.ikasan.dashboard.ui.administration.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class SystemEventFilteringGrid extends Grid<SystemEvent>
{
    private Logger logger = LoggerFactory.getLogger(SystemEventFilteringGrid.class);

    private SystemEventSearchService systemEventSearchService;

    private DataProvider<SystemEvent, SystemEventSearchFilter> dataProvider;
    private ConfigurableFilterDataProvider<SystemEvent,Void, SystemEventSearchFilter> filteredDataProvider;

    private SystemEventSearchFilter searchFilter;

    private long resultSize = 0;
    private long queryTime = 0;

    private NativeLabel resultsLabel;

    /**
     * Constructors
     */
    public SystemEventFilteringGrid(SystemEventSearchService systemEventSearchService,
                                    SystemEventSearchFilter searchFilter, NativeLabel resultsLabel)
    {
        this.systemEventSearchService = systemEventSearchService;
        if(this.systemEventSearchService ==  null)
        {
            throw new IllegalArgumentException("systemEventSearchService cannot be null!");
        }
        this.searchFilter = searchFilter;
        if(this.searchFilter ==  null)
        {
            throw new IllegalArgumentException("SearchFilter cannot be null!");
        }
        this.resultsLabel = resultsLabel;
        if(this.resultsLabel ==  null)
        {
            throw new IllegalArgumentException("resultsLabel cannot be null!");
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
        textField.setId(columnKey);
        textField.setWidthFull();

        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);

        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            if(filteredDataProvider != null) {
                filteredDataProvider.refreshAll();
            }
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
    public void addSelectGridFiltering(HeaderRow hr, Consumer<String> setFilter, List<String> options, String columnKey) {
        ComboBox<String> select = new ComboBox<>();
        select.setItems(options);
        select.setWidthFull();
        select.setItemLabelGenerator(entry -> {
            if(entry == null) {
                return "";
            }

            return entry;
        });

        select.addValueChangeListener(ev-> {

            setFilter.accept(ev.getValue());

            if(filteredDataProvider != null) {
                filteredDataProvider.refreshAll();
            }
        });

        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");

        HorizontalLayout layout = new HorizontalLayout(select, filterIcon);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, filterIcon);

        hr.getCell(getColumnByKey(columnKey)).setComponent(layout);
    }


    public void init(long startTime, long endTime, String searchTerm, List<String> types, boolean negateQuery) {
        this.init(startTime, endTime, searchTerm, types, negateQuery, null);
    }

    public void init(long startTime, long endTime, String searchTerm, List<String> types, boolean negateQuery, SystemEventSearchFilter searchFilter)
    {
        if(searchFilter != null) {
            this.searchFilter = searchFilter;
        }

        this.searchFilter.setStartTime(startTime);
        this.searchFilter.setEndTime(endTime);

        if(searchTerm != null && !searchTerm.isEmpty()) {
            searchFilter.setSearchTerm(searchTerm);
        }
        else {
            searchFilter.setSearchTerm(null);
        }

        dataProvider = DataProvider.fromFilteringCallbacks(query ->
        {
            Optional<SystemEventSearchFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            SearchResults<SystemEvent> results;

            if(query.getSortOrders().size() > 0)
            {
                results = this.getResults(filter.get(), offset, limit, query.getSortOrders().get(0).getSorted()
                    , query.getSortOrders().get(0).getDirection().name());
            }
            else
            {
                results = this.getResults(filter.get(), offset, limit, null, null);
            }

            return results.getResultList().stream();
        }, query ->
        {
            Optional<SystemEventSearchFilter> filter = query.getFilter();

            SearchResults<SystemEvent> results = this.getResults(filter.get(),0, 0, null, null);

            this.resultSize = results.getTotalNumberOfResults();
            this.queryTime = results.getQueryResponseTime();

            this.resultsLabel.setText(String.format(getTranslation("label.search-results-returned",
                UI.getCurrent().getLocale(), null), this.resultSize, this.queryTime));
            this.resultsLabel.getElement().getStyle().set("fontSize", "10pt");

            return (int) results.getTotalNumberOfResults();
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.setDataProvider(filteredDataProvider);
    }

    private SearchResults<SystemEvent> getResults(SystemEventSearchFilter filter, int offset, int limit, String sortField, String sortOrder) {
        return this.systemEventSearchService.findByFilter(filter, limit, offset, sortField, sortOrder);
    }

    public long getResultSize()
    {
        return resultSize;
    }
}
