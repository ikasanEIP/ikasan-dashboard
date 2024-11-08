package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.administration.component.SelectGroupDialog;
import org.ikasan.dashboard.ui.administration.component.SelectUserDialog;
import org.ikasan.dashboard.ui.administration.filter.GroupFilter;
import org.ikasan.dashboard.ui.administration.filter.UserLiteFilter;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.FilteringGrid;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.ContextFilter;
import org.ikasan.dashboard.ui.scheduler.util.ContextViewUpdateEventBroadcaster;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.model.profile.ContextProfileImpl;
import org.ikasan.job.orchestration.model.profile.ContextProfileRecordImpl;
import org.ikasan.scheduled.profile.model.SolrContextProfileSearchFilterImpl;
import org.ikasan.security.model.IkasanPrincipalLite;
import org.ikasan.security.model.UserLite;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;

public class ContextViewManagementDialog extends AbstractCloseableResizableDialog {

    private ContextProfileService contextProfileService;
    private UserService userService;
    private SecurityService securityService;
    private SystemEventLogger systemEventLogger;

    private FilteringGrid<UserLite> userGrid;
    private FilteringGrid<IkasanPrincipalLite> groupGrid;
    private FilteringGrid<String> contextGrid;

    private ContextProfileRecord contextProfileRecord;

    private String parentContextName;
    private String contextName;

    public ContextViewManagementDialog(ContextProfileService contextProfileService, UserService userService,
                                       SecurityService securityService, SystemEventLogger systemEventLogger,
                                       String parentContextName, String contextName) {
        this.contextProfileService = contextProfileService;
        this.userService = userService;
        this.securityService = securityService;
        this.systemEventLogger = systemEventLogger;
        this.parentContextName = parentContextName;
        this.contextName = contextName;

        super.setResizable(false);

        this.init();
    }

