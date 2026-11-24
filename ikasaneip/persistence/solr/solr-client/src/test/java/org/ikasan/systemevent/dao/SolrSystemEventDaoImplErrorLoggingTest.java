package org.ikasan.systemevent.dao;

import org.ikasan.systemevent.model.SolrSystemEventRecordImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import tools.jackson.core.JacksonException;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Checks that a serialisation failure in {@link SolrSystemEventDaoImpl} is logged at ERROR with the original
 * exception and rethrown with it as the cause.
 *
 * solr-client has no SLF4J binding on its test classpath (only slf4j-api), so a logback ListAppender cannot be used
 * here; instead the DAO's static logger is temporarily replaced with a Mockito mock.
 */
public class SolrSystemEventDaoImplErrorLoggingTest {

    private Field loggerField;
    private Object originalLogger;
    private Logger mockLogger;

    @Before
    public void setUp() throws Exception {
        loggerField = SolrSystemEventDaoImpl.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        originalLogger = loggerField.get(null);
        mockLogger = mock(Logger.class);
        loggerField.set(null, mockLogger);
    }

    @After
    public void tearDown() throws Exception {
        loggerField.set(null, originalLogger);
    }

    @Test
    public void convert_whenSerialisationFails_logsErrorWithCauseAndRethrowsWithCause() {
        SolrSystemEventDaoImpl dao = new SolrSystemEventDaoImpl();

        RuntimeException thrown = assertThrows(RuntimeException.class,
            () -> dao.convertEntityToSolrInputDocument(1L, new UnserialisableSystemEvent()));

        assertTrue("cause should be the Jackson exception", thrown.getCause() instanceof JacksonException);
        assertTrue(thrown.getMessage().startsWith("Cannot convert system event to string!"));

        ArgumentCaptor<Throwable> logged = ArgumentCaptor.forClass(Throwable.class);
        verify(mockLogger).error(org.mockito.ArgumentMatchers.eq(thrown.getMessage()), logged.capture());
        assertSame(thrown.getCause(), logged.getValue());
        assertEquals(1, org.mockito.Mockito.mockingDetails(mockLogger).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("error")).count());
    }

    /** A system event whose getter throws, so Jackson cannot serialise it. */
    public static class UnserialisableSystemEvent extends SolrSystemEventRecordImpl {
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
