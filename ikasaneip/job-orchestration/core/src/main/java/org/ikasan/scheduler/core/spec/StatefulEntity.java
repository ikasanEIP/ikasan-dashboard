package org.ikasan.scheduler.core.spec;

import org.ikasan.scheduler.core.model.instance.InstanceStatus;

public interface StatefulEntity {

    public InstanceStatus getStatus();

    public void setStatus(InstanceStatus status);
}
