package org.ikasan.notification.monitor;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.MonitorType;
import org.ikasan.notification.factory.NotificationThreadFactory;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.notification.model.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class StateChangeMonitorImpl extends AbstractMonitorBase<GenericNotificationDetails> implements Monitor<GenericNotificationDetails> {

    private static final Logger LOG = LoggerFactory.getLogger(StateChangeMonitorImpl.class);

    private final Map<String, Future<?>> mapOfRunningJobs = new HashMap<>();

    private Map<String, ExecutorService> mapOfRunningExecutors = new HashMap<>();

    private final boolean notificationEnabled;

    /**
     * Constructor
     *
     * @param executorService
     */
    public StateChangeMonitorImpl(ExecutorService executorService, boolean notificationEnabled) {
        super(executorService);
        LOG.info("StateChangeMonitorImpl is being created!");
        this.notificationEnabled = notificationEnabled;
    }

    @Override
    public void register(ContextInstance contextInstance) {
        if(this.notificationEnabled) {
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstance.getId());

            ExecutorService executorService = Executors.newSingleThreadExecutor(new NotificationThreadFactory("StateChangeNotification"));

            // Create the scheduler to be executed
            Future<?> notificationScheduler = executorService.submit(new ErrorNotificationsRunner(contextMachine));

            mapOfRunningJobs.put(contextInstance.getId(), notificationScheduler);
            mapOfRunningExecutors.put(contextInstance.getId(), executorService);

            LOG.info("StateChangeMonitor has started monitoring for context {}, instance {}", contextInstance.getName(), contextInstance.getId());
            LOG.info(mapOfRunningJobs.size() + " number of Contexts are being monitored now!");
        }
        else {
            LOG.info("Notifications are not enabled!");
        }
    }

    @Override
    public void unregister(ContextInstance contextInstance) {
        if(this.notificationEnabled) {
            if (mapOfRunningJobs.containsKey(contextInstance.getId())) {
                mapOfRunningJobs.get(contextInstance.getId()).cancel(true); // Stop the related SingleThreadScheduledExecutor
                LOG.info("Context {}, Context Instance Id {} notification has been unregistered", contextInstance.getName(), contextInstance.getId());
                mapOfRunningJobs.remove(contextInstance.getId());
            }
            if (mapOfRunningExecutors.containsKey(contextInstance.getId())) {
                mapOfRunningExecutors.get(contextInstance.getId()).shutdown(); // Stop the related ScheduledExecutorService
                LOG.debug("Context {}, Context Instance Id {} notification executor has been unregistered", contextInstance.getName(), contextInstance.getId());
                mapOfRunningExecutors.remove(contextInstance.getId());
            }
            LOG.info("After unregistering Context {}, InstanceId {} - number of Contexts are being monitored now is {}",
                contextInstance.getName(), contextInstance.getId(), mapOfRunningJobs.size());
        } else {
            LOG.info("Notifications are not enabled!");
        }
    }

    @Override
    public void invoke(final GenericNotificationDetails status)
    {
        super.invoke(status);
    }

    protected class ErrorNotificationsRunner implements Runnable, SchedulerJobInstanceStateChangeEventListener {

        private final ContextMachine contextMachine;

        public ErrorNotificationsRunner(ContextMachine contextMachine) {
            this.contextMachine = contextMachine;
        }

        @Override
        public void run() {
            try {
                contextMachine.addSchedulerJobStateChangeEventListener(this);
                LOG.info("ErrorNotificationsRunner is running for Context {} and InstanceId {}",
                    contextMachine.getContext().getName(), contextMachine.getContext().getId());

                while(true) {
                    TimeUnit.SECONDS.sleep(3);
                }

            } catch (Exception e) {
                // do something
                LOG.info("ErrorNotificationsRunner has been Interrupted by an exception, most likely Context Instance has been removed. Context {}, InstanceId: {} - Exception {}",
                    contextMachine.getContext().getName(), contextMachine.getContext().getId(), e.getMessage()+ " - " + e);
            }
        }

        @Override
        public void onSchedulerJobInstanceStateChangeEvent(SchedulerJobInstanceStateChangeEvent event) {

            if (event.getNewStatus().name().equalsIgnoreCase(InstanceStatus.ERROR.name())) {
                GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(event.getSchedulerJobInstance().getAgentName(),
                    event.getContextInstance().getName(), event.getSchedulerJobInstance().getChildContextName(),
                    event.getSchedulerJobInstance().getJobName(), event.getSchedulerJobInstance().getContextInstanceId(), MonitorType.ERROR, event.getNewStatus());
                genericNotificationDetails.setFiredTime(event.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime());
                genericNotificationDetails.setCompletedTime(event.getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime());
                invoke(genericNotificationDetails);
            }
            else if (!event.getPreviousStatus().name().equalsIgnoreCase(InstanceStatus.RUNNING.name()) &&
                       event.getNewStatus().name().equalsIgnoreCase(InstanceStatus.RUNNING.name())) {
                GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(event.getSchedulerJobInstance().getAgentName(),
                    event.getContextInstance().getName(),event.getSchedulerJobInstance().getChildContextName(),
                    event.getSchedulerJobInstance().getJobName(), event.getSchedulerJobInstance().getContextInstanceId(), MonitorType.START, event.getNewStatus());
                genericNotificationDetails.setFiredTime(event.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime());

                invoke(genericNotificationDetails);
            }
            else if (event.getNewStatus().name().equalsIgnoreCase(InstanceStatus.COMPLETE.name())) {
                GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(event.getSchedulerJobInstance().getAgentName(), event.getContextInstance().getName(),
                    event.getSchedulerJobInstance().getChildContextName(),
                    event.getSchedulerJobInstance().getJobName(), event.getSchedulerJobInstance().getContextInstanceId(), MonitorType.COMPLETE, event.getNewStatus());
                genericNotificationDetails.setFiredTime(event.getSchedulerJobInstance().getScheduledProcessEvent().getFireTime());
                genericNotificationDetails.setCompletedTime(event.getSchedulerJobInstance().getScheduledProcessEvent().getCompletionTime());
                invoke(genericNotificationDetails);
            }
        }
    }
}
