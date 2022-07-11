package org.ikasan.dashboard.ui.scheduler;

import com.vaadin.flow.data.provider.QuerySortOrder;
import org.ikasan.dashboard.ui.general.component.Filter;
import org.ikasan.security.model.IkasanPrincipalLite;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class ContextFilter implements Filter<String>
{
    private Collection<String> contexts;
    private String nameFilter = null;

    public void setItems(Collection<String> contexts)
    {
        this.contexts = contexts;
    }

    public String getNameFilter()
    {
        return nameFilter;
    }

    public void setNameFilter(String nameFilter)
    {
        this.nameFilter = nameFilter;
    }

    @Override
    public Stream<String> getFilterStream()
    {
        return contexts
            .stream()
            .filter(group ->
            {
                if(this.getNameFilter() == null || this.getNameFilter().isEmpty())
                {
                    return true;
                }
                else if(group == null)
                {
                    return false;
                }
                else
                {
                    return group.toLowerCase().contains(getNameFilter().toLowerCase());
                }
            });
    }

    @Override
    public Comparator getSortComparator(List<QuerySortOrder> querySortOrders)
    {
        Comparator comparator = null;

        if(querySortOrders.get(0).getSorted().equals("name"))
        {
            comparator = Comparator.comparing(String::toString, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }

        return comparator;
    }

    @Override
    public Collection<String> getItems() {
        return this.contexts;
    }
}
