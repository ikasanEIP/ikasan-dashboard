package org.ikasan.rest.dashboard.service.bigqueue;

import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.component.endpoint.bigqueue.service.AbstractBigQueueManagementService;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.spec.bigqueue.service.exception.BigQueueNotFoundException;

import java.util.Set;

public class BigQueueDashboardServiceImpl extends AbstractBigQueueManagementService {

    // Dashboard Inbound Queue
    private IBigQueue inboundQueue;
    public static final String INBOUND_QUEUE = "dashboard-inbound-queue";

    /**
     * Constructor for BigQueueDashboardServiceImpl class.
     *
     * @param inboundQueue the IBigQueue instance to be used for the dashboard
     */
    public BigQueueDashboardServiceImpl(IBigQueue inboundQueue) {
        this.inboundQueue = inboundQueue;
    }

    /**
     * Gets the BigQueue that are associated to the Dashboard by their name.
     * The dashboard will have 1 default queue that is created via the DashboardComponentFactory
     * The others are found using the ContextMachineCache
     * @param queueName Name of the queue
     * @return the related IBigQueue instance of that queueName
     */
    @Override
    public IBigQueue getBigQueue(String queueName) throws BigQueueNotFoundException {

        // Avoid null pointer exception if somehow we passed a Null string.
        if (queueName == null) {
            throw new BigQueueNotFoundException("Cannot get big queue when provided queueName is null!");
        }

        // Contains the dashboard queue set up in this DashboardComponentFactory class
        // If you are the inbound queue for the dashboard, return it.
        if (inboundQueue != null && INBOUND_QUEUE.equals(queueName)) {
            return inboundQueue;
        }

        // Check the rest of the context instances, if we find the queues then return it.
        Set<String> setOfContextInstance = ContextMachineCache.instance().contextInstanceIdentifiers();
        for(String contextInstance : setOfContextInstance) {
            if (queueName.equals(ContextMachineCache.instance().getByContextInstanceId(contextInstance).getInboundQueueName())) {
                // Inbound
                return ContextMachineCache.instance().getByContextInstanceId(contextInstance).getInboundQueue();
            } else if (queueName.equals(ContextMachineCache.instance().getByContextInstanceId(contextInstance).getOutboundQueueName())) {
                // Outbound
                return ContextMachineCache.instance().getByContextInstanceId(contextInstance).getOutboundQueue();
            }
        }
        // Nothing found, return null
        throw new BigQueueNotFoundException(String.format("Could not get big queue[%s]!", queueName));
    }
}
