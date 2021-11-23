package org.ikasan.dashboard.ui.visualisation.component.filter;

import com.vaadin.flow.data.provider.QuerySortOrder;
import org.ikasan.spec.metadata.ModuleMetaData;

import java.util.Comparator;
import java.util.List;

public class FlowSearchFilter
{
    private String moduleNameFilter = "";
    private String flowNameFilter = "";

    public String getModuleNameFilter()
    {
        return moduleNameFilter;
    }
    public void setModuleNameFilter(String moduleNameFilter)
    {
        if(moduleNameFilter != null) {
            this.moduleNameFilter = moduleNameFilter;
        }
    }

    public String getFlowNameFilter() {
        return flowNameFilter;
    }

    public void setFlowNameFilter(String flowNameFilter) {
        if(flowNameFilter != null) {
            this.flowNameFilter = flowNameFilter;
        }
    }

    public Comparator getSortComparator(List<QuerySortOrder> querySortOrders)
    {
        Comparator comparator = null;

        if(querySortOrders.get(0).getSorted().equals("name"))
        {
            comparator = Comparator.comparing(ModuleMetaData::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }

        return comparator;
    }
}
