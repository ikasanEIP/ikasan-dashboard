package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

public class ContextInstanceAuditFilteringGrid extends Grid<ScheduledContextInstanceAuditAggregateRecord> {
    private Logger logger = LoggerFactory.getLogger(ContextInstanceAuditFilteringGrid.class);

    private ScheduledContextInstanceService contextInstanceService;

    private DataProvider<ScheduledContextInstanceAuditAggregateRecord, ScheduledContextInstanceAuditAggregateSearchFilter> dataProvider;
    private ConfigurableFilterDataProvider<ScheduledContextInstanceAuditAggregateRecord, Void, ScheduledContextInstanceAuditAggregateSearchFilter> filteredDataProvider;

    private ScheduledContextInstanceAuditAggregateSearchFilter searchFilter;

    private long resultSize = 0;

    /**
     * Constructor
     *
     * @param solrSearchService
     * @param searchFilter
     */
    public ContextInstanceAuditFilteringGrid(ScheduledContextInstanceService solrSearchService,
                                             ScheduledContextInstanceAuditAggregateSearchFilter searchFilter) {
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
     * Initialise the grid.
     */
    public void init() {
        dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<ScheduledContextInstanceAuditAggregateSearchFilter> filter = query.getFilter();

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
            Optional<ScheduledContextInstanceAuditAggregateSearchFilter> filter = query.getFilter();

            SearchResults results;

            results = this.getResults(filter.get(), 0, 0, null, null);

            this.resultSize = results.getTotalNumberOfResults();

            return (int) this.resultSize;
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.setDataProvider(filteredDataProvider);
    }

    private SearchResults getResults(ScheduledContextInstanceAuditAggregateSearchFilter filter, int offset, int limit, String sortField, String sortOrder) {
        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        SearchResults results = null;

        try {
            results = this.contextInstanceService.findAllAuditRecordsByFilter(this.searchFilter, limit, offset, sortField, sortOrder);
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
}
