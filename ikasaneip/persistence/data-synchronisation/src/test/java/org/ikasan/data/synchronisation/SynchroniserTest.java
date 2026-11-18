package org.ikasan.data.synchronisation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mongodb.MongoInterruptedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The synchronisation thread's own lifecycle, without Mongo or Solr.
 */
public class SynchroniserTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(Synchroniser.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @Before
    public void captureLogs() {
        appender.start();
        logger.addAppender(appender);
    }

    @After
    public void releaseLogs() {
        logger.detachAppender(appender);
    }

    @Test
    public void an_interrupt_from_stop_inside_a_mongo_call_is_not_reported_as_an_error() throws Exception {
        CountDownLatch inMongoCall = new CountDownLatch(1);
        TestSynchroniser synchroniser = new TestSynchroniser(() -> {
            inMongoCall.countDown();
            try {
                Thread.sleep(30_000);
            } catch (InterruptedException e) {
                // what the Mongo driver does when its thread is interrupted while waiting
                throw new MongoInterruptedException("Interrupted waiting for lock", e);
            }
            return null;
        });

        synchroniser.start();
        assertTrue(inMongoCall.await(10, TimeUnit.SECONDS));
        synchroniser.stop();

        assertFalse(synchroniser.isRunning());
        assertEquals(0, errors());
    }

    @Test
    public void a_failure_that_was_not_caused_by_stop_is_reported() throws Exception {
        TestSynchroniser synchroniser = new TestSynchroniser(() -> {
            throw new IllegalStateException("boom");
        });

        synchroniser.start();
        SyncTestSupport.await("the synchroniser to stop", () -> !synchroniser.isRunning());

        assertEquals(1, errors());
    }

    @Test
    public void a_restarted_synchroniser_reports_failures_again() throws Exception {
        TestSynchroniser synchroniser = new TestSynchroniser(() -> null);
        synchroniser.start();
        synchroniser.stop();

        synchroniser.body = () -> {
            throw new IllegalStateException("boom");
        };
        synchroniser.start();
        SyncTestSupport.await("the synchroniser to stop", () -> !synchroniser.isRunning());

        assertEquals(1, errors());
    }

    private long errors() {
        return appender.list.stream().filter(event -> event.getLevel() == Level.ERROR).count();
    }

    private static final class TestSynchroniser extends Synchroniser {

        volatile Supplier<Void> body;

        TestSynchroniser(Supplier<Void> body) {
            super(null, null, null, new DataSynchronisationProperties());
            this.body = body;
        }

        @Override
        public SyncMode mode() {
            return SyncMode.COMMAND_LISTENER;
        }

        @Override
        protected void run() {
            body.get();
        }
    }
}
