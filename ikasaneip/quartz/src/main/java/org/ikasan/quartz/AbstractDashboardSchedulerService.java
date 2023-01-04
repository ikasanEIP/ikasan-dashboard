package org.ikasan.quartz;

import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.scheduler.DashboardJob;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.*;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public abstract class AbstractDashboardSchedulerService {
    public static final String CONTEXT_GROUP = "context";
    public static final String NOTIFY_GROUP = "notify";
    public static final String CONTEXT_INSTANCE_ID = "contextInstanceId";
    /** Logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger (AbstractDashboardSchedulerService.class);

    /**
     * Scheduler
     */
    private Scheduler scheduler;

    protected ScheduledJobFactory scheduledJobFactory;

    // JobName -> DashboardJob
    protected Map<String, DashboardJob> dashboardJobsMap;

    // JobName -> JobDetail
     protected Map<String, JobDetail> dashboardJobDetailsMap;


    public AbstractDashboardSchedulerService(Scheduler scheduler, ScheduledJobFactory scheduledJobFactory)
    {
        this.scheduler = scheduler;
        if(this.scheduler == null)
        {
            throw new IllegalArgumentException("scheduler cannot be null!");
        }
        this.scheduledJobFactory = scheduledJobFactory;
        if(this.scheduledJobFactory == null)
        {
            throw new IllegalArgumentException("scheduledJobFactory cannot be null!");
        }

        this.dashboardJobsMap = new HashMap<>();
        this.dashboardJobDetailsMap = new HashMap<>();
    }

    @PostConstruct
    public abstract void registerJobs();

    /**
     * The standard trigger has 1 trigger per job
     * @param jobDetail for the job
     */
    public void addJob(final JobDetail jobDetail)
    {
        final JobKey jobkey = jobDetail.getKey();
        String jobName = jobDetail.getKey().getName();
        String jobGroup = jobDetail.getKey().getGroup();

        // for non-context jobs, unschedule the job so that we can reschedule it if the cron expression has changed.
        if (!jobGroup.equals(CONTEXT_GROUP)) {
            this.unscheduleJob(jobName);
        }
        try
        {
            if(jobGroup.equals(CONTEXT_GROUP) || !this.scheduler.checkExists(jobkey))
            {
                final Trigger trigger = getCronTrigger(
                    jobkey,
                    this.dashboardJobsMap.get(jobkey.toString()).getCronExpression(),
                    this.dashboardJobsMap.get(jobkey.toString()).getTimezone());

                final Date scheduledDate = scheduler.scheduleJob(jobDetail, trigger);

                LOG.info("Scheduled job [" + jobkey + "] starting at [" + scheduledDate
                    + "] using cron expression [" + this.dashboardJobsMap.get(jobkey.toString()).getCronExpression()
                    + "] Total triggers for jobkey now [" + scheduler.getTriggersOfJob(jobkey) + "]");
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * The end job has multiple triggers, the first being created with the job itself
     * @param jobDetail for the job
     * @param contextInstanceId to which this end trigger needs to run for
     */
    protected void scheduleEndTrigger(final JobDetail jobDetail, final String contextInstanceId)
    {
        final JobKey jobkey = jobDetail.getKey();
        final TriggerKey triggerKey = new TriggerKey(jobkey.getName() + "-" + contextInstanceId, jobkey.getGroup());
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(CONTEXT_INSTANCE_ID, contextInstanceId);
        final Trigger trigger = getCronTriggerForEndJob(
            triggerKey,
            jobDetail,
            jobDataMap,
            this.dashboardJobsMap.get(jobkey.toString()).getCronExpression(),
            this.dashboardJobsMap.get(jobkey.toString()).getTimezone());
        LOG.info(showAllTriggers(scheduler));
        try
        {
            Date scheduledDate ;
            if (!scheduler.checkExists(jobkey)) {
                // First time so we register the job and the trigger
                scheduledDate = scheduler.scheduleJob(jobDetail, trigger);
            } else {
                // Sebsequent times we onlt need register a new trigger
                scheduledDate = scheduler.scheduleJob(trigger);
            }
            LOG.info("Scheduled job [" + jobkey
                + "] starting at [" + scheduledDate + "] using cron expression ["
                + this.dashboardJobsMap.get(jobkey.toString()).getCronExpression() + "]"
                + "Total triggers for jobkey now [" + scheduler.getTriggersOfJob(jobkey) + "]");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Once the trigger has fired and the specific instance has been dealt with, remove this particular trigger
     * because an end job trigger holds data for / targets a specific context instance ID
     * @param trigger to be removed
     */
    public void removeEndJobTrigger(Trigger trigger)
    {
        try
        {
            this.scheduler.unscheduleJob(trigger.getKey());
        }
        catch (SchedulerException e)
        {
            throw new RuntimeException(e);
        }
        LOG.debug("Tried to remove trigger for [" + trigger.getKey() + "] triggers now set to [" + showAllTriggers(scheduler) + "]");
    }

    /**
     * This is typically invoked when we want to remove all the triggers and the job itself i.e. when we delete a context
     *
     * @param jobName to be removed
     */
    public void removeJob(final String jobName)
    {
        unscheduleJob(jobName);
        dashboardJobDetailsMap.remove(jobName);
        dashboardJobsMap.remove(jobName);
        LOG.info("After remove job" + showAllTriggers(scheduler));
    }

    /**
     * This could be part of complete job delete or refresh of existing job
     * @param jobName to be unscheduled
     */
    public void unscheduleJob(final String jobName)
    {
        JobDetail jobDetail = dashboardJobDetailsMap.get(jobName);
        if (jobDetail != null) {
            try
            {
                if(this.scheduler.checkExists(jobDetail.getKey()))
                {
                    LOG.info("Try to delete for job key " + jobDetail.getKey());
                    this.scheduler.deleteJob(this.dashboardJobDetailsMap.get(jobName).getKey());
                }
                final JobKey endJobKey = new JobKey(jobDetail.getKey().getName() + "-EndJob", jobDetail.getKey().getGroup());
                if(this.scheduler.checkExists(endJobKey))
                {
                    LOG.info("Try to delete for end jobkey " + endJobKey);
                    this.scheduler.deleteJob(endJobKey);
                }
            }
            catch (SchedulerException e)
            {
                throw new RuntimeException(e);
            }
        } else {
            LOG.info("The jobPlan [" + jobName + "] is new, no need to unschedule the previous instances");
        }
    }

    protected Trigger getCronTrigger(final JobKey jobkey, final String cronExpression, final String zoneId)
    {
        return standardTrigger(cronExpression, zoneId)
            .withIdentity(jobkey.getName(), jobkey.getGroup())
            .build();
    }

    private TriggerBuilder standardTrigger(final String cronExpression, final String zoneId) {
        final CronScheduleBuilder cronScheduleBuilder = cronSchedule(cronExpression)
            .inTimeZone(TimeZone.getTimeZone(ZoneId.of(zoneId)));
        return newTrigger()
            .withSchedule(cronScheduleBuilder);
    }

    protected Trigger getCronTriggerForEndJob(final TriggerKey triggerKey, final JobDetail jobDetail,
                                              final JobDataMap jobDataMap, final String cronExpression, final String zoneId)
    {
        return standardTrigger(cronExpression, zoneId)
            .withIdentity(triggerKey)
            .forJob(jobDetail)
            .usingJobData(jobDataMap)
            .build();
    }

    /**
     * This is a helper and debug method user to verify triggers are being maintained correctly
     * @param scheduler for which we need the triggers
     * @return String containing a list of all the current triggers
     */
    public static String showAllTriggers(final Scheduler scheduler)  {
        StringBuilder results = new StringBuilder();
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
                    StringBuilder jobTriggers = new StringBuilder();
                    for (Trigger trigger : triggers) {
                        jobTriggers.append("Trigger key [").append(trigger.getKey())
                            .append("] Job key").append(trigger.getJobKey())
                            .append("] map [").append(trigger.getJobDataMap().get(CONTEXT_INSTANCE_ID))
                            .append("] nextFire [").append(trigger.getNextFireTime()).append("]");
                    }
                    results
                        .append("[jobName] : ")
                        .append(jobKey.getName())
                        .append(" [groupName] : ")
                        .append(jobKey.getGroup())
                        .append(" - ")
                        .append(jobTriggers);
                }
            }
        } catch (SchedulerException e) {
            results.append("Scheduler may be closed due to : ").append(e.getMessage());
        }
        return results.toString();
    }
}
