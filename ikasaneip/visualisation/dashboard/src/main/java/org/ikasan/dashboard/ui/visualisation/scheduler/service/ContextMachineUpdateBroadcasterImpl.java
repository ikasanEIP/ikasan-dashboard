package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import org.ikasan.spec.scheduled.event.service.ContextMachineUpdateBroadcaster;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.function.Consumer;

public class ContextMachineUpdateBroadcasterImpl implements ContextMachineUpdateBroadcaster {

    @Override
    public synchronized void register(Consumer<ContextInstance> listener) {
        org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextMachineUpdateBroadcaster.register(listener);
    }

    @Override
    public synchronized void broadcast(ContextInstance message) {
        org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextMachineUpdateBroadcaster.broadcast(message);
    }
}
