package org.ikasan.job.orchestration.service;

import com.leansoft.bigqueue.IBigQueue;
import org.ikasan.component.endpoint.bigqueue.service.AbstractBigQueueManagementService;

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
    public IBigQueue getBigQueue(String queueName) {

        if (queueName == null) {
            return null;
        } else if (queueName.equals(this.inboundName)) {
            return inboundQueue;
        } else if (queueName.equals(this.outboundName)) {
            return outboundQueue;
        } else {
            return null;
        }
    }
}
