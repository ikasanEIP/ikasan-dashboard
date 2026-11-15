package org.ikasan.mongo.persistence;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.BsonDocument;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MongoCommandLoggingListenerTest {

    private static final String DB = "commandlogging";
    private static final String COLLECTION = "ikasan";

    private static MongoDBContainer mongo;
    private static MongoClient client;
    private static MongoCommandLoggingListener listener;

    private final Logger logger = (Logger) LoggerFactory.getLogger(MongoCommandLoggingListener.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level originalLevel;
    private MongoCollection<Document> collection;

    @BeforeClass
    public static void startMongo() {
        mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongo.start();

        listener = new MongoCommandLoggingListener(200);
        client = MongoClients.create(MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(mongo.getReplicaSetUrl(DB)))
            .addCommandListener(listener)
            .build());
    }

    @AfterClass
    public static void stopMongo() {
        if (client != null) {
            client.close();
        }
        if (mongo != null) {
            mongo.stop();
        }
    }

    @Before
    public void setUp() {
        collection = client.getDatabase(DB).getCollection(COLLECTION);
        collection.drop();

        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    @After
    public void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
    }

    @Test
    public void find_is_logged_with_its_filter_execution_time_and_result_count() {
        collection.insertMany(List.of(new Document("componentName", "a"), new Document("componentName", "a"),
            new Document("componentName", "b")));
        appender.list.clear();

        collection.find(Filters.eq("componentName", "a")).into(new ArrayList<>());

        String message = onlyMessage();
        assertTrue(message, message.matches(
            "find on commandlogging\\.ikasan took \\d+\\.\\d{2} ms, returned 2: \\{\"find\": \"ikasan\", "
                + "\"filter\": \\{\"componentName\": \"a\"}.*"));
    }

    @Test
    public void spring_template_queries_aggregates_counts_and_writes_are_all_logged() {
        MongoTemplate template = new MongoTemplate(client, DB);
        collection.insertOne(new Document("type", "job").append("componentName", "ctx-1"));

        template.find(new Query(Criteria.where("type").is("job")), Document.class, COLLECTION);
        collection.aggregate(List.of(new Document("$match", new Document("type", "job")))).into(new ArrayList<>());
        collection.countDocuments(Filters.eq("type", "job"));
        collection.updateOne(Filters.eq("type", "job"), Updates.set("status", "RUNNING"));
        collection.deleteMany(Filters.eq("type", "job"));

        List<String> messages = messages();
        assertEquals(messages.toString(), 6, messages.size());
        assertTrue(messages.get(0), messages.get(0).startsWith("insert on commandlogging.ikasan took "));
        assertTrue(messages.get(0), messages.get(0).contains(", n=1: "));
        assertTrue(messages.get(1), messages.get(1).startsWith("find on commandlogging.ikasan took "));
        assertTrue(messages.get(1), messages.get(1).contains(", returned 1: "));
        assertTrue(messages.get(2), messages.get(2).startsWith("aggregate on commandlogging.ikasan took "));
        assertTrue(messages.get(2), messages.get(2).contains("\"pipeline\": [{\"$match\": {\"type\": \"job\"}}]"));
        assertTrue(messages.get(3), messages.get(3).startsWith("aggregate on commandlogging.ikasan took ")); // countDocuments
        assertTrue(messages.get(4), messages.get(4).startsWith("update on commandlogging.ikasan took "));
        assertTrue(messages.get(5), messages.get(5).startsWith("delete on commandlogging.ikasan took "));
        assertEquals(0, listener.inFlightCount());
    }

    @Test
    public void driver_added_session_fields_are_not_logged() {
        collection.find().first();

        String message = onlyMessage();
        assertFalse(message, message.contains("lsid"));
        assertFalse(message, message.contains("$clusterTime"));
        assertFalse(message, message.contains("$db"));
    }

    @Test
    public void long_commands_are_truncated() {
        collection.insertOne(new Document("payload", "x".repeat(1000)));

        String message = onlyMessage();
        assertTrue(message, message.matches(".*\\.\\.\\. \\(\\d+ chars, truncated\\)$"));
        assertTrue(message, message.length() < 400);
    }

    @Test
    public void failed_commands_are_logged_with_the_error() {
        assertThrows(MongoCommandException.class, () -> client.getDatabase(DB)
            .runCommand(new Document("aggregate", COLLECTION)
                .append("pipeline", List.of(new Document("$notAStage", 1)))
                .append("cursor", new Document())));

        String message = onlyMessage();
        assertTrue(message, message.startsWith("aggregate on commandlogging.ikasan FAILED after "));
        assertTrue(message, message.contains("(Location40324: Unrecognized pipeline stage name: '$notAStage')"));
        assertFalse(message, message.contains("$clusterTime"));
        assertEquals(0, listener.inFlightCount());
    }

    @Test
    public void nothing_is_logged_or_tracked_when_debug_is_off() {
        logger.setLevel(Level.INFO);

        collection.insertOne(new Document("componentName", "a"));
        collection.find().into(new ArrayList<>());

        assertTrue(appender.list.isEmpty());
        assertEquals(0, listener.inFlightCount());
    }

    @Test
    public void driver_housekeeping_commands_are_ignored() {
        assertTrue(MongoCommandLoggingListener.isIgnored("hello"));
        assertTrue(MongoCommandLoggingListener.isIgnored("isMaster"));
        assertTrue(MongoCommandLoggingListener.isIgnored("saslStart"));
        assertFalse(MongoCommandLoggingListener.isIgnored("find"));
        assertFalse(MongoCommandLoggingListener.isIgnored("aggregate"));

        client.getDatabase(DB).runCommand(new Document("ping", 1));
        assertTrue(messages().toString(), messages().isEmpty());
    }

    @Test
    public void commands_are_logged_in_full_when_max_length_is_not_positive() {
        BsonDocument command = BsonDocument.parse("{\"insert\": \"ikasan\", \"payload\": \"" + "x".repeat(5000) + "\"}");
        String json = new MongoCommandLoggingListener(0).describe(command);
        assertTrue(json.length() > 5000);
        assertFalse(json.contains("truncated"));
    }

    private String onlyMessage() {
        List<String> messages = messages();
        assertEquals(messages.toString(), 1, messages.size());
        return messages.get(0);
    }

    private List<String> messages() {
        return appender.list.stream()
            .filter(event -> event.getLevel() == Level.DEBUG)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }
}
