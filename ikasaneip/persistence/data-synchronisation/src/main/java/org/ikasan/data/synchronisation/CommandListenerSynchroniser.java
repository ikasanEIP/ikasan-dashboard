package org.ikasan.data.synchronisation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Synchronises the writes captured by the {@link WriteCaptureListener}.
 *
 * <p>The listener puts changes on a bounded queue without ever blocking Mongo; this synchroniser's thread takes
 * them off in batches and applies them to Solr. Its limits (deletes by query, writes by other applications, changes
 * still queued when the application stops, changes dropped when the queue is full) are repaired by the reconciler,
 * which runs on a schedule in this mode.
 */
public class CommandListenerSynchroniser extends Synchroniser {

    private static final Logger logger = LoggerFactory.getLogger(CommandListenerSynchroniser.class);

    private final WriteCaptureListener listener;
    private final BlockingQueue<ChangeEvent> queue;
    private final AtomicLong dropped = new AtomicLong();

    public CommandListenerSynchroniser(WriteCaptureListener listener, SyncPipeline pipeline, Reconciler reconciler,
                                       SyncStateStore stateStore, DataSynchronisationProperties properties) {
        super(pipeline, reconciler, stateStore, properties);
        this.listener = listener;
        this.queue = new ArrayBlockingQueue<>(properties.getQueueCapacity());
    }

    @Override
    public SyncMode mode() {
        return SyncMode.COMMAND_LISTENER;
    }

    @Override
    public synchronized void start() {
        logger.warn("Data synchronisation is using the command listener. It only sees this application's writes, does"
            + " not synchronise deletes by query and loses changes still queued when the application stops; the"
            + " scheduled reconcile repairs these. Run Mongo as a replica set to use change streams instead.");
        listener.activate(this::enqueue);
        super.start();
    }

    @Override
    public synchronized void stop() {
        listener.deactivate();
        super.stop();
    }

    /** Called on the Mongo driver's thread: must never block. */
    void enqueue(List<ChangeEvent> events) {
        for (ChangeEvent event : events) {
            if (!queue.offer(event)) {
                long total = dropped.incrementAndGet();
                if (total == 1 || total % 1000 == 0) {
                    logger.warn("The data synchronisation queue is full ({} entries): {} change(s) dropped so far;"
                        + " the next reconcile will repair Solr", properties.getQueueCapacity(), total);
                }
                requestReconcile();
            }
        }
    }

    @Override
    protected void run() throws InterruptedException {
        firstLoadIfNeeded();
        while (shouldRun()) {
            List<ChangeEvent> batch = new ArrayList<>(properties.getBatchSize());
            ChangeEvent first = queue.poll(properties.getBatchMaxWaitMs(), TimeUnit.MILLISECONDS);
            if (first != null) {
                batch.add(first);
                queue.drainTo(batch, properties.getBatchSize() - 1);
            }
            pipeline.apply(batch);
            if (reconcilePending()) {
                // apply everything queued before the reconcile first, so an older change can't overwrite what the
                // reconcile writes
                List<ChangeEvent> queued = new ArrayList<>();
                queue.drainTo(queued);
                pipeline.apply(queued);
                reconcileIfRequested();
            }
        }
    }

    public long droppedCount() {
        return dropped.get();
    }

    public int queued() {
        return queue.size();
    }
}
