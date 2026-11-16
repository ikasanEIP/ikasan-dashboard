package org.ikasan.mongo.persistence.hospital.dao;

import org.bson.Document;
import org.ikasan.mongo.persistence.general.dao.AtlasLocalSupport;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.mongo.persistence.general.dao.PagingConvention;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.mongo.persistence.hospital.model.MongoExclusionEventAction;
import org.ikasan.mongo.persistence.hospital.repository.MongoExclusionEventActionRepository;
import org.ikasan.spec.hospital.model.ExclusionEventAction;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link MongoHospitalDaoImpl#find} against Atlas Local with the shipped search index: every filter must be answered
 * by {@code $search} and return what the regex fallback would.
 */
public class MongoHospitalDaoImplAtlasTest {

    private static final String DB = "atlas_hospital";
    private static final long NOW = System.currentTimeMillis();
    private static final long HOUR = 3_600_000L;

    private static AtlasSearchQueryHelper helper;
    private static MongoHospitalDaoImpl dao;

    @BeforeClass
    public static void seed() {
        MongoTemplate template = AtlasLocalSupport.freshDatabase(DB);
        helper = AtlasLocalSupport.searchHelper(template);
        dao = new MongoHospitalDaoImpl(new MongoRepositoryFactory(template)
            .getRepository(MongoExclusionEventActionRepository.class), template, 7, helper);

        dao.save(action("h1", "m1", "f1", "alice", "order 1 resubmitted", NOW - 3 * HOUR));
        dao.save(action("h2", "m1", "f2", "bob", "order 2 ignored", NOW - 2 * HOUR));
        dao.save(action("h3", "m2", "f1", "Alice.Smith", "invoice 3", NOW - HOUR));
        dao.save(action("h4", "m2", "f1", "carol", "order 4 resubmitted", NOW));
        for (int i = 0; i < 8; i++) {
            dao.save(action("p" + i, "paging", "f1", "dave", "order " + i, NOW - i));
        }
        // another document type in the shared collection that matches every other filter: must never be returned
        template.getCollection(MongoConstants.IKASAN_COLLECTION_NAME).insertOne(new Document("type", "exclusion")
            .append("moduleName", "m1").append("flowName", "f1").append("payload", "order decoy")
            .append("actor", "alice").append("errorUri", "decoy").append("timestamp", NOW - HOUR));

        AtlasLocalSupport.applyShippedIndexes(DB);
        AtlasLocalSupport.awaitSearchable(DB, MongoConstants.IKASAN_COLLECTION_NAME, "type", 13);
    }

    @Before
    public void atlasSearchStillInUse() {
        assertTrue("Atlas Search must not have fallen back to regex", helper.isAtlasSearchAvailable());
    }

    @Test
    public void search_term_matches_the_payload() {
        assertEquals(Set.of("h1", "h2", "h4"), uris(() -> dao.find(List.of("m1", "m2"), null, "order", 0, 0, -1, -1)));
    }

    @Test
    public void search_term_matches_the_actor_case_insensitively() {
        assertEquals(Set.of("h1", "h3"), uris(() -> dao.find(null, null, "ALICE", 0, 0, -1, -1)));
    }

    @Test
    public void search_term_matches_the_error_uri() {
        assertEquals(Set.of("h3"), uris(() -> dao.find(null, null, "h3", 0, 0, -1, -1)));
    }

    @Test
    public void module_and_flow_filters() {
        assertEquals(Set.of("h1"), uris(() -> dao.find(List.of("m1"), List.of("f1"), "order", 0, 0, -1, -1)));
        assertEquals(Set.of("h4"), uris(() -> dao.find(List.of("m2"), List.of("f1"), "order", 0, 0, -1, -1)));
    }

    @Test
    public void time_range_filter() {
        assertEquals(Set.of("h2", "h4"),
            uris(() -> dao.find(List.of("m1", "m2"), null, "order", NOW - 150 * 60_000L, NOW, -1, -1)));
    }

    @Test
    public void no_match() {
        assertEquals(Set.of(), uris(() -> dao.find(null, null, "nonexistent", 0, 0, -1, -1)));
    }

    @Test
    public void paging_convention() {
        PagingConvention.assertFollowed(8, (offset, limit) -> PagingConvention.page(AtlasLocalSupport.pagedListUsingSearch(DB, limit,
            () -> dao.find(List.of("paging"), null, "order", 0, 0, offset, limit))));
    }

    private static Set<String> uris(Supplier<List<ExclusionEventAction>> call) {
        return AtlasLocalSupport.usingSearch(DB, call).stream()
            .map(ExclusionEventAction::getErrorUri)
            .collect(Collectors.toSet());
    }

    private static MongoExclusionEventAction action(String errorUri, String module, String flow, String actor,
                                                    String payload, long timestamp) {
        MongoExclusionEventAction action = new MongoExclusionEventAction(module, flow, errorUri, actor,
            MongoExclusionEventAction.RESUBMIT, payload, timestamp, "comment");
        action.setExpiry(NOW + 7 * 24 * HOUR);
        return action;
    }
}
