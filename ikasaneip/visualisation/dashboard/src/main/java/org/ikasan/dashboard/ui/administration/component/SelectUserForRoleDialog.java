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
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.model.UserFilter;
import org.ikasan.spec.security.model.UserLite;
import org.ikasan.spec.security.service.SecurityService;
import org.ikasan.spec.security.service.UserService;

import java.util.Optional;
import java.util.function.Consumer;

public class SelectUserForRoleDialog extends AbstractCloseableResizableDialog
{
    private Role role;
    private final SecurityService securityService;
    private final SystemEventLogger systemEventLogger;
    private final UserService userService;
    private final Grid<UserLite> userLiteFilteringGrid;
    private ConfigurableFilterDataProvider<UserLite,Void,UserFilter> userGridFilteredDataProvider;

    public SelectUserForRoleDialog(Role role, UserService userService, SecurityService securityService
        , SystemEventLogger systemEventLogger, Grid<UserLite> userLiteFilteringGrid)
    {
        this.role = role;
        if(this.role == null)
        {
            throw new IllegalArgumentException("role cannot be null!");
        }
        this.userService = userService;
        if(this.userService == null)
        {
            throw new IllegalArgumentException("userService cannot be null!");
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
        this.userLiteFilteringGrid = userLiteFilteringGrid;
        if(this.userLiteFilteringGrid == null)
        {
            throw new IllegalArgumentException("userDataProvider cannot be null!");
        }

        init();
    }

    private void init()
    {
        super.title.setText(getTranslation("label.select-user", UI.getCurrent().getLocale()));
        H3 selectUserLabel = new H3(getTranslation("label.select-user", UI.getCurrent().getLocale()));

        UserFilter userFilter = new UserFilterImpl();

        Grid<UserLite> userGrid = new Grid<>();

        userGrid.addColumn(UserLite::getUsername)
            .setKey("username")
            .setHeader(getTranslation("table-header.username", UI.getCurrent().getLocale(), null))
            .setSortable(true)
            .setFlexGrow(2);
        userGrid.addColumn(UserLite::getFirstName)
            .setKey("firstName")
            .setHeader(getTranslation("table-header.firstname", UI.getCurrent().getLocale(), null))
            .setSortable(true)
            .setFlexGrow(2);
        userGrid.addColumn(UserLite::getSurname)
            .setKey("surname")
            .setHeader(getTranslation("table-header.surname", UI.getCurrent().getLocale(), null))
            .setSortable(true)
            .setFlexGrow(2);
        userGrid.addColumn(UserLite::getEmail)
            .setKey("email")
            .setHeader(getTranslation("table-header.email", UI.getCurrent().getLocale(), null))
            .setSortable(true)
            .setFlexGrow(4);
        userGrid.addColumn(UserLite::getDepartment)
            .setKey("department")
            .setHeader(getTranslation("table-header.department", UI.getCurrent().getLocale(), null))
            .setSortable(true)
            .setFlexGrow(4);

        userGrid.addItemDoubleClickListener((ComponentEventListener<ItemDoubleClickEvent<UserLite>>) userLiteItemDoubleClickEvent ->
        {
            this.role = this.securityService.getRoleById(this.role.getId());
            IkasanPrincipal ikasanPrincipal = this.securityService.findPrincipalByName
                (userLiteItemDoubleClickEvent.getItem().getUsername());
            ikasanPrincipal.addRole(this.role);

            this.securityService.savePrincipal(ikasanPrincipal);

            String action = String.format("Role [%s] added to user [%s].", role.getName(), ikasanPrincipal.getName());

            this.systemEventLogger.logEvent(SystemEventConstants.DASHBOARD_PRINCIPAL_ROLE_CHANGED_CONSTANTS, action, null);

            userGrid.getDataProvider().refreshAll();
            this.userLiteFilteringGrid.getDataProvider().refreshAll();
        });

        HeaderRow hr = userGrid.appendHeaderRow();
        this.addGridFiltering(userGrid, hr, userFilter::setUsernameFilter, "username");
        this.addGridFiltering(userGrid, hr, userFilter::setNameFilter, "firstName");
        this.addGridFiltering(userGrid, hr, userFilter::setLastNameFilter, "surname");
        this.addGridFiltering(userGrid, hr, userFilter::setEmailFilter, "email");
        this.addGridFiltering(userGrid, hr, userFilter::setDepartmentFilter, "department");

        // The index of the first item to load
        // The number of items to load
        DataProvider<UserLite, UserFilter> userDataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<UserFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            if (filter.isPresent() && !query.getSortOrders().isEmpty()) {
                filter.get().setSortColumn(query.getSortOrders().get(0).getSorted());
                filter.get().setSortOrder(query.getSortOrders().get(0).getDirection().name());
            } else if (filter.isPresent()) {
                filter.get().setSortColumn(null);
                filter.get().setSortOrder(null);
            }

            return this.userService.getUsersWithoutRole(this.role.getName(), filter.isPresent() ? filter.get() : new UserFilterImpl()
                , limit, offset).stream();
        }, query -> {
            Optional<UserFilter> filter = query.getFilter();

            return this.userService.getUsersWithoutRoleCount(role.getName(), filter.isPresent() ? filter.get() : new UserFilterImpl());
        });

        userGridFilteredDataProvider = userDataProvider.withConfigurableFilter();
        userGridFilteredDataProvider.setFilter(userFilter);

        userGrid.setDataProvider(userGridFilteredDataProvider);

        userGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(selectUserLabel, userGrid);

        super.content.add(layout);
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

            if(this.userGridFilteredDataProvider != null) {
                this.userGridFilteredDataProvider.refreshAll();
            }
        });

        hr.getCell(grid.getColumnByKey(columnKey)).setComponent(textField);
    }

    private class UserFilterImpl implements UserFilter {
        private String username;
        private String name;
        private String lastName;
        private String email;
        private String department;
        private String sortColumn;
        private String sortOrder;

        @Override
        public void setUsernameFilter(String username) {
            this.username = username;
        }

        @Override
        public String getUsernameFilter() {
            return this.username;
        }

        @Override
        public void setNameFilter(String name) {
            this.name = name;
        }

        @Override
        public String getNameFilter() {
            return this.name;
        }

        @Override
        public void setLastNameFilter(String lastName) {
            this.lastName = lastName;
        }

        @Override
        public String getLastNameFilter() {
            return this.lastName;
        }

        @Override
        public void setEmailFilter(String email) {
            this.email = email;
        }

        @Override
        public String getEmailFilter() {
            return this.email;
        }

        @Override
        public void setDepartmentFilter(String department) {
            this.department = department;
        }

        @Override
        public String getDepartmentFilter() {
            return this.department;
        }

        @Override
        public void setSortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
        }

        @Override
        public String getSortColumn() {
            return this.sortColumn;
        }

        @Override
        public void setSortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
        }

        @Override
        public String getSortOrder() {
            return this.sortOrder;
        }
    }
}
