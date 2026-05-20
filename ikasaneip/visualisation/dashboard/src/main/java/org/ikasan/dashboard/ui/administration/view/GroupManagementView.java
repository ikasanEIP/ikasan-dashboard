package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.administration.component.GroupManagementDialog;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DashboardContextNavigator;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.spec.security.model.IkasanPrincipalFilter;
import org.ikasan.spec.security.model.IkasanPrincipalLite;
import org.ikasan.spec.security.service.SecurityService;
import org.ikasan.spec.systemevent.SystemEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.util.Optional;
import java.util.function.Consumer;

@Route(value = "groupManagement", layout = IkasanAppLayout.class)
@UIScope
@PageTitle("Ikasan - Group Management")
@PermitAll
@PreserveOnRefresh
@Component
public class GroupManagementView extends VerticalLayout implements BeforeEnterObserver
{
    private Logger logger = LoggerFactory.getLogger(GroupManagementView.class);

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SystemEventService systemEventService;

    @Autowired
    private SystemEventLogger systemEventLogger;

    private Grid<IkasanPrincipalLite> groupGrid;

    private DataProvider<IkasanPrincipalLite, IkasanPrincipalFilter> dataProvider;
    private ConfigurableFilterDataProvider<IkasanPrincipalLite,Void,IkasanPrincipalFilter> filteredDataProvider;

    private IkasanPrincipalFilter groupFilter = new IkasanPrincipalFilterImpl();

    /**
     * Constructor
     */
    public GroupManagementView()
    {
        super();
        init();
    }

    protected void init()
    {
        this.setSizeFull();
        this.setSpacing(true);

        H2 groupManagementLabel = new H2(getTranslation("label.group-management", UI.getCurrent().getLocale(), null));
        add(groupManagementLabel);

        this.groupGrid = new Grid<>();
        this.groupGrid.setSizeFull();
        this.groupGrid.setClassName("my-grid");

        this.groupFilter.setTypeFilter("application");

        this.groupGrid.addColumn(IkasanPrincipalLite::getName).setHeader(getTranslation("table-header.group-name", UI.getCurrent().getLocale(), null)).setKey("name").setSortable(true);
        this.groupGrid.addColumn(IkasanPrincipalLite::getType).setHeader(getTranslation("table-header.group-type", UI.getCurrent().getLocale(), null)).setKey("type").setSortable(true);
        this.groupGrid.addColumn(IkasanPrincipalLite::getDescription).setHeader(getTranslation("table-header.group-description", UI.getCurrent().getLocale(), null)).setKey("description").setSortable(true);

        HeaderRow hr = groupGrid.appendHeaderRow();
        this.addGridFiltering(hr, groupFilter::setNameFilter, "name");
        this.addGridFiltering(hr, groupFilter::setDescriptionFilter, "description");

        this.groupGrid.addItemDoubleClickListener((ComponentEventListener<ItemDoubleClickEvent<IkasanPrincipalLite>>) userItemDoubleClickEvent ->
        {
            GroupManagementDialog dialog = new GroupManagementDialog(userItemDoubleClickEvent.getItem()
                , this.securityService, this.systemEventService, this.systemEventLogger);

            dialog.open();
        });

        dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<IkasanPrincipalFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            if(!query.getSortOrders().isEmpty()) {
                filter.get().setSortColumn(query.getSortOrders().get(0).getSorted());
                filter.get().setSortOrder(query.getSortOrders().get(0).getDirection().name());
            }
            else {
                filter.get().setSortColumn(null);
                filter.get().setSortOrder(null);
            }

            return this.securityService.getPrincipalLites(filter.get(), limit, offset).stream();
        }, query -> {
            Optional<IkasanPrincipalFilter> filter = query.getFilter();

            return this.securityService.getPrincipalCount(filter.get());
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.groupFilter);

        this.groupGrid.setDataProvider(filteredDataProvider);

        add(this.groupGrid);
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

            if(filteredDataProvider != null) {
                filteredDataProvider.refreshAll();
            }
        });

        hr.getCell(groupGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        if(!ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.GROUP_ADMINISTRATION_ADMIN, SecurityConstants.GROUP_ADMINISTRATION_WRITE, SecurityConstants.GROUP_ADMINISTRATION_READ,
            SecurityConstants.ALL_AUTHORITY)) {
            DashboardContextNavigator.navigateToLandingPage();
        }
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
