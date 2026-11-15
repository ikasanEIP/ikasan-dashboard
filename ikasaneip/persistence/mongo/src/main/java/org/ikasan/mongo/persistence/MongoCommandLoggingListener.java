package org.ikasan.mongo.persistence;

import com.mongodb.MongoCommandException;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Logs every command the MongoDB driver sends (find, aggregate, count, insert, update, delete, ...) at DEBUG level,
 * with the command itself and the server round-trip time measured by the driver.
 *
 * <p>Registered on the {@code MongoClient} built by {@link MongoPersistenceAutoConfiguration}, so it covers every DAO and
 * {@code MongoTemplate} call without changes to them. Enable it with:</p>
 * <pre>
 * logging.level.org.ikasan.mongo.persistence.MongoCommandLoggingListener=DEBUG
 * </pre>
 *
 * <p>Example output:</p>
 * <pre>
 * find on SCHEDULER.ikasan took 3.21 ms, returned 2: {"find": "ikasan", "filter": {"type": "...", "componentName": "..."}}
 * </pre>
 *
 * <p>When DEBUG is off the listener does no work beyond the level check. Driver housekeeping (handshakes, heartbeats,
 * authentication) is not logged. Session and cluster-time fields are stripped from the logged command, and long
 * commands (for example inserts carrying large documents) are truncated to {@code maxLength} characters.</p>
 */
public class MongoCommandLoggingListener implements CommandListener {

    private static final Logger logger = LoggerFactory.getLogger(MongoCommandLoggingListener.class);

    /** Default maximum number of characters of a command to log. */
    public static final int DEFAULT_MAX_LENGTH = 2000;

    /** Driver housekeeping commands, not issued by the application. */
    private static final Set<String> IGNORED_COMMANDS = Set.of(
        "hello", "ismaster", "buildinfo", "ping", "getlasterror", "endsessions", "killcursors",
        "saslstart", "saslcontinue", "authenticate", "getnonce");

    /** Fields the driver adds to every command; they say nothing about the query. */
    private static final Set<String> IGNORED_FIELDS = Set.of(
        "$db", "lsid", "$clusterTime", "$readPreference", "txnNumber", "autocommit", "apiVersion", "apiStrict",
        "apiDeprecationErrors");

    private static final JsonWriterSettings JSON = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

    private final int maxLength;

    /** A command in flight, rendered when it started (the driver may reuse the command buffer afterwards). */
    private record Started(String collection, String command) {
    }

    /** Commands started but not yet completed, by driver request id. */
    private final Map<Integer, Started> inFlight = new ConcurrentHashMap<>();

    public MongoCommandLoggingListener() {
        this(DEFAULT_MAX_LENGTH);
    }

    /**
     * @param maxLength maximum number of characters of a command to log; {@code <= 0} logs commands in full
     */
    public MongoCommandLoggingListener(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public void commandStarted(CommandStartedEvent event) {
        if (!logger.isDebugEnabled() || isIgnored(event.getCommandName())) {
            return;
        }
        inFlight.put(event.getRequestId(),
            new Started(collectionName(event.getCommand()), describe(event.getCommand())));
    }

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        Started started = inFlight.remove(event.getRequestId());
        if (started == null || !logger.isDebugEnabled()) {
            return;
        }
        logger.debug("{} on {}{} took {} ms{}: {}", event.getCommandName(), event.getDatabaseName(),
            started.collection(), millis(event.getElapsedTime(TimeUnit.NANOSECONDS)),
            returnedCount(event.getResponse()), started.command());
    }

    @Override
    public void commandFailed(CommandFailedEvent event) {
        Started started = inFlight.remove(event.getRequestId());
        if (started == null || !logger.isDebugEnabled()) {
            return;
        }
        logger.debug("{} on {}{} FAILED after {} ms ({}): {}", event.getCommandName(), event.getDatabaseName(),
            started.collection(), millis(event.getElapsedTime(TimeUnit.NANOSECONDS)),
            errorMessage(event.getThrowable()), started.command());
    }

    /** Number of commands started but not yet completed; for tests. */
    int inFlightCount() {
        return inFlight.size();
    }

    static boolean isIgnored(String commandName) {
        return commandName == null || IGNORED_COMMANDS.contains(commandName.toLowerCase(Locale.ROOT));
    }

    /**
     * Renders the command as relaxed extended JSON, without driver-added fields and truncated to {@code maxLength}.
     */
    String describe(BsonDocument command) {
        BsonDocument relevant = new BsonDocument();
        for (Map.Entry<String, BsonValue> field : command.entrySet()) {
            if (!IGNORED_FIELDS.contains(field.getKey())) {
                relevant.put(field.getKey(), field.getValue());
            }
        }
        String json = relevant.toJson(JSON);
        if (maxLength > 0 && json.length() > maxLength) {
            return json.substring(0, maxLength) + "... (" + json.length() + " chars, truncated)";
        }
        return json;
    }

    /** The collection a command targets: its first field's value (e.g. {"find": "ikasan"}), or "" if it has none. */
    static String collectionName(BsonDocument command) {
        if (command.isEmpty()) {
            return "";
        }
        BsonValue target = command.get(command.getFirstKey());
        return target != null && target.isString() ? "." + target.asString().getValue() : "";
    }

    /** ", returned N" for cursor and write replies, or "" when the reply has no count. */
    private static String returnedCount(BsonDocument response) {
        if (response == null) {
            return "";
        }
        BsonValue cursor = response.get("cursor");
        if (cursor != null && cursor.isDocument()) {
            BsonDocument cursorDoc = cursor.asDocument();
            BsonValue batch = cursorDoc.containsKey("firstBatch") ? cursorDoc.get("firstBatch") : cursorDoc.get("nextBatch");
            if (batch instanceof BsonArray array) {
                return ", returned " + array.size();
            }
        }
        BsonValue n = response.get("n");
        if (n != null && n.isNumber()) {
            return ", n=" + n.asNumber().longValue();
        }
        return "";
    }

    private static String errorMessage(Throwable error) {
        if (error instanceof MongoCommandException commandError) {
            return commandError.getErrorCodeName() + ": " + commandError.getErrorMessage();
        }
        return String.valueOf(error.getMessage());
    }

    private static String millis(long nanos) {
        return String.format(Locale.ROOT, "%.2f", nanos / 1_000_000.0);
    }
}
