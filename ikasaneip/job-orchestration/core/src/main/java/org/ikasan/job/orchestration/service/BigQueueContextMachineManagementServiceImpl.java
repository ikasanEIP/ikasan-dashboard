package org.ikasan.job.orchestration.service;

import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.component.endpoint.bigqueue.service.AbstractBigQueueManagementService;
import org.ikasan.spec.bigqueue.service.exception.BigQueueNotFoundException;

/**
 * Limits the management of the queues to the Context Machine
 */
public class BigQueueContextMachineManagementServiceImpl extends AbstractBigQueueManagementService {

    private String inboundQueueName;
    private IBigQueue inboundQueue;
    private String outboundQueueName;
    private IBigQueue outboundQueue;
    private String deadLetterQueueName;
    private IBigQueue deadLetterQueue;


    /**
     * Constructs an instance of BigQueueContextMachineManagementServiceImpl with the provided parameters.
     *
     * @param inboundQueueName The name of the inbound queue.
     * @param inboundQueue The IBigQueue representing the inbound queue.
     * @param outboundQueueName The name of the outbound queue.
     * @param outboundQueue The IBigQueue representing the outbound queue.
     * @param deadLetterQueueName The name of the dead letter queue.
     * @param deadLetterQueue The IBigQueue representing the dead letter queue.
     */
    public BigQueueContextMachineManagementServiceImpl(String inboundQueueName, IBigQueue inboundQueue
        , String outboundQueueName, IBigQueue outboundQueue, String deadLetterQueueName, IBigQueue deadLetterQueue) {
        this.inboundQueueName = inboundQueueName;
        this.inboundQueue = inboundQueue;
        this.outboundQueueName = outboundQueueName;
        this.outboundQueue = outboundQueue;
        this.deadLetterQueueName = deadLetterQueueName;
        this.deadLetterQueue = deadLetterQueue;
    }

    @Override
    public IBigQueue getBigQueue(String queueName) throws BigQueueNotFoundException {
        if (queueName == null) {
            throw new BigQueueNotFoundException("Cannot find big queue when queueName is null!");
        }
        else if (queueName.equals(this.inboundQueueName)) {
            return this.inboundQueue;
        }
        else if (queueName.equals(this.outboundQueueName)) {
            return this.outboundQueue;
        }
        else if (queueName.equals(this.deadLetterQueueName)) {
            return this.deadLetterQueue;
        }
        else {
            throw new BigQueueNotFoundException(String.format("Could not find big queue[%s]!", queueName));
        }
    }
}
