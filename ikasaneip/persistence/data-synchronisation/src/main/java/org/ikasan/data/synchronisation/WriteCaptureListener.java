package org.ikasan.data.synchronisation;

import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A Mongo driver {@link CommandListener} that captures the writes this application makes to the synchronised
 * collections, for the {@link SyncMode#COMMAND_LISTENER command listener} mode.
 *
 * <p>It is registered with the Mongo client when the synchronisation is enabled, but stays inactive (and costs
 * nothing) until {@link #activate} is called. Writes are captured when a command starts, because the driver may
 * reuse the command's buffer afterwards, and handed on only when the command succeeds.
 *
 * <p>What it can capture:
 * <ul>
 *     <li>{@code insert}: every inserted document</li>
 *     <li>{@code update} replacing a whole document (which is how the dashboard writes, through
 *     {@code repository.save})</li>
 *     <li>{@code delete} by {@code _id} or {@code _id $in}</li>
 * </ul>
 * Updates with operators and deletes by any other query cannot be resolved to documents; they are logged at DEBUG
 * and left to the reconciler.
 *
 * <p>The listener never blocks a Mongo command: if the sink refuses the changes they are dropped with a warning,
 * and the reconciler repairs Solr.
 */
public class WriteCaptureListener implements CommandListener {

    private static final Logger logger = LoggerFactory.getLogger(WriteCaptureListener.class);

    private static final DocumentCodec DOCUMENT_CODEC = new DocumentCodec();

    private record Captured(String command, List<ChangeEvent> events) {
    }

    private final Map<Integer, Captured> inFlight = new ConcurrentHashMap<>();
    private volatile Consumer<List<ChangeEvent>> sink;

    /**
     * Start capturing writes and passing them to the sink.
     */
    public void activate(Consumer<List<ChangeEvent>> sink) {
        this.sink = sink;
    }

    public void deactivate() {
        this.sink = null;
        inFlight.clear();
    }

    public boolean isActive() {
        return sink != null;
    }

    @Override
    public void commandStarted(CommandStartedEvent event) {
        if (sink == null) {
            return;
        }
        try {
            String commandName = event.getCommandName();
            if (!"insert".equals(commandName) && !"update".equals(commandName) && !"delete".equals(commandName)) {
                return;
            }
            BsonDocument command = event.getCommand();
            BsonValue target = command.get(commandName);
            if (target == null || !target.isString()
                || !SyncCollections.SYNCHRONISED.contains(target.asString().getValue())) {
                return;
            }
            String collection = target.asString().getValue();
            List<ChangeEvent> events = switch (commandName) {
                case "insert" -> inserts(collection, command);
                case "update" -> updates(collection, command);
                default -> deletes(collection, command);
            };
            inFlight.put(event.getRequestId(), new Captured(commandName, events));
        } catch (RuntimeException e) {
            // never let synchronisation break a Mongo command
            logger.error("Could not capture a Mongo " + event.getCommandName() + " for synchronisation", e);
        }
    }

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        Captured captured = inFlight.remove(event.getRequestId());
        Consumer<List<ChangeEvent>> current = sink;
        if (captured == null || current == null) {
            return;
        }
        List<ChangeEvent> events = withoutFailedWrites(captured.events(), event.getResponse());
        if (!events.isEmpty()) {
            current.accept(events);
        }
    }

    @Override
    public void commandFailed(CommandFailedEvent event) {
        inFlight.remove(event.getRequestId());
    }

    private static List<ChangeEvent> inserts(String collection, BsonDocument command) {
        List<ChangeEvent> events = new ArrayList<>();
        for (BsonValue document : array(command, "documents")) {
            events.add(document.isDocument() ? ChangeEvent.upsert(collection, decode(document.asDocument())) : null);
        }
        return events;
    }

    private static List<ChangeEvent> updates(String collection, BsonDocument command) {
        List<ChangeEvent> events = new ArrayList<>();
        for (BsonValue value : array(command, "updates")) {
            ChangeEvent event = null;
            if (value.isDocument()) {
                BsonDocument update = value.asDocument();
                BsonValue replacement = update.get("u");
                if (replacement != null && replacement.isDocument() && isReplacement(replacement.asDocument())) {
                    BsonDocument document = replacement.asDocument().clone();
                    if (!document.containsKey("_id")) {
                        BsonValue id = idOf(update.get("q"));
                        if (id != null) {
                            document.put("_id", id);
                        }
                    }
                    if (document.containsKey("_id")) {
                        event = ChangeEvent.upsert(collection, decode(document));
                    }
                }
                if (event == null) {
                    logger.debug("An update of [{}] cannot be synchronised by the command listener (not a whole"
                        + " document replacement by _id); the reconciler will apply it", collection);
                }
            }
            events.add(event);
        }
        return events;
    }

    private static List<ChangeEvent> deletes(String collection, BsonDocument command) {
        List<ChangeEvent> events = new ArrayList<>();
        for (BsonValue value : array(command, "deletes")) {
            BsonValue query = value.isDocument() ? value.asDocument().get("q") : null;
            BsonValue id = idOf(query);
            if (id != null) {
                events.add(ChangeEvent.delete(collection, ChangeStreamSynchroniser.idOf(id)));
            } else {
                BsonArray ids = idsIn(query);
                if (ids != null) {
                    ids.forEach(each -> events.add(ChangeEvent.delete(collection, ChangeStreamSynchroniser.idOf(each))));
                } else {
                    logger.debug("A delete by query on [{}] cannot be synchronised by the command listener; the"
                        + " reconciler will remove the documents from Solr", collection);
                }
            }
        }
        return events;
    }

    /**
     * Drop the changes for writes the server reported as failed ({@code writeErrors} carry the index of the failed
     * statement), and the statements that could not be captured.
     */
    private static List<ChangeEvent> withoutFailedWrites(List<ChangeEvent> events, BsonDocument response) {
        Set<Integer> failed = new HashSet<>();
        BsonValue writeErrors = response == null ? null : response.get("writeErrors");
        if (writeErrors != null && writeErrors.isArray()) {
            for (BsonValue error : writeErrors.asArray()) {
                if (error.isDocument() && error.asDocument().isNumber("index")) {
                    failed.add(error.asDocument().getNumber("index").intValue());
                }
            }
        }
        List<ChangeEvent> succeeded = new ArrayList<>(events.size());
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i) != null && !failed.contains(i)) {
                succeeded.add(events.get(i));
            }
        }
        return succeeded;
    }

    /** @return the id in a {@code {_id: value}} query, or {@code null} for any other query */
    private static BsonValue idOf(BsonValue query) {
        if (query == null || !query.isDocument()) {
            return null;
        }
        BsonValue id = query.asDocument().get("_id");
        if (id == null || (id.isDocument() && !id.asDocument().isEmpty()
            && id.asDocument().getFirstKey().startsWith("$"))) {
            return null;
        }
        return id;
    }

    /** @return the ids of a {@code {_id: {$in: [...]}}} query, or {@code null} for any other query */
    private static BsonArray idsIn(BsonValue query) {
        if (query == null || !query.isDocument() || query.asDocument().size() != 1) {
            return null;
        }
        BsonValue id = query.asDocument().get("_id");
        if (id != null && id.isDocument() && id.asDocument().size() == 1 && id.asDocument().isArray("$in")) {
            return id.asDocument().getArray("$in");
        }
        return null;
    }

    private static boolean isReplacement(BsonDocument update) {
        return update.keySet().stream().noneMatch(key -> key.startsWith("$"));
    }

    private static List<BsonValue> array(BsonDocument command, String field) {
        BsonValue value = command.get(field);
        return value != null && value.isArray() ? value.asArray().getValues() : List.of();
    }

    private static Document decode(BsonDocument document) {
        return DOCUMENT_CODEC.decode(new BsonDocumentReader(document), DecoderContext.builder().build());
    }
}
