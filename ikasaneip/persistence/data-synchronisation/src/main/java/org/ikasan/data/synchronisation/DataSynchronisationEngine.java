package org.ikasan.data.synchronisation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Starts and stops the synchronisation with the application.
 *
 * <p>On start it works out the mode from the Mongo topology (see {@link MongoTopology}), then, if the lease is
 * enabled, synchronises only while this instance holds the lease. It also restarts a synchroniser that stopped
 * unexpectedly, and triggers the scheduled reconcile.
 */
public class DataSynchronisationEngine implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(DataSynchronisationEngine.class);

    /** The default reconcile schedule in command listener mode: nightly at 02:00. */
    static final String COMMAND_LISTENER_DEFAULT_CRON = "0 0 2 * * *";

    private final DataSynchronisationProperties properties;
    private final MongoTemplate mongoTemplate;
    private final SyncPipeline pipeline;
    private final Reconciler reconciler;
    private final SyncStateStore stateStore;
    private final WriteCaptureListener writeCaptureListener;
    private final SyncLease lease;

    private volatile Synchroniser synchroniser;
    private volatile ScheduledExecutorService scheduler;
    private volatile boolean running;

    public DataSynchronisationEngine(DataSynchronisationProperties properties, MongoTemplate mongoTemplate,
                                     SyncPipeline pipeline, Reconciler reconciler, SyncStateStore stateStore,
                                     WriteCaptureListener writeCaptureListener, SyncLease lease) {
        this.properties = properties;
        this.mongoTemplate = mongoTemplate;
        this.pipeline = pipeline;
        this.reconciler = reconciler;
        this.stateStore = stateStore;
        this.writeCaptureListener = writeCaptureListener;
        this.lease = lease;
    }

    @Override
    public synchronized void start() {
        MongoTopology.Topology topology = MongoTopology.detect(mongoTemplate);
        SyncMode mode = MongoTopology.resolve(properties.getMode(), topology);
        String cron = cron(mode);
        logger.info("Mongo to Solr data synchronisation: Mongo is a {}, so mode {} resolved to {}; lease {}; reconcile"
                + " schedule {}", topology.description(), properties.getMode(), mode,
            lease == null ? "disabled" : "owner " + lease.owner(), cron == null ? "none" : "'" + cron + "'");

        synchroniser = mode == SyncMode.CHANGE_STREAM
            ? new ChangeStreamSynchroniser(mongoTemplate, pipeline, reconciler, stateStore, properties)
            : new CommandListenerSynchroniser(writeCaptureListener, pipeline, reconciler, stateStore, properties);

        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ikasan-data-synchronisation-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        long tickMillis = lease == null ? 10_000 : Math.max(1000, lease.durationMillis() / 3);
        scheduler.scheduleWithFixedDelay(this::tick, 0, tickMillis, TimeUnit.MILLISECONDS);
        if (cron != null) {
            scheduleReconcile(CronExpression.parse(cron));
        }
        running = true;
    }

    /** Hold the lease and keep the synchroniser running while it is held. */
    void tick() {
        try {
            boolean held = lease == null || lease.acquireOrRenew();
            Synchroniser current = synchroniser;
            if (held && !current.isRunning()) {
                if (lease != null) {
                    logger.info("This instance ({}) holds the data synchronisation lease", lease.owner());
                }
                current.start();
            } else if (!held && current.isRunning()) {
                logger.info("Another instance holds the data synchronisation lease: stopping synchronisation here");
                current.stop();
            }
        } catch (RuntimeException e) {
            logger.error("Data synchronisation housekeeping failed; will try again", e);
        }
    }

    private void scheduleReconcile(CronExpression cron) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = cron.next(now);
        if (next == null || scheduler.isShutdown()) {
            return;
        }
        scheduler.schedule(() -> {
            Synchroniser current = synchroniser;
            if (current != null && current.isRunning()) {
                logger.info("Scheduled reconcile of Solr with Mongo");
                current.requestReconcile();
            }
            scheduleReconcile(cron);
        }, Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Ask for a reconcile now; it runs on the synchronisation thread, on the instance holding the lease.
     *
     * @return false if this instance is not synchronising
     */
    public boolean reconcile() {
        Synchroniser current = synchroniser;
        if (current == null || !current.isRunning()) {
            return false;
        }
        current.requestReconcile();
        return true;
    }

    /** @return the cron expression to use, or {@code null} for no scheduled reconcile */
    String cron(SyncMode mode) {
        String configured = properties.getReconcile().getCron();
        if (configured == null || configured.isBlank()) {
            return mode == SyncMode.COMMAND_LISTENER ? COMMAND_LISTENER_DEFAULT_CRON : null;
        }
        return "-".equals(configured.trim()) ? null : configured.trim();
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (synchroniser != null) {
            synchroniser.stop();
        }
        if (lease != null) {
            try {
                lease.release();
            } catch (RuntimeException e) {
                logger.error("Could not release the data synchronisation lease; it will expire", e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /** @return the running synchroniser, for monitoring and tests */
    public Synchroniser synchroniser() {
        return synchroniser;
    }
}
