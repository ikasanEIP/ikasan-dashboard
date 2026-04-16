package org.ikasan.dashboard.ui.administration.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.administration.filter.PolicyFilter;
import org.ikasan.dashboard.ui.administration.filter.RoleJobPlanFilter;
import org.ikasan.dashboard.ui.administration.filter.RoleModuleFilter;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.FilteringGrid;
import org.ikasan.dashboard.ui.general.component.TableButton;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.spec.security.model.*;
import org.ikasan.spec.security.service.SecurityService;
import org.ikasan.spec.security.service.UserService;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.systemevent.SystemEventService;

import java.util.Optional;
import java.util.function.Consumer;


public class RoleManagementDialog extends AbstractCloseableResizableDialog
{
    private Role role;
    private final SecurityService securityService;
    private final SystemEventLogger systemEventLogger;
    private final UserService userService;
    private final ModuleMetaDataService moduleMetadataService;
    private Grid<UserLite> userGrid;
    private ConfigurableFilterDataProvider<UserLite,Void,UserFilter> userGridFilteredDataProvider;
    private Grid<IkasanPrincipalLite> groupGrid;
    private DataProvider<IkasanPrincipalLite, IkasanPrincipalFilter> groupDataProvider;
    private ConfigurableFilterDataProvider<IkasanPrincipalLite,Void,IkasanPrincipalFilter> groupGridFilteredDataProvider;
    private FilteringGrid<Policy> policyGrid;
    private FilteringGrid<RoleModule> roleModuleGrid;
    private FilteringGrid<RoleJobPlan> roleJobPlanGrid;
    private ScheduledContextService scheduledContextService;


    /**
     * Constructs a new {@code RoleManagementDialog} instance and initializes its components.
     * This dialog allows managing a specified role and its associated entities, including users,
     * groups, policies, integration modules, and job plans. The provided services are utilized
     * for role management and system event logging functionality.
     *
     * @param role the role to be managed; must not be null
     * @param securityService the security service for role operations; must not be null
     * @param userService the user service for managing associated users; must not be null
     * @param systemEventService the system event service for logging system events; must not be null
     * @param systemEventLogger the system event logger for logging system interactions; must not be null
     * @param moduleMetadataService the module metadata service for integration modules; must not be null
     * @param scheduledContextService the service for handling scheduled contexts; must not be null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public RoleManagementDialog(Role role, SecurityService securityService, UserService userService,
                                SystemEventService systemEventService, SystemEventLogger systemEventLogger,
                                ModuleMetaDataService moduleMetadataService, ScheduledContextService scheduledContextService)
    {
        this.role = role;
        if(this.role == null)
        {
            throw new IllegalArgumentException("Group cannot be null!");
        }
        this.securityService = securityService;
        if(this.securityService == null)
        {
            throw new IllegalArgumentException("securityService cannot be null!");
        }
        this.userService = userService;
        if(this.userService == null)
        {
            throw new IllegalArgumentException("userService cannot be null!");
        }
        if((SystemEventService) systemEventService == null)
        {
            throw new IllegalArgumentException("systemEventService cannot be null!");
        }
        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger == null)
        {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }
        this.moduleMetadataService = moduleMetadataService;
        if(this.moduleMetadataService == null)
        {
            throw new IllegalArgumentException("moduleMetadataService cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null)
        {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        init();
    }

    /**
     * Initialise this dialog
     */
    private void init()
    {
        super.title.setText(getTranslation("label.ikasan-roles", UI.getCurrent().getLocale()));

        this.role = this.securityService.getRoleById(role.getId());

        Accordion accordion = new Accordion();
        accordion.setWidthFull();
        accordion.add(getTranslation("accordian-label.associated-users", UI.getCurrent().getLocale())
            , createAssociatedUserLayout());
        accordion.add(getTranslation("accordian-label.associated-groups", UI.getCurrent().getLocale())
            , createAssociatedGroupsLayout());
        accordion.add(getTranslation("accordian-label.associated-policies", UI.getCurrent().getLocale())
            , createIkasanPoliciesLayout());
        accordion.add(getTranslation("accordian-label.associated-integration-modules", UI.getCurrent().getLocale())
            , this.createAssociatedIntegrationModules());
        accordion.add(getTranslation("accordian-label.associated-job-plans", UI.getCurrent().getLocale())
            , createAssociatedJobPlans());

        accordion.close();

        VerticalLayout layout = new VerticalLayout(initRoleForm(), accordion);
        layout.setSizeFull();
        this.setWidth("98vw");
        this.setHeight("98vh");
        this.content.setHeight("100%");
        this.content.add(layout);
    }

