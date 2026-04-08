package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.ikasan.dashboard.security.model.IkasanPrincipalImpl;
import org.ikasan.dashboard.ui.UITest;
import org.ikasan.security.dao.SolrIkasanPrincipalDaoImpl;
import org.ikasan.security.dao.SolrPolicyDaoImpl;
import org.ikasan.security.dao.SolrRoleDaoImpl;
import org.ikasan.security.dao.SolrUserDaoImpl;
import org.ikasan.security.initialisation.BaselineSecurityDataLoader;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.service.SecurityService;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.ikasan.spec.security.model.IkasanPrincipal;
import org.springframework.test.annotation.DirtiesContext;

import javax.annotation.Resource;
import java.io.IOException;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static com.github.mvysny.kaributesting.v10.GridKt.*;

@Ignore
public class RoleManagementViewTest extends UITest
{
    @Resource
    private SecurityService securityService;
    @Resource
    private SolrPolicyDaoImpl policyDao;
    @Resource
    private SolrRoleDaoImpl roleDao;
    @Resource
    private SolrIkasanPrincipalDaoImpl principalDao;
    @Resource
    private SolrUserDaoImpl userDao;

    @Override
    public void setup_expectations() throws IOException {
        BaselineSecurityDataLoader baselineSecurityDataLoader
            = new BaselineSecurityDataLoader(this.policyDao, this.roleDao,
                this.principalDao, this.userDao);
        baselineSecurityDataLoader.loadBaselineData();

        IkasanPrincipal ikasanPrincipal = securityService.createPrincipal();
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

    @Test
    public void test_view_is_visible() {
        UI.getCurrent().navigate("roleManagement");

        RoleManagementView roleManagementView = _get(RoleManagementView.class);
        Assertions.assertTrue(roleManagementView.isVisible());
    }

    @Test
    public void test_view_is_sized_full() {
        UI.getCurrent().navigate("roleManagement");

        RoleManagementView roleManagementView = _get(RoleManagementView.class);
        Assertions.assertTrue(roleManagementView.getWidth().equals("100%"));
    }

    @Test
    public void test_role_grid_exists() {
        UI.getCurrent().navigate("roleManagement");

        Grid roleGrid = _get(Grid.class, spec -> spec.withId("roleGrid"));
        Assertions.assertNotNull(roleGrid);
    }

    @Test
    public void test_role_grid_is_sized_full() {
        UI.getCurrent().navigate("roleManagement");

        Grid roleGrid = _get(Grid.class, spec -> spec.withId("roleGrid"));
        Assertions.assertTrue(roleGrid.getWidth().equals("100%"));
    }

    @Test
    public void test_role_grid_has_columns() {
        UI.getCurrent().navigate("roleManagement");

        Grid roleGrid = _get(Grid.class, spec -> spec.withId("roleGrid"));
        Assertions.assertTrue(roleGrid.getColumns().size() > 0);
    }

    @Test
    public void test_add_role_button_exists() {
        UI.getCurrent().navigate("roleManagement");

        Button addRoleButton = _get(Button.class, spec -> spec.withId("addRoleButton"));
        Assertions.assertNotNull(addRoleButton);
    }

    @Test
    public void test_view_has_children() {
        UI.getCurrent().navigate("roleManagement");

        RoleManagementView roleManagementView = _get(RoleManagementView.class);
        Assertions.assertTrue(roleManagementView.getChildren().count() > 0);
    }

    @Test
    public void test_navigation_successful() {
        UI.getCurrent().navigate("roleManagement");

        RoleManagementView roleManagementView = _get(RoleManagementView.class);
        Assertions.assertTrue(roleManagementView.isAttached());
    }

    @Test
    public void test_view_has_spacing() {
        UI.getCurrent().navigate("roleManagement");

        RoleManagementView roleManagementView = _get(RoleManagementView.class);
        Assertions.assertTrue(roleManagementView.isSpacing());
    }

    @Test
    public void test_role_grid_has_default_roles() {
        UI.getCurrent().navigate("roleManagement");

        Grid roleGrid = _get(Grid.class, spec -> spec.withId("roleGrid"));

        // Verify grid has default roles (should have 5 by default)
        Assertions.assertTrue(_size(roleGrid) >= 5);
    }

    @Test
    @DirtiesContext
    public void test_add_role_button_opens_dialog() {
        UI.getCurrent().navigate("roleManagement");

        Button addRoleButton = _get(Button.class, spec -> spec.withId("addRoleButton"));
        _click(addRoleButton);

        // Verify dialog fields appear
        TextField nameTf = _get(TextField.class, spec -> spec.withId("nameTf"));
        Assertions.assertNotNull(nameTf);

        TextArea descriptionTf = _get(TextArea.class, spec -> spec.withId("descriptionTf"));
        Assertions.assertNotNull(descriptionTf);

        Button saveButton = _get(Button.class, spec -> spec.withId("saveButton"));
        Assertions.assertNotNull(saveButton);
    }

    @Test
    public void test_role_grid_has_name_column() {
        UI.getCurrent().navigate("roleManagement");

        Grid roleGrid = _get(Grid.class, spec -> spec.withId("roleGrid"));

        // Verify name column exists
        Assertions.assertNotNull(roleGrid.getColumnByKey("name"));
    }

    @Test
    public void test_role_grid_has_description_column() {
        UI.getCurrent().navigate("roleManagement");

        Grid roleGrid = _get(Grid.class, spec -> spec.withId("roleGrid"));

        // Verify description column exists
        Assertions.assertNotNull(roleGrid.getColumnByKey("description"));
    }

    @Test
    public void test_role_grid_columns_are_sortable() {
        UI.getCurrent().navigate("roleManagement");

        Grid roleGrid = _get(Grid.class, spec -> spec.withId("roleGrid"));

        // Verify columns are sortable
        Assertions.assertTrue(roleGrid.getColumnByKey("name").isSortable());
        Assertions.assertTrue(roleGrid.getColumnByKey("description").isSortable());
    }

    @Test
    public void test_view_initialization() {
        UI.getCurrent().navigate("roleManagement");

        RoleManagementView roleManagementView = _get(RoleManagementView.class);

        Assertions.assertTrue(roleManagementView.isAttached());
        Assertions.assertTrue(roleManagementView.isVisible());
        Assertions.assertTrue(roleManagementView.getWidth().equals("100%"));
        Assertions.assertTrue(roleManagementView.isSpacing());
    }

    @Test
    public void test_header_exists() {
        UI.getCurrent().navigate("roleManagement");

        com.vaadin.flow.component.html.H2 header = _get(com.vaadin.flow.component.html.H2.class);
        Assertions.assertNotNull(header);
    }

    @Test
    public void test_role_grid_has_header_row() {
        UI.getCurrent().navigate("roleManagement");

        Grid roleGrid = _get(Grid.class, spec -> spec.withId("roleGrid"));

        // Verify grid has header rows (includes filter row)
        Assertions.assertTrue(roleGrid.getHeaderRows().size() > 0);
    }
}
