package org.ikasan.mongo.persistence.systemevent.dao;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.mongo.persistence.systemevent.model.MongoSystemEventRecordImpl;
import org.ikasan.mongo.persistence.systemevent.repository.MongoSystemEventRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import tools.jackson.core.JacksonException;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Checks that a serialisation failure in {@link MongoSystemEventDaoImpl#save(org.ikasan.spec.systemevent.SystemEvent)}
 * is logged at ERROR with the original exception and rethrown with it as the cause.
 */
public class MongoSystemEventDaoImplErrorLoggingTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(MongoSystemEventDaoImpl.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level originalLevel;

    @Before
    public void setUp() {
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    @After
    public void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(originalLevel);
    }

    @Test
    public void save_whenSerialisationFails_logsErrorWithCauseAndRethrowsWithCause() {
        MongoSystemEventRepository repository = mock(MongoSystemEventRepository.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        MongoSystemEventDaoImpl dao = new MongoSystemEventDaoImpl(repository, mongoTemplate, 1,
            mock(AtlasSearchQueryHelper.class));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> dao.save(new UnserialisableSystemEvent()));

        assertTrue("cause should be the Jackson exception", thrown.getCause() instanceof JacksonException);
        assertTrue(thrown.getMessage().startsWith("Cannot convert system event to string!"));

        List<ILoggingEvent> errors = appender.list.stream()
            .filter(event -> event.getLevel() == Level.ERROR)
            .toList();
        assertEquals(1, errors.size());
        ILoggingEvent error = errors.get(0);
        assertEquals(thrown.getMessage(), error.getFormattedMessage());
        assertNotNull("ERROR event should carry the exception", error.getThrowableProxy());
        assertSame(thrown.getCause(), ((ThrowableProxy) error.getThrowableProxy()).getThrowable());

        verifyNoInteractions(repository);
    }

    /** A system event whose getter throws, so Jackson cannot serialise it. */
    public static class UnserialisableSystemEvent extends MongoSystemEventRecordImpl {
        public UnserialisableSystemEvent() {
            setModuleName("module");
            setSubject("subject");
        }

        @Override
        public String getActor() {
            throw new IllegalStateException("actor unavailable");
        }
    }
}
