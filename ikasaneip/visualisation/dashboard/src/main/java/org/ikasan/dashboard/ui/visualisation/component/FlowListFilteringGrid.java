package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.visualisation.component.filter.FlowSearchFilter;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.FlowMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FlowListFilteringGrid extends Grid<FlowMetaData>
{
    private Logger logger = LoggerFactory.getLogger(FlowListFilteringGrid.class);

    private List<FlowMetaData> flows;

    private DataProvider<FlowMetaData, FlowSearchFilter> dataProvider;
    private ConfigurableFilterDataProvider<FlowMetaData, Void, FlowSearchFilter> filteredDataProvider;

    private FlowSearchFilter searchFilter;

    private long resultSize = 0;

    /**
     * Constructor
     */
    public FlowListFilteringGrid(List<FlowMetaData> flows,
                                 FlowSearchFilter searchFilter)
    {
        this.flows = flows;
        if(this.flows ==  null)
        {
            throw new IllegalArgumentException("flows cannot be null!");
        }
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

    /**
     * Add testfiel filtering
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

    public void init()
    {
        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        dataProvider = DataProvider.fromFilteringCallbacks(query ->
        {
            Optional<FlowSearchFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            List<FlowMetaData> results = this.getResults(filter.get(), offset, limit);

            return results.stream();
        }, query ->
        {
            Optional<FlowSearchFilter> filter = query.getFilter();

            List<FlowMetaData> results;

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            results = this.getResults(filter.get(), offset, limit);

            this.resultSize = results.size();

            return (int) this.resultSize;
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.setDataProvider(filteredDataProvider);
    }

    private List<FlowMetaData> getResults(FlowSearchFilter filter, int offset, int limit)
    {
        List<FlowMetaData> filteredFlows = this.flows;

        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if(!authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY) && !authentication.hasGrantedAuthority(SecurityConstants.SCHEDULER_ADMIN)) {
            filteredFlows = this.flows.stream()
                .filter(flowMetaData -> SecurityUtils.getAccessibleModules(authentication)
                    .contains(flowMetaData.getName().substring(0, flowMetaData.getName().indexOf("."))))
                .collect(Collectors.toList());
        }

        return filteredFlows.subList(offset, limit+offset<filteredFlows.size() ? limit+offset : filteredFlows.size());
    }

    public long getResultSize()
    {
        return resultSize;
    }
}
