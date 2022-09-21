package org.ikasan.notification.monitor;

import org.apache.commons.lang3.time.DateUtils;
import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.MonitorType;
import org.ikasan.notification.exception.StopNotificationRunnerException;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.notification.model.Monitor;
import org.ikasan.spec.search.SearchResults;
import org.joda.time.DateTime;
import org.quartz.TriggerUtils;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OverdueFileMonitorImpl extends AbstractMonitorBase<GenericNotificationDetails> implements Monitor<GenericNotificationDetails> {

    private static final Logger LOG = LoggerFactory.getLogger(OverdueFileMonitorImpl.class);

    private SchedulerJobInstanceService schedulerJobInstanceService;

    private List<ScheduledFuture<?>> overdueFileNotificationsExecutors = new ArrayList<>();

    private Integer fileArrivalToleranceInMinutes;

    /**
     * Constructor
     * @param fileArrivalToleranceInMinutes
     * @param executorService
     */
    public OverdueFileMonitorImpl(Integer fileArrivalToleranceInMinutes, ExecutorService executorService, SchedulerJobInstanceService schedulerJobInstanceService) {
        super(executorService);
        LOG.info("OverdueFileMonitorImpl is being created!");

        this.fileArrivalToleranceInMinutes = fileArrivalToleranceInMinutes;
        this.schedulerJobInstanceService = schedulerJobInstanceService;

        overdueFileNotificationsExecutors.clear();
    }

    @Override
    public void invoke(final GenericNotificationDetails status)
    {
        super.invoke(status);
    }

    @Override
    public void register(ContextInstance contextInstance) {
        overdueFileNotificationsExecutors.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new OverdueFileNotificationsRunner(contextInstance),1,1, TimeUnit.MINUTES));
        LOG.info("OverdueFileMonitor has started monitoring on "+contextInstance.getName());
        LOG.info(overdueFileNotificationsExecutors.size() + " number of Contexts are being monitored now!");
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

                if (contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.RUNNING.toString()) ||
                    contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.ERROR.toString()) ||
                    contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.WAITING.toString()) ||
                    contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.RELEASED.toString())) {

                    DateTime dateTime = new DateTime().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(1);

                    SearchResults<SchedulerJobInstanceRecord> searchResults = schedulerJobInstanceService.getSchedulerJobInstancesByContextName(contextInstance.getName(), -1, -1, null, null);

                    for (SchedulerJobInstanceRecord schedulerJobInstanceRecord : searchResults.getResultList()) {

                        SchedulerJobInstance schedulerJobInstance  = schedulerJobInstanceRecord.getSchedulerJobInstance();

                        if (schedulerJobInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.RUNNING.toString()) ||
                            schedulerJobInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.WAITING.toString()) ||
                            schedulerJobInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.COMPLETE.toString()) ) {

                            if(schedulerJobInstance instanceof FileEventDrivenJobInstance) {

                                FileEventDrivenJobInstance fileEventDrivenJobInstance = (FileEventDrivenJobInstance) schedulerJobInstance;

                                long fireTime = new DateTime().toDate().getTime();
                                if (fileEventDrivenJobInstance.getScheduledProcessEvent() != null && fileEventDrivenJobInstance.getScheduledProcessEvent().getFireTime() > 0) {
                                    fireTime = fileEventDrivenJobInstance.getScheduledProcessEvent().getFireTime();
                                }

                                if (isJobOverdued(dateTime.toDate(), fireTime, fileEventDrivenJobInstance.getCronExpression())) {
                                    GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(fileEventDrivenJobInstance.getChildContextNames().get(0),
                                        fileEventDrivenJobInstance.getJobName(), contextInstance.getId(), MonitorType.OVERDUE, InstanceStatus.ERROR);

                                    invoke(genericNotificationDetails);
                                }
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

    private boolean isJobOverdued(Date startTime, Long fireTime, String cronExpression) throws ParseException {

        CronTriggerImpl ct = new CronTriggerImpl("foo", "goo", cronExpression);
        ct.setStartTime(startTime);
        List<Date> fireTimes = TriggerUtils.computeFireTimes(ct, null, 1);
        Date firstFireTime = fireTimes.iterator().next();

        Date firstFireTimeWithTolerance = DateUtils.addMinutes(firstFireTime, fileArrivalToleranceInMinutes);
        return firstFireTimeWithTolerance.before(new DateTime(fireTime).toDate());
    }

}
