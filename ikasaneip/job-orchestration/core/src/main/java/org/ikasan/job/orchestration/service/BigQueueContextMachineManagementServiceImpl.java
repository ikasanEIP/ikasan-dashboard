package org.ikasan.job.orchestration.service;

import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.component.endpoint.bigqueue.service.AbstractBigQueueManagementService;
import org.ikasan.spec.bigqueue.service.exception.BigQueueNotFoundException;

/**
 * Limits the management of the queues to the Context Machine
 */
public class BigQueueContextMachineManagementServiceImpl extends AbstractBigQueueManagementService {

    private String inboundName;
    private IBigQueue inboundQueue;
    private String outboundName;
    private IBigQueue outboundQueue;

    public BigQueueContextMachineManagementServiceImpl(String inboundName, IBigQueue inboundQueue, String outboundName, IBigQueue outboundQueue) {
        this.inboundName = inboundName;
        this.inboundQueue = inboundQueue;
        this.outboundName = outboundName;
        this.outboundQueue = outboundQueue;
    }

    @Override
    public IBigQueue getBigQueue(String queueName) throws BigQueueNotFoundException {
        if (queueName == null) {
            throw new BigQueueNotFoundException("Cannot find big queue when queueName is null!");
        } else if (queueName.equals(this.inboundName)) {
            return inboundQueue;
        } else if (queueName.equals(this.outboundName)) {
            return outboundQueue;
        } else {
            throw new BigQueueNotFoundException(String.format("Could not find big queue[%s]!", queueName));
        }
    }
}
