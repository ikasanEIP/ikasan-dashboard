package org.ikasan.data.synchronisation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs one synchronisation mode on its own thread.
 *
 * <p>A reconcile is run on the same thread, between batches of changes, so a reconcile and the change processing
 * never write the same document concurrently (which could leave an older version in Solr).
 */
public abstract class Synchroniser {

    private static final Logger logger = LoggerFactory.getLogger(Synchroniser.class);

    protected final SyncPipeline pipeline;
    protected final Reconciler reconciler;
    protected final SyncStateStore stateStore;
    protected final DataSynchronisationProperties properties;

    private final AtomicBoolean reconcileRequested = new AtomicBoolean();
    private volatile Thread thread;
    private volatile boolean running;
    /** Set by {@link #stop()}, so failures caused by its interrupt (e.g. inside a Mongo call) aren't reported. */
    private volatile boolean stopRequested;

    protected Synchroniser(SyncPipeline pipeline, Reconciler reconciler, SyncStateStore stateStore,
                           DataSynchronisationProperties properties) {
        this.pipeline = pipeline;
        this.reconciler = reconciler;
        this.stateStore = stateStore;
        this.properties = properties;
    }

    public abstract SyncMode mode();

    /** The synchronisation loop; returns when stopped or interrupted. */
    protected abstract void run() throws InterruptedException;

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        stopRequested = false;
        thread = new Thread(() -> {
            try {
                run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                if (stopRequested) {
                    logger.debug("Data synchronisation ({}) interrupted while stopping", mode(), e);
                } else {
                    logger.error("Data synchronisation (" + mode() + ") stopped unexpectedly", e);
                }
            } finally {
                running = false;
                logger.info("Data synchronisation ({}) stopped", mode());
            }
        }, "ikasan-data-synchronisation-" + mode().name().toLowerCase());
        thread.setDaemon(true);
        thread.start();
        logger.info("Data synchronisation ({}) started", mode());
    }

    public synchronized void stop() {
        stopRequested = true;
        running = false;
        Thread current = thread;
        if (current != null) {
            current.interrupt();
            try {
                current.join(30_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
    }

    public boolean isRunning() {
        return running;
    }

    /** Ask for a reconcile; it runs on the synchronisation thread at the next opportunity. */
    public void requestReconcile() {
        reconcileRequested.set(true);
    }

    protected boolean shouldRun() {
        return running && !Thread.currentThread().isInterrupted();
    }

    protected boolean reconcilePending() {
        return reconcileRequested.get();
    }

    /** Run a requested reconcile, if any. */
    protected void reconcileIfRequested() throws InterruptedException {
        if (reconcileRequested.getAndSet(false)) {
            reconciler.reconcile();
        }
    }

    /** Run the first full load if Solr has never been loaded and that is configured. */
    protected void firstLoadIfNeeded() throws InterruptedException {
        if (properties.getReconcile().isOnFirstStart() && stateStore.lastFullLoad().isEmpty()) {
            logger.info("Solr has never been loaded from Mongo: loading everything now");
            reconciler.reconcile();
        }
    }
}
