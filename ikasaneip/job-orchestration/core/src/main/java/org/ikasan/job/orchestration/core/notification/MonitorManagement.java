package org.ikasan.job.orchestration.core.notification;

import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.spec.scheduled.notification.model.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MonitorManagement {

    private static final Logger LOG = LoggerFactory.getLogger(MonitorManagement.class);

    private static List<Monitor> monitors = new ArrayList<>();

    public void registerMonitor(Monitor monitor) {
        monitors.add(monitor);
    }

    public void unRegisterMonitor(Monitor monitor) {
        monitors.remove(monitor);
    }

    public static void startMonitoring(ContextMachine contextMachine) {
        for (Monitor monitor : monitors) {
            monitor.register(contextMachine.getContext());
            LOG.info(contextMachine.getContext().getName()+" has registered to "+monitor);
        }
    }

    public static void stopMonitoring(ContextMachine contextMachine) {
        for (Monitor monitor : monitors) {
            monitor.unregister(contextMachine.getContext());
            LOG.info(contextMachine.getContext().getName()+" is being unregistered context is ending");
        }
    }
}
