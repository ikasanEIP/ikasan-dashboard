package org.ikasan.notification.monitor;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.MonitorType;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.notification.model.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class StateChangeMonitorImpl extends AbstractMonitorBase<GenericNotificationDetails> implements Monitor<GenericNotificationDetails> {

    private static final Logger LOG = LoggerFactory.getLogger(StateChangeMonitorImpl.class);

    private List<Future<?>> errorNotificationsExecutors = new ArrayList<>();

    private boolean notificationEnabled;

    /**
     * Constructor
     * @param executorService
     */
    public StateChangeMonitorImpl(ExecutorService executorService, boolean notificationEnabled) {
        super(executorService);
        LOG.info("StateChangeMonitorImpl is being created!");
        this.notificationEnabled = notificationEnabled;

        errorNotificationsExecutors.clear();
    }

    @Override
    public void register(ContextInstance contextInstance) {
        if(this.notificationEnabled) {
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(contextInstance.getName());
            errorNotificationsExecutors.add(Executors.newSingleThreadExecutor().submit(new ErrorNotificationsRunner(contextMachine)));
            LOG.info("StateChangeMonitor has started monitoring on " + contextInstance.getName());
            LOG.info(errorNotificationsExecutors.size() + " number of Contexts are being monitored now!");
        }
        else {
            LOG.info("Notifications are not enabled!");
        }
    }

    @Override
    public void invoke(final GenericNotificationDetails status)
    {
        super.invoke(status);
    }

    protected class ErrorNotificationsRunner implements Runnable, SchedulerJobInstanceStateChangeEventListener {

        private ContextMachine contextMachine;

        public ErrorNotificationsRunner(ContextMachine contextMachine) {
            this.contextMachine = contextMachine;
        }

        @Override
        public void run() {
            try {
                contextMachine.addSchedulerJobStateChangeEventListener(this);

                while(true) {
                    TimeUnit.SECONDS.sleep(3);
                }

            } catch (Exception e) {
                // do something
                e.printStackTrace();
            } finally {

            }
        }

        @Override
        public void onSchedulerJobInstanceStateChangeEvent(SchedulerJobInstanceStateChangeEvent event) {

            if (event.getNewStatus().name().equalsIgnoreCase(InstanceStatus.ERROR.name())) {
                GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(event.getSchedulerJobInstance().getChildContextName(),
                    event.getSchedulerJobInstance().getJobName(), event.getSchedulerJobInstance().getContextInstanceId(), MonitorType.ERROR, event.getNewStatus());

                invoke(genericNotificationDetails);
            }
            else if (!event.getPreviousStatus().name().equalsIgnoreCase(InstanceStatus.RUNNING.name()) &&
                       event.getNewStatus().name().equalsIgnoreCase(InstanceStatus.RUNNING.name())) {
                GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(event.getSchedulerJobInstance().getChildContextName(),
                    event.getSchedulerJobInstance().getJobName(), event.getSchedulerJobInstance().getContextInstanceId(), MonitorType.START, event.getNewStatus());

                invoke(genericNotificationDetails);
            }
            else if (event.getNewStatus().name().equalsIgnoreCase(InstanceStatus.COMPLETE.name())) {
                GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(event.getSchedulerJobInstance().getChildContextName(),
                    event.getSchedulerJobInstance().getJobName(), event.getSchedulerJobInstance().getContextInstanceId(), MonitorType.COMPLETE, event.getNewStatus());

                invoke(genericNotificationDetails);
            }
        }
    }


}
