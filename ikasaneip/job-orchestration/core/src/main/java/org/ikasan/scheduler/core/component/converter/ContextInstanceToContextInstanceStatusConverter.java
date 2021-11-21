package org.ikasan.scheduler.core.component.converter;

import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.instance.SchedulerJobInstance;
import org.ikasan.scheduler.core.model.status.ContextInstanceStatus;
import org.ikasan.scheduler.core.model.status.SchedulerJobInstanceStatus;
import org.ikasan.spec.component.transformation.Converter;
import org.ikasan.spec.component.transformation.TransformationException;

import java.util.ArrayList;
import java.util.List;

public class ContextInstanceToContextInstanceStatusConverter implements Converter<ContextInstance, ContextInstanceStatus> {

    @Override
    public ContextInstanceStatus convert(ContextInstance contextInstance) throws TransformationException {
        ContextInstanceStatus contextInstanceStatus = new ContextInstanceStatus();

        try {
            return this.populateStatus(contextInstanceStatus, contextInstance);
        }
        catch (Exception e) {
            throw new TransformationException("An error has occurred trying to convert a ContextInstance to a ContextInstanceStatus", e);
        }
    }

    private ContextInstanceStatus populateStatus(ContextInstanceStatus contextInstanceStatus, ContextInstance contextInstance){
        contextInstanceStatus.setContextName(contextInstance.getName());
        contextInstanceStatus.setInstanceStatus(contextInstance.getStatus());

        if(contextInstance.getScheduledJobs() != null) {
            List<SchedulerJobInstanceStatus> schedulerJobInstanceStatuses = new ArrayList<>();
            for(SchedulerJobInstance schedulerJobInstance: contextInstance.getScheduledJobs()) {
                SchedulerJobInstanceStatus instance = new SchedulerJobInstanceStatus();

                instance.setJobName(schedulerJobInstance.getJobName());
                instance.setAgentName(schedulerJobInstance.getAgentName());
                instance.setInstanceStatus(schedulerJobInstance.getStatus());

                schedulerJobInstanceStatuses.add(instance);
            }

            contextInstanceStatus.setSchedulerJobInstanceStatuses(schedulerJobInstanceStatuses);
        }

        if(contextInstance.getContexts() != null) {
            List<ContextInstanceStatus> contextInstanceStatuses = new ArrayList<>();
            for(ContextInstance instance: contextInstance.getContexts()) {
                contextInstanceStatuses.add(this.populateStatus(new ContextInstanceStatus(), instance));
            }

            contextInstanceStatus.setContextInstanceStatuses(contextInstanceStatuses);
        }

        return contextInstanceStatus;
    }
}
