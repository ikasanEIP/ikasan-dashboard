package org.ikasan.mongo.persistence.replay.dao;

import org.bson.Document;
import org.ikasan.mongo.persistence.general.dao.AtlasLocalSupport;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.mongo.persistence.general.dao.PagingConvention;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.mongo.persistence.replay.model.MongoReplayEventImpl;
import org.ikasan.mongo.persistence.replay.repository.MongoReplayEventRepository;
import org.ikasan.spec.replay.ReplayEvent;
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
 * {@link MongoReplayDaoImpl#find} against Atlas Local with the shipped search index: every filter must be answered by
 * {@code $search} and return what the regex fallback would.
 */
public class MongoReplayDaoImplAtlasTest {

    private static final String DB = "atlas_replay";
    private static final long NOW = System.currentTimeMillis();
    private static final long HOUR = 3_600_000L;

    private static AtlasSearchQueryHelper helper;
    private static MongoReplayDaoImpl dao;

    @BeforeClass
    public static void seed() {
        MongoTemplate template = AtlasLocalSupport.freshDatabase(DB);
        helper = AtlasLocalSupport.searchHelper(template);
        dao = new MongoReplayDaoImpl(new MongoRepositoryFactory(template)
            .getRepository(MongoReplayEventRepository.class), template, helper);

        dao.save(replay("evt-7f3a-0001", "m1", "f1", "order 1 received", NOW - 3 * HOUR));
        dao.save(replay("evt-7f3a-0002", "m1", "f2", "order 2 received", NOW - 2 * HOUR));
        dao.save(replay("evt-9c1d-0003", "m2", "f1", "invoice 3 received", NOW - HOUR));
        dao.save(replay("evt-9c1d-0004", "m2", "f1", "order 4 received", NOW));
        for (int i = 0; i < 8; i++) {
            dao.save(replay("paging-" + i, "paging", "f1", "order " + i, NOW - i));
        }
        // another document type in the shared collection that matches every other filter: must never be returned
        template.getCollection(MongoConstants.IKASAN_COLLECTION_NAME).insertOne(new Document("type", "exclusion")
            .append("moduleName", "m1").append("flowName", "f1").append("payload", "order decoy")
            .append("event", "evt-7f3a-0001").append("timestamp", NOW - HOUR));

        AtlasLocalSupport.applyShippedIndexes(DB);
        AtlasLocalSupport.awaitSearchable(DB, MongoConstants.IKASAN_COLLECTION_NAME, "type", 13);
    }

    @Before
    public void atlasSearchStillInUse() {
        assertTrue("Atlas Search must not have fallen back to regex", helper.isAtlasSearchAvailable());
    }

    @Test
    public void search_term_matches_the_payload() {
        assertEquals(Set.of("evt-7f3a-0001", "evt-7f3a-0002", "evt-9c1d-0004"),
            eventIds(() -> dao.find(List.of("m1", "m2"), null, "Order", 0, 0, -1, -1)));
    }

    @Test
    public void search_term_matches_a_whole_event_id() {
        assertEquals(Set.of("evt-7f3a-0002"), eventIds(() -> dao.find(null, null, "evt-7f3a-0002", 0, 0, -1, -1)));
    }

    @Test
    public void search_term_matches_part_of_an_event_id() {
        assertEquals(Set.of("evt-9c1d-0003", "evt-9c1d-0004"),
            eventIds(() -> dao.find(null, null, "9c1d", 0, 0, -1, -1)));
    }

    @Test
    public void module_and_flow_filters() {
        assertEquals(Set.of("evt-7f3a-0001"),
            eventIds(() -> dao.find(List.of("m1"), List.of("f1"), "order", 0, 0, -1, -1)));
    }

    @Test
    public void time_range_filter() {
        assertEquals(Set.of("evt-7f3a-0002", "evt-9c1d-0004"),
            eventIds(() -> dao.find(List.of("m1", "m2"), null, "order", NOW - 150 * 60_000L, NOW, -1, -1)));
    }

    @Test
    public void no_match() {
        assertEquals(Set.of(), eventIds(() -> dao.find(null, null, "nonexistent", 0, 0, -1, -1)));
    }

    @Test
    public void paging_convention() {
        PagingConvention.assertFollowed(8, (offset, limit) -> PagingConvention.page(AtlasLocalSupport.pagedListUsingSearch(DB, limit,
            () -> dao.find(List.of("paging"), null, "order", 0, 0, offset, limit))));
    }

    private static Set<String> eventIds(Supplier<List<ReplayEvent>> call) {
        return AtlasLocalSupport.usingSearch(DB, call).stream()
            .map(ReplayEvent::getEventId)
            .collect(Collectors.toSet());
    }

    private static MongoReplayEventImpl replay(String eventId, String module, String flow, String payload,
                                               long timestamp) {
        MongoReplayEventImpl event = new MongoReplayEventImpl();
        event.setModuleName(module);
        event.setFlowName(flow);
        event.setEventId(eventId);
        event.setEvent(payload.getBytes());
        event.setEventAsString(payload);
        event.setTimestamp(timestamp);
        event.setExpiry(NOW + 7 * 24 * HOUR);
        return event;
    }
}
