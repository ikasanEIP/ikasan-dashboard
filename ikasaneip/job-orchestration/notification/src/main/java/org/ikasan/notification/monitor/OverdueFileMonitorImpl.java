package org.ikasan.notification.monitor;

import org.apache.commons.lang3.time.DateUtils;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.MonitorType;
import org.ikasan.notification.exception.StopNotificationRunnerException;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.model.Monitor;
import org.joda.time.DateTime;
import org.quartz.TriggerUtils;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OverdueFileMonitorImpl extends AbstractMonitorBase<GenericNotificationDetails> implements Monitor<GenericNotificationDetails> {

    private SchedulerJobService schedulerJobService;

    private List<ScheduledFuture<?>> overdueFileNotificationsExecutors = new ArrayList<>();

    private Integer fileArrivalToleranceInMinutes;

    /**
     * Constructor
     * @param fileArrivalToleranceInMinutes
     * @param executorService
     */
    public OverdueFileMonitorImpl(Integer fileArrivalToleranceInMinutes, ExecutorService executorService, SchedulerJobService schedulerJobService) {
        super(executorService);

        this.fileArrivalToleranceInMinutes = fileArrivalToleranceInMinutes;
        this.schedulerJobService = schedulerJobService;

        overdueFileNotificationsExecutors.clear();
        for ( Object contextName : ContextMachineCache.instance().contextNames() ) {
            ContextInstance contextInstance = ContextMachineCache.instance().getByContextName((String) contextName).getContext();
            overdueFileNotificationsExecutors.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new OverdueFileNotificationsRunner(contextInstance),1,1, TimeUnit.MINUTES));
        }
    }

    @Override
    public void invoke(final GenericNotificationDetails status)
    {
        super.invoke(status);
    }

    protected class OverdueFileNotificationsRunner implements Runnable {

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

                    DateTime dateTime = new DateTime().withHourOfDay(1).withMinuteOfHour(0).withSecondOfMinute(0);

                    for (SchedulerJobInstance schedulerJobInstance : contextInstance.getScheduledJobs()) {
                        SchedulerJobRecord schedulerJobRecord = schedulerJobService.findByContextIdAndJobName(schedulerJobInstance.getContextId(), schedulerJobInstance.getJobName());

                        SchedulerJob job  = schedulerJobRecord.getJob();

                        if(job instanceof FileEventDrivenJob) {

                            FileEventDrivenJob fileEventDrivenJob = (FileEventDrivenJob) job;
                            if (isJobOverdued(dateTime.toDate(), fileEventDrivenJob.getCronExpression())) {
                                GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(fileEventDrivenJob.getContextId(),
                                    fileEventDrivenJob.getJobName(), schedulerJobInstance.getContextInstanceId(), MonitorType.OVERDUE, InstanceStatus.ERROR);

                                invoke(genericNotificationDetails);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                // do something
                e.printStackTrace();
            } finally {

            }
        }
    }

    private boolean isJobOverdued(Date startTime, String cronExpression) throws ParseException {

        CronTriggerImpl ct = new CronTriggerImpl("foo", "goo", cronExpression);
        ct.setStartTime(startTime);
        List<Date> fireTimes = TriggerUtils.computeFireTimes(ct, null, 1);
        Date firstFireTime = fireTimes.iterator().next();

        Date firstFireTimeWithTolerance = DateUtils.addMinutes(firstFireTime, fileArrivalToleranceInMinutes);
        return firstFireTimeWithTolerance.before(new DateTime().toDate());
    }

}
