package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import org.ikasan.dashboard.ui.visualisation.component.filter.FlowSearchFilter;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.FlowMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
        return this.flows.subList(offset, flows.size()<limit+offset ? flows.size() : limit+offset);
//        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
//
//        final List<String> moduleNames = new ArrayList<>();
//        Set<String> accessibleModules = new HashSet<>();
//
//        if(!authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY)) {
//            accessibleModules = SecurityUtils.getAccessibleModules(authentication);
//            moduleNames.addAll(accessibleModules);
//        }
//
//        if(filter.getModuleNameFilter() != null && !filter.getModuleNameFilter().isEmpty()) {
//            moduleNames.clear();
//            if(!authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY)) {
//                accessibleModules.stream().forEach(accessibleModule -> {
//                    if(accessibleModule.toLowerCase().contains(filter.getModuleNameFilter().toLowerCase())) {
//                        moduleNames.add(accessibleModule);
//                    }
//                });
//            }
//            else {
//                moduleNames.add("*" + ClientUtils.escapeQueryChars(filter.getModuleNameFilter()) + "*");
//            }
//        }
//
//        if(!authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY) && moduleNames.isEmpty()){
//            moduleNames.add(SearchConstants.NONSENSE_STRING);
//        }
//
//        ModuleMetadataSearchResults results;
//
//        try {
//            results =  this.solrSearchService.find(moduleNames, offset, limit);
//        }
//        catch (Exception e) {
//            final UI current = UI.getCurrent();
//            final I18NProvider i18NProvider = VaadinService.getCurrent().getInstantiator().getI18NProvider();
//            NotificationHelper.showErrorNotification(i18NProvider.getTranslation("error.solr-unavailable"
//                , current.getLocale()));
//
//            results = new ModuleMetadataSearchResults(new ArrayList<>(), 0, 0);
//        }
//
//        return results.getResultList()
//            .stream()
//            .flatMap(metaData -> metaData.getFlows().stream().map(flowMetaData -> new Flow(metaData.getName(), flowMetaData.getName())))
//            .collect(Collectors.toList());
    }

    public long getResultSize()
    {
        return resultSize;
    }
}
