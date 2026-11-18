package org.ikasan.data.synchronisation;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Works out whether the Mongo deployment supports change streams: a replica set or a sharded cluster does, a
 * standalone server does not.
 */
public final class MongoTopology {

    private MongoTopology() {
    }

    public record Topology(boolean supportsChangeStreams, String description) {
    }

    public static Topology detect(MongoTemplate mongoTemplate) {
        Document hello = mongoTemplate.getDb().runCommand(new Document("hello", 1));
        String setName = hello.getString("setName");
        if (setName != null) {
            return new Topology(true, "replica set [" + setName + "]");
        }
        if ("isdbgrid".equals(hello.getString("msg"))) {
            return new Topology(true, "sharded cluster");
        }
        return new Topology(false, "standalone server");
    }

    /**
     * Resolve {@link SyncMode#AUTO} to the mode to run.
     *
     * @throws IllegalStateException if change streams are required but not supported
     */
    public static SyncMode resolve(SyncMode configured, Topology topology) {
        return switch (configured) {
            case AUTO -> topology.supportsChangeStreams() ? SyncMode.CHANGE_STREAM : SyncMode.COMMAND_LISTENER;
            case CHANGE_STREAM -> {
                if (!topology.supportsChangeStreams()) {
                    throw new IllegalStateException(DataSynchronisationProperties.PREFIX + ".mode=change-stream needs"
                        + " Mongo to be a replica set or sharded cluster, but it is a " + topology.description()
                        + ". Start mongod with --replSet and run rs.initiate(), or use mode=auto or"
                        + " mode=command-listener.");
                }
                yield SyncMode.CHANGE_STREAM;
            }
            case COMMAND_LISTENER -> SyncMode.COMMAND_LISTENER;
        };
    }
}
