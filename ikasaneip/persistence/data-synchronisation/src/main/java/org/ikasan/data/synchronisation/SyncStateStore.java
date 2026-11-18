package org.ikasan.data.synchronisation;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.BsonDocument;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Date;
import java.util.Optional;

/**
 * The synchronisation's own state, kept in the {@value SyncCollections#STATE} collection (never synchronised).
 *
 * <ul>
 *     <li>{@code resumeToken}: where the change stream carries on from after a restart</li>
 *     <li>{@code reconcile}: when Mongo was last fully loaded into Solr</li>
 *     <li>{@code lease}: which instance is synchronising (see {@link SyncLease})</li>
 * </ul>
 */
public class SyncStateStore {

    static final String RESUME_TOKEN_ID = "resumeToken";
    static final String RECONCILE_ID = "reconcile";

    private final MongoTemplate mongoTemplate;

    public SyncStateStore(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    MongoCollection<Document> collection() {
        return mongoTemplate.getCollection(SyncCollections.STATE);
    }

    public Optional<BsonDocument> resumeToken() {
        Document document = collection().find(Filters.eq("_id", RESUME_TOKEN_ID)).first();
        return Optional.ofNullable(document)
            .map(d -> d.getString("token"))
            .map(BsonDocument::parse);
    }

    public void saveResumeToken(BsonDocument token) {
        collection().replaceOne(Filters.eq("_id", RESUME_TOKEN_ID),
            new Document("_id", RESUME_TOKEN_ID).append("token", token.toJson()).append("updated", new Date()),
            new ReplaceOptions().upsert(true));
    }

    public void clearResumeToken() {
        collection().deleteOne(Filters.eq("_id", RESUME_TOKEN_ID));
    }

    /** @return when Mongo was last fully loaded into Solr, if ever */
    public Optional<Date> lastFullLoad() {
        Document document = collection().find(Filters.eq("_id", RECONCILE_ID)).first();
        return Optional.ofNullable(document).map(d -> d.getDate("lastFullLoad"));
    }

    public void saveFullLoad(Date completed, long documents, long orphansRemoved) {
        collection().replaceOne(Filters.eq("_id", RECONCILE_ID),
            new Document("_id", RECONCILE_ID)
                .append("lastFullLoad", completed)
                .append("documents", documents)
                .append("orphansRemoved", orphansRemoved),
            new ReplaceOptions().upsert(true));
    }
}
