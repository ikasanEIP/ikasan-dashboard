package org.ikasan.data.synchronisation;

/**
 * How writes to Mongo are detected.
 */
public enum SyncMode {
    /** Change streams when Mongo is a replica set or sharded cluster, otherwise the command listener. */
    AUTO,
    /** Mongo change streams: durable (resumable), sees every committed write, requires a replica set. */
    CHANGE_STREAM,
    /** A Mongo driver command listener: works on a standalone Mongo, but only sees this JVM's writes and cannot
     * synchronise deletes by query; the reconciler repairs what it misses. */
    COMMAND_LISTENER
}
