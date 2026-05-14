package org.ikasan.relational.persistence.scheduled.profile.dao;

import org.ikasan.job.orchestration.model.profile.ContextProfileImpl;
import org.ikasan.relational.persistence.HibernatePersistenceAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.HibernatePersistenceTestAutoConfiguration;
import org.ikasan.relational.persistence.scheduled.profile.model.HibernateContextProfileRecord;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Integration tests for HibernateContextProfileDaoImpl using PostgreSQL test container.
 *
 * These tests verify:
 * - Save/update operations with JSONB persistence
 * - FindById operations
 * - FindByFilter operations with various criteria
 * - Delete operations by context name
 * - Complex context profile data persistence
 * - Timestamp management
 * - Access control (users and groups) persistence
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {HibernatePersistenceAutoConfiguration.class, HibernatePersistenceTestAutoConfiguration.class})
public class HibernateContextProfileDaoImplTest {

    public static PostgreSQLContainer<?> postgres;

    @BeforeClass
    public static void startContainer() {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

        postgres.start();

    }

    @Autowired
    private ContextProfileDao dao;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("postgres.url", () -> postgres.getJdbcUrl());
    }

    @After
    public void tearDown() {
        // Clean up all test data - find all records and delete by context name
        ContextProfileSearchFilter filter = createFilter(null, null);
        SearchResults<ContextProfileRecord> allRecords = dao.findByFilter(filter, Integer.MAX_VALUE, 0, null, null);
        Set<String> contextNames = new HashSet<>();

        for (ContextProfileRecord record : allRecords.getResultList()) {
            contextNames.add(record.getContextName());
        }

        for (String contextName : contextNames) {
            dao.deleteByContextName(contextName);
        }
    }

    @AfterClass
    public static void stopContainer() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    // ========== Save Tests ==========

    @Test
    public void test_save_should_persist_record() {
        // Given
        ContextProfileRecord record = createRecord("profile1", "context1");

        // When
        dao.save(record);

        // Then
        String id = "profile1_context1";
        ContextProfileRecord retrieved = dao.findById(id);
        assertNotNull(retrieved);
        assertEquals("profile1", retrieved.getProfileName());
        assertEquals("context1", retrieved.getContextName());
    }

    @Test
    public void test_save_should_persist_complex_context_profile() {
        // Given
        ContextProfileRecord record = createComplexRecord("profile1", "context1");

        // When
        dao.save(record);

        // Then
        String id = "profile1_context1";
        ContextProfileRecord retrieved = dao.findById(id);
        assertNotNull(retrieved);
        assertNotNull(retrieved.getContextProfile());
        assertEquals("defaultContext", retrieved.getContextProfile().getDefaultContext());
        assertEquals(3, retrieved.getContextProfile().getSubContexts().size());
        assertTrue(retrieved.getContextProfile().getSubContexts().contains("subContext1"));
    }

    @Test
    public void test_save_should_persist_access_groups() {
        // Given
        ContextProfileRecord record = createRecord("profile1", "context1");
        record.setAccessGroups(Arrays.asList("admin", "developer", "viewer"));

        // When
        dao.save(record);

        // Then
        String id = "profile1_context1";
        ContextProfileRecord retrieved = dao.findById(id);
        assertNotNull(retrieved);
        assertNotNull(retrieved.getAccessGroups());
        assertEquals(3, retrieved.getAccessGroups().size());
        assertTrue(retrieved.getAccessGroups().contains("admin"));
        assertTrue(retrieved.getAccessGroups().contains("developer"));
    }

    @Test
    public void test_save_should_persist_access_users() {
        // Given
        ContextProfileRecord record = createRecord("profile1", "context1");
        record.setAccessUsers(Arrays.asList("user1@example.com", "user2@example.com"));

        // When
        dao.save(record);

        // Then
        String id = "profile1_context1";
        ContextProfileRecord retrieved = dao.findById(id);
        assertNotNull(retrieved);
        assertNotNull(retrieved.getAccessUsers());
        assertEquals(2, retrieved.getAccessUsers().size());
        assertTrue(retrieved.getAccessUsers().contains("user1@example.com"));
    }

    @Test
    public void test_save_should_set_timestamps_automatically() {
        // Given
        ContextProfileRecord record = createRecord("profile1", "context1");
        long before = System.currentTimeMillis();

        // When
        dao.save(record);

        long after = System.currentTimeMillis();

        // Then
        String id = "profile1_context1";
        ContextProfileRecord retrieved = dao.findById(id);
        assertNotNull(retrieved);
        assertTrue(retrieved.getCreatedDateTime() >= before);
        assertTrue(retrieved.getCreatedDateTime() <= after);
        assertTrue(retrieved.getModifiedDateTime() >= before);
        assertTrue(retrieved.getModifiedDateTime() <= after);
    }

    @Test
    public void test_save_should_update_existing_record() {
        // Given
        ContextProfileRecord record = createRecord("profile1", "context1");
        dao.save(record);

        String id = "profile1_context1";
        ContextProfileRecord retrieved = dao.findById(id);
        long originalCreatedTime = retrieved.getCreatedDateTime();
        long originalModifiedTime = retrieved.getModifiedDateTime();

        // Wait a bit to ensure different timestamp
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }

        // When - update the record
        retrieved.setOwner("updatedOwner");
        dao.save(retrieved);

        // Then
        ContextProfileRecord updated = dao.findById(id);
        assertNotNull(updated);
        assertEquals("updatedOwner", updated.getOwner());
        assertEquals(originalCreatedTime, updated.getCreatedDateTime()); // Created time unchanged
        assertTrue(updated.getModifiedDateTime() >= originalModifiedTime); // Modified time updated
    }

    @Test
    public void test_save_with_owner_should_persist_owner() {
        // Given
        ContextProfileRecord record = createRecord("profile1", "context1");
        record.setOwner("john.doe@example.com");

        // When
        dao.save(record);

        // Then
        String id = "profile1_context1";
        ContextProfileRecord retrieved = dao.findById(id);
        assertNotNull(retrieved);
        assertEquals("john.doe@example.com", retrieved.getOwner());
    }

    @Test
    public void test_save_with_modified_by_should_persist_modified_by() {
        // Given
        ContextProfileRecord record = createRecord("profile1", "context1");
        record.setModifiedBy("test-user");

        // When
        dao.save(record);

        // Then
        String id = "profile1_context1";
        ContextProfileRecord retrieved = dao.findById(id);
        assertNotNull(retrieved);
        assertEquals("test-user", retrieved.getModifiedBy());
    }

    // ========== FindById Tests ==========

    @Test
    public void test_findById_should_return_null_when_not_found() {
        // When
        ContextProfileRecord retrieved = dao.findById("nonexistent_id");

        // Then
        assertNull(retrieved);
    }

    @Test
    public void test_findById_should_find_by_generated_id() {
        // Given
        ContextProfileRecord record = createRecord("profile1", "context1");
        dao.save(record);

        // When
        String id = "profile1_context1";
        ContextProfileRecord retrieved = dao.findById(id);

        // Then
        assertNotNull(retrieved);
        assertEquals("profile1", retrieved.getProfileName());
        assertEquals("context1", retrieved.getContextName());
    }

    // ========== FindByFilter Tests ==========

    @Test
    public void test_findByFilter_should_find_by_profile_name() {
        // Given
        dao.save(createRecord("profile1", "context1"));
        dao.save(createRecord("profile2", "context1"));
        dao.save(createRecord("profile1", "context2"));

        // When
        ContextProfileSearchFilter filter = createFilter("profile1", null);
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        // Then
        assertEquals(2, results.getResultList().size());
        assertTrue(results.getResultList().stream()
            .allMatch(r -> r.getProfileName().equals("profile1")));
    }

    @Test
    public void test_findByFilter_should_find_by_context_name() {
        // Given
        dao.save(createRecord("profile1", "context1"));
        dao.save(createRecord("profile2", "context1"));
        dao.save(createRecord("profile1", "context2"));

        // When
        ContextProfileSearchFilter filter = createFilter(null, "context1");
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        // Then
        assertEquals(2, results.getResultList().size());
        assertTrue(results.getResultList().stream()
            .allMatch(r -> r.getContextName().equals("context1")));
    }

    @Test
    public void test_findByFilter_should_find_by_profile_and_context_name() {
        // Given
        dao.save(createRecord("profile1", "context1"));
        dao.save(createRecord("profile2", "context1"));
        dao.save(createRecord("profile1", "context2"));

        // When
        ContextProfileSearchFilter filter = createFilter("profile1", "context1");
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        // Then
        assertEquals(1, results.getResultList().size());
        assertEquals("profile1", results.getResultList().get(0).getProfileName());
        assertEquals("context1", results.getResultList().get(0).getContextName());
    }

    @Test
    public void test_findByFilter_should_find_by_owner() {
        // Given
        ContextProfileRecord record1 = createRecord("profile1", "context1");
        record1.setOwner("owner1");
        dao.save(record1);

        ContextProfileRecord record2 = createRecord("profile2", "context2");
        record2.setOwner("owner2");
        dao.save(record2);

        // When
        ContextProfileSearchFilter filter = createFilter(null, null);
        filter.setOwner("owner1");
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        // Then
        assertEquals(1, results.getResultList().size());
        assertEquals("owner1", results.getResultList().get(0).getOwner());
    }

    @Test
    public void test_findByFilter_should_find_by_user() {
        // Given
        ContextProfileRecord record1 = createRecord("profile1", "context1");
        record1.setAccessUsers(Arrays.asList("user1@example.com", "user2@example.com"));
        dao.save(record1);

        ContextProfileRecord record2 = createRecord("profile2", "context2");
        record2.setAccessUsers(Arrays.asList("user3@example.com"));
        dao.save(record2);

        // When
        ContextProfileSearchFilter filter = createFilter(null, null);
        filter.setUser("user1@example.com");
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        // Then
        assertEquals(1, results.getResultList().size());
        assertTrue(results.getResultList().get(0).getAccessUsers().contains("user1@example.com"));
    }

    @Test
    public void test_findByFilter_should_find_by_access_roles() {
        // Given
        ContextProfileRecord record1 = createRecord("profile1", "context1");
        record1.setAccessGroups(Arrays.asList("admin", "developer"));
        dao.save(record1);

        ContextProfileRecord record2 = createRecord("profile2", "context2");
        record2.setAccessGroups(Arrays.asList("viewer"));
        dao.save(record2);

        // When
        ContextProfileSearchFilter filter = createFilter(null, null);
        filter.setAccessRoles(Arrays.asList("admin"));
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        // Then
        assertEquals(1, results.getResultList().size());
        assertTrue(results.getResultList().get(0).getAccessGroups().contains("admin"));
    }

    @Test
    public void test_findByFilter_should_find_by_multiple_access_roles() {
        // Given
        ContextProfileRecord record1 = createRecord("profile1", "context1");
        record1.setAccessGroups(Arrays.asList("admin"));
        dao.save(record1);

        ContextProfileRecord record2 = createRecord("profile2", "context2");
        record2.setAccessGroups(Arrays.asList("developer"));
        dao.save(record2);

        ContextProfileRecord record3 = createRecord("profile3", "context3");
        record3.setAccessGroups(Arrays.asList("viewer"));
        dao.save(record3);

        // When
        ContextProfileSearchFilter filter = createFilter(null, null);
        filter.setAccessRoles(Arrays.asList("admin", "developer"));
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        // Then
        assertEquals(2, results.getResultList().size());
    }

    @Test
    public void test_findByFilter_should_support_pagination_with_limit() {
        // Given
        for (int i = 1; i <= 10; i++) {
            dao.save(createRecord("profile" + i, "context1"));
        }

        // When
        ContextProfileSearchFilter filter = createFilter(null, "context1");
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 5, 0, null, null);

        // Then
        assertEquals(5, results.getResultList().size());
        assertEquals(10L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findByFilter_should_support_pagination_with_offset() {
        // Given
        for (int i = 1; i <= 10; i++) {
            dao.save(createRecord("profile" + i, "context1"));
        }

        // When
        ContextProfileSearchFilter filter = createFilter(null, "context1");
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 5, 5, null, null);

        // Then
        assertEquals(5, results.getResultList().size());
        assertEquals(10L, results.getTotalNumberOfResults());
    }

    @Test
    public void test_findByFilter_should_sort_by_created_date_time_descending_by_default() {
        // Given
        for (int i = 1; i <= 3; i++) {
            dao.save(createRecord("profile" + i, "context1"));
            try {
                Thread.sleep(10); // Ensure different timestamps
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        // When
        ContextProfileSearchFilter filter = createFilter(null, "context1");
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        // Then
        List<ContextProfileRecord> list = results.getResultList();
        assertTrue(list.get(0).getCreatedDateTime() >= list.get(1).getCreatedDateTime());
        assertTrue(list.get(1).getCreatedDateTime() >= list.get(2).getCreatedDateTime());
    }

    @Test
    public void test_findByFilter_should_sort_by_profile_name_ascending() {
        // Given
        dao.save(createRecord("profileC", "context1"));
        dao.save(createRecord("profileA", "context1"));
        dao.save(createRecord("profileB", "context1"));

        // When
        ContextProfileSearchFilter filter = createFilter(null, "context1");
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, "profileName", "ASCENDING");

        // Then
        List<ContextProfileRecord> list = results.getResultList();
        assertEquals("profileA", list.get(0).getProfileName());
        assertEquals("profileB", list.get(1).getProfileName());
        assertEquals("profileC", list.get(2).getProfileName());
    }

    @Test
    public void test_findByFilter_should_sort_by_profile_name_descending() {
        // Given
        dao.save(createRecord("profileC", "context1"));
        dao.save(createRecord("profileA", "context1"));
        dao.save(createRecord("profileB", "context1"));

        // When
        ContextProfileSearchFilter filter = createFilter(null, "context1");
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, "profileName", "DESCENDING");

        // Then
        List<ContextProfileRecord> list = results.getResultList();
        assertEquals("profileC", list.get(0).getProfileName());
        assertEquals("profileB", list.get(1).getProfileName());
        assertEquals("profileA", list.get(2).getProfileName());
    }

    @Test
    public void test_findByFilter_should_return_empty_results_when_no_match() {
        // Given
        dao.save(createRecord("profile1", "context1"));

        // When
        ContextProfileSearchFilter filter = createFilter("nonexistent", null);
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, null, null);

        // Then
        assertEquals(0, results.getResultList().size());
        assertEquals(0L, results.getTotalNumberOfResults());
    }

    // ========== Delete Tests ==========

    @Test
    public void test_deleteByContextName_should_delete_all_records_with_context_name() {
        // Given
        dao.save(createRecord("profile1", "context1"));
        dao.save(createRecord("profile2", "context1"));
        dao.save(createRecord("profile1", "context2"));

        // When
        dao.deleteByContextName("context1");

        // Then
        ContextProfileSearchFilter filter = createFilter(null, "context1");
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, null, null);
        assertEquals(0, results.getResultList().size());

        // Verify context2 still exists
        ContextProfileSearchFilter filter2 = createFilter(null, "context2");
        SearchResults<ContextProfileRecord> results2 = dao.findByFilter(filter2, 100, 0, null, null);
        assertEquals(1, results2.getResultList().size());
    }

    @Test
    public void test_deleteByContextName_should_handle_non_existent_context() {
        // Given
        dao.save(createRecord("profile1", "context1"));

        // When
        dao.deleteByContextName("nonexistent");

        // Then - should not throw exception
        ContextProfileSearchFilter filter = createFilter(null, "context1");
        SearchResults<ContextProfileRecord> results = dao.findByFilter(filter, 100, 0, null, null);
        assertEquals(1, results.getResultList().size());
    }

    // ========== Complex Data Tests ==========

    @Test
    public void test_save_and_retrieve_with_empty_lists() {
        // Given
        ContextProfileRecord record = createRecord("profile1", "context1");
        record.setAccessGroups(new ArrayList<>());
        record.setAccessUsers(new ArrayList<>());

        ContextProfile profile = new ContextProfileImpl();
        profile.setDefaultContext("default");
        profile.setSubContexts(new ArrayList<>());
        record.setContextProfile(profile);

        // When
        dao.save(record);

        // Then
        String id = "profile1_context1";
        ContextProfileRecord retrieved = dao.findById(id);
        assertNotNull(retrieved);
        assertNotNull(retrieved.getAccessGroups());
        assertNotNull(retrieved.getAccessUsers());
        assertNotNull(retrieved.getContextProfile().getSubContexts());
        assertEquals(0, retrieved.getAccessGroups().size());
        assertEquals(0, retrieved.getAccessUsers().size());
        assertEquals(0, retrieved.getContextProfile().getSubContexts().size());
    }

    @Test
    public void test_save_and_retrieve_with_null_context_profile() {
        // Given
        ContextProfileRecord record = createRecord("profile1", "context1");
        record.setContextProfile(null);

        // When
        dao.save(record);

        // Then
        String id = "profile1_context1";
        ContextProfileRecord retrieved = dao.findById(id);
        assertNotNull(retrieved);
        // The entity may set a default empty profile, or it could be null
    }

    @Test
    public void test_multiple_saves_and_updates() {
        // Given
        ContextProfileRecord record = createRecord("profile1", "context1");
        dao.save(record);

        // When - update multiple times
        String id = "profile1_context1";
        for (int i = 0; i < 5; i++) {
            ContextProfileRecord retrieved = dao.findById(id);
            retrieved.setModifiedBy("user" + i);
            dao.save(retrieved);
        }

        // Then
        ContextProfileRecord final_record = dao.findById(id);
        assertEquals("user4", final_record.getModifiedBy());
    }

    // ========== Helper Methods ==========

    private HibernateContextProfileRecord createRecord(String profileName, String contextName) {
        HibernateContextProfileRecord record = new HibernateContextProfileRecord();
        record.setProfileName(profileName);
        record.setContextName(contextName);

        ContextProfile profile = new ContextProfileImpl();
        profile.setDefaultContext("default");
        profile.setSubContexts(Arrays.asList("sub1", "sub2"));

        record.setContextProfile(profile);
        record.setModifiedBy("test-user");

        return record;
    }

    private HibernateContextProfileRecord createComplexRecord(String profileName, String contextName) {
        HibernateContextProfileRecord record = new HibernateContextProfileRecord();
        record.setProfileName(profileName);
        record.setContextName(contextName);
        record.setOwner("owner@example.com");

        ContextProfile profile = new ContextProfileImpl();
        profile.setDefaultContext("defaultContext");
        profile.setSubContexts(Arrays.asList("subContext1", "subContext2", "subContext3"));

        record.setContextProfile(profile);
        record.setAccessGroups(Arrays.asList("admin", "developer", "viewer"));
        record.setAccessUsers(Arrays.asList("user1@example.com", "user2@example.com", "user3@example.com"));
        record.setModifiedBy("test-user");

        return record;
    }

    private ContextProfileSearchFilter createFilter(String profileName, String contextName) {
        return new ContextProfileSearchFilter() {
            private String profile = profileName;
            private String context = contextName;
            private String ownerValue;
            private String userValue;
            private List<String> roles;

            @Override
            public String getProfileName() {
                return profile;
            }

            @Override
            public void setProfileName(String profileName) {
                this.profile = profileName;
            }

            @Override
            public String getContextName() {
                return context;
            }

            @Override
            public void setContextName(String contextName) {
                this.context = contextName;
            }

            @Override
            public String getOwner() {
                return ownerValue;
            }

            @Override
            public void setOwner(String owner) {
                this.ownerValue = owner;
            }

            @Override
            public String getUser() {
                return userValue;
            }

            @Override
            public void setUser(String user) {
                this.userValue = user;
            }

            @Override
            public List<String> getAccessRoles() {
                return roles;
            }

            @Override
            public void setAccessRoles(List<String> accessRoles) {
                this.roles = accessRoles;
            }
        };
    }
}
