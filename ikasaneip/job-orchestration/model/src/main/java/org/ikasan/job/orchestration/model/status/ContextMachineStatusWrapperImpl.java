package org.ikasan.job.orchestration.model.status;

import org.ikasan.spec.scheduled.status.model.ContextMachineStatus;
import org.ikasan.spec.scheduled.status.model.ContextMachineStatusWrapper;

import java.util.List;

public class ContextMachineStatusWrapperImpl implements ContextMachineStatusWrapper {

    List<ContextMachineStatus> contextMachineStatusList;

    @Override
    public List<ContextMachineStatus> getContextMachineStatusList() {
        return contextMachineStatusList;
    }

    @Override
    public void setContextMachineStatusList(List<ContextMachineStatus> contextMachineStatusList) {
        this.contextMachineStatusList = contextMachineStatusList;
    }
}
