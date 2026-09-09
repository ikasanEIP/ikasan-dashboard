package org.ikasan.dashboard.ui.search.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.search.component.filter.SearchFilter;
import org.ikasan.dashboard.ui.util.SearchConstants;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SearchFilteringGrid extends Grid<IkasanESBDocument>
{
    private Logger logger = LoggerFactory.getLogger(SearchFilteringGrid.class);

    private ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService;

    private DataProvider<IkasanESBDocument,SearchFilter> dataProvider;
    private ConfigurableFilterDataProvider<IkasanESBDocument,Void, SearchFilter> filteredDataProvider;

    private SearchFilter searchFilter;

    private long resultSize = 0;
    private long queryTime = 0;

    private NativeLabel resultsLabel;

    /**
     * Constructors
     */
    public SearchFilteringGrid(ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService,
                               SearchFilter searchFilter, NativeLabel resultsLabel)
    {
        this.esbSearchService = esbSearchService;
        if(this.esbSearchService ==  null)
        {
            throw new IllegalArgumentException("solrSearchService cannot be null!");
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


    public void init(long startTime, long endTime, String searchTerm, List<String> types, boolean negateQuery) {
        this.init(startTime, endTime, searchTerm, types, negateQuery, null);
    }

    public void init(long startTime, long endTime, String searchTerm, List<String> types, boolean negateQuery, SearchFilter searchFilter)
    {
        if(searchFilter != null) {
            this.searchFilter = searchFilter;
        }

        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        dataProvider = DataProvider.fromFilteringCallbacks(query ->
        {
            Optional<SearchFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            IkasanDocumentSearchResults results;

            if(query.getSortOrders().size() > 0)
            {
                results = this.getResults(authentication, filter.get(), startTime, endTime
                    , searchTerm, offset, limit, types, negateQuery, query.getSortOrders().get(0).getSorted()
                    , query.getSortOrders().get(0).getDirection().name());
            }
            else
            {
                results = this.getResults(authentication, filter.get(), startTime, endTime
                    , searchTerm, offset, limit, types, negateQuery, null, null);
            }

            return results.getResultList().stream();
        }, query ->
        {
            Optional<SearchFilter> filter = query.getFilter();

            IkasanDocumentSearchResults results = this.getResults(authentication, filter.get(), startTime, endTime
            , searchTerm, 0, 0, types, negateQuery, null, null);

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

    private IkasanDocumentSearchResults getResults(IkasanAuthentication authentication, SearchFilter filter, long startTime
        , long endTime, String searchTerm, int offset, int limit, List<String> types, boolean negateQuery, String sortField, String sortOrder)
    {
        Set<String> allowedModuleNames = SecurityUtils.getAccessibleModules(authentication);

        Set<String> moduleNames = null;

        if(filter.getSystemEventFilter() != null && !filter.getSystemEventFilter().isEmpty()) {
            String systemEventFilters = filter.getSystemEventFilter().values().stream()
                .filter(value -> value.length() > 0)
                .collect(Collectors.joining(" AND "));

            if(searchTerm != null && searchTerm.length() > 0 && systemEventFilters.length() > 0) {
                searchTerm += " AND " + systemEventFilters;
            }
            else if((searchTerm == null || searchTerm.length() == 0) && systemEventFilters.length() > 0) {
                searchTerm = systemEventFilters;
            }
        }

        if(filter.isValidModuleNameFilter()) {
            moduleNames = new HashSet<>();

            if(this.canAccessAllModules(authentication)) {
                if(filter.getModuleNameFilter() != null) {
                    moduleNames.add(filter.getModuleNameFilter());
                }
            }
            else {
                for(String allowedModuleName: allowedModuleNames){
                    if(allowedModuleName.toLowerCase().startsWith(filter.getModuleNameFilter().toLowerCase())) {
                        moduleNames.add(allowedModuleName);
                    }
                }
            }

            if(moduleNames.isEmpty()) {
                moduleNames.add(SearchConstants.NONSENSE_STRING);
            }
        }
        else if(filter.getModuleNameFilter() != null && !filter.getModuleNameFilter().isEmpty()) {
            moduleNames = new HashSet<>();
            moduleNames.add(SearchConstants.NONSENSE_STRING);
        }
        else if(filter.getModuleNamesFilterList() != null && !filter.getModuleNamesFilterList().isEmpty())
        {
            moduleNames = new HashSet<>();

            if(!this.canAccessAllModules(authentication))
            {
                moduleNames.addAll(allowedModuleNames.stream()
                    .filter(name -> filter.getModuleNamesFilterList()
                        .stream()
                        .anyMatch(moduleName -> moduleName.startsWith(name)))
                    .collect(Collectors.toList()));

                if(moduleNames.isEmpty()){
                    moduleNames.add(SearchConstants.NONSENSE_STRING);
                }
            }
            else
            {
                moduleNames = filter.getModuleNamesFilterList()
                    .stream()
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.toSet());
            }
        }

        Set<String> flowNames = null;

        if(filter.isValidFlowNameFilter()) {
            flowNames = new HashSet<>();
            if(filter.getFlowNameFilter() != null && !filter.getFlowNameFilter().isEmpty()) {
                flowNames.add(filter.getFlowNameFilter());
            }
        }
        else if(filter.getFlowNameFilter() != null && !filter.getFlowNameFilter().isEmpty()) {
            flowNames = new HashSet<>();
            flowNames.add(SearchConstants.NONSENSE_STRING);
        }
        else if(filter.getFlowNamesFilterList() != null && !filter.getFlowNamesFilterList().isEmpty())
        {
            flowNames = new HashSet<>(filter.getFlowNamesFilterList());
        }

        HashSet<String> componentNames = null;

        if(filter.getComponentNameFilter() != null && !filter.getComponentNameFilter().isEmpty())
        {
            componentNames = new HashSet<>();
            if(filter.getComponentNameFilter() != null && !filter.getComponentNameFilter().isEmpty()) {
                componentNames.add(filter.getComponentNameFilter());
            }
        }

        String eventId = null;

        if(filter.getEventIdFilter() != null && !filter.getEventIdFilter().isEmpty())
        {
            eventId = filter.getEventIdFilter();
        }

        if(!this.canAccessAllModules(authentication) && moduleNames == null)
        {
            moduleNames = allowedModuleNames;

            if(moduleNames.isEmpty()){
                moduleNames.add(SearchConstants.NONSENSE_STRING);
            }
        }

        if(types.isEmpty()) {
            types.add(SearchConstants.NONSENSE_STRING);
        }

        try {
            return this.esbSearchService.search(moduleNames, flowNames, componentNames, eventId, searchTerm,
                startTime, endTime, offset, limit, types, negateQuery, sortField, sortOrder);
        }
        catch (Exception e) {
            logger.error("An error has occurred performing search!", e);
            final UI current = UI.getCurrent();
            final I18NProvider i18NProvider = VaadinService.getCurrent().getInstantiator().getI18NProvider();
            NotificationHelper.showErrorNotification(i18NProvider.getTranslation("error.solr-unavailable"
                , current.getLocale()));
        }

        return new IkasanDocumentSearchResults() {
            @Override
            public List<IkasanESBDocument> getResultList() {
                return List.of();
            }

            @Override
            public long getTotalNumberOfResults() {
                return 0;
            }

            @Override
            public long getQueryResponseTime() {
                return 0;
            }
        };
    }

    private boolean canAccessAllModules(IkasanAuthentication authentication) {
        return authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY) ||
            authentication.hasGrantedAuthority(SecurityConstants.WIRETAP_ALL_MODULES_READ) ||
            authentication.hasGrantedAuthority(SecurityConstants.WIRETAP_ALL_MODULES_WRITE) ||
            authentication.hasGrantedAuthority(SecurityConstants.WIRETAP_ALL_MODULES_ADMIN) ||
            authentication.hasGrantedAuthority(SecurityConstants.EXCLUSION_ALL_MODULES_READ) ||
            authentication.hasGrantedAuthority(SecurityConstants.EXCLUSION_ALL_MODULES_WRITE) ||
            authentication.hasGrantedAuthority(SecurityConstants.EXCLUSION_ALL_MODULES_ADMIN) ||
            authentication.hasGrantedAuthority(SecurityConstants.ERROR_ALL_MODULES_READ) ||
            authentication.hasGrantedAuthority(SecurityConstants.ERROR_ALL_MODULES_WRITE) ||
            authentication.hasGrantedAuthority(SecurityConstants.ERROR_ALL_MODULES_ADMIN) ||
            authentication.hasGrantedAuthority(SecurityConstants.REPLAY_ALL_MODULES_READ) ||
            authentication.hasGrantedAuthority(SecurityConstants.REPLAY_ALL_MODULES_WRITE) ||
            authentication.hasGrantedAuthority(SecurityConstants.REPLAY_ALL_MODULES_ADMIN);

    }

    public long getResultSize()
    {
        return resultSize;
    }
}
