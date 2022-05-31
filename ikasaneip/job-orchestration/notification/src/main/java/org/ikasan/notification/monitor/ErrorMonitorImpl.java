package org.ikasan.notification.monitor;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.Monitor;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ErrorMonitorImpl<T> extends AbstractMonitorBase<T> implements Monitor<T> {

    private List<Future<?>> errorNotificationsExecutors = new ArrayList<>();

    /**
     * Constructor
     * @param executorService
     */
    public ErrorMonitorImpl(ExecutorService executorService) {
        super(executorService);

        errorNotificationsExecutors.clear();
        for ( Object contextName : ContextMachineCache.instance().contextNames() ) {
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextName((String) contextName);
            errorNotificationsExecutors.add(Executors.newSingleThreadExecutor().submit(new ErrorNotificationsRunner(contextMachine)));
        }

    }

    @Override
    public void invoke(final T status)
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
            //do the logic!!
            //create GenericNotificationDetails
            if (event.getNewStatus().name().equalsIgnoreCase(InstanceStatus.ERROR.name())) {
                invoke(null);
            }


        }
    }


}
