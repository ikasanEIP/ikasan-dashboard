package org.ikasan.dashboard.ui.administration.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.spec.security.model.IkasanPrincipal;
import org.ikasan.spec.security.model.IkasanPrincipalFilter;
import org.ikasan.spec.security.model.IkasanPrincipalLite;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.service.SecurityService;

import java.util.Optional;
import java.util.function.Consumer;

public class SelectGroupForRoleDialog extends AbstractCloseableResizableDialog
{
    private Role role;
    private final SecurityService securityService;
    private final SystemEventLogger systemEventLogger;
    private final Grid<IkasanPrincipalLite> ikasanPrincipalLiteFilteringGrid;
    private ConfigurableFilterDataProvider<IkasanPrincipalLite,Void,IkasanPrincipalFilter> groupGridFilteredDataProvider;

    public SelectGroupForRoleDialog(Role role, SecurityService securityService
        , SystemEventLogger systemEventLogger, Grid<IkasanPrincipalLite> ikasanPrincipalLiteFilteringGrid)
    {
        this.role = role;
        if(this.role == null)
        {
            throw new IllegalArgumentException("role cannot be null!");
        }
        this.securityService = securityService;
        if(this.securityService == null)
        {
            throw new IllegalArgumentException("securityService cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger == null)
        {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.ikasanPrincipalLiteFilteringGrid = ikasanPrincipalLiteFilteringGrid;
        if(this.ikasanPrincipalLiteFilteringGrid == null)
        {
            throw new IllegalArgumentException("groupDataProvider cannot be null!");
        }

        init();
    }

    private void init()
    {
        super.title.setText(getTranslation("label.select-group", UI.getCurrent().getLocale()));
        H3 selectGroupLabel = new H3(getTranslation("label.select-group", UI.getCurrent().getLocale()));

        IkasanPrincipalFilter groupFilter = new IkasanPrincipalFilterImpl();
        groupFilter.setTypeFilter("application");
        
        Grid<IkasanPrincipalLite> groupGrid = new Grid<>();
        groupGrid.setId("groupSelectGrid");
        groupGrid.setClassName("my-grid");

        groupGrid.addColumn(IkasanPrincipalLite::getName)
            .setHeader(getTranslation("table-header.group-name", UI.getCurrent().getLocale(), null))
            .setKey("name")
            .setSortable(true)
            .setFlexGrow(2);
        groupGrid.addColumn(IkasanPrincipalLite::getDescription)
            .setHeader(getTranslation("table-header.group-description", UI.getCurrent().getLocale(), null))
            .setKey("description")
            .setSortable(true)
            .setFlexGrow(8);

        groupGrid.addItemDoubleClickListener((ComponentEventListener<ItemDoubleClickEvent<IkasanPrincipalLite>>) ikasanPrincipalLiteItemDoubleClickEvent ->
        {
                // Refresh the role to make sure we do not have a dirty role.
                this.role = this.securityService.getRoleById(this.role.getId());

                IkasanPrincipal ikasanPrincipal = this.securityService
                    .findPrincipalByName(ikasanPrincipalLiteItemDoubleClickEvent.getItem().getName());
                ikasanPrincipal.addRole(this.role);

                this.securityService.savePrincipal(ikasanPrincipal);

                String action = String.format("Role [%s] added to user [%s].", role.getName(), ikasanPrincipal.getName());

                this.systemEventLogger.logEvent(SystemEventConstants.DASHBOARD_PRINCIPAL_ROLE_CHANGED_CONSTANTS, action, null);

                this.ikasanPrincipalLiteFilteringGrid.getDataProvider().refreshAll();
                groupGrid.getDataProvider().refreshAll();
        });

        HeaderRow hr = groupGrid.appendHeaderRow();
        addGridFiltering(groupGrid, hr, groupFilter::setNameFilter, "name");
        addGridFiltering(groupGrid, hr, groupFilter::setDescriptionFilter, "description");

        groupGrid.setSizeFull();

        // The index of the first item to load
        // The number of items to load
        DataProvider<IkasanPrincipalLite, IkasanPrincipalFilter> groupDataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<IkasanPrincipalFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            if (!query.getSortOrders().isEmpty()) {
                filter.get().setSortColumn(query.getSortOrders().get(0).getSorted());
                filter.get().setSortOrder(query.getSortOrders().get(0).getDirection().name());
            } else {
                filter.get().setSortColumn(null);
                filter.get().setSortOrder(null);
            }

            return this.securityService.getAllPrincipalsWithoutRole(this.role.getName(), filter.get(), limit, offset).stream();
        }, query -> {
            Optional<IkasanPrincipalFilter> filter = query.getFilter();

            return this.securityService.getPrincipalsWithoutRoleCount(role.getName(), filter.get());
        });

        groupGridFilteredDataProvider = groupDataProvider.withConfigurableFilter();
        groupGridFilteredDataProvider.setFilter(groupFilter);

        groupGrid.setDataProvider(groupGridFilteredDataProvider);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(selectGroupLabel, groupGrid);

        this.content.add(layout);
        super.setWidth("1200px");
        super.setHeight("700px");
    }

    /**
     * Add filtering to a column.
     *
     * @param hr
     * @param setFilter
     * @param columnKey
     */
    public void addGridFiltering(Grid grid, HeaderRow hr, Consumer<String> setFilter, String columnKey)
    {
        TextField textField = new TextField();
        textField.setWidthFull();

        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            if(groupGridFilteredDataProvider != null) {
                groupGridFilteredDataProvider.refreshAll();
            }
        });

        hr.getCell(grid.getColumnByKey(columnKey)).setComponent(textField);
    }
    
    private class IkasanPrincipalFilterImpl implements IkasanPrincipalFilter {
        private String nameFilter;
        private String descriptionFilter;
        private String typeFilter;
        private String sortColumn;
        private String sortOrder;

        @Override
        public String getNameFilter() {
            return nameFilter;
        }

        @Override
        public void setNameFilter(String nameFilter) {
            this.nameFilter = nameFilter;
        }

        @Override
        public String getDescriptionFilter() {
            return descriptionFilter;
        }

        @Override
        public void setDescriptionFilter(String descriptionFilter) {
            this.descriptionFilter = descriptionFilter;
        }

        @Override
        public String getTypeFilter() {
            return typeFilter;
        }

        @Override
        public void setTypeFilter(String typeFilter) {
            this.typeFilter = typeFilter;
        }

        @Override
        public String getSortColumn() {
            return sortColumn;
        }

        @Override
        public void setSortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
        }

        @Override
        public String getSortOrder() {
            return sortOrder;
        }

        @Override
        public void setSortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}
