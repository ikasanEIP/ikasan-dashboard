package org.ikasan.dashboard.notification.scheduler.service;

import org.ikasan.dashboard.notification.scheduler.SchedulerNotificationJob;
import org.ikasan.dashboard.schedule.AbstractDashboardSchedulerService;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.List;

public class SchedulerNotificationSchedulerService extends AbstractDashboardSchedulerService {
    /** Logger for this class */
    private static Logger logger = LoggerFactory.getLogger(SchedulerNotificationSchedulerService.class);

    /**
     * Scheduler
     */
    private List<SchedulerNotificationJob> schedulerNotificationJobs;


    public SchedulerNotificationSchedulerService(Scheduler scheduler, ScheduledJobFactory scheduledJobFactory,
                                                 List<SchedulerNotificationJob> schedulerNotificationJobs)
    {
        super(scheduler, scheduledJobFactory);

        this.schedulerNotificationJobs = schedulerNotificationJobs;
        if(this.schedulerNotificationJobs == null)
        {
            throw new IllegalArgumentException("schedulerNotificationJobs cannot be null!");
        }
    }

    @PostConstruct
    public void registerJobs()
    {
        if(this.schedulerNotificationJobs.isEmpty()) {
            logger.info("There are no scheduled job notification configured!");
        }

        for(SchedulerNotificationJob job: this.schedulerNotificationJobs)
        {
            JobDetail jobDetail = this.scheduledJobFactory.createJobDetail
                (job, SchedulerNotificationJob.class, job.getJobName(), "notify");

            super.dashboardJobDetailsMap.put(job.getJobName(), jobDetail);
            super.dashboardJobsMap.put(jobDetail.getKey().toString(), job);
        }

        for(JobDetail jobDetail: super.dashboardJobDetailsMap.values())
        {
            logger.info(String.format("Registering scheduler notification job[%s]", jobDetail.getKey().getName()));
            this.addJob(jobDetail.getKey().getName());
        }
    }

}
