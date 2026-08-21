package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.security.model.MongoPolicyImpl;
import org.ikasan.mongo.persistence.security.model.MongoRoleImpl;
import org.ikasan.mongo.persistence.security.model.MongoRoleJobPlanImpl;
import org.ikasan.mongo.persistence.security.repository.MongoPolicyRepository;
import org.ikasan.mongo.persistence.security.repository.MongoRoleRepository;
import org.ikasan.spec.security.model.Policy;
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

import static org.ikasan.spec.security.dao.PolicyDao.POLICY_TYPE;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoPolicyDaoImplTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoPolicyRepository repository;

    @Autowired
    private MongoRoleRepository mongoRoleRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoPolicyDaoImpl dao;
    private MongoRoleDaoImpl mongoRoleDaoImpl;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoPolicyRepository repository, MongoRoleRepository mongoRoleRepository, MongoTemplate mongoTemplate) {
        // Step 1: Create MongoPolicyDaoImpl first (no dependencies)
        this.dao = new MongoPolicyDaoImpl(repository, mongoTemplate);

        // Step 2: Create MongoRoleDaoImpl with MongoPolicyDaoImpl
        this.mongoRoleDaoImpl = new MongoRoleDaoImpl(mongoRoleRepository, mongoTemplate, this.dao);

        // Step 3: Set circular reference
        this.dao.setMongoRoleDao(this.mongoRoleDaoImpl);
    }

    @After
    public void teardown() {
        repository.deleteAll();
        mongoRoleRepository.deleteAll();
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_createPolicy() {
        Policy policy = dao.createPolicy();

        Assert.assertNotNull(policy);
        Assert.assertTrue(policy instanceof MongoPolicyImpl);
    }

    @Test
    public void test_saveOrUpdatePolicy_and_getPolicyByName() {
        MongoPolicyImpl policy = new MongoPolicyImpl();
        policy.setName("TestPolicy");
        policy.setDescription("Test policy description");
        policy.setCreatedDateTime(new Date(1000000L));
        policy.setUpdatedDateTime(new Date(2000000L));

        dao.saveOrUpdatePolicy(policy);

        Policy found = dao.getPolicyByName("TestPolicy");

        Assert.assertNotNull(found);
        Assert.assertEquals("TestPolicy", found.getName());
        Assert.assertEquals("Test policy description", found.getDescription());
    }

    @Test
    public void test_getPolicyByName_not_found() {
        Policy found = dao.getPolicyByName("NonExistentPolicy");

        Assert.assertNull(found);
    }

    @Test
    public void test_saveOrUpdatePolicy_update_existing() {
        MongoPolicyImpl policy = new MongoPolicyImpl();
        policy.setName("TestPolicy");
        policy.setDescription("Original description");
        policy.setCreatedDateTime(new Date(1000000L));
        policy.setUpdatedDateTime(new Date(2000000L));

        dao.saveOrUpdatePolicy(policy);

        Policy found = dao.getPolicyByName("TestPolicy");
        Assert.assertEquals("Original description", found.getDescription());

        // Update the policy
        policy.setDescription("Updated description");
        policy.setUpdatedDateTime(new Date(3000000L));
        dao.saveOrUpdatePolicy(policy);

        found = dao.getPolicyByName("TestPolicy");

        Assert.assertNotNull(found);
        Assert.assertEquals("TestPolicy", found.getName());
        Assert.assertEquals("Updated description", found.getDescription());
    }

    @Test
    public void test_deletePolicy() {
        MongoPolicyImpl policy = new MongoPolicyImpl();
        policy.setName("PolicyToDelete");
        policy.setDescription("This policy will be deleted");
        policy.setCreatedDateTime(new Date());
        policy.setUpdatedDateTime(new Date());

        dao.saveOrUpdatePolicy(policy);

        Policy found = dao.getPolicyByName("PolicyToDelete");
        Assert.assertNotNull(found);

        // Delete the policy
        dao.deletePolicy(policy);

        found = dao.getPolicyByName("PolicyToDelete");
        Assert.assertNull(found);
    }

    @Test
    public void test_getAllPolicies() {
        // Save multiple policies
        for (int i = 0; i < 5; i++) {
            MongoPolicyImpl policy = new MongoPolicyImpl();
            policy.setName("Policy" + i);
            policy.setDescription("Description for policy " + i);
            policy.setCreatedDateTime(new Date(1000000L + i));
            policy.setUpdatedDateTime(new Date(2000000L + i));

            dao.saveOrUpdatePolicy(policy);
        }

        List<Policy> policies = dao.getAllPolicies();

        Assert.assertNotNull(policies);
        Assert.assertEquals(5, policies.size());
    }

    @Test
    public void test_getAllPolicies_empty() {
        List<Policy> policies = dao.getAllPolicies();

        Assert.assertNotNull(policies);
        Assert.assertTrue(policies.isEmpty());
    }

    @Test
    public void test_getPolicyByNameLike() {
        // Save policies with different names
        MongoPolicyImpl policy1 = new MongoPolicyImpl();
        policy1.setName("ReadOnlyPolicy");
        policy1.setDescription("Read only access");
        policy1.setCreatedDateTime(new Date());
        policy1.setUpdatedDateTime(new Date());
        dao.saveOrUpdatePolicy(policy1);

        MongoPolicyImpl policy2 = new MongoPolicyImpl();
        policy2.setName("ReadWritePolicy");
        policy2.setDescription("Read and write access");
        policy2.setCreatedDateTime(new Date());
        policy2.setUpdatedDateTime(new Date());
        dao.saveOrUpdatePolicy(policy2);

        MongoPolicyImpl policy3 = new MongoPolicyImpl();
        policy3.setName("AdminPolicy");
        policy3.setDescription("Admin access");
        policy3.setCreatedDateTime(new Date());
        policy3.setUpdatedDateTime(new Date());
        dao.saveOrUpdatePolicy(policy3);

        List<Policy> policies = dao.getPolicyByNameLike("Read");

        Assert.assertNotNull(policies);
        Assert.assertEquals(2, policies.size());
    }

    @Test
    public void test_getPolicyByNameLike_no_match() {
        MongoPolicyImpl policy = new MongoPolicyImpl();
        policy.setName("TestPolicy");
        policy.setDescription("Test description");
        policy.setCreatedDateTime(new Date());
        policy.setUpdatedDateTime(new Date());
        dao.saveOrUpdatePolicy(policy);

        List<Policy> policies = dao.getPolicyByNameLike("NonExistent");

        Assert.assertNotNull(policies);
        Assert.assertTrue(policies.isEmpty());
    }

    @Test
    public void test_getAllPoliciesWithRole() {
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("TestRole");
        role.setDescription("Test role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        MongoRoleJobPlanImpl jobPlan = new MongoRoleJobPlanImpl();
        jobPlan.setJobPlanName("ExistingJobPlan");
        role.addRoleJobPlan(jobPlan);

        this.mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Save policies
        for (int i = 0; i < 3; i++) {
            MongoPolicyImpl policy = new MongoPolicyImpl();
            policy.setName("Policy" + i);
            policy.setId(policy.getName()+ "-" + POLICY_TYPE);
            policy.setDescription("Description " + i);
            policy.setCreatedDateTime(new Date());
            policy.setUpdatedDateTime(new Date());
            role.addPolicy(policy);
            dao.saveOrUpdatePolicy(policy);
            this.mongoRoleDaoImpl.saveOrUpdateRole(role);
        }

        for (int i = 3; i < 6; i++) {
            MongoPolicyImpl policy = new MongoPolicyImpl();
            policy.setName("Policy" + i);
            policy.setDescription("Description " + i);
            policy.setCreatedDateTime(new Date());
            policy.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePolicy(policy);
        }

        // In MongoDB implementation, this returns all policies
        List<Policy> policies = dao.getAllPoliciesWithRole("TestRole");

        Assert.assertNotNull(policies);
        Assert.assertEquals(3, policies.size());
    }

    @Test
    public void test_getPolicyById() {
        MongoPolicyImpl policy = new MongoPolicyImpl();
        policy.setName("TestPolicy");
        policy.setDescription("Test description");
        policy.setCreatedDateTime(new Date());
        policy.setUpdatedDateTime(new Date());

        dao.saveOrUpdatePolicy(policy);

        Policy record = dao.getPolicyById("TestPolicy" + "-" + POLICY_TYPE);

        Assert.assertNotNull(record);
        Assert.assertEquals("TestPolicy", record.getName());
        Assert.assertEquals("TestPolicy"+ "-" + POLICY_TYPE, record.getId());
    }

    @Test
    public void test_getPolicyById_not_found() {
        Policy record = dao.getPolicyById("NonExistent");

        Assert.assertNull(record);
    }

    @Test
    public void test_saveOrUpdatePolicy_with_null_dates() {
        MongoPolicyImpl policy = new MongoPolicyImpl();
        policy.setName("PolicyWithNullDates");
        policy.setDescription("Policy with null dates");
        policy.setCreatedDateTime(null);
        policy.setUpdatedDateTime(null);

        // Should not throw exception and use current time instead
        dao.saveOrUpdatePolicy(policy);

        Policy found = dao.getPolicyByName("PolicyWithNullDates");

        Assert.assertNotNull(found);
        Assert.assertEquals("PolicyWithNullDates", found.getName());
    }

    @Test
    public void test_saveOrUpdatePolicy_with_special_characters() {
        MongoPolicyImpl policy = new MongoPolicyImpl();
        policy.setName("Policy-With_Special.Characters");
        policy.setDescription("Description with special chars: <>&\"'");
        policy.setCreatedDateTime(new Date());
        policy.setUpdatedDateTime(new Date());

        dao.saveOrUpdatePolicy(policy);

        Policy found = dao.getPolicyByName("Policy-With_Special.Characters");

        Assert.assertNotNull(found);
        Assert.assertEquals("Policy-With_Special.Characters", found.getName());
        Assert.assertEquals("Description with special chars: <>&\"'", found.getDescription());
    }

    @Test
    public void test_multiple_operations_in_sequence() {
        // Create
        MongoPolicyImpl policy = new MongoPolicyImpl();
        policy.setName("SequencePolicy");
        policy.setDescription("Initial description");
        policy.setCreatedDateTime(new Date());
        policy.setUpdatedDateTime(new Date());
        dao.saveOrUpdatePolicy(policy);

        // Read
        Policy found = dao.getPolicyByName("SequencePolicy");
        Assert.assertNotNull(found);
        Assert.assertEquals("Initial description", found.getDescription());

        // Update
        policy.setDescription("Updated description");
        dao.saveOrUpdatePolicy(policy);
        found = dao.getPolicyByName("SequencePolicy");
        Assert.assertEquals("Updated description", found.getDescription());

        // List
        List<Policy> policies = dao.getAllPolicies();
        Assert.assertEquals(1, policies.size());

        // Delete
        dao.deletePolicy(policy);
        found = dao.getPolicyByName("SequencePolicy");
        Assert.assertNull(found);

        // Verify empty
        policies = dao.getAllPolicies();
        Assert.assertTrue(policies.isEmpty());
    }
}
