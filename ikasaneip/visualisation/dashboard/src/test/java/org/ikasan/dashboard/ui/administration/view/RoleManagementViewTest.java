package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.ui.UITest;
import org.ikasan.security.model.Role;
import org.ikasan.security.service.SecurityService;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.ikasan.security.model.IkasanPrincipal;
import org.springframework.test.annotation.DirtiesContext;

import javax.annotation.Resource;
import java.io.IOException;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static com.github.mvysny.kaributesting.v10.GridKt.*;

public class RoleManagementViewTest extends UITest
{
    @Resource
    private SecurityService securityService;

    @Override
    public void setup_expectations() {
        IkasanPrincipal ikasanPrincipal = new IkasanPrincipal();
        ikasanPrincipal.setName("sample_group");
        ikasanPrincipal.setDescription("description");
        ikasanPrincipal.setType("application");

        securityService.savePrincipal(ikasanPrincipal);
    }

    @After
    public void teardown() {
        securityService.deletePrincipal(securityService.findPrincipalByName("sample_group"));
    }

    @Test
    @DirtiesContext
    public void testRoleManagementView() throws IOException
    {
        UI.getCurrent().navigate("roleManagement");

        RoleManagementView roleManagementView = _get(RoleManagementView.class);
        Assertions.assertNotNull(roleManagementView);
    }

    @Test
    @DirtiesContext
    public void test_add_new_role() throws IOException
    {
        // Navigate to the role management view
        UI.getCurrent().navigate("roleManagement");

        RoleManagementView roleManagementView = _get(RoleManagementView.class);
        Assertions.assertNotNull(roleManagementView);

        // Add a new role
        Button addRoleButton = _get(Button.class, spec -> spec.withId("addRoleButton"));
        _click(addRoleButton);

        TextField nameTf = _get(TextField.class, spec -> spec.withId("nameTf"));
        Assertions.assertNotNull(nameTf);
        nameTf.setValue("new_role");

        TextArea descriptionTf = _get(TextArea.class, spec -> spec.withId("descriptionTf"));
        Assertions.assertNotNull(descriptionTf);
        descriptionTf.setValue("description");

        Button saveButton = _get(Button.class, spec -> spec.withId("saveButton"));
        _click(saveButton);

        // Confirm that the new role has been created
        Grid roleGrid = _get(Grid.class, spec -> spec.withId("roleGrid"));
        Assertions.assertNotNull(roleGrid);
        expectRows(roleGrid, 6);

        // Open new role in role management view
        _doubleClickItem(roleGrid, 5);

        // Associate a policy with the new role
        Grid policyGrid = _get(Grid.class, spec -> spec.withId("policyGrid"));
        Assertions.assertNotNull(policyGrid);
        expectRows(policyGrid, 0);

        Button addPolicyButton = _get(Button.class, spec -> spec.withId("addPolicyButton"));
        Assertions.assertNotNull(addPolicyButton);
        _click(addPolicyButton);

        Grid selectPolicyGrid = _get(Grid.class, spec -> spec.withId("selectPolicyGrid"));
        Assertions.assertNotNull(selectPolicyGrid);
        expectRows(selectPolicyGrid, 104);

        // add a policy to the role
        _doubleClickItem(selectPolicyGrid, 5);

        // Now adding group to role
        Button addGroupButton = _get(Button.class, spec -> spec.withId("addGroupButton"));
        Assertions.assertNotNull(addGroupButton);
        _click(addGroupButton);

        // Select the group
        Grid groupSelectGrid = _get(Grid.class, spec -> spec.withId("groupSelectGrid"));
        Assertions.assertNotNull(groupSelectGrid);
        expectRows(groupSelectGrid, 1);

        _doubleClickItem(groupSelectGrid, 0);

        // Confirm that the selected group has been added
        Grid groupGrid = _get(Grid.class, spec -> spec.withId("groupGrid"));
        Assertions.assertNotNull(groupGrid);
        expectRows(groupGrid, 1);

        // Confirm that all as expected in the data
        Role newRole = this.securityService.findRoleByName("new_role");
        Assertions.assertNotNull(newRole);
        Assertions.assertEquals(1, newRole.getPolicies().size());

        IkasanPrincipal ikasanPrincipal = securityService.findPrincipalByName("sample_group");
        Assertions.assertNotNull(ikasanPrincipal);
        Assertions.assertEquals(1, ikasanPrincipal.getRoles().size());
    }
}
