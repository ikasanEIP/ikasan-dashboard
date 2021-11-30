package org.ikasan.scheduler.core.spec;

public interface StatefulEntity {

    InstanceStatus getStatus();

    void setStatus(InstanceStatus status);
}
