package org.ikasan.job.orchestration.core.notification;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;

import javax.annotation.Resource;
import java.util.List;

public class ErrorNotificationsRunner implements Runnable {

    @Resource
    private SchedulerJobService schedulerJobService;

    private ContextualisedScheduledProcessEvent event;

    public ErrorNotificationsRunner(ContextualisedScheduledProcessEvent event) {
        this.event = event;
    }

    @Override
    public void run() {
        try {



        }
        catch (Exception e) {
            // do something
            e.printStackTrace();
        }
        finally {

        }
    }
}