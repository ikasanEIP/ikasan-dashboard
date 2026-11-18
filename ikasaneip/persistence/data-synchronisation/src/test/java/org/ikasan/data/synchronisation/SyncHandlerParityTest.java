package org.ikasan.data.synchronisation;

import org.bson.Document;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Writes one entity of each sample type through the real Mongo DAOs and synchronises them.
 *
 * <p>For every deletable type the Solr document must have the same id as the Mongo document: that is what lets a
 * Mongo delete be applied to Solr by id, and lets the reconciler find orphans.
 */
public class SyncHandlerParityTest extends SyncTestSupport {

    @Test
    public void every_sample_type_has_a_handler() {
        SyncPipeline all = newPipeline(List.of());
        samples.oneOfEach();
        for (Document document : mongoDocuments()) {
            assertNotNull("no handler for type " + document.get("type"),
                all.handlerFor(SyncCollections.IKASAN, document));
        }
    }

    @Test
    public void deletable_types_have_the_same_id_in_solr_as_in_mongo() throws Exception {
        samples.oneOfEach();

        pipeline.apply(mongoDocuments().stream().map(d -> ChangeEvent.upsert(SyncCollections.IKASAN, d)).toList());

        Map<String, List<Document>> byType = mongoDocuments().stream()
            .collect(Collectors.groupingBy(d -> d.getString(EntityFields.TYPE)));
        assertTrue("sample data missing types: " + byType.keySet(), byType.keySet().containsAll(SampleData.TYPES));

        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, List<Document>> entry : byType.entrySet()) {
            String type = entry.getKey();
            SyncHandler handler = pipeline.handlerFor(SyncCollections.IKASAN, entry.getValue().get(0));
            List<String> mongoIds = mongoIds(type);
            List<String> solrIds = solrIds(type);
            if (handler.deletable()) {
                if (!mongoIds.equals(solrIds)) {
                    mismatches.add(type + ": mongo " + mongoIds + " solr " + solrIds);
                }
            } else {
                assertEquals("insert-only type " + type + " should be in Solr once per Mongo document",
                    mongoIds.size(), solrIds.size());
            }
        }
        assertTrue("Solr ids differ from Mongo ids: " + mismatches, mismatches.isEmpty());
    }

    @Test
    public void scheduled_process_events_are_synchronised_from_their_own_collection() throws Exception {
        samples.scheduledProcessEvent("job-1");
        List<Document> events = mongoTemplate.getCollection(SyncCollections.SCHEDULED_PROCESS_EVENT)
            .find().into(new ArrayList<>());
        assertEquals(1, events.size());

        pipeline.apply(List.of(ChangeEvent.upsert(SyncCollections.SCHEDULED_PROCESS_EVENT, events.get(0))));

        assertEquals(1, solrIds(SyncCollections.SCHEDULED_PROCESS_EVENT_TYPE).size());
    }

    @Test
    public void solr_documents_can_be_read_back_through_the_solr_daos() throws Exception {
        samples.oneOfEach();
        pipeline.apply(mongoDocuments().stream().map(d -> ChangeEvent.upsert(SyncCollections.IKASAN, d)).toList());

        assertNotNull(solrDaos.jobLockCache.get("env-1"));
        assertEquals("env-1", solrDaos.jobLockCache.get("env-1").getEnvironment());
        assertNotNull(solrDaos.policy.getPolicyByName("policy-1"));
        assertNotNull(solrDaos.role.getRoleByName("role-1"));
        assertNotNull(solrDaos.user.getUser("user-1"));
        assertEquals("user-1@example.com", solrDaos.user.getUser("user-1").getEmail());
        assertNotNull(solrDaos.internalEventDrivenJob.findById("internalEventDrivenJob_agent_job-1_context"));

        // types read back through their Mongo DAO before being written to Solr
        assertEquals("module-1", solrDaos.moduleMetadata.findById("module-1").getName());
        assertEquals("stream stream-1", solrDaos.businessStreamMetadata.findById("stream-1").getName());
        assertEquals("configuration-1",
            solrDaos.componentConfigurationMetadata.findById("configuration-1").getConfigurationId());
        assertEquals("principal principal-1", solrDaos.principal.getPrincipalByName("principal-1").getDescription());
        assertNotNull(solrDaos.setup.getDashboardPlatformSetup());

        // scheduler types
        // the Solr DAO appends the type to the context name to make the id
        assertEquals("context-1", solrDaos.scheduledContext.findById("context-1").getContextName());
        assertEquals("profile-1",
            solrDaos.contextProfile.findById("profile-1-context-" + ContextProfileDao.CONTEXT_PROFILE_TYPE).getProfileName());
    }
}
