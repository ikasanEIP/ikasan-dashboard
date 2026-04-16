package org.ikasan.job.orchestration.service;

import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.spec.bigqueue.service.exception.BigQueueNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class BigQueueContextMachineManagementServiceImplTest {

    @Mock
    private IBigQueue inboundQueue;

    @Mock
    private IBigQueue outboundQueue;

    @Mock
    private IBigQueue deadLetterQueue;

    private BigQueueContextMachineManagementServiceImpl service;

    private static final String INBOUND_QUEUE_NAME = "inboundQueue";
    private static final String OUTBOUND_QUEUE_NAME = "outboundQueue";
    private static final String DEAD_LETTER_QUEUE_NAME = "deadLetterQueue";

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        service = new BigQueueContextMachineManagementServiceImpl(
            INBOUND_QUEUE_NAME, inboundQueue,
            OUTBOUND_QUEUE_NAME, outboundQueue,
            DEAD_LETTER_QUEUE_NAME, deadLetterQueue
        );
    }

    @Test
    public void test_getBigQueue_inbound() throws BigQueueNotFoundException {
        IBigQueue result = service.getBigQueue(INBOUND_QUEUE_NAME);

        Assert.assertNotNull(result);
        Assert.assertEquals(inboundQueue, result);
    }

    @Test
    public void test_getBigQueue_outbound() throws BigQueueNotFoundException {
        IBigQueue result = service.getBigQueue(OUTBOUND_QUEUE_NAME);

        Assert.assertNotNull(result);
        Assert.assertEquals(outboundQueue, result);
    }

    @Test
    public void test_getBigQueue_deadLetter() throws BigQueueNotFoundException {
        IBigQueue result = service.getBigQueue(DEAD_LETTER_QUEUE_NAME);

        Assert.assertNotNull(result);
        Assert.assertEquals(deadLetterQueue, result);
    }

    @Test(expected = BigQueueNotFoundException.class)
    public void test_getBigQueue_null_queueName() throws BigQueueNotFoundException {
        service.getBigQueue(null);
    }

    @Test
    public void test_getBigQueue_null_queueName_exception_message() {
        try {
            service.getBigQueue(null);
            Assert.fail("Expected BigQueueNotFoundException");
        } catch (BigQueueNotFoundException e) {
            Assert.assertEquals("Cannot find big queue when queueName is null!", e.getMessage());
        }
    }

    @Test(expected = BigQueueNotFoundException.class)
    public void test_getBigQueue_unknown_queueName() throws BigQueueNotFoundException {
        service.getBigQueue("unknownQueue");
    }

    @Test
    public void test_getBigQueue_unknown_queueName_exception_message() {
        try {
            service.getBigQueue("unknownQueue");
            Assert.fail("Expected BigQueueNotFoundException");
        } catch (BigQueueNotFoundException e) {
            Assert.assertEquals("Could not find big queue[unknownQueue]!", e.getMessage());
        }
    }

    @Test(expected = BigQueueNotFoundException.class)
    public void test_getBigQueue_empty_queueName() throws BigQueueNotFoundException {
        service.getBigQueue("");
    }

    @Test
    public void test_getBigQueue_empty_queueName_exception_message() {
        try {
            service.getBigQueue("");
            Assert.fail("Expected BigQueueNotFoundException");
        } catch (BigQueueNotFoundException e) {
            Assert.assertEquals("Could not find big queue[]!", e.getMessage());
        }
    }

    @Test
    public void test_getBigQueue_all_queues_can_be_retrieved() throws BigQueueNotFoundException {
        // Verify all three queues can be retrieved
        IBigQueue inbound = service.getBigQueue(INBOUND_QUEUE_NAME);
        IBigQueue outbound = service.getBigQueue(OUTBOUND_QUEUE_NAME);
        IBigQueue deadLetter = service.getBigQueue(DEAD_LETTER_QUEUE_NAME);

        Assert.assertEquals(inboundQueue, inbound);
        Assert.assertEquals(outboundQueue, outbound);
        Assert.assertEquals(deadLetterQueue, deadLetter);
    }

    @Test
    public void test_getBigQueue_case_sensitive() {
        try {
            service.getBigQueue("INBOUNDQUEUE");
            Assert.fail("Expected BigQueueNotFoundException - queue names are case sensitive");
        } catch (BigQueueNotFoundException e) {
            Assert.assertTrue(e.getMessage().contains("INBOUNDQUEUE"));
        }
    }

    @Test
    public void test_getBigQueue_with_spaces_in_name() {
        try {
            service.getBigQueue("inbound queue");
            Assert.fail("Expected BigQueueNotFoundException");
        } catch (BigQueueNotFoundException e) {
            Assert.assertTrue(e.getMessage().contains("inbound queue"));
        }
    }

    @Test
    public void test_getBigQueue_multiple_calls_same_queue() throws BigQueueNotFoundException {
        // Multiple calls should return the same queue instance
        IBigQueue result1 = service.getBigQueue(INBOUND_QUEUE_NAME);
        IBigQueue result2 = service.getBigQueue(INBOUND_QUEUE_NAME);
        IBigQueue result3 = service.getBigQueue(INBOUND_QUEUE_NAME);

        Assert.assertEquals(result1, result2);
        Assert.assertEquals(result2, result3);
        Assert.assertEquals(inboundQueue, result1);
    }

    @Test
    public void test_constructor_with_null_queues() throws BigQueueNotFoundException {
        // Service can be constructed with null queue instances
        BigQueueContextMachineManagementServiceImpl serviceWithNulls =
            new BigQueueContextMachineManagementServiceImpl(
                "queue1", null,
                "queue2", null,
                "queue3", null
            );

        IBigQueue result = serviceWithNulls.getBigQueue("queue1");
        Assert.assertNull(result);
    }

    @Test
    public void test_constructor_with_null_queue_names() {
        // Service can be constructed with null queue names
        BigQueueContextMachineManagementServiceImpl serviceWithNullNames =
            new BigQueueContextMachineManagementServiceImpl(
                null, inboundQueue,
                null, outboundQueue,
                null, deadLetterQueue
            );

        // Should not throw exception during construction
        Assert.assertNotNull(serviceWithNullNames);
    }

    @Test
    public void test_getBigQueue_similar_queue_names() throws BigQueueNotFoundException {
        BigQueueContextMachineManagementServiceImpl serviceWithSimilarNames =
            new BigQueueContextMachineManagementServiceImpl(
                "queue", inboundQueue,
                "queue1", outboundQueue,
                "queue2", deadLetterQueue
            );

        // Exact match required
        IBigQueue result = serviceWithSimilarNames.getBigQueue("queue");
        Assert.assertEquals(inboundQueue, result);

        // Should not match partial names
        try {
            serviceWithSimilarNames.getBigQueue("que");
            Assert.fail("Expected BigQueueNotFoundException");
        } catch (BigQueueNotFoundException e) {
            Assert.assertTrue(e.getMessage().contains("que"));
        }
    }

    @Test
    public void test_getBigQueue_with_special_characters_in_queue_name() throws BigQueueNotFoundException {
        BigQueueContextMachineManagementServiceImpl serviceWithSpecialChars =
            new BigQueueContextMachineManagementServiceImpl(
                "queue-with-dashes", inboundQueue,
                "queue_with_underscores", outboundQueue,
                "queue.with.dots", deadLetterQueue
            );

        Assert.assertEquals(inboundQueue, serviceWithSpecialChars.getBigQueue("queue-with-dashes"));
        Assert.assertEquals(outboundQueue, serviceWithSpecialChars.getBigQueue("queue_with_underscores"));
        Assert.assertEquals(deadLetterQueue, serviceWithSpecialChars.getBigQueue("queue.with.dots"));
    }

    @Test
    public void test_getBigQueue_does_not_modify_queue() throws BigQueueNotFoundException {
        // Getting a queue should not invoke any methods on it
        IBigQueue result = service.getBigQueue(INBOUND_QUEUE_NAME);

        Assert.assertEquals(inboundQueue, result);
        verifyNoInteractions(inboundQueue);
    }

    @Test
    public void test_multiple_services_independent() throws BigQueueNotFoundException {
        IBigQueue anotherInboundQueue = mock(IBigQueue.class);
        IBigQueue anotherOutboundQueue = mock(IBigQueue.class);
        IBigQueue anotherDeadLetterQueue = mock(IBigQueue.class);

        BigQueueContextMachineManagementServiceImpl anotherService =
            new BigQueueContextMachineManagementServiceImpl(
                INBOUND_QUEUE_NAME, anotherInboundQueue,
                OUTBOUND_QUEUE_NAME, anotherOutboundQueue,
                DEAD_LETTER_QUEUE_NAME, anotherDeadLetterQueue
            );

        // Each service should return its own queue instances
        Assert.assertEquals(inboundQueue, service.getBigQueue(INBOUND_QUEUE_NAME));
        Assert.assertEquals(anotherInboundQueue, anotherService.getBigQueue(INBOUND_QUEUE_NAME));
        Assert.assertNotEquals(service.getBigQueue(INBOUND_QUEUE_NAME), anotherService.getBigQueue(INBOUND_QUEUE_NAME));
    }
}
