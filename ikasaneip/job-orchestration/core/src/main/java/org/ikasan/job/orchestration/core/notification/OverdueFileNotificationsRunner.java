package org.ikasan.job.orchestration.core.notification;

import com.google.common.util.concurrent.Futures;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;

import javax.annotation.Resource;
import java.util.List;

public class OverdueFileNotificationsRunner implements Runnable {

    @Resource
    private SchedulerJobService schedulerJobService;

    private ContextInstance contextInstance;

    public OverdueFileNotificationsRunner(ContextInstance contextInstance) {
        this.contextInstance = contextInstance;
    }

    @Override
    public void run() {
        try {
            if (contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.COMPLETE.toString())) {
                throw new StopNotificationRunnerException(contextInstance.getName()+" is already Complete, stopping the OverdueFileNotificationsRunner");
            }

            if (contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.RUNNING.toString())) {
                List<SchedulerJobInstance> jobs = contextInstance.getScheduledJobs();
                for (SchedulerJobInstance schedulerJobInstance : jobs) {
                    SchedulerJobRecord schedulerJobRecord = schedulerJobService.findByContextIdAndJobName(schedulerJobInstance.getContextId(), schedulerJobInstance.getJobName());

                    SchedulerJob job  = schedulerJobRecord.getJob();

                    if(job instanceof FileEventDrivenJob) {

                    }



                }




            }
        }
        catch (Exception e) {
            // do something
            e.printStackTrace();
        }
        finally {

        }
    }
}