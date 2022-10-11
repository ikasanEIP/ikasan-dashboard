package org.ikasan.rest.dashboard.service.bigqueue;

import com.leansoft.bigqueue.IBigQueue;
import org.ikasan.component.endpoint.bigqueue.service.AbstractBigQueueManagementService;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;

import java.util.Set;

public class BigQueueDashboardServiceImpl extends AbstractBigQueueManagementService {

    // Dashboard Inbound Queue
    private IBigQueue inboundQueue;
    private static final String INBOUND_QUEUE = "dashboard-inbound-queue";

    public BigQueueDashboardServiceImpl() {}

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
    public IBigQueue getBigQueue(String queueName) {

        // Contains the dashboard queue set up in this DashboardComponentFactory class
        // If you are the inbound queue for the dashboard, return it.
        if (inboundQueue != null && INBOUND_QUEUE.equals(queueName)) {
            return inboundQueue;
        }

        // Check the rest of the context, if we find the queues then return it.
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
        return null;
    }
}
