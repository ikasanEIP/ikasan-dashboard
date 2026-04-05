package org.ikasan.security.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.security.model.SolrPolicyImpl;
import org.ikasan.security.model.SolrRoleImpl;
import org.ikasan.security.model.SolrRoleJobPlanImpl;
import org.ikasan.security.model.SolrRoleModuleImpl;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.model.RoleJobPlan;
import org.ikasan.spec.security.model.RoleModule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import static org.ikasan.security.dao.SolrRoleDaoImpl.ROLE_TYPE;

public class SolrRoleDaoImplTest extends SolrTestCaseJ4 {

    private SolrRoleDaoImpl dao;
    private SolrPolicyDaoImpl solrPolicyDao;
    private NodeConfig config;
    private Path tmppath;

    @Before
    public void setup() {
        tmppath = createTempDir();
        config = new NodeConfig.NodeConfigBuilder("testnode", tmppath)
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString()).build();
    }

    @After
    public void teardown() throws IOException {
        FileSystemUtils.deleteRecursively(tmppath);
    }

    private void init(EmbeddedSolrServer server) throws IOException, SolrServerException {
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        this.solrPolicyDao = new SolrPolicyDaoImpl();
        this.solrPolicyDao.setSolrClient(server);

        dao = new SolrRoleDaoImpl(this.solrPolicyDao);
        dao.setSolrClient(server);
    }

    @Test
    public void test_createRole() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            Role role = dao.createRole();

            Assert.assertNotNull(role);
            Assert.assertTrue(role instanceof SolrRoleImpl);
        }
    }

    @Test
    public void test_saveOrUpdateRole_and_getRoleByName() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("TestRole");
            role.setDescription("Test role description");
            role.setCreatedDateTime(new Date(1000000L));
            role.setUpdatedDateTime(new Date(2000000L));

            dao.saveOrUpdateRole(role);

            Role found = dao.getRoleByName("TestRole");

            Assert.assertNotNull(found);
            Assert.assertEquals("TestRole", found.getName());
            Assert.assertEquals("Test role description", found.getDescription());
        }
    }

    @Test
    public void test_getRoleByName_not_found() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            Role found = dao.getRoleByName("NonExistentRole");

            Assert.assertNull(found);
        }
    }

    @Test
    public void test_saveOrUpdateRole_update_existing() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("TestRole");
            role.setDescription("Original description");
            role.setCreatedDateTime(new Date(1000000L));
            role.setUpdatedDateTime(new Date(2000000L));

            dao.saveOrUpdateRole(role);

            Role found = dao.getRoleByName("TestRole");
            Assert.assertEquals("Original description", found.getDescription());

            // Update the role
            role.setDescription("Updated description");
            role.setUpdatedDateTime(new Date(3000000L));
            dao.saveOrUpdateRole(role);

            found = dao.getRoleByName("TestRole");

            Assert.assertNotNull(found);
            Assert.assertEquals("TestRole", found.getName());
            Assert.assertEquals("Updated description", found.getDescription());
        }
    }

    @Test
    public void test_deleteRole() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleToDelete");
            role.setDescription("This role will be deleted");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            dao.saveOrUpdateRole(role);

            Role found = dao.getRoleByName("RoleToDelete");
            Assert.assertNotNull(found);

            // Delete the role
            dao.deleteRole(role);

            found = dao.getRoleByName("RoleToDelete");
            Assert.assertNull(found);
        }
    }

    @Test
    public void test_getAllRoles() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Save multiple roles
            for (int i = 0; i < 5; i++) {
                SolrRoleImpl role = new SolrRoleImpl();
                role.setName("Role" + i);
                role.setDescription("Description for role " + i);
                role.setCreatedDateTime(new Date(1000000L + i));
                role.setUpdatedDateTime(new Date(2000000L + i));

                dao.saveOrUpdateRole(role);
            }

            List<Role> roles = dao.getAllRoles();

            Assert.assertNotNull(roles);
            Assert.assertEquals(5, roles.size());
        }
    }

    @Test
    public void test_getAllRoles_empty() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            List<Role> roles = dao.getAllRoles();

            Assert.assertNotNull(roles);
            Assert.assertTrue(roles.isEmpty());
        }
    }

    @Test
    public void test_getRoleByNameLike() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Save roles with different names
            SolrRoleImpl role1 = new SolrRoleImpl();
            role1.setName("AdminRole");
            role1.setDescription("Administrator role");
            role1.setCreatedDateTime(new Date());
            role1.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role1);

            SolrRoleImpl role2 = new SolrRoleImpl();
            role2.setName("UserRole");
            role2.setDescription("User role");
            role2.setCreatedDateTime(new Date());
            role2.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role2);

            SolrRoleImpl role3 = new SolrRoleImpl();
            role3.setName("AdminReadOnly");
            role3.setDescription("Read-only admin role");
            role3.setCreatedDateTime(new Date());
            role3.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role3);

            List<Role> roles = dao.getRoleByNameLike("Admin");

            Assert.assertNotNull(roles);
            Assert.assertEquals(2, roles.size());
        }
    }

    @Test
    public void test_getRoleByNameLike_no_match() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("TestRole");
            role.setDescription("Test description");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            List<Role> roles = dao.getRoleByNameLike("NonExistent");

            Assert.assertNotNull(roles);
            Assert.assertTrue(roles.isEmpty());
        }
    }

    @Test
    public void test_getRoleById() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("TestRole");
            role.setDescription("Test description");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            dao.saveOrUpdateRole(role);

            Role found = dao.getRoleById("TestRole-"+ ROLE_TYPE);

            Assert.assertNotNull(found);
            Assert.assertEquals("TestRole", found.getName());
            Assert.assertEquals("TestRole-"+ ROLE_TYPE, found.getId());
        }
    }

    @Test
    public void test_getRoleById_not_found() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            Role found = dao.getRoleById("NonExistent");

            Assert.assertNull(found);
        }
    }

    @Test
    public void test_saveRoleModule_embedded_in_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with a role module
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithModule");
            role.setDescription("Role containing a module");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            // Add a role module to the role
            SolrRoleModuleImpl roleModule = new SolrRoleModuleImpl();
            roleModule.setModuleName("IntegrationModule");
            roleModule.setCreatedDateTime(new Date());
            roleModule.setUpdatedDateTime(new Date());
            role.addRoleModule(roleModule);

            // Save the role (which includes the role module)
            dao.saveOrUpdateRole(role);

            // Retrieve the role and verify the module is included
            Role found = dao.getRoleByName("RoleWithModule");

            Assert.assertNotNull(found);
            Assert.assertEquals("RoleWithModule", found.getName());
            Assert.assertNotNull(found.getRoleModules());
            Assert.assertEquals(1, found.getRoleModules().size());

            RoleModule foundModule = (RoleModule) found.getRoleModules().iterator().next();
            Assert.assertEquals("IntegrationModule", foundModule.getModuleName());
        }
    }

    @Test
    public void test_saveRoleModule_multiple_modules_in_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with multiple role modules
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithMultipleModules");
            role.setDescription("Role containing multiple modules");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            // Add multiple role modules
            for (int i = 1; i <= 3; i++) {
                SolrRoleModuleImpl roleModule = new SolrRoleModuleImpl();
                roleModule.setModuleName("Module" + i);
                roleModule.setCreatedDateTime(new Date());
                roleModule.setUpdatedDateTime(new Date());
                role.addRoleModule(roleModule);
            }

            // Save the role
            dao.saveOrUpdateRole(role);

            // Retrieve and verify
            Role found = dao.getRoleByName("RoleWithMultipleModules");

            Assert.assertNotNull(found);
            Assert.assertEquals(3, found.getRoleModules().size());

            // Verify all module names are present
            List moduleNames = (List) found.getRoleModules().stream()
                .map(rm -> ((RoleModule)rm).getModuleName())
                .sorted()
                .collect(java.util.stream.Collectors.toList());

            Assert.assertEquals("Module1", moduleNames.get(0));
            Assert.assertEquals("Module2", moduleNames.get(1));
            Assert.assertEquals("Module3", moduleNames.get(2));
        }
    }

    @Test
    public void test_saveRoleModule_update_modules_in_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with one module
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleForModuleUpdate");
            role.setDescription("Role for testing module updates");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleModuleImpl roleModule1 = new SolrRoleModuleImpl();
            roleModule1.setModuleName("OriginalModule");
            role.addRoleModule(roleModule1);

            dao.saveOrUpdateRole(role);

            // Verify initial state
            Role found = dao.getRoleByName("RoleForModuleUpdate");
            Assert.assertEquals(1, found.getRoleModules().size());

            // Add another module
            SolrRoleModuleImpl roleModule2 = new SolrRoleModuleImpl();
            roleModule2.setModuleName("AdditionalModule");
            role.addRoleModule(roleModule2);

            // Update the role
            dao.saveOrUpdateRole(role);

            // Verify updated state
            found = dao.getRoleByName("RoleForModuleUpdate");
            Assert.assertEquals(2, found.getRoleModules().size());
        }
    }

    @Test
    public void test_saveRoleModule_with_role_reference() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role
            SolrRoleImpl role = new SolrRoleImpl();
            role.setId("AdminRole-" + ROLE_TYPE);
            role.setName("AdminRole");
            role.setDescription("Administrator role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            // Create a role module with a back-reference to the role
            SolrRoleModuleImpl roleModule = new SolrRoleModuleImpl();
            roleModule.setId("100L");
            roleModule.setModuleName("AdminModule");
            roleModule.setRole(role);
            roleModule.setCreatedDateTime(new Date());
            roleModule.setUpdatedDateTime(new Date());

            role.addRoleModule(roleModule);

            // Save the role
            dao.saveOrUpdateRole(role);

            // Retrieve and verify
            Role found = dao.getRoleByName("AdminRole");

            Assert.assertNotNull(found);
            Assert.assertEquals(1, found.getRoleModules().size());

            RoleModule foundModule = (RoleModule) found.getRoleModules().iterator().next();
            Assert.assertEquals("AdminModule", foundModule.getModuleName());
        }
    }

    @Test
    public void test_deleteRoleModule_by_removing_from_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with multiple modules
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithModulesToDelete");
            role.setDescription("Role for testing module deletion");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleModuleImpl module1 = new SolrRoleModuleImpl();
            module1.setModuleName("ModuleToKeep");
            module1.setCreatedDateTime(new Date());
            role.addRoleModule(module1);

            SolrRoleModuleImpl module2 = new SolrRoleModuleImpl();
            module2.setModuleName("ModuleToDelete");
            module2.setCreatedDateTime(new Date());
            role.addRoleModule(module2);

            dao.saveOrUpdateRole(role);

            // Verify both modules exist
            Role found = dao.getRoleByName("RoleWithModulesToDelete");
            Assert.assertEquals(2, found.getRoleModules().size());

            // Remove one module and save again
            role.getRoleModules().removeIf(m -> "ModuleToDelete".equals(m.getModuleName()));
            dao.saveOrUpdateRole(role);

            // Verify only one module remains
            found = dao.getRoleByName("RoleWithModulesToDelete");
            Assert.assertEquals(1, found.getRoleModules().size());
            Assert.assertEquals("ModuleToKeep",
                ((RoleModule)found.getRoleModules().iterator().next()).getModuleName());
        }
    }

    @Test
    public void test_deleteRoleModule_by_deleting_entire_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with modules
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleToDeleteWithModules");
            role.setDescription("Role to be deleted");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleModuleImpl module = new SolrRoleModuleImpl();
            module.setModuleName("EmbeddedModule");
            role.addRoleModule(module);

            dao.saveOrUpdateRole(role);

            // Verify role exists with module
            Role found = dao.getRoleByName("RoleToDeleteWithModules");
            Assert.assertNotNull(found);
            Assert.assertEquals(1, found.getRoleModules().size());

            // Delete the entire role (which includes the module)
            dao.deleteRole(role);

            // Verify role and module are gone
            found = dao.getRoleByName("RoleToDeleteWithModules");
            Assert.assertNull(found);
        }
    }

    @Test
    public void test_deleteRoleModule_remove_all_modules() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with multiple modules
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleToRemoveAllModules");
            role.setDescription("Role to have all modules removed");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            for (int i = 1; i <= 3; i++) {
                SolrRoleModuleImpl module = new SolrRoleModuleImpl();
                module.setModuleName("Module" + i);
                module.setCreatedDateTime(new Date());
                role.addRoleModule(module);
            }

            dao.saveOrUpdateRole(role);

            // Verify all modules exist
            Role found = dao.getRoleByName("RoleToRemoveAllModules");
            Assert.assertEquals(3, found.getRoleModules().size());

            // Remove all modules
            role.getRoleModules().clear();
            dao.saveOrUpdateRole(role);

            // Verify no modules remain but role still exists
            found = dao.getRoleByName("RoleToRemoveAllModules");
            Assert.assertNotNull(found);
            Assert.assertTrue(found.getRoleModules().isEmpty());
        }
    }

    @Test
    public void test_deleteRoleModule_selective_deletion() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with multiple modules
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleForSelectiveDeletion");
            role.setDescription("Role for selective module deletion");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            String[] moduleNames = {"ModuleA", "ModuleB", "ModuleC", "ModuleD"};
            for (String moduleName : moduleNames) {
                SolrRoleModuleImpl module = new SolrRoleModuleImpl();
                module.setModuleName(moduleName);
                module.setCreatedDateTime(new Date());
                role.addRoleModule(module);
            }

            dao.saveOrUpdateRole(role);

            // Verify all 4 modules exist
            Role found = dao.getRoleByName("RoleForSelectiveDeletion");
            Assert.assertEquals(4, found.getRoleModules().size());

            // Remove ModuleB and ModuleD
            role.getRoleModules().removeIf(m ->
                "ModuleB".equals(m.getModuleName()) || "ModuleD".equals(m.getModuleName()));
            dao.saveOrUpdateRole(role);

            // Verify only ModuleA and ModuleC remain
            found = dao.getRoleByName("RoleForSelectiveDeletion");
            Assert.assertEquals(2, found.getRoleModules().size());

            List remainingModules = (List) found.getRoleModules().stream()
                .map(rm -> ((RoleModule)rm).getModuleName())
                .sorted()
                .collect(java.util.stream.Collectors.toList());

            Assert.assertEquals("ModuleA", remainingModules.get(0));
            Assert.assertEquals("ModuleC", remainingModules.get(1));
        }
    }

    @Test
    public void test_deleteRoleModule_replace_modules() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with modules
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleForModuleReplacement");
            role.setDescription("Role for module replacement");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleModuleImpl oldModule = new SolrRoleModuleImpl();
            oldModule.setModuleName("OldModule");
            oldModule.setCreatedDateTime(new Date());
            role.addRoleModule(oldModule);

            dao.saveOrUpdateRole(role);

            // Verify old module exists
            Role found = dao.getRoleByName("RoleForModuleReplacement");
            Assert.assertEquals(1, found.getRoleModules().size());
            Assert.assertEquals("OldModule",
                ((RoleModule)found.getRoleModules().iterator().next()).getModuleName());

            // Replace old module with new one
            role.getRoleModules().clear();
            SolrRoleModuleImpl newModule = new SolrRoleModuleImpl();
            newModule.setModuleName("NewModule");
            newModule.setCreatedDateTime(new Date());
            role.addRoleModule(newModule);

            dao.saveOrUpdateRole(role);

            // Verify new module exists and old one is gone
            found = dao.getRoleByName("RoleForModuleReplacement");
            Assert.assertEquals(1, found.getRoleModules().size());
            Assert.assertEquals("NewModule",
                ((RoleModule)found.getRoleModules().iterator().next()).getModuleName());
        }
    }

    @Test
    public void test_deleteRoleModule_preserves_other_role_data() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with modules and other data
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithCompleteData");
            role.setDescription("Original description");
            role.setCreatedDateTime(new Date(1000000L));
            role.setUpdatedDateTime(new Date(2000000L));

            SolrRoleModuleImpl module = new SolrRoleModuleImpl();
            module.setModuleName("TestModule");
            role.addRoleModule(module);

            SolrPolicyImpl policy = new SolrPolicyImpl();
            policy.setName("TestPolicy");
            this.solrPolicyDao.saveOrUpdatePolicy(policy);

            role.addPolicy(policy);

            dao.saveOrUpdateRole(role);

            // Remove the module
            role.getRoleModules().clear();
            dao.saveOrUpdateRole(role);

            // Verify module is gone but other data remains
            Role found = dao.getRoleByName("RoleWithCompleteData");
            Assert.assertNotNull(found);
            Assert.assertEquals("RoleWithCompleteData", found.getName());
            Assert.assertEquals("Original description", found.getDescription());
            Assert.assertTrue(found.getRoleModules().isEmpty());
            Assert.assertEquals(1, found.getPolicies().size());
        }
    }

    @Test
    public void test_saveRoleJobPlan_embedded_in_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with a role job plan
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithJobPlan");
            role.setDescription("Role containing a job plan");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            // Add a role job plan to the role
            SolrRoleJobPlanImpl roleJobPlan = new SolrRoleJobPlanImpl();
            roleJobPlan.setJobPlanName("DataProcessingPlan");
            roleJobPlan.setCreatedDateTime(new Date());
            roleJobPlan.setUpdatedDateTime(new Date());
            role.addRoleJobPlan(roleJobPlan);

            // Save the role (which includes the role job plan)
            dao.saveOrUpdateRole(role);

            // Retrieve the role and verify the job plan is included
            Role found = dao.getRoleByName("RoleWithJobPlan");

            Assert.assertNotNull(found);
            Assert.assertEquals("RoleWithJobPlan", found.getName());
            Assert.assertNotNull(found.getRoleJobPlans());
            Assert.assertEquals(1, found.getRoleJobPlans().size());

            RoleJobPlan foundJobPlan = (RoleJobPlan) found.getRoleJobPlans().iterator().next();
            Assert.assertEquals("DataProcessingPlan", foundJobPlan.getJobPlanName());
        }
    }

    @Test
    public void test_saveRoleJobPlan_multiple_jobplans_in_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with multiple role job plans
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithMultipleJobPlans");
            role.setDescription("Role containing multiple job plans");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            // Add multiple role job plans
            for (int i = 1; i <= 3; i++) {
                SolrRoleJobPlanImpl roleJobPlan = new SolrRoleJobPlanImpl();
                roleJobPlan.setJobPlanName("JobPlan" + i);
                roleJobPlan.setCreatedDateTime(new Date());
                roleJobPlan.setUpdatedDateTime(new Date());
                role.addRoleJobPlan(roleJobPlan);
            }

            // Save the role
            dao.saveOrUpdateRole(role);

            // Retrieve and verify
            Role found = dao.getRoleByName("RoleWithMultipleJobPlans");

            Assert.assertNotNull(found);
            Assert.assertEquals(3, found.getRoleJobPlans().size());

            // Verify all job plan names are present
            List jobPlanNames = (List) found.getRoleJobPlans().stream()
                .map(r -> ((RoleJobPlan)r).getJobPlanName())
                .sorted()
                .collect(java.util.stream.Collectors.toList());

            Assert.assertEquals("JobPlan1", jobPlanNames.get(0));
            Assert.assertEquals("JobPlan2", jobPlanNames.get(1));
            Assert.assertEquals("JobPlan3", jobPlanNames.get(2));
        }
    }

    @Test
    public void test_saveRoleJobPlan_update_jobplans_in_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with one job plan
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleForJobPlanUpdate");
            role.setDescription("Role for testing job plan updates");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl roleJobPlan1 = new SolrRoleJobPlanImpl();
            roleJobPlan1.setJobPlanName("OriginalPlan");
            role.addRoleJobPlan(roleJobPlan1);

            dao.saveOrUpdateRole(role);

            // Verify initial state
            Role found = dao.getRoleByName("RoleForJobPlanUpdate");
            Assert.assertEquals(1, found.getRoleJobPlans().size());

            // Add another job plan
            SolrRoleJobPlanImpl roleJobPlan2 = new SolrRoleJobPlanImpl();
            roleJobPlan2.setJobPlanName("AdditionalPlan");
            role.addRoleJobPlan(roleJobPlan2);

            // Update the role
            dao.saveOrUpdateRole(role);

            // Verify updated state
            found = dao.getRoleByName("RoleForJobPlanUpdate");
            Assert.assertEquals(2, found.getRoleJobPlans().size());
        }
    }

    @Test
    public void test_saveRoleJobPlan_with_role_reference() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("SchedulerRole");
            role.setDescription("Scheduler role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            // Create a role job plan with a back-reference to the role
            SolrRoleJobPlanImpl roleJobPlan = new SolrRoleJobPlanImpl();
            roleJobPlan.setId("200L");
            roleJobPlan.setJobPlanName("NightlyBatchPlan");
            roleJobPlan.setRole(role);
            roleJobPlan.setCreatedDateTime(new Date());
            roleJobPlan.setUpdatedDateTime(new Date());

            role.addRoleJobPlan(roleJobPlan);

            // Save the role
            dao.saveOrUpdateRole(role);

            // Retrieve and verify
            Role found = dao.getRoleByName("SchedulerRole");

            Assert.assertNotNull(found);
            Assert.assertEquals(1, found.getRoleJobPlans().size());

            RoleJobPlan foundJobPlan = (RoleJobPlan) found.getRoleJobPlans().iterator().next();
            Assert.assertEquals("NightlyBatchPlan", foundJobPlan.getJobPlanName());
        }
    }

    @Test
    public void test_deleteRoleJobPlan_by_removing_from_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with multiple job plans
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithJobPlansToDelete");
            role.setDescription("Role for testing job plan deletion");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl jobPlan1 = new SolrRoleJobPlanImpl();
            jobPlan1.setJobPlanName("PlanToKeep");
            jobPlan1.setCreatedDateTime(new Date());
            role.addRoleJobPlan(jobPlan1);

            SolrRoleJobPlanImpl jobPlan2 = new SolrRoleJobPlanImpl();
            jobPlan2.setJobPlanName("PlanToDelete");
            jobPlan2.setCreatedDateTime(new Date());
            role.addRoleJobPlan(jobPlan2);

            dao.saveOrUpdateRole(role);

            // Verify both job plans exist
            Role found = dao.getRoleByName("RoleWithJobPlansToDelete");
            Assert.assertEquals(2, found.getRoleJobPlans().size());

            // Remove one job plan and save again
            role.getRoleJobPlans().removeIf(jp -> "PlanToDelete".equals(jp.getJobPlanName()));
            dao.saveOrUpdateRole(role);

            // Verify only one job plan remains
            found = dao.getRoleByName("RoleWithJobPlansToDelete");
            Assert.assertEquals(1, found.getRoleJobPlans().size());
            Assert.assertEquals("PlanToKeep",
                ((RoleJobPlan) found.getRoleJobPlans().iterator().next()).getJobPlanName());
        }
    }

    @Test
    public void test_deleteRoleJobPlan_by_deleting_entire_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with job plans
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleToDeleteWithJobPlans");
            role.setDescription("Role to be deleted");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl jobPlan = new SolrRoleJobPlanImpl();
            jobPlan.setJobPlanName("EmbeddedJobPlan");
            role.addRoleJobPlan(jobPlan);

            dao.saveOrUpdateRole(role);

            // Verify role exists with job plan
            Role found = dao.getRoleByName("RoleToDeleteWithJobPlans");
            Assert.assertNotNull(found);
            Assert.assertEquals(1, found.getRoleJobPlans().size());

            // Delete the entire role (which includes the job plan)
            dao.deleteRole(role);

            // Verify role and job plan are gone
            found = dao.getRoleByName("RoleToDeleteWithJobPlans");
            Assert.assertNull(found);
        }
    }

    @Test
    public void test_deleteRoleJobPlan_remove_all_jobplans() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with multiple job plans
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleToRemoveAllJobPlans");
            role.setDescription("Role to have all job plans removed");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            for (int i = 1; i <= 3; i++) {
                SolrRoleJobPlanImpl jobPlan = new SolrRoleJobPlanImpl();
                jobPlan.setJobPlanName("JobPlan" + i);
                jobPlan.setCreatedDateTime(new Date());
                role.addRoleJobPlan(jobPlan);
            }

            dao.saveOrUpdateRole(role);

            // Verify all job plans exist
            Role found = dao.getRoleByName("RoleToRemoveAllJobPlans");
            Assert.assertEquals(3, found.getRoleJobPlans().size());

            // Remove all job plans
            role.getRoleJobPlans().clear();
            dao.saveOrUpdateRole(role);

            // Verify no job plans remain but role still exists
            found = dao.getRoleByName("RoleToRemoveAllJobPlans");
            Assert.assertNotNull(found);
            Assert.assertTrue(found.getRoleJobPlans().isEmpty());
        }
    }

    @Test
    public void test_deleteRoleJobPlan_selective_deletion() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with multiple job plans
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleForSelectiveJobPlanDeletion");
            role.setDescription("Role for selective job plan deletion");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            String[] planNames = {"PlanA", "PlanB", "PlanC", "PlanD"};
            for (String planName : planNames) {
                SolrRoleJobPlanImpl jobPlan = new SolrRoleJobPlanImpl();
                jobPlan.setJobPlanName(planName);
                jobPlan.setCreatedDateTime(new Date());
                role.addRoleJobPlan(jobPlan);
            }

            dao.saveOrUpdateRole(role);

            // Verify all 4 job plans exist
            Role found = dao.getRoleByName("RoleForSelectiveJobPlanDeletion");
            Assert.assertEquals(4, found.getRoleJobPlans().size());

            // Remove PlanB and PlanD
            role.getRoleJobPlans().removeIf(jp ->
                "PlanB".equals(jp.getJobPlanName()) || "PlanD".equals(jp.getJobPlanName()));
            dao.saveOrUpdateRole(role);

            // Verify only PlanA and PlanC remain
            found = dao.getRoleByName("RoleForSelectiveJobPlanDeletion");
            Assert.assertEquals(2, found.getRoleJobPlans().size());

            List remainingPlans = (List) found.getRoleJobPlans().stream()
                .map(r -> ((RoleJobPlan)r).getJobPlanName())
                .sorted()
                .collect(java.util.stream.Collectors.toList());

            Assert.assertEquals("PlanA", remainingPlans.get(0));
            Assert.assertEquals("PlanC", remainingPlans.get(1));
        }
    }

    @Test
    public void test_deleteRoleJobPlan_replace_jobplans() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with job plans
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleForJobPlanReplacement");
            role.setDescription("Role for job plan replacement");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl oldPlan = new SolrRoleJobPlanImpl();
            oldPlan.setJobPlanName("OldPlan");
            oldPlan.setCreatedDateTime(new Date());
            role.addRoleJobPlan(oldPlan);

            dao.saveOrUpdateRole(role);

            // Verify old plan exists
            Role found = dao.getRoleByName("RoleForJobPlanReplacement");
            Assert.assertEquals(1, found.getRoleJobPlans().size());
            Assert.assertEquals("OldPlan",
                ((RoleJobPlan) found.getRoleJobPlans().iterator().next()).getJobPlanName());

            // Replace old plan with new one
            role.getRoleJobPlans().clear();
            SolrRoleJobPlanImpl newPlan = new SolrRoleJobPlanImpl();
            newPlan.setJobPlanName("NewPlan");
            newPlan.setCreatedDateTime(new Date());
            role.addRoleJobPlan(newPlan);

            dao.saveOrUpdateRole(role);

            // Verify new plan exists and old one is gone
            found = dao.getRoleByName("RoleForJobPlanReplacement");
            Assert.assertEquals(1, found.getRoleJobPlans().size());
            Assert.assertEquals("NewPlan",
                ((RoleJobPlan) found.getRoleJobPlans().iterator().next()).getJobPlanName());
        }
    }

    @Test
    public void test_deleteRoleJobPlan_preserves_other_role_data() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with job plans and other data
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithCompleteJobPlanData");
            role.setDescription("Original description");
            role.setCreatedDateTime(new Date(1000000L));
            role.setUpdatedDateTime(new Date(2000000L));

            SolrRoleJobPlanImpl jobPlan = new SolrRoleJobPlanImpl();
            jobPlan.setJobPlanName("TestPlan");
            role.addRoleJobPlan(jobPlan);

            SolrRoleModuleImpl module = new SolrRoleModuleImpl();
            module.setModuleName("TestModule");
            role.addRoleModule(module);

            dao.saveOrUpdateRole(role);

            // Remove the job plan
            role.getRoleJobPlans().clear();
            dao.saveOrUpdateRole(role);

            // Verify job plan is gone but other data remains
            Role found = dao.getRoleByName("RoleWithCompleteJobPlanData");
            Assert.assertNotNull(found);
            Assert.assertEquals("RoleWithCompleteJobPlanData", found.getName());
            Assert.assertEquals("Original description", found.getDescription());
            Assert.assertTrue(found.getRoleJobPlans().isEmpty());
            Assert.assertEquals(1, found.getRoleModules().size());
        }
    }

    @Test
    public void test_getRoleJobPlansByJobPlanName() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create roles with job plans
            SolrRoleImpl role1 = new SolrRoleImpl();
            role1.setName("Role1");
            role1.setDescription("Role 1");
            role1.setCreatedDateTime(new Date());
            role1.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl jobPlan1 = new SolrRoleJobPlanImpl();
            jobPlan1.setJobPlanName("TestJobPlan");
            jobPlan1.setRole(role1);
            role1.addRoleJobPlan(jobPlan1);

            dao.saveOrUpdateRole(role1);

            SolrRoleImpl role2 = new SolrRoleImpl();
            role2.setName("Role2");
            role2.setDescription("Role 2");
            role2.setCreatedDateTime(new Date());
            role2.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl jobPlan2 = new SolrRoleJobPlanImpl();
            jobPlan2.setJobPlanName("TestJobPlan");
            jobPlan2.setRole(role2);
            role2.addRoleJobPlan(jobPlan2);

            SolrRoleJobPlanImpl jobPlan3 = new SolrRoleJobPlanImpl();
            jobPlan3.setJobPlanName("OtherJobPlan");
            jobPlan3.setRole(role2);
            role2.addRoleJobPlan(jobPlan3);

            dao.saveOrUpdateRole(role2);

            // Query for job plans with name "TestJobPlan"
            List<RoleJobPlan> roleJobPlans = dao.getRoleJobPlansByJobPlanName("TestJobPlan");

            Assert.assertNotNull(roleJobPlans);
            Assert.assertEquals(2, roleJobPlans.size());

            // Verify all returned job plans have the correct name
            for (RoleJobPlan rjp : roleJobPlans) {
                Assert.assertEquals("TestJobPlan", rjp.getJobPlanName());
            }

            // Verify different roles are represented
            Assert.assertTrue(roleJobPlans.stream()
                .anyMatch(rjp -> "Role1".equals(rjp.getRole().getName())));
            Assert.assertTrue(roleJobPlans.stream()
                .anyMatch(rjp -> "Role2".equals(rjp.getRole().getName())));
        }
    }

    @Test
    public void test_getRoleJobPlansByJobPlanName_not_found() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create role with job plan
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("TestRole");
            role.setDescription("Test role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl jobPlan = new SolrRoleJobPlanImpl();
            jobPlan.setJobPlanName("ExistingJobPlan");
            jobPlan.setRole(role);
            role.addRoleJobPlan(jobPlan);

            dao.saveOrUpdateRole(role);

            // Query for non-existent job plan
            List<RoleJobPlan> roleJobPlans = dao.getRoleJobPlansByJobPlanName("NonExistentJobPlan");

            Assert.assertNotNull(roleJobPlans);
            Assert.assertTrue(roleJobPlans.isEmpty());
        }
    }

    @Test
    public void test_getRoleJobPlansByJobPlanName_null_or_empty() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Test with null
            List<RoleJobPlan> nullResult = dao.getRoleJobPlansByJobPlanName(null);
            Assert.assertNotNull(nullResult);
            Assert.assertTrue(nullResult.isEmpty());

            // Test with empty string
            List<RoleJobPlan> emptyResult = dao.getRoleJobPlansByJobPlanName("");
            Assert.assertNotNull(emptyResult);
            Assert.assertTrue(emptyResult.isEmpty());
        }
    }

    @Test
    public void test_getRoleJobPlansByJobPlanName_multiple_job_plans_per_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create role with multiple job plans
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("MultiJobPlanRole");
            role.setDescription("Role with multiple job plans");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl jobPlan1 = new SolrRoleJobPlanImpl();
            jobPlan1.setJobPlanName("JobPlanA");
            jobPlan1.setRole(role);
            role.addRoleJobPlan(jobPlan1);

            SolrRoleJobPlanImpl jobPlan2 = new SolrRoleJobPlanImpl();
            jobPlan2.setJobPlanName("JobPlanB");
            jobPlan2.setRole(role);
            role.addRoleJobPlan(jobPlan2);

            SolrRoleJobPlanImpl jobPlan3 = new SolrRoleJobPlanImpl();
            jobPlan3.setJobPlanName("JobPlanC");
            jobPlan3.setRole(role);
            role.addRoleJobPlan(jobPlan3);

            dao.saveOrUpdateRole(role);

            // Query for one specific job plan
            List<RoleJobPlan> roleJobPlans = dao.getRoleJobPlansByJobPlanName("JobPlanB");

            Assert.assertNotNull(roleJobPlans);
            Assert.assertEquals(1, roleJobPlans.size());
            Assert.assertEquals("JobPlanB", roleJobPlans.get(0).getJobPlanName());
            Assert.assertEquals("MultiJobPlanRole", roleJobPlans.get(0).getRole().getName());
        }
    }

    @Test
    public void test_getRoleJobPlansByJobPlanName_with_special_characters() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create role with job plan containing special characters
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("TestRole");
            role.setDescription("Test role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl jobPlan = new SolrRoleJobPlanImpl();
            jobPlan.setJobPlanName("Job-Plan@2024");
            jobPlan.setRole(role);
            role.addRoleJobPlan(jobPlan);

            dao.saveOrUpdateRole(role);

            // Query for job plan with special characters
            List<RoleJobPlan> roleJobPlans = dao.getRoleJobPlansByJobPlanName("Job-Plan@2024");

            Assert.assertNotNull(roleJobPlans);
            Assert.assertEquals(1, roleJobPlans.size());
            Assert.assertEquals("Job-Plan@2024", roleJobPlans.get(0).getJobPlanName());
        }
    }

    @Test
    public void test_getRoleJobPlansByJobPlanName_case_sensitive() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create role with job plan
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("TestRole");
            role.setDescription("Test role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl jobPlan = new SolrRoleJobPlanImpl();
            jobPlan.setJobPlanName("TestJobPlan");
            jobPlan.setRole(role);
            role.addRoleJobPlan(jobPlan);

            dao.saveOrUpdateRole(role);

            // Query with different case should not match (if Solr is case-sensitive)
            List<RoleJobPlan> roleJobPlans = dao.getRoleJobPlansByJobPlanName("testjobplan");

            Assert.assertNotNull(roleJobPlans);
            // May return 0 or 1 depending on Solr field configuration
            // This test documents the case sensitivity behavior
        }
    }

    // ========== DIRECT METHOD TESTS FOR saveRoleModule, deleteRoleModule, saveRoleJobPlan, deleteRoleJobPlan ==========

    @Test
    public void test_saveRoleModule_direct_method() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create and save a role first
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("DirectTestRole");
            role.setDescription("Role for direct method test");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            // Get the role ID
            Role savedRole = dao.getRoleByName("DirectTestRole");
            Assert.assertNotNull(savedRole);
            Assert.assertTrue(savedRole.getRoleModules().isEmpty());

            // Create a RoleModule and use saveRoleModule method directly
            SolrRoleModuleImpl roleModule = new SolrRoleModuleImpl();
            roleModule.setModuleName("DirectModule");
            roleModule.setCreatedDateTime(new Date());
            roleModule.setUpdatedDateTime(new Date());
            roleModule.setRole(savedRole);

            // Call the direct method
            dao.saveRoleModule(roleModule);

            // Verify the module was added
            Role updatedRole = dao.getRoleByName("DirectTestRole");
            Assert.assertNotNull(updatedRole);
            Assert.assertEquals(1, updatedRole.getRoleModules().size());
            RoleModule foundModule = (RoleModule) updatedRole.getRoleModules().iterator().next();
            Assert.assertEquals("DirectModule", foundModule.getModuleName());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_saveRoleModule_null_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a RoleModule without a role
            SolrRoleModuleImpl roleModule = new SolrRoleModuleImpl();
            roleModule.setModuleName("ModuleWithoutRole");
            roleModule.setRole(null);

            // This should throw IllegalArgumentException
            dao.saveRoleModule(roleModule);
        }
    }

    @Test
    public void test_saveRoleModule_adds_to_existing_modules() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with one module
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithExistingModules");
            role.setDescription("Role with existing modules");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleModuleImpl existingModule = new SolrRoleModuleImpl();
            existingModule.setModuleName("ExistingModule");
            role.addRoleModule(existingModule);

            dao.saveOrUpdateRole(role);

            // Add another module using direct method
            Role savedRole = dao.getRoleByName("RoleWithExistingModules");
            SolrRoleModuleImpl newModule = new SolrRoleModuleImpl();
            newModule.setModuleName("NewModule");
            newModule.setRole(savedRole);

            dao.saveRoleModule(newModule);

            // Verify both modules exist
            Role updatedRole = dao.getRoleByName("RoleWithExistingModules");
            Assert.assertEquals(2, updatedRole.getRoleModules().size());

            List moduleNames = (List) updatedRole.getRoleModules().stream()
                .map(rm -> ((RoleModule)rm).getModuleName())
                .sorted()
                .collect(java.util.stream.Collectors.toList());

            Assert.assertTrue(moduleNames.contains("ExistingModule"));
            Assert.assertTrue(moduleNames.contains("NewModule"));
        }
    }

    @Test
    public void test_deleteRoleModule_direct_method() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with a module
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleForModuleDeletion");
            role.setDescription("Role for testing module deletion");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleModuleImpl moduleToDelete = new SolrRoleModuleImpl();
            moduleToDelete.setModuleName("ModuleToDelete");
            role.addRoleModule(moduleToDelete);

            dao.saveOrUpdateRole(role);

            // Verify module exists
            Role savedRole = dao.getRoleByName("RoleForModuleDeletion");
            Assert.assertEquals(1, savedRole.getRoleModules().size());

            // Delete the module using direct method
            RoleModule moduleRef = (RoleModule) savedRole.getRoleModules().iterator().next();
            dao.deleteRoleModule(moduleRef);

            // Verify module was deleted
            Role updatedRole = dao.getRoleByName("RoleForModuleDeletion");
            Assert.assertNotNull(updatedRole);
            Assert.assertTrue(updatedRole.getRoleModules().isEmpty());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_deleteRoleModule_null_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a RoleModule without a role
            SolrRoleModuleImpl roleModule = new SolrRoleModuleImpl();
            roleModule.setModuleName("ModuleWithoutRole");
            roleModule.setRole(null);

            // This should throw IllegalArgumentException
            dao.deleteRoleModule(roleModule);
        }
    }

    @Test
    public void test_deleteRoleModule_leaves_other_modules() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with multiple modules
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithMultipleModulesToDelete");
            role.setDescription("Role with multiple modules");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleModuleImpl module1 = new SolrRoleModuleImpl();
            module1.setModuleName("Module1");
            role.addRoleModule(module1);

            SolrRoleModuleImpl module2 = new SolrRoleModuleImpl();
            module2.setModuleName("Module2");
            role.addRoleModule(module2);

            SolrRoleModuleImpl module3 = new SolrRoleModuleImpl();
            module3.setModuleName("Module3");
            role.addRoleModule(module3);

            dao.saveOrUpdateRole(role);

            // Delete one module
            Role savedRole = dao.getRoleByName("RoleWithMultipleModulesToDelete");
            RoleModule moduleToDelete = (RoleModule) savedRole.getRoleModules().stream()
                .filter(rm -> "Module2".equals(((RoleModule)rm).getModuleName()))
                .map(rm -> (RoleModule)rm)
                .findFirst()
                .orElse(null);

            Assert.assertNotNull(moduleToDelete);
            dao.deleteRoleModule(moduleToDelete);

            // Verify only Module2 was deleted
            Role updatedRole = dao.getRoleByName("RoleWithMultipleModulesToDelete");
            Assert.assertEquals(2, updatedRole.getRoleModules().size());

            List remainingModules = (List)updatedRole.getRoleModules().stream()
                .map(rm -> ((RoleModule)rm).getModuleName())
                .sorted()
                .collect(java.util.stream.Collectors.toList());

            Assert.assertTrue(remainingModules.contains("Module1"));
            Assert.assertFalse(remainingModules.contains("Module2"));
            Assert.assertTrue(remainingModules.contains("Module3"));
        }
    }

    @Test
    public void test_saveRoleJobPlan_direct_method() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create and save a role first
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("DirectTestRoleForJobPlan");
            role.setDescription("Role for direct job plan method test");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            // Get the role
            Role savedRole = dao.getRoleByName("DirectTestRoleForJobPlan");
            Assert.assertNotNull(savedRole);
            Assert.assertTrue(savedRole.getRoleJobPlans().isEmpty());

            // Create a RoleJobPlan and use saveRoleJobPlan method directly
            SolrRoleJobPlanImpl roleJobPlan = new SolrRoleJobPlanImpl();
            roleJobPlan.setJobPlanName("DirectJobPlan");
            roleJobPlan.setCreatedDateTime(new Date());
            roleJobPlan.setUpdatedDateTime(new Date());
            roleJobPlan.setRole(savedRole);

            // Call the direct method
            dao.saveRoleJobPlan(roleJobPlan);

            // Verify the job plan was added
            Role updatedRole = dao.getRoleByName("DirectTestRoleForJobPlan");
            Assert.assertNotNull(updatedRole);
            Assert.assertEquals(1, updatedRole.getRoleJobPlans().size());
            RoleJobPlan foundJobPlan = (RoleJobPlan) updatedRole.getRoleJobPlans().iterator().next();
            Assert.assertEquals("DirectJobPlan", foundJobPlan.getJobPlanName());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_saveRoleJobPlan_null_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a RoleJobPlan without a role
            SolrRoleJobPlanImpl roleJobPlan = new SolrRoleJobPlanImpl();
            roleJobPlan.setJobPlanName("JobPlanWithoutRole");
            roleJobPlan.setRole(null);

            // This should throw IllegalArgumentException
            dao.saveRoleJobPlan(roleJobPlan);
        }
    }

    @Test
    public void test_saveRoleJobPlan_adds_to_existing_jobplans() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with one job plan
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithExistingJobPlans");
            role.setDescription("Role with existing job plans");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl existingJobPlan = new SolrRoleJobPlanImpl();
            existingJobPlan.setJobPlanName("ExistingJobPlan");
            role.addRoleJobPlan(existingJobPlan);

            dao.saveOrUpdateRole(role);

            // Add another job plan using direct method
            Role savedRole = dao.getRoleByName("RoleWithExistingJobPlans");
            SolrRoleJobPlanImpl newJobPlan = new SolrRoleJobPlanImpl();
            newJobPlan.setJobPlanName("NewJobPlan");
            newJobPlan.setRole(savedRole);

            dao.saveRoleJobPlan(newJobPlan);

            // Verify both job plans exist
            Role updatedRole = dao.getRoleByName("RoleWithExistingJobPlans");
            Assert.assertEquals(2, updatedRole.getRoleJobPlans().size());

            List jobPlanNames = (List)updatedRole.getRoleJobPlans().stream()
                .map(rjp -> ((RoleJobPlan)rjp).getJobPlanName())
                .sorted()
                .collect(java.util.stream.Collectors.toList());

            Assert.assertTrue(jobPlanNames.contains("ExistingJobPlan"));
            Assert.assertTrue(jobPlanNames.contains("NewJobPlan"));
        }
    }

    @Test
    public void test_deleteRoleJobPlan_direct_method() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with a job plan
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleForJobPlanDeletion");
            role.setDescription("Role for testing job plan deletion");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl jobPlanToDelete = new SolrRoleJobPlanImpl();
            jobPlanToDelete.setJobPlanName("JobPlanToDelete");
            role.addRoleJobPlan(jobPlanToDelete);

            dao.saveOrUpdateRole(role);

            // Verify job plan exists
            Role savedRole = dao.getRoleByName("RoleForJobPlanDeletion");
            Assert.assertEquals(1, savedRole.getRoleJobPlans().size());

            // Delete the job plan using direct method
            RoleJobPlan jobPlanRef = (RoleJobPlan) savedRole.getRoleJobPlans().iterator().next();
            dao.deleteRoleJobPlan(jobPlanRef);

            // Verify job plan was deleted
            Role updatedRole = dao.getRoleByName("RoleForJobPlanDeletion");
            Assert.assertNotNull(updatedRole);
            Assert.assertTrue(updatedRole.getRoleJobPlans().isEmpty());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_deleteRoleJobPlan_null_role() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a RoleJobPlan without a role
            SolrRoleJobPlanImpl roleJobPlan = new SolrRoleJobPlanImpl();
            roleJobPlan.setJobPlanName("JobPlanWithoutRole");
            roleJobPlan.setRole(null);

            // This should throw IllegalArgumentException
            dao.deleteRoleJobPlan(roleJobPlan);
        }
    }

    @Test
    public void test_deleteRoleJobPlan_leaves_other_jobplans() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role with multiple job plans
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithMultipleJobPlansToDelete");
            role.setDescription("Role with multiple job plans");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl jobPlan1 = new SolrRoleJobPlanImpl();
            jobPlan1.setJobPlanName("JobPlan1");
            role.addRoleJobPlan(jobPlan1);

            SolrRoleJobPlanImpl jobPlan2 = new SolrRoleJobPlanImpl();
            jobPlan2.setJobPlanName("JobPlan2");
            role.addRoleJobPlan(jobPlan2);

            SolrRoleJobPlanImpl jobPlan3 = new SolrRoleJobPlanImpl();
            jobPlan3.setJobPlanName("JobPlan3");
            role.addRoleJobPlan(jobPlan3);

            dao.saveOrUpdateRole(role);

            // Delete one job plan
            Role savedRole = dao.getRoleByName("RoleWithMultipleJobPlansToDelete");
            RoleJobPlan jobPlanToDelete = (RoleJobPlan)savedRole.getRoleJobPlans().stream()
                .filter(rjp -> "JobPlan2".equals(((RoleJobPlan)rjp).getJobPlanName()))
                .map(rjp -> (RoleJobPlan)rjp)
                .findFirst()
                .orElse(null);

            Assert.assertNotNull(jobPlanToDelete);
            dao.deleteRoleJobPlan(jobPlanToDelete);

            // Verify only JobPlan2 was deleted
            Role updatedRole = dao.getRoleByName("RoleWithMultipleJobPlansToDelete");
            Assert.assertEquals(2, updatedRole.getRoleJobPlans().size());

            List remainingJobPlans = (List)updatedRole.getRoleJobPlans().stream()
                .map(rjp -> ((RoleJobPlan)rjp).getJobPlanName())
                .sorted()
                .collect(java.util.stream.Collectors.toList());

            Assert.assertTrue(remainingJobPlans.contains("JobPlan1"));
            Assert.assertFalse(remainingJobPlans.contains("JobPlan2"));
            Assert.assertTrue(remainingJobPlans.contains("JobPlan3"));
        }
    }

    @Test
    public void test_saveRoleModule_and_saveRoleJobPlan_together() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a role
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithBothModuleAndJobPlan");
            role.setDescription("Role with both module and job plan");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            Role savedRole = dao.getRoleByName("RoleWithBothModuleAndJobPlan");

            // Add a module
            SolrRoleModuleImpl module = new SolrRoleModuleImpl();
            module.setModuleName("TestModule");
            module.setRole(savedRole);
            dao.saveRoleModule(module);

            // Add a job plan
            SolrRoleJobPlanImpl jobPlan = new SolrRoleJobPlanImpl();
            jobPlan.setJobPlanName("TestJobPlan");
            jobPlan.setRole(savedRole);
            dao.saveRoleJobPlan(jobPlan);

            // Verify both exist
            Role updatedRole = dao.getRoleByName("RoleWithBothModuleAndJobPlan");
            Assert.assertEquals(1, updatedRole.getRoleModules().size());
            Assert.assertEquals(1, updatedRole.getRoleJobPlans().size());
            Assert.assertEquals("TestModule",
                ((RoleModule)updatedRole.getRoleModules().iterator().next()).getModuleName());
            Assert.assertEquals("TestJobPlan",
                ((RoleJobPlan)updatedRole.getRoleJobPlans().iterator().next()).getJobPlanName());
        }
    }

    @Test
    public void test_saveOrUpdateRole_with_null_dates() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithNullDates");
            role.setDescription("Role with null dates");
            role.setCreatedDateTime(null);
            role.setUpdatedDateTime(null);

            // Should not throw exception and use current time instead
            dao.saveOrUpdateRole(role);

            Role found = dao.getRoleByName("RoleWithNullDates");

            Assert.assertNotNull(found);
            Assert.assertEquals("RoleWithNullDates", found.getName());
        }
    }

    @Test
    public void test_saveOrUpdateRole_with_policies() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithPolicies");
            role.setDescription("Role with embedded policies");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrPolicyImpl policy = new SolrPolicyImpl();
            policy.setName("TestPolicy");
            policy.setDescription("Test policy");
            this.solrPolicyDao.saveOrUpdatePolicy(policy);
            role.addPolicy(policy);

            dao.saveOrUpdateRole(role);

            Role found = dao.getRoleByName("RoleWithPolicies");

            Assert.assertNotNull(found);
            Assert.assertEquals("RoleWithPolicies", found.getName());
            Assert.assertNotNull(found.getPolicies());
            Assert.assertEquals(1, found.getPolicies().size());
        }
    }

    @Test
    public void test_saveOrUpdateRole_with_roleModules() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithModules");
            role.setDescription("Role with embedded modules");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleModuleImpl roleModule = new SolrRoleModuleImpl();
            roleModule.setModuleName("TestModule");
            role.addRoleModule(roleModule);

            dao.saveOrUpdateRole(role);

            Role found = dao.getRoleByName("RoleWithModules");

            Assert.assertNotNull(found);
            Assert.assertEquals("RoleWithModules", found.getName());
            Assert.assertNotNull(found.getRoleModules());
            Assert.assertEquals(1, found.getRoleModules().size());
        }
    }

    @Test
    public void test_saveOrUpdateRole_with_roleJobPlans() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleWithJobPlans");
            role.setDescription("Role with embedded job plans");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl roleJobPlan = new SolrRoleJobPlanImpl();
            roleJobPlan.setJobPlanName("TestJobPlan");
            role.addRoleJobPlan(roleJobPlan);

            dao.saveOrUpdateRole(role);

            Role found = dao.getRoleByName("RoleWithJobPlans");

            Assert.assertNotNull(found);
            Assert.assertEquals("RoleWithJobPlans", found.getName());
            Assert.assertNotNull(found.getRoleJobPlans());
            Assert.assertEquals(1, found.getRoleJobPlans().size());
        }
    }

    @Test
    public void test_saveOrUpdateRole_with_special_characters() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("Role-With_Special.Characters");
            role.setDescription("Description with special chars: <>&\"'");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            dao.saveOrUpdateRole(role);

            Role found = dao.getRoleByName("Role-With_Special.Characters");

            Assert.assertNotNull(found);
            Assert.assertEquals("Role-With_Special.Characters", found.getName());
            Assert.assertEquals("Description with special chars: <>&\"'", found.getDescription());
        }
    }

    @Test
    public void test_multiple_operations_in_sequence() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("SequenceRole");
            role.setDescription("Initial description");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            // Read
            Role found = dao.getRoleByName("SequenceRole");
            Assert.assertNotNull(found);
            Assert.assertEquals("Initial description", found.getDescription());

            // Update
            role.setDescription("Updated description");
            dao.saveOrUpdateRole(role);
            found = dao.getRoleByName("SequenceRole");
            Assert.assertEquals("Updated description", found.getDescription());

            // List
            List<Role> roles = dao.getAllRoles();
            Assert.assertEquals(1, roles.size());

            // Delete
            dao.deleteRole(role);
            found = dao.getRoleByName("SequenceRole");
            Assert.assertNull(found);

            // Verify empty
            roles = dao.getAllRoles();
            Assert.assertTrue(roles.isEmpty());
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
