package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.filter.ContextInstanceSearchFilter;
import org.ikasan.dashboard.ui.util.SearchConstants;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.visualisation.component.filter.ModuleSearchFilter;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.function.Consumer;

public class ContextInstanceAuditFilteringGrid extends Grid<ScheduledContextInstanceAuditRecord> {
    private Logger logger = LoggerFactory.getLogger(ContextInstanceAuditFilteringGrid.class);

    private ScheduledContextInstanceService contextInstanceService;

    private DataProvider<ScheduledContextInstanceAuditRecord, ContextInstanceSearchFilter> dataProvider;
    private ConfigurableFilterDataProvider<ScheduledContextInstanceAuditRecord, Void, ContextInstanceSearchFilter> filteredDataProvider;

    private ContextInstanceSearchFilter searchFilter;

    private long resultSize = 0;

    /**
     * Constructor
     *
     * @param solrSearchService
     * @param searchFilter
     */
    public ContextInstanceAuditFilteringGrid(ScheduledContextInstanceService solrSearchService,
                                             ContextInstanceSearchFilter searchFilter) {
        this.contextInstanceService = solrSearchService;
        if(this.contextInstanceService ==  null) {
            throw new IllegalArgumentException("contextInstanceService cannot be null!");
        }
        this.searchFilter = searchFilter;
        if(this.searchFilter ==  null) {
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
    public void addGridFiltering(HeaderRow hr, Consumer<String> setFilter, String columnKey) {
        TextField textField = new TextField();
        textField.setWidthFull();

        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });

        hr.getCell(getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * Add general filter
     *
     * @param textField
     * @param setFilter
     */
    public void addGridFiltering(TextField textField, Consumer<String> setFilter) {
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

            results = this.getResults(filter.get(), offset, limit);

            return results.getResultList().stream();
        }, query -> {
            Optional<ContextInstanceSearchFilter> filter = query.getFilter();

            SearchResults results;

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            results = this.getResults(filter.get(), offset, limit);

            this.resultSize = results.getTotalNumberOfResults();

            return (int) this.resultSize;
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.setDataProvider(filteredDataProvider);
    }

    private SearchResults getResults(ContextInstanceSearchFilter filter, int offset, int limit) {
        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        SearchResults results;

        try {
            if(filter.getContextSearchFilter() != null && !filter.getContextSearchFilter().isEmpty()) {
                results = this.contextInstanceService.findAllAuditRecordsByContextId(filter.getContextSearchFilter(), offset, limit);
            }
            else {
                results = this.contextInstanceService.findAllAuditRecords(offset, limit);
            }
        }
        catch (Exception e) {
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
}
