package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.security.model.MongoAuthenticationMethodImpl;
import org.ikasan.mongo.persistence.security.repository.MongoAuthenticationMethodRepository;
import org.ikasan.spec.security.dao.AuthenticationMethodDao;
import org.ikasan.spec.security.model.AuthenticationMethod;
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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoAuthenticationMethodDaoImplTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoAuthenticationMethodRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoAuthenticationMethodDaoImpl dao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoAuthenticationMethodRepository repository, MongoTemplate mongoTemplate) {
        this.dao = new MongoAuthenticationMethodDaoImpl(repository, mongoTemplate);
    }

    @After
    public void teardown() {
        repository.deleteAll();
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_createAuthenticationMethod() {
        AuthenticationMethod authMethod = dao.createAuthenticationMethod();

        Assert.assertNotNull(authMethod);
        Assert.assertTrue(authMethod instanceof MongoAuthenticationMethodImpl);
    }

    @Test
    public void test_saveOrUpdateAuthenticationMethod_and_getAuthenticationMethod() {
        MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
        authMethod.setName("LDAP");
        authMethod.setOrder(1L);
        authMethod.setMethod("ldap");
        authMethod.setLastSynchronised(new Date(1000000L));

        dao.saveOrUpdateAuthenticationMethod(authMethod);

        AuthenticationMethod found = dao.getAuthenticationMethod("LDAP-" + AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE);

        Assert.assertNotNull(found);
        Assert.assertEquals("LDAP", found.getName());
        Assert.assertEquals(Long.valueOf(1L), found.getOrder());
        Assert.assertEquals("ldap", found.getMethod());
    }

    @Test
    public void test_saveOrUpdateAuthenticationMethod_update_existing() {
        MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
        authMethod.setName("LOCAL");
        authMethod.setOrder(1L);
        authMethod.setMethod("local");

        dao.saveOrUpdateAuthenticationMethod(authMethod);

        // Update the authentication method
        authMethod.setOrder(2L);
        authMethod.setMethod("local-updated");

        dao.saveOrUpdateAuthenticationMethod(authMethod);

        AuthenticationMethod found = dao.getAuthenticationMethod
            ("LOCAL-" + AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE);

        Assert.assertNotNull(found);
        Assert.assertEquals("LOCAL", found.getName());
        Assert.assertEquals(Long.valueOf(2L), found.getOrder());
        Assert.assertEquals("local-updated", found.getMethod());
    }

    @Test
    public void test_getAuthenticationMethod_not_found() {
        AuthenticationMethod found = dao.getAuthenticationMethod("NonExistent");

        Assert.assertNull(found);
    }

    @Test
    public void test_getAuthenticationMethods() {
        MongoAuthenticationMethodImpl authMethod1 = new MongoAuthenticationMethodImpl();
        authMethod1.setName("LDAP");
        authMethod1.setOrder(2L);
        authMethod1.setMethod("ldap");

        MongoAuthenticationMethodImpl authMethod2 = new MongoAuthenticationMethodImpl();
        authMethod2.setName("LOCAL");
        authMethod2.setOrder(1L);
        authMethod2.setMethod("local");

        MongoAuthenticationMethodImpl authMethod3 = new MongoAuthenticationMethodImpl();
        authMethod3.setName("SAML");
        authMethod3.setOrder(3L);
        authMethod3.setMethod("saml");

        dao.saveOrUpdateAuthenticationMethod(authMethod1);
        dao.saveOrUpdateAuthenticationMethod(authMethod2);
        dao.saveOrUpdateAuthenticationMethod(authMethod3);

        List<AuthenticationMethod> allMethods = dao.getAuthenticationMethods();

        Assert.assertEquals(3, allMethods.size());
        // Verify they are ordered by order field
        Assert.assertEquals("LOCAL", allMethods.get(0).getName());
        Assert.assertEquals("LDAP", allMethods.get(1).getName());
        Assert.assertEquals("SAML", allMethods.get(2).getName());
    }

    @Test
    public void test_getAuthenticationMethods_empty() {
        List<AuthenticationMethod> allMethods = dao.getAuthenticationMethods();

        Assert.assertNotNull(allMethods);
        Assert.assertTrue(allMethods.isEmpty());
    }

    @Test
    public void test_getNumberOfAuthenticationMethods() {
        MongoAuthenticationMethodImpl authMethod1 = new MongoAuthenticationMethodImpl();
        authMethod1.setName("LDAP");
        authMethod1.setOrder(1L);

        MongoAuthenticationMethodImpl authMethod2 = new MongoAuthenticationMethodImpl();
        authMethod2.setName("LOCAL");
        authMethod2.setOrder(2L);

        dao.saveOrUpdateAuthenticationMethod(authMethod1);
        dao.saveOrUpdateAuthenticationMethod(authMethod2);

        long count = dao.getNumberOfAuthenticationMethods();

        Assert.assertEquals(2, count);
    }

    @Test
    public void test_getNumberOfAuthenticationMethods_empty() {
        long count = dao.getNumberOfAuthenticationMethods();

        Assert.assertEquals(0, count);
    }

    @Test
    public void test_getAuthenticationMethodByOrder() {
        MongoAuthenticationMethodImpl authMethod1 = new MongoAuthenticationMethodImpl();
        authMethod1.setName("LDAP");
        authMethod1.setOrder(5L);
        authMethod1.setMethod("ldap");

        MongoAuthenticationMethodImpl authMethod2 = new MongoAuthenticationMethodImpl();
        authMethod2.setName("LOCAL");
        authMethod2.setOrder(10L);
        authMethod2.setMethod("local");

        dao.saveOrUpdateAuthenticationMethod(authMethod1);
        dao.saveOrUpdateAuthenticationMethod(authMethod2);

        AuthenticationMethod found = dao.getAuthenticationMethodByOrder(5L);

        Assert.assertNotNull(found);
        Assert.assertEquals("LDAP", found.getName());
        Assert.assertEquals(Long.valueOf(5L), found.getOrder());
        Assert.assertEquals("ldap", found.getMethod());
    }

    @Test
    public void test_getAuthenticationMethodByOrder_not_found() {
        MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
        authMethod.setName("LDAP");
        authMethod.setOrder(Long.valueOf(5L));

        dao.saveOrUpdateAuthenticationMethod(authMethod);

        AuthenticationMethod found = dao.getAuthenticationMethodByOrder(99L);

        Assert.assertNull(found);
    }

    @Test
    public void test_deleteAuthenticationMethod() {
        MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
        authMethod.setName("LDAP");
        authMethod.setOrder(1L);

        dao.saveOrUpdateAuthenticationMethod(authMethod);

        AuthenticationMethod found = dao.getAuthenticationMethod
            ("LDAP-" + AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE);
        Assert.assertNotNull(found);

        dao.deleteAuthenticationMethod(authMethod);

        AuthenticationMethod notFound = dao.getAuthenticationMethod
            ("LDAP-" + AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE);
        Assert.assertNull(notFound);
    }

    @Test
    public void test_deleteAuthenticationMethod_and_verify_count() {
        MongoAuthenticationMethodImpl authMethod1 = new MongoAuthenticationMethodImpl();
        authMethod1.setName("LDAP");
        authMethod1.setOrder(1L);

        MongoAuthenticationMethodImpl authMethod2 = new MongoAuthenticationMethodImpl();
        authMethod2.setName("LOCAL");
        authMethod2.setOrder(2L);

        dao.saveOrUpdateAuthenticationMethod(authMethod1);
        dao.saveOrUpdateAuthenticationMethod(authMethod2);

        Assert.assertEquals(2, dao.getNumberOfAuthenticationMethods());

        dao.deleteAuthenticationMethod(authMethod1);

        Assert.assertEquals(1, dao.getNumberOfAuthenticationMethods());
        Assert.assertNull(dao.getAuthenticationMethod("LDAP-" + AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE));
        Assert.assertNotNull(dao.getAuthenticationMethod("LOCAL-" + AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE));
    }

    @Test
    public void test_saveOrUpdateAuthenticationMethod_with_null_lastSynchronised() {
        MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
        authMethod.setName("LDAP");
        authMethod.setOrder(1L);
        authMethod.setMethod("ldap");
        authMethod.setLastSynchronised(null);

        dao.saveOrUpdateAuthenticationMethod(authMethod);

        AuthenticationMethod found = dao.getAuthenticationMethod
            ("LDAP-" + AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE);

        Assert.assertNotNull(found);
        Assert.assertEquals("LDAP", found.getName());
    }

    @Test
    public void test_getAuthenticationMethods_ordering_with_gaps() {
        MongoAuthenticationMethodImpl authMethod1 = new MongoAuthenticationMethodImpl();
        authMethod1.setName("Method1");
        authMethod1.setOrder(100L);

        MongoAuthenticationMethodImpl authMethod2 = new MongoAuthenticationMethodImpl();
        authMethod2.setName("Method2");
        authMethod2.setOrder(10L);

        MongoAuthenticationMethodImpl authMethod3 = new MongoAuthenticationMethodImpl();
        authMethod3.setName("Method3");
        authMethod3.setOrder(50L);

        dao.saveOrUpdateAuthenticationMethod(authMethod1);
        dao.saveOrUpdateAuthenticationMethod(authMethod2);
        dao.saveOrUpdateAuthenticationMethod(authMethod3);

        List<AuthenticationMethod> allMethods = dao.getAuthenticationMethods();

        Assert.assertEquals(3, allMethods.size());
        Assert.assertEquals("Method2", allMethods.get(0).getName());
        Assert.assertEquals(Long.valueOf(10L), allMethods.get(0).getOrder());
        Assert.assertEquals("Method3", allMethods.get(1).getName());
        Assert.assertEquals(Long.valueOf(50L), allMethods.get(1).getOrder());
        Assert.assertEquals("Method1", allMethods.get(2).getName());
        Assert.assertEquals(Long.valueOf(100L), allMethods.get(2).getOrder());
    }

    @Test
    public void test_multiple_authentication_methods_with_same_order() {
        MongoAuthenticationMethodImpl authMethod1 = new MongoAuthenticationMethodImpl();
        authMethod1.setName("LDAP");
        authMethod1.setOrder(1L);

        MongoAuthenticationMethodImpl authMethod2 = new MongoAuthenticationMethodImpl();
        authMethod2.setName("LOCAL");
        authMethod2.setOrder(1L);

        dao.saveOrUpdateAuthenticationMethod(authMethod1);
        dao.saveOrUpdateAuthenticationMethod(authMethod2);

        // Both should be retrievable
        AuthenticationMethod found1 = dao.getAuthenticationMethod
            ("LDAP-" + AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE);
        AuthenticationMethod found2 = dao.getAuthenticationMethod
            ("LOCAL-" + AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE);

        Assert.assertNotNull(found1);
        Assert.assertNotNull(found2);
        Assert.assertEquals(2, dao.getNumberOfAuthenticationMethods());
    }

    @Test
    public void test_saveOrUpdateAuthenticationMethod_with_complex_data() {
        MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
        authMethod.setName("ComplexAuth");
        authMethod.setOrder(1L);
        authMethod.setMethod("complex-method-type");
        authMethod.setLastSynchronised(new Date());

        dao.saveOrUpdateAuthenticationMethod(authMethod);

        AuthenticationMethod found = dao.getAuthenticationMethod
            ("ComplexAuth-" + AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE);

        Assert.assertNotNull(found);
        Assert.assertEquals("ComplexAuth", found.getName());
        Assert.assertEquals("complex-method-type", found.getMethod());
        Assert.assertEquals(Long.valueOf(1L), found.getOrder());
    }

    @Test
    public void test_getAuthenticationMethod_with_special_characters_in_name() {
        MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
        authMethod.setName("Auth-Method_1");
        authMethod.setOrder(1L);

        dao.saveOrUpdateAuthenticationMethod(authMethod);

        AuthenticationMethod found = dao.getAuthenticationMethod
            ("Auth-Method_1-" + AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE);

        Assert.assertNotNull(found);
        Assert.assertEquals("Auth-Method_1", found.getName());
    }

    @Test
    public void test_deleteAuthenticationMethod_not_existing() {
        MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
        authMethod.setName("NonExistent");
        authMethod.setOrder(1L);

        // Should not throw exception
        dao.deleteAuthenticationMethod(authMethod);

        Assert.assertEquals(0, dao.getNumberOfAuthenticationMethods());
    }

    @Test
    public void test_saveOrUpdateAuthenticationMethod_order_zero() {
        MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
        authMethod.setName("ZeroOrder");
        authMethod.setOrder(0L);

        dao.saveOrUpdateAuthenticationMethod(authMethod);

        AuthenticationMethod found = dao.getAuthenticationMethodByOrder(0L);

        Assert.assertNotNull(found);
        Assert.assertEquals("ZeroOrder", found.getName());
        Assert.assertEquals(Long.valueOf(0L), found.getOrder());
    }

    @Test
    public void test_saveOrUpdateAuthenticationMethod_negative_order() {
        MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
        authMethod.setName("NegativeOrder");
        authMethod.setOrder(-1L);

        dao.saveOrUpdateAuthenticationMethod(authMethod);

        AuthenticationMethod found = dao.getAuthenticationMethodByOrder(-1L);

        Assert.assertNotNull(found);
        Assert.assertEquals("NegativeOrder", found.getName());
        Assert.assertEquals(Long.valueOf(-1L), found.getOrder());
    }
}
