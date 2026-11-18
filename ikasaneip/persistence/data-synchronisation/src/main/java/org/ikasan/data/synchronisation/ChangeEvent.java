package org.ikasan.data.synchronisation;

import org.bson.Document;

/**
 * A single write to a synchronised Mongo collection.
 *
 * @param operation whether the document was written or removed
 * @param collection the Mongo collection
 * @param id the document {@code _id} as a string
 * @param document the full document for an {@link Operation#UPSERT}; {@code null} for a delete
 */
public record ChangeEvent(Operation operation, String collection, String id, Document document) {

    public enum Operation { UPSERT, DELETE }

    public static ChangeEvent upsert(String collection, Document document) {
        return new ChangeEvent(Operation.UPSERT, collection, String.valueOf(document.get("_id")), document);
    }

    public static ChangeEvent delete(String collection, Object id) {
        return new ChangeEvent(Operation.DELETE, collection, String.valueOf(id), null);
    }
}
