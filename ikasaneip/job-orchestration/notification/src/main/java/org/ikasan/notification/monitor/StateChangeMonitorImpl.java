package org.ikasan.notification.monitor;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.MonitorType;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.notification.model.Monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class StateChangeMonitorImpl extends AbstractMonitorBase<GenericNotificationDetails> implements Monitor<GenericNotificationDetails> {

    private List<Future<?>> errorNotificationsExecutors = new ArrayList<>();

    /**
     * Constructor
     * @param executorService
     */
    public StateChangeMonitorImpl(ExecutorService executorService) {
        super(executorService);

        errorNotificationsExecutors.clear();
        for ( Object contextName : ContextMachineCache.instance().contextNames() ) {
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextName((String) contextName);
            errorNotificationsExecutors.add(Executors.newSingleThreadExecutor().submit(new ErrorNotificationsRunner(contextMachine)));
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
                GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(event.getSchedulerJobInstance().getContextId(),
                    event.getSchedulerJobInstance().getJobName(), event.getSchedulerJobInstance().getContextInstanceId(), MonitorType.ERROR, event.getNewStatus());

                invoke(genericNotificationDetails);
            }
            else if (!event.getPreviousStatus().name().equalsIgnoreCase(InstanceStatus.RUNNING.name()) &&
                       event.getNewStatus().name().equalsIgnoreCase(InstanceStatus.RUNNING.name())) {
                GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(event.getSchedulerJobInstance().getContextId(),
                    event.getSchedulerJobInstance().getJobName(), event.getSchedulerJobInstance().getContextInstanceId(), MonitorType.START, event.getNewStatus());

                invoke(genericNotificationDetails);
            }
            else if (event.getNewStatus().name().equalsIgnoreCase(InstanceStatus.COMPLETE.name())) {
                GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(event.getSchedulerJobInstance().getContextId(),
                    event.getSchedulerJobInstance().getJobName(), event.getSchedulerJobInstance().getContextInstanceId(), MonitorType.COMPLETE, event.getNewStatus());

                invoke(genericNotificationDetails);
            }
        }
    }


}
