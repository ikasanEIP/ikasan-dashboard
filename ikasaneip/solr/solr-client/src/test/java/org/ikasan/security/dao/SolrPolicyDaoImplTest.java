package org.ikasan.security.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.security.model.*;
import org.ikasan.spec.security.model.Policy;
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

import static org.ikasan.security.dao.SolrPolicyDaoImpl.POLICY_TYPE;

public class SolrPolicyDaoImplTest extends SolrTestCaseJ4 {

    private SolrPolicyDaoImpl dao;
    private SolrRoleDaoImpl solrRoleDao;
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

        dao = new SolrPolicyDaoImpl();
        dao.setSolrClient(server);

        this.solrRoleDao = new SolrRoleDaoImpl(this.dao);
        this.solrRoleDao.setSolrClient(server);
    }

    @Test
    public void test_createPolicy() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            Policy policy = dao.createPolicy();

            Assert.assertNotNull(policy);
            Assert.assertTrue(policy instanceof SolrPolicyImpl);
        }
    }

    @Test
    public void test_saveOrUpdatePolicy_and_getPolicyByName() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrPolicyImpl policy = new SolrPolicyImpl();
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
    }

    @Test
    public void test_getPolicyByName_not_found() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            Policy found = dao.getPolicyByName("NonExistentPolicy");

            Assert.assertNull(found);
        }
    }

    @Test
    public void test_saveOrUpdatePolicy_update_existing() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrPolicyImpl policy = new SolrPolicyImpl();
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
    }

    @Test
    public void test_deletePolicy() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrPolicyImpl policy = new SolrPolicyImpl();
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
    }

    @Test
    public void test_getAllPolicies() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Save multiple policies
            for (int i = 0; i < 5; i++) {
                SolrPolicyImpl policy = new SolrPolicyImpl();
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
    }

    @Test
    public void test_getAllPolicies_empty() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            List<Policy> policies = dao.getAllPolicies();

            Assert.assertNotNull(policies);
            Assert.assertTrue(policies.isEmpty());
        }
    }

    @Test
    public void test_getPolicyByNameLike() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Save policies with different names
            SolrPolicyImpl policy1 = new SolrPolicyImpl();
            policy1.setName("ReadOnlyPolicy");
            policy1.setDescription("Read only access");
            policy1.setCreatedDateTime(new Date());
            policy1.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePolicy(policy1);

            SolrPolicyImpl policy2 = new SolrPolicyImpl();
            policy2.setName("ReadWritePolicy");
            policy2.setDescription("Read and write access");
            policy2.setCreatedDateTime(new Date());
            policy2.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePolicy(policy2);

            SolrPolicyImpl policy3 = new SolrPolicyImpl();
            policy3.setName("AdminPolicy");
            policy3.setDescription("Admin access");
            policy3.setCreatedDateTime(new Date());
            policy3.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePolicy(policy3);

            List<Policy> policies = dao.getPolicyByNameLike("Read");

            Assert.assertNotNull(policies);
            Assert.assertEquals(2, policies.size());
        }
    }

    @Test
    public void test_getPolicyByNameLike_no_match() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrPolicyImpl policy = new SolrPolicyImpl();
            policy.setName("TestPolicy");
            policy.setDescription("Test description");
            policy.setCreatedDateTime(new Date());
            policy.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePolicy(policy);

            List<Policy> policies = dao.getPolicyByNameLike("NonExistent");

            Assert.assertNotNull(policies);
            Assert.assertTrue(policies.isEmpty());
        }
    }

    @Test
    public void test_getAllPoliciesWithRole() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("TestRole");
            role.setDescription("Test role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            SolrRoleJobPlanImpl jobPlan = new SolrRoleJobPlanImpl();
            jobPlan.setJobPlanName("ExistingJobPlan");
            jobPlan.setRole(role);
            role.addRoleJobPlan(jobPlan);

            this.solrRoleDao.saveOrUpdateRole(role);

            // Save policies
            for (int i = 0; i < 3; i++) {
                SolrPolicyImpl policy = new SolrPolicyImpl();
                policy.setName("Policy" + i);
                policy.setDescription("Description " + i);
                policy.setCreatedDateTime(new Date());
                policy.setUpdatedDateTime(new Date());
                role.addPolicy(policy);
                dao.saveOrUpdatePolicy(policy);
                this.solrRoleDao.saveOrUpdateRole(role);
            }

            for (int i = 3; i < 6; i++) {
                SolrPolicyImpl policy = new SolrPolicyImpl();
                policy.setName("Policy" + i);
                policy.setDescription("Description " + i);
                policy.setCreatedDateTime(new Date());
                policy.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePolicy(policy);
            }

            // In Solr implementation, this returns all policies
            List<Policy> policies = dao.getAllPoliciesWithRole("TestRole");

            Assert.assertNotNull(policies);
            Assert.assertEquals(3, policies.size());
        }
    }

    @Test
    public void test_getPolicyById() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrPolicyImpl policy = new SolrPolicyImpl();
            policy.setName("TestPolicy");
            policy.setDescription("Test description");
            policy.setCreatedDateTime(new Date());
            policy.setUpdatedDateTime(new Date());

            dao.saveOrUpdatePolicy(policy);

            Policy record = dao.getPolicyById("TestPolicy-"+POLICY_TYPE);

            Assert.assertNotNull(record);
            Assert.assertEquals("TestPolicy", record.getName());
            Assert.assertEquals("TestPolicy-"+POLICY_TYPE, record.getId());
        }
    }

    @Test
    public void test_getPolicyById_not_found() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            Policy record = dao.getPolicyById("NonExistent");

            Assert.assertNull(record);
        }
    }

    @Test
    public void test_saveOrUpdatePolicy_with_null_dates() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrPolicyImpl policy = new SolrPolicyImpl();
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
    }

    @Test
    public void test_saveOrUpdatePolicy_with_special_characters() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrPolicyImpl policy = new SolrPolicyImpl();
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
    }

    @Test
    public void test_multiple_operations_in_sequence() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create
            SolrPolicyImpl policy = new SolrPolicyImpl();
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

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
