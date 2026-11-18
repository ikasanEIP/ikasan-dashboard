package org.ikasan.data.synchronisation;

import com.mongodb.MongoException;
import com.mongodb.ErrorCategory;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.Date;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.or;

/**
 * A lease, held in the state collection, so that only one of several dashboard instances sharing a Mongo database
 * synchronises it. The holder renews it well before it expires; if the holder dies, another instance takes over once
 * it has expired.
 */
public class SyncLease {

    static final String LEASE_ID = "lease";

    private final SyncStateStore store;
    private final String owner;
    private final long durationMillis;

    public SyncLease(SyncStateStore store, String owner, long durationMillis) {
        this.store = store;
        this.owner = owner;
        this.durationMillis = durationMillis;
    }

    /**
     * Acquire the lease, or renew it if already held.
     *
     * @return whether this instance now holds the lease
     */
    public boolean acquireOrRenew() {
        Date now = new Date();
        try {
            Document lease = store.collection().findOneAndUpdate(
                and(eq("_id", LEASE_ID), or(eq("owner", owner), lt("expiresAt", now))),
                Updates.combine(Updates.set("owner", owner),
                    Updates.set("expiresAt", new Date(now.getTime() + durationMillis))),
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
            return lease != null && owner.equals(lease.getString("owner"));
        } catch (MongoException e) {
            if (ErrorCategory.fromErrorCode(e.getCode()) == ErrorCategory.DUPLICATE_KEY) {
                // the filter didn't match (another instance holds an unexpired lease), so the upsert tried to
                // insert a second lease document
                return false;
            }
            throw e;
        }
    }

    /** Give the lease up so another instance can take over straight away. */
    public void release() {
        store.collection().deleteOne(and(eq("_id", LEASE_ID), eq("owner", owner)));
    }

    public String owner() {
        return owner;
    }

    public long durationMillis() {
        return durationMillis;
    }
}
