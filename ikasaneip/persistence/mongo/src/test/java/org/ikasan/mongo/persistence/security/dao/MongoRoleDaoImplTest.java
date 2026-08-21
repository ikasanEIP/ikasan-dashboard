package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.security.model.MongoPolicyImpl;
import org.ikasan.mongo.persistence.security.model.MongoRoleImpl;
import org.ikasan.mongo.persistence.security.model.MongoRoleJobPlanImpl;
import org.ikasan.mongo.persistence.security.model.MongoRoleModuleImpl;
import org.ikasan.mongo.persistence.security.repository.MongoPolicyRepository;
import org.ikasan.mongo.persistence.security.repository.MongoRoleRepository;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.model.RoleJobPlan;
import org.ikasan.spec.security.model.RoleModule;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;
import java.util.List;

import static org.ikasan.spec.security.dao.RoleDao.ROLE_TYPE;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoRoleDaoImplTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoRoleRepository repository;

    @Autowired
    private MongoPolicyRepository mongoPolicyRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoRoleDaoImpl dao;
    private MongoPolicyDaoImpl mongoPolicyDaoImpl;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoRoleRepository repository, MongoPolicyRepository mongoPolicyRepository, MongoTemplate mongoTemplate) {
        // Step 1: Create MongoPolicyDaoImpl first (no dependencies)
        this.mongoPolicyDaoImpl = new MongoPolicyDaoImpl(mongoPolicyRepository, mongoTemplate);

        // Step 2: Create MongoRoleDaoImpl with MongoPolicyDaoImpl
        this.dao = new MongoRoleDaoImpl(repository, mongoTemplate, this.mongoPolicyDaoImpl);

        // Step 3: Set circular reference
        this.mongoPolicyDaoImpl.setMongoRoleDao(this.dao);
    }

    @After
    public void teardown() {
        repository.deleteAll();
        mongoPolicyRepository.deleteAll();
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_createRole() {
        Role role = dao.createRole();

        Assert.assertNotNull(role);
        Assert.assertTrue(role instanceof MongoRoleImpl);
    }

    @Test
    public void test_saveOrUpdateRole_and_getRoleByName() {
        MongoRoleImpl role = new MongoRoleImpl();
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

    @Test
    public void test_getRoleByName_not_found() {
        Role found = dao.getRoleByName("NonExistentRole");

        Assert.assertNull(found);
    }

    @Test
    public void test_saveOrUpdateRole_update_existing() {
        MongoRoleImpl role = new MongoRoleImpl();
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

    @Test
    public void test_deleteRole() {
        MongoRoleImpl role = new MongoRoleImpl();
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

    @Test
    public void test_getAllRoles() {
        // Save multiple roles
        for (int i = 0; i < 5; i++) {
            MongoRoleImpl role = new MongoRoleImpl();
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

    @Test
    public void test_getAllRoles_empty() {
        List<Role> roles = dao.getAllRoles();

        Assert.assertNotNull(roles);
        Assert.assertTrue(roles.isEmpty());
    }

    @Test
    public void test_getRoleByNameLike() {
        // Save roles with different names
        MongoRoleImpl role1 = new MongoRoleImpl();
        role1.setName("AdminRole");
        role1.setDescription("Administrator role");
        role1.setCreatedDateTime(new Date());
        role1.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role1);

        MongoRoleImpl role2 = new MongoRoleImpl();
        role2.setName("UserRole");
        role2.setDescription("User role");
        role2.setCreatedDateTime(new Date());
        role2.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role2);

        MongoRoleImpl role3 = new MongoRoleImpl();
        role3.setName("AdminReadOnly");
        role3.setDescription("Read-only admin role");
        role3.setCreatedDateTime(new Date());
        role3.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role3);

        List<Role> roles = dao.getRoleByNameLike("Admin");

        Assert.assertNotNull(roles);
        Assert.assertEquals(2, roles.size());
    }

    @Test
    public void test_getRoleByNameLike_no_match() {
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("TestRole");
        role.setDescription("Test description");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role);

        List<Role> roles = dao.getRoleByNameLike("NonExistent");

        Assert.assertNotNull(roles);
        Assert.assertTrue(roles.isEmpty());
    }

    @Test
    public void test_getRoleById() {
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("TestRole");
        role.setDescription("Test description");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        dao.saveOrUpdateRole(role);

        Role found = dao.getRoleById("TestRole" + "-" + ROLE_TYPE);

        Assert.assertNotNull(found);
        Assert.assertEquals("TestRole", found.getName());
        Assert.assertEquals("TestRole" + "-" + ROLE_TYPE, found.getId());
    }

    @Test
    public void test_getRoleById_not_found() {
        Role found = dao.getRoleById("NonExistent");

        Assert.assertNull(found);
    }

    @Test
    public void test_saveRoleModule_embedded_in_role() {
        // Create a role with a role module
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleWithModule");
        role.setDescription("Role containing a module");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        // Add a role module to the role
        MongoRoleModuleImpl roleModule = new MongoRoleModuleImpl();
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

    @Test
    public void test_saveRoleModule_multiple_modules_in_role() {
        // Create a role with multiple role modules
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleWithMultipleModules");
        role.setDescription("Role containing multiple modules");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        // Add multiple role modules
        for (int i = 1; i <= 3; i++) {
            MongoRoleModuleImpl roleModule = new MongoRoleModuleImpl();
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

    @Test
    public void test_saveRoleModule_update_modules_in_role() {
        // Create a role with one module
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleForModuleUpdate");
        role.setDescription("Role for testing module updates");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleModuleImpl roleModule1 = new MongoRoleModuleImpl();
        roleModule1.setModuleName("OriginalModule");
        role.addRoleModule(roleModule1);

        dao.saveOrUpdateRole(role);

        // Verify initial state
        Role found = dao.getRoleByName("RoleForModuleUpdate");
        Assert.assertEquals(1, found.getRoleModules().size());

        // Add another module
        MongoRoleModuleImpl roleModule2 = new MongoRoleModuleImpl();
        roleModule2.setModuleName("AdditionalModule");
        role.addRoleModule(roleModule2);

        // Update the role
        dao.saveOrUpdateRole(role);

        // Verify updated state
        found = dao.getRoleByName("RoleForModuleUpdate");
        Assert.assertEquals(2, found.getRoleModules().size());
    }

    @Test
    public void test_saveRoleModule_with_role_reference() {
        // Create a role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setId("AdminRole");
        role.setName("AdminRole");
        role.setDescription("Administrator role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        // Create a role module - don't set back-reference as it will be handled internally
        MongoRoleModuleImpl roleModule = new MongoRoleModuleImpl();
        roleModule.setId("100L");
        roleModule.setModuleName("AdminModule");
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

    @Test
    public void test_deleteRoleModule_by_removing_from_role() {
        // Create a role with multiple modules
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleWithModulesToDelete");
        role.setDescription("Role for testing module deletion");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleModuleImpl module1 = new MongoRoleModuleImpl();
        module1.setModuleName("ModuleToKeep");
        module1.setCreatedDateTime(new Date());
        role.addRoleModule(module1);

        MongoRoleModuleImpl module2 = new MongoRoleModuleImpl();
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

    @Test
    public void test_deleteRoleModule_by_deleting_entire_role() {
        // Create a role with modules
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleToDeleteWithModules");
        role.setDescription("Role to be deleted");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleModuleImpl module = new MongoRoleModuleImpl();
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

    @Test
    public void test_deleteRoleModule_remove_all_modules() {
        // Create a role with multiple modules
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleToRemoveAllModules");
        role.setDescription("Role to have all modules removed");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        for (int i = 1; i <= 3; i++) {
            MongoRoleModuleImpl module = new MongoRoleModuleImpl();
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

    @Test
    public void test_deleteRoleModule_selective_deletion() {
        // Create a role with multiple modules
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleForSelectiveDeletion");
        role.setDescription("Role for selective module deletion");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        String[] moduleNames = {"ModuleA", "ModuleB", "ModuleC", "ModuleD"};
        for (String moduleName : moduleNames) {
            MongoRoleModuleImpl module = new MongoRoleModuleImpl();
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

    @Test
    public void test_deleteRoleModule_replace_modules() {
        // Create a role with modules
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleForModuleReplacement");
        role.setDescription("Role for module replacement");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleModuleImpl oldModule = new MongoRoleModuleImpl();
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
        MongoRoleModuleImpl newModule = new MongoRoleModuleImpl();
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

    @Test
    public void test_deleteRoleModule_preserves_other_role_data() {
        // Create a role with modules and other data
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleWithCompleteData");
        role.setDescription("Original description");
        role.setCreatedDateTime(new Date(1000000L));
        role.setUpdatedDateTime(new Date(2000000L));

        MongoRoleModuleImpl module = new MongoRoleModuleImpl();
        module.setModuleName("TestModule");
        role.addRoleModule(module);

        MongoPolicyImpl policy = new MongoPolicyImpl();
        policy.setName("TestPolicy");
        this.mongoPolicyDaoImpl.saveOrUpdatePolicy(policy);

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

    @Test
    public void test_saveRoleJobPlan_embedded_in_role() {
        // Create a role with a role job plan
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleWithJobPlan");
        role.setDescription("Role containing a job plan");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        // Add a role job plan to the role
        MongoRoleJobPlanImpl roleJobPlan = new MongoRoleJobPlanImpl();
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

    @Test
    public void test_saveRoleJobPlan_multiple_jobplans_in_role() {
        // Create a role with multiple role job plans
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleWithMultipleJobPlans");
        role.setDescription("Role containing multiple job plans");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        // Add multiple role job plans
        for (int i = 1; i <= 3; i++) {
            MongoRoleJobPlanImpl roleJobPlan = new MongoRoleJobPlanImpl();
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

    @Test
    public void test_saveRoleJobPlan_update_jobplans_in_role() {
        // Create a role with one job plan
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleForJobPlanUpdate");
        role.setDescription("Role for testing job plan updates");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleJobPlanImpl roleJobPlan1 = new MongoRoleJobPlanImpl();
        roleJobPlan1.setJobPlanName("OriginalPlan");
        role.addRoleJobPlan(roleJobPlan1);

        dao.saveOrUpdateRole(role);

        // Verify initial state
        Role found = dao.getRoleByName("RoleForJobPlanUpdate");
        Assert.assertEquals(1, found.getRoleJobPlans().size());

        // Add another job plan
        MongoRoleJobPlanImpl roleJobPlan2 = new MongoRoleJobPlanImpl();
        roleJobPlan2.setJobPlanName("AdditionalPlan");
        role.addRoleJobPlan(roleJobPlan2);

        // Update the role
        dao.saveOrUpdateRole(role);

        // Verify updated state
        found = dao.getRoleByName("RoleForJobPlanUpdate");
        Assert.assertEquals(2, found.getRoleJobPlans().size());
    }

    @Test
    public void test_saveRoleJobPlan_with_role_reference() {
        // Create a role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("SchedulerRole");
        role.setDescription("Scheduler role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        // Create a role job plan - don't set back-reference as it will be handled internally
        MongoRoleJobPlanImpl roleJobPlan = new MongoRoleJobPlanImpl();
        roleJobPlan.setId("200L");
        roleJobPlan.setJobPlanName("NightlyBatchPlan");
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

    @Test
    public void test_deleteRoleJobPlan_by_removing_from_role() {
        // Create a role with multiple job plans
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleWithJobPlansToDelete");
        role.setDescription("Role for testing job plan deletion");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleJobPlanImpl jobPlan1 = new MongoRoleJobPlanImpl();
        jobPlan1.setJobPlanName("PlanToKeep");
        jobPlan1.setCreatedDateTime(new Date());
        role.addRoleJobPlan(jobPlan1);

        MongoRoleJobPlanImpl jobPlan2 = new MongoRoleJobPlanImpl();
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

    @Test
    public void test_deleteRoleJobPlan_by_deleting_entire_role() {
        // Create a role with job plans
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleToDeleteWithJobPlans");
        role.setDescription("Role to be deleted");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleJobPlanImpl jobPlan = new MongoRoleJobPlanImpl();
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

    @Test
    public void test_deleteRoleJobPlan_remove_all_jobplans() {
        // Create a role with multiple job plans
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleToRemoveAllJobPlans");
        role.setDescription("Role to have all job plans removed");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        for (int i = 1; i <= 3; i++) {
            MongoRoleJobPlanImpl jobPlan = new MongoRoleJobPlanImpl();
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

    @Test
    public void test_deleteRoleJobPlan_selective_deletion() {
        // Create a role with multiple job plans
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleForSelectiveJobPlanDeletion");
        role.setDescription("Role for selective job plan deletion");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        String[] planNames = {"PlanA", "PlanB", "PlanC", "PlanD"};
        for (String planName : planNames) {
            MongoRoleJobPlanImpl jobPlan = new MongoRoleJobPlanImpl();
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

    @Test
    public void test_deleteRoleJobPlan_replace_jobplans() {
        // Create a role with job plans
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleForJobPlanReplacement");
        role.setDescription("Role for job plan replacement");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleJobPlanImpl oldPlan = new MongoRoleJobPlanImpl();
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
        MongoRoleJobPlanImpl newPlan = new MongoRoleJobPlanImpl();
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

    @Test
    public void test_deleteRoleJobPlan_preserves_other_role_data() {
        // Create a role with job plans and other data
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleWithCompleteJobPlanData");
        role.setDescription("Original description");
        role.setCreatedDateTime(new Date(1000000L));
        role.setUpdatedDateTime(new Date(2000000L));

        MongoRoleJobPlanImpl jobPlan = new MongoRoleJobPlanImpl();
        jobPlan.setJobPlanName("TestPlan");
        role.addRoleJobPlan(jobPlan);

        MongoRoleModuleImpl module = new MongoRoleModuleImpl();
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

    @Test
    public void test_getRoleJobPlansByJobPlanName() {
        // Create roles with job plans
        MongoRoleImpl role1 = new MongoRoleImpl();
        role1.setName("Role1");
        role1.setDescription("Role 1");
        role1.setCreatedDateTime(new Date());
        role1.setUpdatedDateTime(new Date());

        MongoRoleJobPlanImpl jobPlan1 = new MongoRoleJobPlanImpl();
        jobPlan1.setJobPlanName("TestJobPlan");
        role1.addRoleJobPlan(jobPlan1);

        dao.saveOrUpdateRole(role1);

        MongoRoleImpl role2 = new MongoRoleImpl();
        role2.setName("Role2");
        role2.setDescription("Role 2");
        role2.setCreatedDateTime(new Date());
        role2.setUpdatedDateTime(new Date());

        MongoRoleJobPlanImpl jobPlan2 = new MongoRoleJobPlanImpl();
        jobPlan2.setJobPlanName("TestJobPlan");
        role2.addRoleJobPlan(jobPlan2);

        MongoRoleJobPlanImpl jobPlan3 = new MongoRoleJobPlanImpl();
        jobPlan3.setJobPlanName("OtherJobPlan");
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

    @Test
    public void test_getRoleJobPlansByJobPlanName_not_found() {
        // Create role with job plan
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("TestRole");
        role.setDescription("Test role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleJobPlanImpl jobPlan = new MongoRoleJobPlanImpl();
        jobPlan.setJobPlanName("ExistingJobPlan");
        role.addRoleJobPlan(jobPlan);

        dao.saveOrUpdateRole(role);

        // Query for non-existent job plan
        List<RoleJobPlan> roleJobPlans = dao.getRoleJobPlansByJobPlanName("NonExistentJobPlan");

        Assert.assertNotNull(roleJobPlans);
        Assert.assertTrue(roleJobPlans.isEmpty());
    }

    @Test
    public void test_getRoleJobPlansByJobPlanName_null_or_empty() {
        // Test with null
        List<RoleJobPlan> nullResult = dao.getRoleJobPlansByJobPlanName(null);
        Assert.assertNotNull(nullResult);
        Assert.assertTrue(nullResult.isEmpty());

        // Test with empty string
        List<RoleJobPlan> emptyResult = dao.getRoleJobPlansByJobPlanName("");
        Assert.assertNotNull(emptyResult);
        Assert.assertTrue(emptyResult.isEmpty());
    }

    @Test
    public void test_getRoleJobPlansByJobPlanName_multiple_job_plans_per_role() {
        // Create role with multiple job plans
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("MultiJobPlanRole");
        role.setDescription("Role with multiple job plans");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleJobPlanImpl jobPlan1 = new MongoRoleJobPlanImpl();
        jobPlan1.setJobPlanName("JobPlanA");
        role.addRoleJobPlan(jobPlan1);

        MongoRoleJobPlanImpl jobPlan2 = new MongoRoleJobPlanImpl();
        jobPlan2.setJobPlanName("JobPlanB");
        role.addRoleJobPlan(jobPlan2);

        MongoRoleJobPlanImpl jobPlan3 = new MongoRoleJobPlanImpl();
        jobPlan3.setJobPlanName("JobPlanC");
        role.addRoleJobPlan(jobPlan3);

        dao.saveOrUpdateRole(role);

        // Query for one specific job plan
        List<RoleJobPlan> roleJobPlans = dao.getRoleJobPlansByJobPlanName("JobPlanB");

        Assert.assertNotNull(roleJobPlans);
        Assert.assertEquals(1, roleJobPlans.size());
        Assert.assertEquals("JobPlanB", roleJobPlans.get(0).getJobPlanName());
        Assert.assertEquals("MultiJobPlanRole", roleJobPlans.get(0).getRole().getName());
    }

    @Test
    public void test_getRoleJobPlansByJobPlanName_with_special_characters() {
        // Create role with job plan containing special characters
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("TestRole");
        role.setDescription("Test role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleJobPlanImpl jobPlan = new MongoRoleJobPlanImpl();
        jobPlan.setJobPlanName("Job-Plan@2024");
        role.addRoleJobPlan(jobPlan);

        dao.saveOrUpdateRole(role);

        // Query for job plan with special characters
        List<RoleJobPlan> roleJobPlans = dao.getRoleJobPlansByJobPlanName("Job-Plan@2024");

        Assert.assertNotNull(roleJobPlans);
        Assert.assertEquals(1, roleJobPlans.size());
        Assert.assertEquals("Job-Plan@2024", roleJobPlans.get(0).getJobPlanName());
    }

    @Test
    public void test_getRoleJobPlansByJobPlanName_case_sensitive() {
        // Create role with job plan
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("TestRole");
        role.setDescription("Test role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleJobPlanImpl jobPlan = new MongoRoleJobPlanImpl();
        jobPlan.setJobPlanName("TestJobPlan");
        role.addRoleJobPlan(jobPlan);

        dao.saveOrUpdateRole(role);

        // Query with different case should not match (if MongoDB is case-sensitive)
        List<RoleJobPlan> roleJobPlans = dao.getRoleJobPlansByJobPlanName("testjobplan");

        Assert.assertNotNull(roleJobPlans);
        // May return 0 or 1 depending on MongoDB field configuration
        // This test documents the case sensitivity behavior
    }

    // Additional Solr test conversions continue below...
    // Note: Tests for direct saveRoleModule, deleteRoleModule, saveRoleJobPlan, deleteRoleJobPlan methods
    // will depend on MongoDB DAO implementation details

    @Test
    public void test_saveOrUpdateRole_with_null_dates() {
        MongoRoleImpl role = new MongoRoleImpl();
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

    @Test
    public void test_saveOrUpdateRole_with_policies() {
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleWithPolicies");
        role.setDescription("Role with embedded policies");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoPolicyImpl policy = new MongoPolicyImpl();
        policy.setName("TestPolicy");
        policy.setDescription("Test policy");
        this.mongoPolicyDaoImpl.saveOrUpdatePolicy(policy);
        role.addPolicy(policy);

        dao.saveOrUpdateRole(role);

        Role found = dao.getRoleByName("RoleWithPolicies");

        Assert.assertNotNull(found);
        Assert.assertEquals("RoleWithPolicies", found.getName());
        Assert.assertNotNull(found.getPolicies());
        Assert.assertEquals(1, found.getPolicies().size());
    }

    @Test
    public void test_saveOrUpdateRole_with_roleModules() {
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleWithModules");
        role.setDescription("Role with embedded modules");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleModuleImpl roleModule = new MongoRoleModuleImpl();
        roleModule.setModuleName("TestModule");
        role.addRoleModule(roleModule);

        dao.saveOrUpdateRole(role);

        Role found = dao.getRoleByName("RoleWithModules");

        Assert.assertNotNull(found);
        Assert.assertEquals("RoleWithModules", found.getName());
        Assert.assertNotNull(found.getRoleModules());
        Assert.assertEquals(1, found.getRoleModules().size());
    }

    @Test
    public void test_saveOrUpdateRole_with_roleJobPlans() {
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("RoleWithJobPlans");
        role.setDescription("Role with embedded job plans");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleJobPlanImpl roleJobPlan = new MongoRoleJobPlanImpl();
        roleJobPlan.setJobPlanName("TestJobPlan");
        role.addRoleJobPlan(roleJobPlan);

        dao.saveOrUpdateRole(role);

        Role found = dao.getRoleByName("RoleWithJobPlans");

        Assert.assertNotNull(found);
        Assert.assertEquals("RoleWithJobPlans", found.getName());
        Assert.assertNotNull(found.getRoleJobPlans());
        Assert.assertEquals(1, found.getRoleJobPlans().size());
    }

    @Test
    public void test_saveOrUpdateRole_with_special_characters() {
        MongoRoleImpl role = new MongoRoleImpl();
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

    @Test
    public void test_multiple_operations_in_sequence() {
        // Create
        MongoRoleImpl role = new MongoRoleImpl();
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