    /**
     * Create the policy layout
     *
     * @return layout containing the relevant policy components.
     */
    private VerticalLayout createIkasanPoliciesLayout()
    {
        H3 policyLabel = new H3(getTranslation("label.role-ikasan-policies", UI.getCurrent().getLocale()));

        PolicyFilter policyFilter = new PolicyFilter();

        this.policyGrid = new FilteringGrid<>(policyFilter);
        this.policyGrid.setId("policyGrid");
        this.policyGrid.setClassName("my-userGrid");
        this.policyGrid.addColumn(Policy::getName).setKey("name").setHeader(getTranslation("table-header.role-name", UI.getCurrent().getLocale(), null)).setSortable(true).setFlexGrow(1);
        this.policyGrid.addColumn(Policy::getDescription).setKey("description").setHeader(getTranslation("table-header.role-description", UI.getCurrent().getLocale(), null)).setSortable(true).setFlexGrow(4);
        this.policyGrid.addColumn(new ComponentRenderer<>(policy->
        {
            Button deleteButton = new TableButton(VaadinIcon.TRASH.create());
            deleteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
            {
                role = this.securityService.getRoleById(role.getId());
                role.getPolicies().remove(policy);
                securityService.saveRole(role);

                String action = String.format("Policy [%s] removed from role [%s]", policy.getName(), role.getName());

                this.systemEventLogger.logEvent(SystemEventConstants.DASHBOARD_PRINCIPAL_ROLE_CHANGED_CONSTANTS, action, null);

                this.updatePoliciesGrid();
            });

            deleteButton.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.POLICY_ADMINISTRATION_WRITE,
                SecurityConstants.POLICY_ADMINISTRATION_ADMIN, SecurityConstants.ALL_AUTHORITY));

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.add(deleteButton);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, deleteButton);
            return layout;
        })).setFlexGrow(1);

        HeaderRow hr = this.policyGrid.appendHeaderRow();
        this.policyGrid.addGridFiltering(hr, policyFilter::setNameFilter, "name");
        this.policyGrid.addGridFiltering(hr, policyFilter::setDescriptionFilter, "description");

        policyGrid.setSizeFull();

        Button addPolicyButton = new Button(getTranslation("button.add-policy", UI.getCurrent().getLocale(), null));
        addPolicyButton.setId("addPolicyButton");
        addPolicyButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
        {
            SelectPolicyForRoleDialog dialog = new SelectPolicyForRoleDialog(this.role, this.securityService
                , this.systemEventLogger, this.policyGrid);

            dialog.open();
        });

        this.updatePoliciesGrid();

        return this.layoutAssociatedEntityComponents(policyGrid, addPolicyButton, policyLabel);
    }

    /**
     * Helper method to update the policies grid.
     */
    private void updatePoliciesGrid()
    {
        this.policyGrid.setItems(role.getPolicies());
    }

    /**
     * Create the associated users layout
     *
     * @return layout containing the relevant assoicated users components.
     */
    private VerticalLayout createAssociatedUserLayout()
    {
        H3 associatedUsersLabel = new H3(getTranslation("label.role-associated-users", UI.getCurrent().getLocale(), null));

        UserFilter userFilter = new UserFilterImpl();

        this.userGrid = new Grid<>();

        userGrid.setClassName("my-userGrid");
        userGrid.addColumn(UserLite::getUsername).setKey("username").setHeader(getTranslation("table-header.username", UI.getCurrent().getLocale(), null)).setSortable(true).setFlexGrow(2);
        userGrid.addColumn(UserLite::getFirstName).setKey("firstname").setHeader(getTranslation("table-header.firstname", UI.getCurrent().getLocale(), null)).setSortable(true).setFlexGrow(2);
        userGrid.addColumn(UserLite::getSurname).setKey("surname").setHeader(getTranslation("table-header.surname", UI.getCurrent().getLocale(), null)).setSortable(true).setFlexGrow(4);
        userGrid.addColumn(UserLite::getEmail).setKey("email").setHeader(getTranslation("table-header.email", UI.getCurrent().getLocale(), null)).setSortable(true).setFlexGrow(4);
        userGrid.addColumn(UserLite::getDepartment).setKey("department").setHeader(getTranslation("table-header.department", UI.getCurrent().getLocale(), null)).setSortable(true);
        userGrid.addColumn(new ComponentRenderer<>(userLite->
        {
            Button deleteButton = new TableButton(VaadinIcon.TRASH.create());
            deleteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
            {
                IkasanPrincipal ikasanPrincipal = this.securityService.findPrincipalByName(userLite.getUsername());
                ikasanPrincipal.getRoles().remove(this.role);

                this.securityService.savePrincipal(ikasanPrincipal);

                String action = String.format("User [%s] removed from role [%s]", userLite.getUsername(), role.getName());

                this.systemEventLogger.logEvent(SystemEventConstants.DASHBOARD_PRINCIPAL_ROLE_CHANGED_CONSTANTS, action, null);

                this.updateAssociatedUsersGrid();
            });

            deleteButton.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.USER_ADMINISTRATION_WRITE,
                SecurityConstants.USER_ADMINISTRATION_ADMIN, SecurityConstants.ALL_AUTHORITY));

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.add(deleteButton);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, deleteButton);
            return layout;
        })).setFlexGrow(1);

        HeaderRow hr = userGrid.appendHeaderRow();
        this.addGridFiltering(this.userGrid, hr, userFilter::setUsernameFilter, "username");
        this.addGridFiltering(this.userGrid, hr, userFilter::setNameFilter, "firstname");
        this.addGridFiltering(this.userGrid, hr, userFilter::setLastNameFilter, "surname");
        this.addGridFiltering(this.userGrid, hr, userFilter::setEmailFilter, "email");
        this.addGridFiltering(this.userGrid, hr, userFilter::setDepartmentFilter, "department");

        Button addUser = new Button(getTranslation("button.add-user", UI.getCurrent().getLocale(), null));
        addUser.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
        {
            SelectUserForRoleDialog dialog = new SelectUserForRoleDialog(this.role, this.userService,
                this.securityService, this.systemEventLogger, this.userGrid);

            dialog.open();
        });

        // The index of the first item to load
        // The number of items to load
        DataProvider<UserLite, UserFilter> userDataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<UserFilter> filter = query.getFilter();

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

            return this.userService.getUsersWithRole(this.role.getName(), filter.get(), limit, offset).stream();
        }, query -> {
            Optional<UserFilter> filter = query.getFilter();

            return this.userService.getUsersWithRoleCount(role.getName(), filter.get());
        });

        userGridFilteredDataProvider = userDataProvider.withConfigurableFilter();
        userGridFilteredDataProvider.setFilter(userFilter);

        this.userGrid.setDataProvider(userGridFilteredDataProvider);

        userGrid.setSizeFull();

        this.updateAssociatedUsersGrid();

        return this.layoutAssociatedEntityComponents(userGrid, addUser, associatedUsersLabel);
    }

    /**
     * Helper method to update the associated users grid.
     */
    private void updateAssociatedUsersGrid()
    {
        this.userGrid.getDataProvider().refreshAll();
    }

    /**
     * Create the associated groups layout
     *
     * @return layout containing the relevant associated groups components.
     */
    private VerticalLayout createAssociatedGroupsLayout()
    {
        H3 associatedGroupsLabel = new H3(getTranslation("label.role-associated-groups", UI.getCurrent().getLocale(), null));

        IkasanPrincipalFilter groupFilter = new IkasanPrincipalFilterImpl();
        groupFilter.setTypeFilter("application");

        groupGrid = new Grid<>();
        groupGrid.setId("groupGrid");
        groupGrid.setClassName("my-userGrid");
        groupGrid.addColumn(IkasanPrincipalLite::getName).setKey("name").setHeader(getTranslation("table-header.group-name", UI.getCurrent().getLocale(), null)).setSortable(true);
        groupGrid.addColumn(IkasanPrincipalLite::getDescription).setKey("description").setHeader(getTranslation("table-header.group-description", UI.getCurrent().getLocale(), null)).setSortable(true);
        groupGrid.addColumn(new ComponentRenderer<>(principalLite->
        {
            Button deleteButton = new TableButton(VaadinIcon.TRASH.create());
            deleteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
            {
                IkasanPrincipal ikasanPrincipal = this.securityService.findPrincipalByName(principalLite.getName());
                ikasanPrincipal.getRoles().remove(this.role);

                this.securityService.savePrincipal(ikasanPrincipal);

                String action = String.format("Group [%s] removed from role [%s]", principalLite.getName(), role.getName());

                this.systemEventLogger.logEvent(SystemEventConstants.DASHBOARD_PRINCIPAL_ROLE_CHANGED_CONSTANTS, action, null);

                this.updateAssociatedGroupsGrid();
            });

            deleteButton.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.GROUP_ADMINISTRATION_WRITE,
                SecurityConstants.GROUP_ADMINISTRATION_ADMIN, SecurityConstants.ALL_AUTHORITY));

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.add(deleteButton);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, deleteButton);
            return layout;
        })).setFlexGrow(1);

        HeaderRow hr = groupGrid.appendHeaderRow();
        this.addGridFiltering(this.groupGrid, hr, groupFilter::setNameFilter, "name");
        this.addGridFiltering(this.groupGrid, hr, groupFilter::setDescriptionFilter, "description");

        Button addGroup = new Button(getTranslation("button.add-group", UI.getCurrent().getLocale(), null));
        addGroup.setId("addGroupButton");
        addGroup.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
        {
            SelectGroupForRoleDialog dialog = new SelectGroupForRoleDialog(this.role
                , this.securityService, this.systemEventLogger, this.groupGrid);
            dialog.addOpenedChangeListener((ComponentEventListener<OpenedChangeEvent>) dialogOpenedChangeEvent ->
            {
                if(dialogOpenedChangeEvent.isOpened() == false)
                {
                    this.updateAssociatedGroupsGrid();
                }
            });

            dialog.open();
        });

        groupDataProvider = DataProvider.fromFilteringCallbacks(query -> {
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

            return this.securityService.getAllPrincipalsWithRole(this.role.getName(), filter.get(), limit, offset).stream();
        }, query -> {
            Optional<IkasanPrincipalFilter> filter = query.getFilter();

            return this.securityService.getPrincipalsWithRoleCount(role.getName(), filter.get());
        });

        groupGridFilteredDataProvider = groupDataProvider.withConfigurableFilter();
        groupGridFilteredDataProvider.setFilter(groupFilter);

        this.groupGrid.setDataProvider(groupGridFilteredDataProvider);

        groupGrid.setSizeFull();

        return this.layoutAssociatedEntityComponents(groupGrid, addGroup, associatedGroupsLabel);
    }


    /**
     * Adds grid filtering functionality to a specified HeaderRow in a grid for a given column key
     *
     * @param hr the HeaderRow in which the filtering component will be added
     * @param setFilter a Consumer function to set the filter value
     * @param columnKey the key of the column in the grid to which the filter applies
     */
    public void addGridFiltering(Grid grid, HeaderRow hr, Consumer<String> setFilter, String columnKey)
    {
        TextField textField = new TextField();
        textField.setWidthFull();

        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());
            grid.getDataProvider().refreshAll();
        });

        hr.getCell(grid.getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * Helper method to update the associated groups grid.
     */
    private void updateAssociatedGroupsGrid()
    {
        this.groupGrid.getDataProvider().refreshAll();
    }

    /**
     * General layout for all associated entities.
     *
     * @param grid
     * @param button
     * @param label
     *
     * @return the general layout
     */
    private VerticalLayout layoutAssociatedEntityComponents(Grid grid, Button button, H3 label)
    {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(button);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, button);

        ComponentSecurityVisibility.applySecurity(button, SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.USER_ADMINISTRATION_ADMIN
            , SecurityConstants.USER_ADMINISTRATION_WRITE);

        HorizontalLayout labelLayout = new HorizontalLayout();
        labelLayout.setWidthFull();
        labelLayout.add(label);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.add(labelLayout, buttonLayout);

        VerticalLayout layout = new VerticalLayout();
        layout.add(headerLayout, grid);
        layout.setWidth("100%");
        layout.setHeight("600px");
        return layout;
    }

    /**
     * Create the associated integration modules
     *
     * @return layout containing the relevant associated integration module components.
     */
    private VerticalLayout createAssociatedIntegrationModules()
    {
        VerticalLayout verticalLayout = new VerticalLayout();

        H3 associatedRoleModulesLabel = new H3(getTranslation("label.role-associated-integration-modules", UI.getCurrent().getLocale(), null));

        verticalLayout.add(associatedRoleModulesLabel);

        RoleModuleFilter roleModuleFilter = new RoleModuleFilter();

        this.roleModuleGrid = new FilteringGrid<>(roleModuleFilter);
        this.roleModuleGrid.setClassName("my-userGrid");
        this.roleModuleGrid.addColumn(RoleModule::getModuleName).setKey("name").setHeader(getTranslation("table-header.moduleName", UI.getCurrent().getLocale(), null)).setSortable(true).setFlexGrow(2);
        this.roleModuleGrid.addColumn(new ComponentRenderer<>(roleModule->
        {
            Button deleteButton = new TableButton(VaadinIcon.TRASH.create());
            deleteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
            {
                this.role = this.securityService.getRoleById(role.getId());
                this.role.getRoleModules().remove(roleModule);
                this.securityService.saveRole(role);

                String action = String.format("Module [%s] removed from role [%s]", roleModule.getModuleName(), role.getName());

                this.systemEventLogger.logEvent(SystemEventConstants.DASHBOARD_MODULE_ROLE_CHANGE_CONSTANTS, action, null);

                this.updateRoleModuleGrid();
            });

            deleteButton.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ROLE_ADMINISTRATION_WRITE,
                SecurityConstants.ROLE_ADMINISTRATION_WRITE, SecurityConstants.ALL_AUTHORITY));

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.add(deleteButton);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, deleteButton);
            return layout;
        })).setFlexGrow(1);

        HeaderRow hr = roleModuleGrid.appendHeaderRow();
        this.roleModuleGrid.addGridFiltering(hr, roleModuleFilter::setModuleNameFilter, "name");

        Button addModule = new Button(getTranslation("button.add-role-module", UI.getCurrent().getLocale(), null));
        addModule.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
        {
            SelectModuleForRoleDialog dialog = new SelectModuleForRoleDialog(this.role, this.moduleMetadataService,
                this.securityService, this.systemEventLogger, this.roleModuleGrid);

            dialog.open();
        });

        userGrid.setSizeFull();

        this.updateRoleModuleGrid();

        return this.layoutAssociatedEntityComponents(this.roleModuleGrid, addModule, associatedRoleModulesLabel);
    }

    /**
     * Create the associated jobs plans
     *
     * @return layout containing the relevant associated job plans components.
     */
    private VerticalLayout createAssociatedJobPlans()
    {
        VerticalLayout verticalLayout = new VerticalLayout();

        H3 associatedRoleModulesLabel = new H3(getTranslation("label.role-associated-job-plans", UI.getCurrent().getLocale(), null));

        verticalLayout.add(associatedRoleModulesLabel);

        RoleJobPlanFilter roleJobPlanFilter = new RoleJobPlanFilter();

        this.roleJobPlanGrid = new FilteringGrid<>(roleJobPlanFilter);
        this.roleJobPlanGrid.setClassName("my-userGrid");
        this.roleJobPlanGrid.addColumn(RoleJobPlan::getJobPlanName).setKey("name").setHeader(getTranslation("table-header.moduleName", UI.getCurrent().getLocale(), null)).setSortable(true).setFlexGrow(2);
        this.roleJobPlanGrid.addColumn(new ComponentRenderer<>(roleJobPlan->
        {
            Button deleteButton = new TableButton(VaadinIcon.TRASH.create());
            deleteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
            {
                this.role = this.securityService.getRoleById(role.getId());
                this.role.getRoleJobPlans().remove(roleJobPlan);
                this.securityService.saveRole(role);
                this.securityService.deleteRoleJobPlan(roleJobPlan);

                String action = String.format("Job plan [%s] removed from role [%s]", roleJobPlan.getJobPlanName(), role.getName());

                this.systemEventLogger.logEvent(SystemEventConstants.DASHBOARD_JOB_PLAN_ROLE_CHANGE_CONSTANTS, action, null);

                this.updateRoleJobPlanGrid();
            });

            deleteButton.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ROLE_ADMINISTRATION_WRITE,
                SecurityConstants.ROLE_ADMINISTRATION_WRITE, SecurityConstants.ALL_AUTHORITY));

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.add(deleteButton);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, deleteButton);
            return layout;
        })).setFlexGrow(1);

        HeaderRow hr = roleJobPlanGrid.appendHeaderRow();
        this.roleJobPlanGrid.addGridFiltering(hr, roleJobPlanFilter::setModuleNameFilter, "name");

        Button addJobPlan = new Button(getTranslation("button.add-job-plan", UI.getCurrent().getLocale(), null));
        addJobPlan.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
        {
            SelectJobPlanForRoleDialog dialog = new SelectJobPlanForRoleDialog(this.role, this.scheduledContextService,
                this.securityService, this.systemEventLogger, this.roleJobPlanGrid);

            dialog.open();
        });

        userGrid.setSizeFull();

        this.updateRoleJobPlanGrid();

        return this.layoutAssociatedEntityComponents(this.roleJobPlanGrid, addJobPlan, associatedRoleModulesLabel);
    }

    protected void updateRoleModuleGrid()
    {
        this.roleModuleGrid.setItems(this.role.getRoleModules());
    }

    protected void updateRoleJobPlanGrid()
    {
        this.roleJobPlanGrid.setItems(this.role.getRoleJobPlans());
    }

    /**
     * Init the role form.
     *
     * @return
     */
    private VerticalLayout initRoleForm()
    {
        H3 userProfileLabel = new H3(String.format(getTranslation("label.role-profile", UI.getCurrent().getLocale(), null), this.role.getName()));

        FormLayout formLayout = new FormLayout();

        TextField groupName = new TextField(getTranslation("text-field.group-name", UI.getCurrent().getLocale(), null));
        groupName.setReadOnly(true);
        groupName.setValue(this.role.getName());
        formLayout.add(groupName);
        formLayout.setColspan(groupName, 2);

        TextArea description = new TextArea(getTranslation("text-field.group-description", UI.getCurrent().getLocale(), null));
        description.setReadOnly(true);
        description.setValue(this.role.getDescription());
        description.setHeight("130px");
        formLayout.add(description);
        formLayout.setColspan(description, 2);

        Div result = new Div();
        result.add(formLayout);
        result.setSizeFull();

        formLayout.setSizeFull();

        VerticalLayout layout = new VerticalLayout();
        layout.add(userProfileLabel, formLayout);
        return layout;
    }
    
    private class UserFilterImpl implements UserFilter {
        private String usernameFilter;
        private String nameFilter;
        private String lastNameFilter;
        private String emailFilter;
        private String departmentFilter;
        private String sortColumn;
        private String sortOrder;

        @Override
        public String getUsernameFilter() {
            return usernameFilter;
        }

        @Override
        public void setUsernameFilter(String usernameFilter) {
            this.usernameFilter = usernameFilter;
        }

        @Override
        public String getNameFilter() {
            return nameFilter;
        }

        @Override
        public void setNameFilter(String nameFilter) {
            this.nameFilter = nameFilter;
        }

        @Override
        public String getLastNameFilter() {
            return lastNameFilter;
        }

        @Override
        public void setLastNameFilter(String lastNameFilter) {
            this.lastNameFilter = lastNameFilter;
        }

        @Override
        public String getEmailFilter() {
            return emailFilter;
        }

        @Override
        public void setEmailFilter(String emailFilter) {
            this.emailFilter = emailFilter;
        }

        @Override
        public String getDepartmentFilter() {
            return departmentFilter;
        }

        @Override
        public void setDepartmentFilter(String departmentFilter) {
            this.departmentFilter = departmentFilter;
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
