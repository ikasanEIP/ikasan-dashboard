package org.ikasan.mongo.persistence.scheduled.profile.dao;

import com.mongodb.client.MongoClients;
import org.ikasan.job.orchestration.model.profile.ContextProfileSearchFilterImpl;
import org.ikasan.mongo.persistence.scheduled.profile.model.MongoContextProfileRecordImpl;
import org.ikasan.mongo.persistence.scheduled.profile.model.MongoContextProfileImpl;
import org.ikasan.mongo.persistence.scheduled.profile.repository.MongoContextProfileRepository;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Test class for MongoContextProfileDao.
 */
public class MongoContextProfileDaoTest {

    private static MongoDBContainer mongoDBContainer;
    private MongoTemplate mongoTemplate;
    private MongoContextProfileDao dao;
    private MongoContextProfileRepository repository;

    @Before
    public void setup() {
        if (mongoDBContainer == null) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
            mongoDBContainer.start();
        }

        var mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoTemplate = new MongoTemplate(mongoClient, "test");

        var factory = new org.springframework.data.mongodb.repository.support.MongoRepositoryFactory(mongoTemplate);
        repository = factory.getRepository(MongoContextProfileRepository.class);

        dao = new MongoContextProfileDao(repository, mongoTemplate);
    }

    @After
    public void tearDown() {
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(MongoContextProfileRecordImpl.class);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_save_null_context_profile_null_access_users_null_access_roles_and_find_success() {
        MongoContextProfileRecordImpl record = new MongoContextProfileRecordImpl();
        record.setProfileName("profileName");
        record.setContextName("contextName");
        record.setOwner("owner");
        record.setModifiedBy("modifiedBy");

        dao.save(record);

        ContextProfileRecord found = dao.findById("profileName-contextName-contextProfile");

        Assert.assertNotNull(found);
        Assert.assertEquals("profileName", found.getProfileName());
        Assert.assertEquals("contextName", found.getContextName());
        Assert.assertEquals("owner", found.getOwner());
        Assert.assertEquals(0, found.getContextProfile().getSubContexts().size());
        Assert.assertEquals(0, found.getAccessGroups().size());
        Assert.assertEquals(0, found.getAccessUsers().size());
        Assert.assertEquals("modifiedBy", found.getModifiedBy());

        Assert.assertNull(dao.findById("bad_id"));
    }

    @Test
    public void test_save_and_find_success() {
        MongoContextProfileRecordImpl record = new MongoContextProfileRecordImpl();
        record.setProfileName("profileName");
        record.setContextName("contextName");
        record.setOwner("owner");
        record.setModifiedBy("modifiedBy");
        record.setAccessGroups(List.of("role1"));
        record.setAccessUsers(List.of("user1"));

        MongoContextProfileImpl contextProfile = new MongoContextProfileImpl();
        contextProfile.setSubContexts(List.of("context1", "context2"));

        record.setContextProfile(contextProfile);

        dao.save(record);

        ContextProfileRecord found = dao.findById("profileName-contextName-contextProfile");

        Assert.assertNotNull(found);
        Assert.assertEquals("profileName", found.getProfileName());
        Assert.assertEquals("contextName", found.getContextName());
        Assert.assertEquals("owner", found.getOwner());
        Assert.assertEquals(2, found.getContextProfile().getSubContexts().size());
        Assert.assertEquals(1, found.getAccessGroups().size());
        Assert.assertEquals(1, found.getAccessUsers().size());
        Assert.assertEquals("modifiedBy", found.getModifiedBy());

        Assert.assertNull(dao.findById("bad_id"));
    }

    @Test
    public void test_find_by_filter() {
        addRecords(100);

        SearchResults<ContextProfileRecord> results = dao.findByFilter(new ContextProfileSearchFilterImpl(), -1, -1, null, null);

        Assert.assertEquals(100, results.getTotalNumberOfResults());
        Assert.assertEquals(100, results.getResultList().size());

        ContextProfileSearchFilterImpl filter = new ContextProfileSearchFilterImpl();
        filter.setUser("user1");

        results = dao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(11, results.getTotalNumberOfResults());
        Assert.assertEquals(11, results.getResultList().size());

        filter = new ContextProfileSearchFilterImpl();
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role18", "role19"));

        results = dao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(2, results.getTotalNumberOfResults());
        Assert.assertEquals(2, results.getResultList().size());

        filter = new ContextProfileSearchFilterImpl();
        filter.setProfileName("profileName18");
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role18", "role19"));

        results = dao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(1, results.getTotalNumberOfResults());
        Assert.assertEquals(1, results.getResultList().size());

        filter = new ContextProfileSearchFilterImpl();
        filter.setProfileName("profileName18");
        filter.setContextName("contextName18");
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role18", "role19"));

        results = dao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(1, results.getTotalNumberOfResults());
        Assert.assertEquals(1, results.getResultList().size());

        filter = new ContextProfileSearchFilterImpl();
        filter.setProfileName("profileName18");
        filter.setContextName("contextName18");
        filter.setOwner("owner18");
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role18", "role19"));

        results = dao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(1, results.getTotalNumberOfResults());
        Assert.assertEquals(1, results.getResultList().size());

        filter = new ContextProfileSearchFilterImpl();
        filter.setProfileName("profileName18");
        filter.setContextName("contextName18");
        filter.setOwner("owner18");
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role18"));

        results = dao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(1, results.getTotalNumberOfResults());
        Assert.assertEquals(1, results.getResultList().size());

        filter = new ContextProfileSearchFilterImpl();
        filter.setProfileName("profileName18");
        filter.setContextName("contextName18");
        filter.setOwner("owner18");
        filter.setUser("user1");
        filter.setAccessRoles(List.of("role88"));

        results = dao.findByFilter(filter, -1, -1, null, null);

        Assert.assertEquals(0, results.getTotalNumberOfResults());
        Assert.assertEquals(0, results.getResultList().size());
    }

    @Test
    public void test_find_by_filter_with_pagination() {
        addRecords(100);

        ContextProfileSearchFilterImpl filter = new ContextProfileSearchFilterImpl();
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 10, 0, null, null);

        Assert.assertEquals(100, results.getTotalNumberOfResults());
        Assert.assertEquals(10, results.getResultList().size());

        results = dao.findByFilter(filter, 10, 10, null, null);

        Assert.assertEquals(100, results.getTotalNumberOfResults());
        Assert.assertEquals(10, results.getResultList().size());
    }

    @Test
    public void test_find_by_filter_with_sorting() {
        addRecords(10);

        ContextProfileSearchFilterImpl filter = new ContextProfileSearchFilterImpl();
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, -1, -1, "profileName", "ASCENDING");

        Assert.assertEquals(10, results.getTotalNumberOfResults());
        Assert.assertEquals("profileName0", results.getResultList().get(0).getProfileName());
        Assert.assertEquals("profileName9", results.getResultList().get(9).getProfileName());

        results = dao.findByFilter(filter, -1, -1, "profileName", "DESCENDING");

        Assert.assertEquals(10, results.getTotalNumberOfResults());
        Assert.assertEquals("profileName9", results.getResultList().get(0).getProfileName());
        Assert.assertEquals("profileName0", results.getResultList().get(9).getProfileName());
    }

    @Test
    public void test_delete_by_context_name() {
        MongoContextProfileRecordImpl record1 = new MongoContextProfileRecordImpl();
        record1.setProfileName("profile1");
        record1.setContextName("contextToDelete");
        record1.setOwner("owner1");
        record1.setModifiedBy("user1");

        MongoContextProfileRecordImpl record2 = new MongoContextProfileRecordImpl();
        record2.setProfileName("profile2");
        record2.setContextName("contextToDelete");
        record2.setOwner("owner2");
        record2.setModifiedBy("user2");

        MongoContextProfileRecordImpl record3 = new MongoContextProfileRecordImpl();
        record3.setProfileName("profile3");
        record3.setContextName("contextToKeep");
        record3.setOwner("owner3");
        record3.setModifiedBy("user3");

        dao.save(record1);
        dao.save(record2);
        dao.save(record3);

        // Verify all saved
        ContextProfileSearchFilterImpl filter = new ContextProfileSearchFilterImpl();
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, -1, -1, null, null);
        Assert.assertEquals(3, results.getTotalNumberOfResults());

        // Delete by context name
        dao.deleteByContextName("contextToDelete");

        // Verify only one record remains
        results = dao.findByFilter(filter, -1, -1, null, null);
        Assert.assertEquals(1, results.getTotalNumberOfResults());
        Assert.assertEquals("contextToKeep", results.getResultList().get(0).getContextName());
    }

    @Test
    public void test_update_existing_record() {
        MongoContextProfileRecordImpl record = new MongoContextProfileRecordImpl();
        record.setProfileName("profileName");
        record.setContextName("contextName");
        record.setOwner("owner1");
        record.setModifiedBy("user1");
        record.setAccessGroups(List.of("role1"));
        record.setAccessUsers(List.of("user1"));

        dao.save(record);

        ContextProfileRecord found = dao.findById("profileName-contextName-contextProfile");
        Assert.assertNotNull(found);
        Assert.assertEquals("owner1", found.getOwner());
        Assert.assertEquals(1, found.getAccessGroups().size());

        // Update the record
        record.setOwner("owner2");
        record.setAccessGroups(List.of("role1", "role2"));
        record.setModifiedBy("user2");

        dao.save(record);

        found = dao.findById("profileName-contextName-contextProfile");
        Assert.assertNotNull(found);
        Assert.assertEquals("owner2", found.getOwner());
        Assert.assertEquals(2, found.getAccessGroups().size());
        Assert.assertEquals("user2", found.getModifiedBy());
    }

    private void addRecords(int count) {
        IntStream.range(0, count).forEach(i -> {
            MongoContextProfileRecordImpl record = new MongoContextProfileRecordImpl();
            record.setProfileName("profileName" + i);
            record.setContextName("contextName" + i);
            record.setOwner("owner" + i);
            record.setModifiedBy("modifiedBy" + i);
            record.setAccessGroups(List.of("role" + i));
            record.setAccessUsers(List.of("user" + i));

            MongoContextProfileImpl contextProfile = new MongoContextProfileImpl();
            contextProfile.setSubContexts(List.of("context" + i));

            record.setContextProfile(contextProfile);

            dao.save(record);
        });
    }
}
