package org.ikasan.data.synchronisation;

import org.bson.Document;

import java.util.List;

/**
 * Writes Mongo documents of one {@code type} to Solr through the matching Solr DAO.
 */
public interface SyncHandler {

    /** The Mongo {@code type} value handled (for documents outside the {@code ikasan} collection, a pseudo type). */
    String type();

    /** The Mongo collection the documents live in. */
    default String collection() {
        return SyncCollections.IKASAN;
    }

    /**
     * Whether the Solr document has the same id as the Mongo document, so a Mongo delete can be applied to Solr by
     * id and the reconciler can safely re-send it. False for types whose Solr DAO generates a random id on every save;
     * those are written once and expire through Solr's own housekeeping.
     */
    boolean deletable();

    /**
     * Convert the documents and save them to Solr.
     *
     * @param documents full Mongo documents of this handler's type
     */
    void upsert(List<Document> documents);
}
