package org.ikasan.scheduler.core.spec;

public interface StatefulEntity {

    public InstanceStatus getStatus();

    public void setStatus(InstanceStatus status);
}
