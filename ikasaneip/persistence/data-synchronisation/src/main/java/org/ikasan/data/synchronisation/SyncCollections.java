package org.ikasan.data.synchronisation;

import org.ikasan.mongo.persistence.general.model.MongoConstants;

import java.util.Set;

/**
 * The Mongo collections whose writes are synchronised to Solr.
 */
public final class SyncCollections {

    /** The collection holding every dashboard entity, distinguished by their {@code type} field. */
    public static final String IKASAN = MongoConstants.IKASAN_COLLECTION_NAME;

    /** Scheduled process events have their own collection and no {@code type} field. */
    public static final String SCHEDULED_PROCESS_EVENT = "contextualisedScheduledProcessEventImpl";

    /** Pseudo type used to route scheduled process events, which have no {@code type} field. */
    public static final String SCHEDULED_PROCESS_EVENT_TYPE = "scheduledProcessEvent";

    /** The collection holding this module's own state; never synchronised. */
    public static final String STATE = "ikasanDataSynchronisation";

    public static final Set<String> SYNCHRONISED = Set.of(IKASAN, SCHEDULED_PROCESS_EVENT);

    private SyncCollections() {
    }
}
