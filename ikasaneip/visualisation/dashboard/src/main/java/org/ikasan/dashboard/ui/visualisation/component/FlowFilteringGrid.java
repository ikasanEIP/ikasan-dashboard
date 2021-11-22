package org.ikasan.dashboard.ui.visualisation.component;

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
import org.ikasan.dashboard.ui.util.SearchConstants;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.visualisation.component.filter.FlowSearchFilter;
import org.ikasan.dashboard.ui.visualisation.model.designer.business.stream.Flow;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FlowFilteringGrid extends Grid<Flow>
{
    private Logger logger = LoggerFactory.getLogger(FlowFilteringGrid.class);

    private ModuleMetaDataService solrSearchService;

    private DataProvider<Flow, FlowSearchFilter> dataProvider;
    private ConfigurableFilterDataProvider<Flow, Void, FlowSearchFilter> filteredDataProvider;

    private FlowSearchFilter searchFilter;

    private long resultSize = 0;
    private ModuleType moduleType;

    /**
     * Constructor
     */
    public FlowFilteringGrid(ModuleMetaDataService solrSearchService,
                             FlowSearchFilter searchFilter, ModuleType moduleType)
    {
        this.solrSearchService = solrSearchService;
        if(this.solrSearchService ==  null)
        {
            throw new IllegalArgumentException("solrSearchService cannot be null!");
        }
        this.searchFilter = searchFilter;
        if(this.searchFilter ==  null)
        {
            throw new IllegalArgumentException("SearchFilter cannot be null!");
        }
        this.moduleType = moduleType;
        if(this.moduleType ==  null)
        {
            throw new IllegalArgumentException("moduleType cannot be null!");
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
        dataProvider = DataProvider.fromFilteringCallbacks(query ->
        {
            Optional<FlowSearchFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            List<Flow> results = this.getResults(filter.get(), offset, limit);

            return results.stream();
        }, query ->
        {
            Optional<FlowSearchFilter> filter = query.getFilter();

            List<Flow> results;

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

    private List<Flow> getResults(FlowSearchFilter filter, int offset, int limit)
    {
        IkasanAuthentication authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();

        final List<String> moduleNames = new ArrayList<>();
        Set<String> accessibleModules = new HashSet<>();

        if(!authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY)) {
            accessibleModules = SecurityUtils.getAccessibleModules(authentication);
            moduleNames.addAll(accessibleModules);
        }

        if(filter.getModuleNameFilter() != null && !filter.getModuleNameFilter().isEmpty()) {
            moduleNames.clear();
            if(!authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY)) {
                accessibleModules.stream().forEach(accessibleModule -> {
                    if(accessibleModule.toLowerCase().contains(filter.getModuleNameFilter().toLowerCase())) {
                        moduleNames.add(accessibleModule);
                    }
                });
            }
            else {
                moduleNames.add("*" + ClientUtils.escapeQueryChars(filter.getModuleNameFilter()) + "*");
            }
        }

        if(!authentication.hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY)
            && !authentication.hasGrantedAuthority(SecurityConstants.BUSINESS_STREAM_ADMIN) && moduleNames.isEmpty()){
            moduleNames.add(SearchConstants.NONSENSE_STRING);
        }

        ModuleMetadataSearchResults results;

        try {
            if(moduleType == ModuleType.SCHEDULER_AGENT) {
                results = this.solrSearchService.find(moduleNames, this.moduleType, offset, limit);
            }
            else {
                results =  this.solrSearchService.find(moduleNames, offset, limit);

                List<ModuleMetaData> moduleMetaData = results.getResultList().stream().filter(module ->
                    module.getType() == null || module.getType() != ModuleType.SCHEDULER_AGENT)
                    .collect(Collectors.toList());

                results = new ModuleMetadataSearchResults(moduleMetaData, moduleMetaData.size()
                    , results.getQueryResponseTime());
            }
        }
        catch (Exception e) {
            final UI current = UI.getCurrent();
            final I18NProvider i18NProvider = VaadinService.getCurrent().getInstantiator().getI18NProvider();
            NotificationHelper.showErrorNotification(i18NProvider.getTranslation("error.solr-unavailable"
                , current.getLocale()));

            results = new ModuleMetadataSearchResults(new ArrayList<>(), 0, 0);
        }

        List<Flow> flows = results.getResultList()
            .stream()
            .flatMap(metaData -> metaData.getFlows().stream().map(flowMetaData -> new Flow(metaData.getName(), flowMetaData.getName())))
            .collect(Collectors.toList());

        return flows;
    }

    public long getResultSize()
    {
        return resultSize;
    }
}
