package org.ikasan.data.synchronisation;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MongoTopologyTest {

    private static final MongoTopology.Topology REPLICA_SET = new MongoTopology.Topology(true, "replica set [rs]");
    private static final MongoTopology.Topology STANDALONE = new MongoTopology.Topology(false, "standalone server");

    @Test
    public void auto_uses_change_streams_on_a_replica_set_and_the_listener_otherwise() {
        assertEquals(SyncMode.CHANGE_STREAM, MongoTopology.resolve(SyncMode.AUTO, REPLICA_SET));
        assertEquals(SyncMode.COMMAND_LISTENER, MongoTopology.resolve(SyncMode.AUTO, STANDALONE));
    }

    @Test
    public void explicit_modes_are_kept() {
        assertEquals(SyncMode.CHANGE_STREAM, MongoTopology.resolve(SyncMode.CHANGE_STREAM, REPLICA_SET));
        assertEquals(SyncMode.COMMAND_LISTENER, MongoTopology.resolve(SyncMode.COMMAND_LISTENER, REPLICA_SET));
        assertEquals(SyncMode.COMMAND_LISTENER, MongoTopology.resolve(SyncMode.COMMAND_LISTENER, STANDALONE));
    }

    @Test
    public void change_streams_on_a_standalone_server_fail_with_a_clear_message() {
        try {
            MongoTopology.resolve(SyncMode.CHANGE_STREAM, STANDALONE);
            fail("expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("--replSet"));
        }
    }

    @Test
    public void detects_a_replica_set() {
        assertTrue(MongoTopology.detect(TestEnvironment.get().mongoTemplate()).supportsChangeStreams());
    }

    @Test
    public void detects_a_standalone_server() {
        try (GenericContainer<?> standalone = new GenericContainer<>(DockerImageName.parse("mongo:7.0"))
            .withExposedPorts(27017)) {
            standalone.start();
            try (MongoClient client = MongoClients.create("mongodb://" + standalone.getHost() + ":"
                + standalone.getMappedPort(27017))) {
                MongoTopology.Topology topology = MongoTopology.detect(new MongoTemplate(client, "test"));
                assertFalse(topology.supportsChangeStreams());
                assertEquals("standalone server", topology.description());
            }
        }
    }
}