    private void init() {
        super.setHeight("200px");
        super.setWidth("700px");
        super.showResize(false);
        super.title.setText(getTranslation("label.context-profile-management", UI.getCurrent().getLocale()));

        BeanValidationBinder<ContextProfileRecord> binder = new BeanValidationBinder<>(ContextProfileRecord.class);

        FormLayout formLayout = new FormLayout();

        formLayout.setResponsiveSteps(
            // Use four columns by default
            new FormLayout.ResponsiveStep("0", 4)
        );

        formLayout.setWidth("100%");

        Select<ContextProfileRecord> profileNames = new Select<>();
        profileNames.setLabel(getTranslation("label.profiles", UI.getCurrent().getLocale()));
        profileNames.setWidth("450px");

        ContextProfileSearchFilter contextProfileSearchFilter = new SolrContextProfileSearchFilterImpl();
        contextProfileSearchFilter.setOwner(SecurityContextHolder.getContext().getAuthentication().getName());
        contextProfileSearchFilter.setContextName(this.parentContextName);

        SearchResults<ContextProfileRecord> contextProfiles = this.contextProfileService
            .findByFilter(contextProfileSearchFilter, -1, -1, null, null);

        profileNames.setItems(contextProfiles.getResultList());
        profileNames.setItemLabelGenerator(ContextProfileRecord::getProfileName);

        TextField profileName = new TextField(getTranslation("label.profiles-name", UI.getCurrent().getLocale()));
        profileName.setVisible(false);
        profileName.setRequired(true);
        profileName.setWidth("100%");

        binder.forField(profileName)
            .withValidator(name -> name != null && !name.isEmpty(), getTranslation("error.profile-name-required", UI.getCurrent().getLocale()))
            .bind(ContextProfileRecord::getProfileName, ContextProfileRecord::setProfileName);

        Button newContextProfile = new Button(getTranslation("button.new-profile", UI.getCurrent().getLocale()), VaadinIcon.PLUS.create());
        newContextProfile.setIconAfterText(true);
        newContextProfile.getElement().getStyle().set("position", "absolute");
        newContextProfile.getElement().getStyle().set("right", "10px");
        newContextProfile.getElement().getStyle().set("top", "80px");

        Accordion accordion = new Accordion();

        HorizontalLayout buttonLayout = new HorizontalLayout();

        Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
        saveButton.setVisible(false);
        saveButton.addClickListener(event -> {
            try {
                binder.writeBean(this.contextProfileRecord);
            }
            catch (ValidationException e) {
                NotificationHelper.showErrorNotification(getTranslation("error.could-not-save-context", UI.getCurrent().getLocale()));
                return;
            }

            this.contextProfileService.save(this.contextProfileRecord);
            NotificationHelper.showUserNotification(getTranslation("notification.context-profile-saved", UI.getCurrent().getLocale()));

            this.close();
            ContextViewUpdateEventBroadcaster.broadcast("update!");
        });

        Button cancelButton = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale()));
        cancelButton.addClickListener(event -> this.close());
        cancelButton.setVisible(false);

        buttonLayout.add(saveButton, cancelButton);
        buttonLayout.getElement().getStyle().set("margin-top", "30px");
        buttonLayout.getElement().getStyle().set("position", "absolute");
        buttonLayout.getElement().getStyle().set("bottom", "30px");
        
        newContextProfile.addClickListener(event -> {
            profileName.setVisible(true);
            accordion.setVisible(true);

            super.setHeight("1100px");
            super.setWidth("1350px");

            profileNames.setVisible(false);
            newContextProfile.setVisible(false);

            this.contextProfileRecord = new ContextProfileRecordImpl();
            this.contextProfileRecord.setContextName(this.parentContextName);
            this.contextProfileRecord.setOwner(SecurityContextHolder.getContext().getAuthentication().getName());
            this.contextProfileRecord.setAccessUsers(List.of());
            this.contextProfileRecord.setAccessGroups(List.of());

            ContextProfile contextProfile = new ContextProfileImpl();
            contextProfile.setSubContexts(List.of(contextName));

            this.contextProfileRecord.setContextProfile(contextProfile);

            binder.readBean(contextProfileRecord);

            saveButton.setVisible(true);
            cancelButton.setVisible(true);

            this.updateAssociatedContextsGrid();
            this.updateAssociatedGroupsGrid();
            this.updateAssociatedUsersGrid();
        });


        formLayout.add(profileNames, 3);
        formLayout.add(newContextProfile, 1);
        formLayout.add(profileName, 4);

        accordion.add(getTranslation("accordian-label.associated-contexts", UI.getCurrent().getLocale()), this.createAssociatedContextsLayout());
        accordion.add(getTranslation("accordian-label.associated-users", UI.getCurrent().getLocale()), this.createAssociatedUserLayout());
        accordion.add(getTranslation("accordian-label.associated-groups", UI.getCurrent().getLocale()), this.createAssociatedGroupsLayout());

        accordion.setVisible(false);

        formLayout.add(accordion, 4);

        profileNames.addValueChangeListener(event -> {
            this.contextProfileRecord = event.getValue();
            if(this.contextProfileRecord != null) {
                binder.readBean(contextProfileRecord);
                accordion.setVisible(true);
                super.setHeight("1100px");
                super.setWidth("1350px");

                ContextProfile contextProfile = this.contextProfileRecord.getContextProfile();
                List<String> subContexts = contextProfile.getSubContexts();
                subContexts.add(this.contextName);
                contextProfile.setSubContexts(subContexts);
                this.contextProfileRecord.setContextProfile(contextProfile);

                profileName.setVisible(true);
                profileName.setEnabled(false);

                profileNames.setVisible(false);
                newContextProfile.setVisible(false);

                saveButton.setVisible(true);
                cancelButton.setVisible(true);

                this.updateAssociatedUsersGrid();
                this.updateAssociatedContextsGrid();
                this.updateAssociatedGroupsGrid();
            }
        });

        super.content.add(formLayout);


        super.content.add(buttonLayout);
        super.content.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);
    }

    /**
     * Create the associated users layout
     *
     * @return layout containing the relevant assoicated users components.
     */
    private VerticalLayout createAssociatedUserLayout()
    {
        H3 associatedUsersLabel = new H3(getTranslation("label.role-associated-users", UI.getCurrent().getLocale(), null));

        UserLiteFilter userLiteFilter = new UserLiteFilter();

        this.userGrid = new org.ikasan.dashboard.ui.general.component.FilteringGrid<>(userLiteFilter);

        userGrid.addColumn(UserLite::getUsername).setKey("username").setHeader(getTranslation("table-header.username", UI.getCurrent().getLocale(), null)).setSortable(true).setFlexGrow(2);
        userGrid.addColumn(UserLite::getFirstName).setKey("firstname").setHeader(getTranslation("table-header.firstname", UI.getCurrent().getLocale(), null)).setSortable(true).setFlexGrow(2);
        userGrid.addColumn(UserLite::getSurname).setKey("surname").setHeader(getTranslation("table-header.surname", UI.getCurrent().getLocale(), null)).setSortable(true).setFlexGrow(4);
        userGrid.addColumn(UserLite::getEmail).setKey("email").setHeader(getTranslation("table-header.email", UI.getCurrent().getLocale(), null)).setSortable(true).setFlexGrow(4);
        userGrid.addColumn(UserLite::getDepartment).setKey("department").setHeader(getTranslation("table-header.department", UI.getCurrent().getLocale(), null)).setSortable(true);
        userGrid.addColumn(new ComponentRenderer<>(userLite-> {
            Icon deleteButton = IconDecorator.decorate(VaadinIcon.TRASH.create(), "", "14pt", IkasanColours.IKASAN_ORANGE);
            deleteButton.addClickListener(buttonClickEvent -> {
                List<String> users = this.contextProfileRecord.getAccessUsers();
                users.remove(userLite.getUsername());
                this.contextProfileRecord.setAccessUsers(users);
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
        this.userGrid.addGridFiltering(hr, userLiteFilter::setUsernameFilter, "username");
        this.userGrid.addGridFiltering(hr, userLiteFilter::setNameFilter, "firstname");
        this.userGrid.addGridFiltering(hr, userLiteFilter::setLastNameFilter, "surname");
        this.userGrid.addGridFiltering(hr, userLiteFilter::setEmailFilter, "email");
        this.userGrid.addGridFiltering(hr, userLiteFilter::setDepartmentFilter, "department");

        Button addUser = new Button(getTranslation("button.add-user", UI.getCurrent().getLocale(), null));
        addUser.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
        {
            SelectUserDialog dialog = new SelectUserDialog(this.userService, this.contextProfileRecord.getAccessUsers(),
                this.securityService, this.systemEventLogger, this.userGrid);

            dialog.open();

            dialog.addOpenedChangeListener(event -> {
                if(!event.isOpened()) {
                    List<String> accessUsers = this.userGrid.getItems().stream()
                        .map(userLite -> userLite.getUsername())
                        .collect(Collectors.toList());

                    this.contextProfileRecord.setAccessUsers(accessUsers);
                }
            });
        });

        userGrid.setSizeFull();

        return this.layoutAssociatedEntityComponents(userGrid, addUser, associatedUsersLabel);
    }

    /**
     * Helper method to update the users grid.
     */
    private void updateAssociatedUsersGrid()
    {
        this.userGrid.setItems(this.userService.getUserLites().stream()
            .filter(user -> this.contextProfileRecord.getAccessUsers().contains(user.getUsername()))
            .collect(Collectors.toList()));
    }

    /**
     * Create the associated groups layout
     *
     * @return layout containing the relevant associated groups components.
     */
    private VerticalLayout createAssociatedContextsLayout()
    {
        H3 associatedContextsLabel = new H3(getTranslation("accordian-label.associated-contexts", UI.getCurrent().getLocale()));

        ContextFilter contextFilter = new ContextFilter();

        contextGrid = new FilteringGrid<>(contextFilter);
        contextGrid.addColumn(String::toString).setKey("name").setHeader(getTranslation("table-header.context-name", UI.getCurrent().getLocale()))
            .setSortable(true)
            .setFlexGrow(5);
        contextGrid.addColumn(new ComponentRenderer<>(context->
        {
            Icon deleteButton = IconDecorator.decorate(VaadinIcon.TRASH.create(), "", "14pt", IkasanColours.IKASAN_ORANGE);
            deleteButton.addClickListener(buttonClickEvent -> {
                contextGrid.getItems().remove(context);
                contextGrid.getDataProvider().refreshAll();
            });

            deleteButton.setVisible(ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.GROUP_ADMINISTRATION_WRITE,
                SecurityConstants.GROUP_ADMINISTRATION_ADMIN, SecurityConstants.ALL_AUTHORITY));

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.add(deleteButton);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, deleteButton);
            return layout;
        })).setFlexGrow(1);

        HeaderRow hr = contextGrid.appendHeaderRow();
        this.contextGrid.addGridFiltering(hr, contextFilter::setNameFilter, "name");

        Button addContextButton = new Button();
        addContextButton.setVisible(false);

        contextGrid.setSizeFull();

        return this.layoutAssociatedEntityComponents(contextGrid, addContextButton, associatedContextsLabel);
    }

    /**
     * Helper method to update the users grid.
     */
    private void updateAssociatedContextsGrid()
    {
        this.contextGrid.setItems(this.contextProfileRecord.getContextProfile().getSubContexts());
    }

    /**
     * Create the associated groups layout
     *
     * @return layout containing the relevant associated groups components.
     */
    private VerticalLayout createAssociatedGroupsLayout()
    {
        H3 associatedGroupsLabel = new H3(getTranslation("label.role-associated-groups", UI.getCurrent().getLocale(), null));

        GroupFilter groupFilter = new GroupFilter();

        groupGrid = new FilteringGrid<>(groupFilter);
        groupGrid.addColumn(IkasanPrincipalLite::getName).setKey("name").setHeader(getTranslation("table-header.group-name", UI.getCurrent().getLocale(), null)).setSortable(true);
        groupGrid.addColumn(IkasanPrincipalLite::getDescription).setKey("description").setHeader(getTranslation("table-header.group-description", UI.getCurrent().getLocale(), null)).setSortable(true);
        groupGrid.addColumn(new ComponentRenderer<>(principalLite->
        {
            Icon deleteButton = IconDecorator.decorate(VaadinIcon.TRASH.create(), "", "14pt", IkasanColours.IKASAN_ORANGE);
            deleteButton.addClickListener(buttonClickEvent -> {
                List<String> accessGroups = this.contextProfileRecord.getAccessGroups();
                accessGroups.remove(principalLite.getName());
                this.contextProfileRecord.setAccessGroups(accessGroups);
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
        this.groupGrid.addGridFiltering(hr, groupFilter::setNameFilter, "name");
        this.groupGrid.addGridFiltering(hr, groupFilter::setDescriptionFilter, "description");

        Button addGroup = new Button(getTranslation("button.add-group", UI.getCurrent().getLocale(), null));
        addGroup.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            SelectGroupDialog dialog = new SelectGroupDialog(this.contextProfileRecord.getAccessGroups()
                , this.securityService, this.systemEventLogger, this.groupGrid);

            dialog.open();

            dialog.addOpenedChangeListener(event -> {
                if(!event.isOpened()) {
                    List<String> accessGroups = this.groupGrid.getItems().stream()
                        .map(ikasanPrincipalLite -> ikasanPrincipalLite.getName())
                        .collect(Collectors.toList());

                    this.contextProfileRecord.setAccessGroups(accessGroups);
                }
            });
        });

        groupGrid.setSizeFull();

        return this.layoutAssociatedEntityComponents(groupGrid, addGroup, associatedGroupsLabel);
    }

    /**
     * Helper method to update the users grid.
     */
    private void updateAssociatedGroupsGrid()
    {
        this.groupGrid.setItems(this.securityService.getAllPrincipalLites().stream()
            .filter(principalLite -> this.contextProfileRecord.getAccessGroups().contains(principalLite.getName()))
            .collect(Collectors.toList()));
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
        buttonLayout.setMargin(false);
        buttonLayout.setSpacing(false);
        buttonLayout.add(button);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.END, button);
        buttonLayout.getThemeList().remove("spacing");

        ComponentSecurityVisibility.applySecurity(button, SecurityConstants.ALL_AUTHORITY
            , SecurityConstants.USER_ADMINISTRATION_ADMIN
            , SecurityConstants.USER_ADMINISTRATION_WRITE);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setMargin(false);
        headerLayout.add(buttonLayout);

        VerticalLayout layout = new VerticalLayout();
        layout.add(headerLayout, grid);
        layout.setWidth("100%");
        layout.setHeight("600px");
        layout.setMargin(false);
        layout.setSpacing(false);
        layout.getThemeList().remove("padding");
        return layout;
    }
}
