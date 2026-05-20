package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.componentfactory.Tooltip;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.RouteScope;
import org.ikasan.dashboard.ui.administration.component.NewUserDialog;
import org.ikasan.dashboard.ui.administration.component.UserManagementDialog;
import org.ikasan.dashboard.ui.general.component.TooltipHelper;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.spec.security.model.User;
import org.ikasan.spec.security.model.UserFilter;
import org.ikasan.spec.security.service.SecurityService;
import org.ikasan.spec.security.service.UserService;
import org.ikasan.spec.systemevent.SystemEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.security.PermitAll;
import java.util.Optional;
import java.util.function.Consumer;

@Route(value = "userManagement", layout = IkasanAppLayout.class)
@RouteScope
@PageTitle("Ikasan - User Management")
@PermitAll
@PreserveOnRefresh
@Component
public class UserManagementView extends VerticalLayout implements BeforeEnterObserver
{
    private Logger logger = LoggerFactory.getLogger(UserManagementView.class);

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SystemEventService systemEventService;

    @Autowired
    private SystemEventLogger systemEventLogger;

    @Autowired
    private  DateFormatter dateFormatter;

    private Grid<User> userGrid;

    private DataProvider<User, UserFilter> dataProvider;
    private ConfigurableFilterDataProvider<User,Void,UserFilter> filteredDataProvider;

    private UserFilter userFilter = new UserFilterImpl();

    private Tooltip newUserTooltip;
    private Button addNewUserButton;

    /**
     * Constructor
     */
    public UserManagementView()
    {
        super();
        init();
    }

    protected void init()
    {
        this.setSizeFull();
        this.setSpacing(true);

        H2 userDirectoriesLabel = new H2(getTranslation("label.user-management", UI.getCurrent().getLocale(), null));

        HorizontalLayout labelLayout = new HorizontalLayout();
        labelLayout.setJustifyContentMode(JustifyContentMode.START);
        labelLayout.setVerticalComponentAlignment(Alignment.CENTER, userDirectoriesLabel);
        labelLayout.setWidth("100%");
        labelLayout.add(userDirectoriesLabel);

        this.addNewUserButton = new Button(VaadinIcon.PLUS.create());
        this.addNewUserButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            NewUserDialog newUserDialog = new NewUserDialog(this.userService,  this.systemEventLogger, this.securityService);
            newUserDialog.open();
            newUserDialog.addOpenedChangeListener(dialogOpenedChangeEvent ->
            {
                if(dialogOpenedChangeEvent.isOpened() == false)
                {
                    this.dataProvider.refreshAll();
                }
            });
        });

        this.addNewUserButton.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.USER_ADMINISTRATION_WRITE,
            SecurityConstants.USER_ADMINISTRATION_ADMIN, SecurityConstants.ALL_AUTHORITY));

        this.newUserTooltip = TooltipHelper.getTooltipForComponentBottom(this.addNewUserButton, getTranslation("tooltip.add-new-user"
            , UI.getCurrent().getLocale()));

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
        buttonLayout.setMargin(true);
        buttonLayout.setVerticalComponentAlignment(Alignment.CENTER, this.addNewUserButton);
        buttonLayout.setWidth("100%");
        buttonLayout.add(this.addNewUserButton, newUserTooltip);

        HorizontalLayout wrapperLayout = new HorizontalLayout();
        wrapperLayout.setWidth("100%");

        wrapperLayout.add(labelLayout, buttonLayout);
        add(wrapperLayout);

        this.userGrid = new Grid<>();
        this.userGrid.setSizeFull();
        this.userGrid.setClassName("my-grid");

        this.userGrid.addColumn(User::getUsername)
            .setKey("username")
            .setHeader(getTranslation("table-header.username", UI.getCurrent().getLocale(), null))
            .setSortable(true)
            .setFlexGrow(1);
        this.userGrid.addColumn(User::getFirstName)
            .setKey("firstName")
            .setHeader(getTranslation("table-header.firstname", UI.getCurrent().getLocale(), null))
            .setSortable(true)
            .setFlexGrow(1);
        this.userGrid.addColumn(User::getSurname)
            .setKey("surname")
            .setHeader(getTranslation("table-header.surname", UI.getCurrent().getLocale(), null))
            .setSortable(true)
            .setFlexGrow(1);
        this.userGrid.addColumn(User::getEmail).setKey("email")
            .setHeader(getTranslation("table-header.email", UI.getCurrent().getLocale(), null))
            .setFlexGrow(2)
            .setSortable(true);
        this.userGrid.addColumn(User::getDepartment)
            .setKey("department").setHeader(getTranslation("table-header.department", UI.getCurrent().getLocale(), null))
            .setSortable(true)
            .setFlexGrow(1);
        this.userGrid.addColumn(LitRenderer.<User>of(
            "<div style='white-space:normal'>${item.date}</div>")
            .withProperty("date",
                user -> this.dateFormatter.getFormattedDate(user.getPreviousAccessTimestamp())))
            .setKey("previousAccessTimestamp")
            .setHeader(getTranslation("table-header.last-access", UI.getCurrent().getLocale(), null))
            .setSortable(true)
            .setWidth("90px");

        HeaderRow hr = userGrid.appendHeaderRow();
        this.addGridFiltering(hr, userFilter::setUsernameFilter, "username");
        this.addGridFiltering(hr, userFilter::setNameFilter, "firstName");
        this.addGridFiltering(hr, userFilter::setLastNameFilter, "surname");
        this.addGridFiltering(hr, userFilter::setEmailFilter, "email");
        this.addGridFiltering(hr, userFilter::setDepartmentFilter, "department");

        this.userGrid.addItemDoubleClickListener((ComponentEventListener<ItemDoubleClickEvent<User>>) userItemDoubleClickEvent ->
        {
            UserManagementDialog dialog = new UserManagementDialog(userItemDoubleClickEvent.getItem(), userService
                , this.securityService, this.systemEventService, this.systemEventLogger);

            dialog.open();
        });

        dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<UserFilter> filter = query.getFilter();

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

            return this.userService.getUsers(filter.get(), limit, offset).stream();
        }, query -> {
            Optional<UserFilter> filter = query.getFilter();

            return this.userService.getUserCount(filter.get());
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.userFilter);

        this.userGrid.setDataProvider(filteredDataProvider);

        add(this.userGrid);
    }
    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        if(!ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.USER_ADMINISTRATION_ADMIN, SecurityConstants.USER_ADMINISTRATION_WRITE,  SecurityConstants.USER_ADMINISTRATION_READ,
            SecurityConstants.ALL_AUTHORITY)) {
            DashboardContextNavigator.navigateToLandingPage();
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

            if(filteredDataProvider != null) {
                filteredDataProvider.refreshAll();
            }
        });

        hr.getCell(userGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)
    {
        super.onAttach(attachEvent);
        this.newUserTooltip.attachToComponent(this.addNewUserButton);
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
