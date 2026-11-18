package org.ikasan.data.synchronisation;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A {@link SyncHandler} that converts each Mongo document to the object the Solr DAO accepts and saves them in one
 * call.
 *
 * <p>A document that cannot be converted is logged with its stack trace and skipped, so one bad document does not
 * stop the others; the reconciler re-sends it later. A failure to save to Solr is thrown, so the caller can retry
 * the whole batch.
 *
 * @param <S> the type the Solr DAO saves
 */
public class EntitySyncHandler<S> implements SyncHandler {

    private static final Logger logger = LoggerFactory.getLogger(EntitySyncHandler.class);

    private final String type;
    private final String collection;
    private final boolean deletable;
    private final Function<Document, S> converter;
    private final Consumer<List<S>> saver;

    public EntitySyncHandler(String type, String collection, boolean deletable,
                             Function<Document, S> converter, Consumer<List<S>> saver) {
        this.type = Objects.requireNonNull(type);
        this.collection = Objects.requireNonNull(collection);
        this.deletable = deletable;
        this.converter = Objects.requireNonNull(converter);
        this.saver = Objects.requireNonNull(saver);
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public String collection() {
        return collection;
    }

    @Override
    public boolean deletable() {
        return deletable;
    }

    @Override
    public void upsert(List<Document> documents) {
        List<S> converted = new ArrayList<>(documents.size());
        for (Document document : documents) {
            try {
                S entity = converter.apply(document);
                if (entity == null) {
                    logger.warn("Skipping [{}] document [{}]: it could not be read back from Mongo", type,
                        document.get("_id"));
                } else {
                    converted.add(entity);
                }
            } catch (RuntimeException e) {
                logger.error(String.format("Skipping [%s] document [%s]: it could not be converted for Solr", type,
                    document.get("_id")), e);
            }
        }
        if (!converted.isEmpty()) {
            saver.accept(converted);
        }
    }

    @Override
    public String toString() {
        return "EntitySyncHandler{type='" + type + "', collection='" + collection + "', deletable=" + deletable + '}';
    }
}
