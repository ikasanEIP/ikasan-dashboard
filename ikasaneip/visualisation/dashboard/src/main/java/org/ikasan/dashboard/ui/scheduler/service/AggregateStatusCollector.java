package org.ikasan.dashboard.ui.scheduler.service;

import org.ikasan.job.orchestration.broadcast.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.broadcast.ContextInstanceSavedEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceAggregateJobStatus;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AggregateStatusCollector implements SchedulerJobStateChangeEventBroadcastListener,
    ContextInstanceStateChangeEventBroadcastListener, ContextInstanceSavedEventBroadcastListener {
    private static AggregateStatusCollector instance;

    private SchedulerJobInstanceService schedulerJobInstanceService;
    private List<ContextInstanceAggregateJobStatus> jobStatuses = null;

    /**
     * Constructor for the AggregateStatusCollector class
     *
     * @param schedulerJobInstanceService the service used to retrieve job status count for context instances
     */
    private AggregateStatusCollector(SchedulerJobInstanceService schedulerJobInstanceService) {
        this.schedulerJobInstanceService = schedulerJobInstanceService;
    }

    /**
     * Initializes the {@link AggregateStatusCollector} singleton instance if it has not been initialized before,
     * and returns the instance.
     *
     * @param schedulerJobInstanceService the service used to retrieve job status count for context instances
     * @return the initialized and registered {@link AggregateStatusCollector} instance
     */
    public synchronized static AggregateStatusCollector init(SchedulerJobInstanceService schedulerJobInstanceService) {
        if (instance != null)
        {
            return instance;
        }

        instance = new AggregateStatusCollector(schedulerJobInstanceService);
        SchedulerJobStateChangeEventBroadcaster.register(instance);
        ContextInstanceStateChangeEventBroadcaster.register(instance);
        ContextInstanceSavedEventBroadcaster.register(instance);
        return instance;
    }

    /**
     * Returns the singleton instance of the AggregateStatusCollector class.
     *
     * @return the singleton instance of the AggregateStatusCollector class.
     */
    public static AggregateStatusCollector instance() {
        return instance;
    }

    /**
     * Retrieves the list of ContextInstanceAggregateJobStatuses. If the list has not been populated yet, it populates
     * the list by calling the populateJobStatuses method.
     *
     * @return the list of ContextInstanceAggregateJobStatuses
     */
    public List<ContextInstanceAggregateJobStatus> getContextInstanceAggregateJobStatuses() {
        if(this.jobStatuses == null) {
            this.populateJobStatuses();
        }

        return this.jobStatuses;
    }

    /**
     * Populates the job statuses by retrieving the count of job statuses for context instances
     * from the {@link SchedulerJobInstanceService}.
     */
    private void populateJobStatuses() {
        List<String> contextInstanceIdentifiers = new ArrayList<>(ContextMachineCache.instance().contextInstanceIdentifiers());

        contextInstanceIdentifiers = contextInstanceIdentifiers.stream()
            .filter(id -> ContextMachineCache.instance().getByContextInstanceId(id) != null
                && !ContextMachineCache.instance().getByContextInstanceId(id).getContext().getStatus().equals(InstanceStatus.PREPARED))
            .collect(Collectors.toList());

        this.jobStatuses = this.schedulerJobInstanceService
            .getJobStatusCountForContextInstances(contextInstanceIdentifiers);
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent schedulerJobInstanceStateChangeEvent) {
        populateJobStatuses();
    }

    @Override
    public void receiveBroadcast(ContextInstance contextInstance) {
        if(contextInstance.getStatus().equals(InstanceStatus.PREPARED) ||
            contextInstance.getStatus().equals(InstanceStatus.WAITING) ||
            contextInstance.getStatus().equals(InstanceStatus.ENDED)) {
            this.populateJobStatuses();
        }
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        if(event.getContextInstance().getStatus().equals(InstanceStatus.PREPARED) ||
            event.getContextInstance().getStatus().equals(InstanceStatus.WAITING) ||
            event.getContextInstance().getStatus().equals(InstanceStatus.ENDED)) {
            this.populateJobStatuses();
        }
    }
}
