package org.ikasan.dashboard.ui.administration.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.service.SecurityService;

public class NewRoleDialog extends AbstractCloseableResizableDialog
{
    private static final Logger logger = LoggerFactory.getLogger(NewRoleDialog.class);

    private SecurityService securityService;
    private SystemEventLogger systemEventLogger;

    public NewRoleDialog(SecurityService securityService, SystemEventLogger systemEventLogger)
    {
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
        init();
    }

    private void init()
    {
        H3 newRoleLabel = new H3(getTranslation("label.new-role", UI.getCurrent().getLocale(), null));

        FormLayout formLayout = new FormLayout();

        Binder<Role> binder = new Binder<>(Role.class);

        TextField nameTf = new TextField(getTranslation("text-field.new-role-name", UI.getCurrent().getLocale(), null));
        nameTf.setId("nameTf");
        binder.forField(nameTf)
            .withValidator(name -> name != null && name.length() > 0, getTranslation("message.new-role-name-missing", UI.getCurrent().getLocale(), null))
            .bind(Role::getName, Role::setName);
        formLayout.add(nameTf);
        formLayout.setColspan(nameTf, 2);

        TextArea descriptionTf = new TextArea(getTranslation("text-field.new-role-description", UI.getCurrent().getLocale(), null));
        descriptionTf.setId("descriptionTf");
        binder.forField(descriptionTf)
            .withValidator(description -> description != null && description.length() > 0, getTranslation("message.new-role-description-missing", UI.getCurrent().getLocale(), null))
            .bind(Role::getDescription, Role::setDescription);
        descriptionTf.setHeight("150px");

        Role role = this.securityService.createRole();
        binder.readBean(role);

        formLayout.add(descriptionTf);
        formLayout.setColspan(descriptionTf, 2);

        Div result = new Div();
        result.add(formLayout);
        result.setSizeFull();

        formLayout.setSizeFull();

        Button save = new Button(getTranslation("button.save", UI.getCurrent().getLocale(), null));
        save.setId("saveButton");
        save.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
        {
            try
            {
                binder.writeBean(role);

                // Role ids are derived from the role name, so saving a role whose name is already taken would
                // silently overwrite the existing role (clearing its policies) rather than fail.
                if(roleNameExists(role.getName()))
                {
                    NotificationHelper.showErrorNotification(getTranslation("message.role-name-must-be-unique",
                        UI.getCurrent().getLocale(), null));
                    return;
                }

                this.securityService.saveRole(role);

                this.systemEventLogger.logEvent(SystemEventConstants.DASHBOARD_ROLE_ADDED
                    , "New role " + role.getName() + " added.", null);

                this.close();
            }
            catch (ValidationException e)
            {
                logger.debug("New role form failed validation", e);
                // Ignore as the form will provide feedback to the user via the validation mechanism.
            }
            catch (Exception e)
            {
                logger.error("Could not save new role", e);
                NotificationHelper.showErrorNotification(String.format(getTranslation("message.role-name-must-be-unique", UI.getCurrent().getLocale(), null)));
            }
        });

        Button cancel = new Button(getTranslation("button.cancel", UI.getCurrent().getLocale(), null));
        cancel.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            this.close();
        });

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(save, cancel);

        VerticalLayout layout = new VerticalLayout();
        layout.add(newRoleLabel, formLayout, buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);
        this.content.add(layout);
        this.setHeight("475px");
        this.setWidth("600px");
    }

    /**
     * Is the name already used by a role? Compares names exactly against all roles: name lookups on some
     * backends are token based (a lookup of 'admin' would also match 'super-admin').
     *
     * @param name the new role's name
     * @return true if a role with exactly this name exists
     */
    private boolean roleNameExists(String name)
    {
        return name != null && this.securityService.getAllRoles().stream()
            .anyMatch(existing -> name.equals(existing.getName()));
    }
}
