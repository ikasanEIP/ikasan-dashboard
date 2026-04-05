package org.ikasan.dashboard.ui.administration.filter;

import com.vaadin.flow.data.provider.QuerySortOrder;
import org.ikasan.dashboard.ui.general.component.Filter;
import org.ikasan.security.model.SolrRoleModuleImpl;
import org.ikasan.spec.security.model.RoleJobPlan;
import org.ikasan.spec.security.model.RoleModule;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class RoleJobPlanFilter implements Filter<RoleJobPlan>
{
    private Collection<RoleJobPlan> roleJobPlans;
    private String moduleNameFilter = null;

    public void setItems(Collection<RoleJobPlan> roleJobPlans)
    {
        this.roleJobPlans = roleJobPlans;
    }

    public String getModuleNameFilter()
    {
        return moduleNameFilter;
    }

    public void setModuleNameFilter(String moduleNameFilter)
    {
        this.moduleNameFilter = moduleNameFilter;
    }

    @Override
    public Stream<RoleJobPlan> getFilterStream()
    {
        return roleJobPlans
            .stream()
            .filter(group ->
            {
                if(this.getModuleNameFilter() == null || this.getModuleNameFilter().isEmpty())
                {
                    return true;
                }
                else if(group.getJobPlanName() == null)
                {
                    return false;
                }
                else
                {
                    return group.getJobPlanName().toLowerCase().contains(getModuleNameFilter().toLowerCase());
                }
            });
    }

    @Override
    public Comparator getSortComparator(List<QuerySortOrder> querySortOrders)
    {
        Comparator comparator = null;

        if(querySortOrders.get(0).getSorted().equals("name"))
        {
            comparator = Comparator.comparing(SolrRoleModuleImpl::getModuleName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }

        return comparator;
    }

    @Override
    public Collection<RoleJobPlan> getItems() {
        return this.roleJobPlans;
    }
}
