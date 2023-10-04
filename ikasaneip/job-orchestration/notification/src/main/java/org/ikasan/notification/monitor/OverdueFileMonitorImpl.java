package org.ikasan.notification.monitor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.MonitorType;
import org.ikasan.notification.exception.StopNotificationRunnerException;
import org.ikasan.notification.factory.NotificationThreadFactory;
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
import java.util.*;
import java.util.concurrent.*;

public class OverdueFileMonitorImpl extends AbstractMonitorBase<GenericNotificationDetails> implements Monitor<GenericNotificationDetails> {

    private static final Logger LOG = LoggerFactory.getLogger(OverdueFileMonitorImpl.class);

    private SchedulerJobInstanceService schedulerJobInstanceService;

    private Map<String, ScheduledFuture<?>> mapOfRunningJobs = new HashMap<>();

    private Map<String, ScheduledExecutorService> mapOfRunningExecutors = new HashMap<>();

    private Integer fileArrivalToleranceInMinutes;

    private boolean notificationEnabled;
    private int notificationPollingInterval;

    /**
     * Constructor
     * @param fileArrivalToleranceInMinutes
     * @param executorService
     */
    public OverdueFileMonitorImpl(Integer fileArrivalToleranceInMinutes, ExecutorService executorService, SchedulerJobInstanceService schedulerJobInstanceService
        , boolean notificationEnabled, int notificationPollingInterval) {
        super(executorService);
        LOG.info("OverdueFileMonitorImpl is being created!");

        this.fileArrivalToleranceInMinutes = fileArrivalToleranceInMinutes;
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.notificationEnabled = notificationEnabled;
        this.notificationPollingInterval = notificationPollingInterval;
    }

    @Override
    public void invoke(final GenericNotificationDetails status)
    {
        super.invoke(status);
    }

    @Override
    public void register(ContextInstance contextInstance) {
        if(this.notificationEnabled) {

            // Create the executor
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new NotificationThreadFactory("OverdueNotification"));

            // Create the scheduler to be executed
            ScheduledFuture<?> notificationScheduler = executorService.scheduleAtFixedRate(new OverdueFileNotificationsRunner(contextInstance)
                , 1, this.notificationPollingInterval, TimeUnit.MINUTES);

            mapOfRunningJobs.put(contextInstance.getId(), notificationScheduler);
            mapOfRunningExecutors.put(contextInstance.getId(), executorService);

            LOG.info("OverdueFileMonitor has started monitoring for context {}, instance {}", contextInstance.getName(), contextInstance.getId());
            LOG.info(mapOfRunningJobs.size() + " number of Contexts are being monitored now!");
        }
        else {
            LOG.info("Notifications are not enabled!");
        }
    }

    @Override
    public void unregister(ContextInstance contextInstance) {
        if(this.notificationEnabled) {
            if (mapOfRunningJobs.containsKey(contextInstance.getId())) {
                mapOfRunningJobs.get(contextInstance.getId()).cancel(true); // Stop the related SingleThreadScheduledExecutor
                LOG.info("Context {}, Context Instance Id {} notification has been unregistered", contextInstance.getName(), contextInstance.getId());
                mapOfRunningJobs.remove(contextInstance.getId());
            }
            if (mapOfRunningExecutors.containsKey(contextInstance.getId())) {
                mapOfRunningExecutors.get(contextInstance.getId()).shutdown(); // Stop the related ScheduledExecutorService
                LOG.debug("Context {}, Context Instance Id {} notification executor has been unregistered", contextInstance.getName(), contextInstance.getId());
                mapOfRunningExecutors.remove(contextInstance.getId());
            }
            LOG.info("After unregistering Context {}, InstanceId {} - number of Contexts are being monitored now is {}",
                contextInstance.getName(), contextInstance.getId(), mapOfRunningExecutors.size());
        } else {
            LOG.info("Notifications are not enabled!");
        }
    }

    protected class OverdueFileNotificationsRunner implements Runnable {

        private ContextInstance contextInstance;

        public OverdueFileNotificationsRunner(ContextInstance contextInstance) {
            this.contextInstance = contextInstance;
        }

        @Override
        public void run() {
            try {
                if (contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.COMPLETE.toString()) ||
                    contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.ENDED.toString())) {
                    unregister(contextInstance);
                    throw new StopNotificationRunnerException("Context:" + contextInstance.getName() + ", InstanceId: " + contextInstance.getId() +
                        " is " + contextInstance.getStatus().toString() + ", stopping the OverdueFileNotificationsRunner");
                }

                if (contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.RUNNING.toString()) ||
                    contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.ERROR.toString()) ||
                    contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.WAITING.toString()) ||
                    contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.RELEASED.toString())) {

                    //DateTime dateTime = new DateTime().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(1);
                    // set the start time based on when the context instance was started
                    DateTime dateTime = new DateTime().withMillis(contextInstance.getCreatedDateTime());

                    SearchResults<SchedulerJobInstanceRecord> searchResults = schedulerJobInstanceService.getSchedulerJobInstancesByContextInstanceId(contextInstance.getId(), -1, -1, null, null);

                    int count = 0;
                    for (SchedulerJobInstanceRecord schedulerJobInstanceRecord : searchResults.getResultList()) {

                        SchedulerJobInstance schedulerJobInstance  = schedulerJobInstanceRecord.getSchedulerJobInstance();

                        if (schedulerJobInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.RUNNING.toString()) ||
                            schedulerJobInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.WAITING.toString()) ||
                            schedulerJobInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.COMPLETE.toString()) ) {

                            if(schedulerJobInstance instanceof FileEventDrivenJobInstance) {

                                FileEventDrivenJobInstance fileEventDrivenJobInstance = (FileEventDrivenJobInstance) schedulerJobInstance;

                                // Check if SLA has not been defined, if it has not then no test for notification will be done.
                                if (StringUtils.isBlank(fileEventDrivenJobInstance.getSlaCronExpression())) {
                                    continue;
                                }

                                long fireTime = new DateTime().toDate().getTime();
                                if (fileEventDrivenJobInstance.getScheduledProcessEvent() != null && fileEventDrivenJobInstance.getScheduledProcessEvent().getFireTime() > 0) {
                                    fireTime = fileEventDrivenJobInstance.getScheduledProcessEvent().getFireTime();
                                }

                                if (isJobOverdued(dateTime.toDate(), fireTime, fileEventDrivenJobInstance.getSlaCronExpression())) {
                                    GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(fileEventDrivenJobInstance.getAgentName(), contextInstance.getName(), fileEventDrivenJobInstance.getChildContextNames().get(0),
                                        fileEventDrivenJobInstance.getJobName(), contextInstance.getId(), MonitorType.OVERDUE, InstanceStatus.ERROR);

                                    // Add FileNames and Paths
                                    StringBuilder fileNames = new StringBuilder();
                                    fileEventDrivenJobInstance.getFilenames().forEach(s -> {
                                        fileNames.append(s).append(System.lineSeparator());
                                    });
                                    genericNotificationDetails.setFileName(fileNames.toString());
                                    genericNotificationDetails.setFilePath(fileEventDrivenJobInstance.getFilePath());

                                    invoke(genericNotificationDetails);
                                    count++;
                                }
                            }
                        }
                    }
                    LOG.debug("OVERDUE HAS CREATED {} EVENTS for the context {} !", count, contextInstance.getName());
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
        LOG.debug("Start Time = {}, FireTime = {}, cronExpression = {}, firstFireTime = {}, firstFireTimeWithTolerance = {}, Return is = {}",
            startTime, new DateTime(fireTime).toDate(), cronExpression, firstFireTime, firstFireTimeWithTolerance, firstFireTimeWithTolerance.before(new DateTime(fireTime).toDate()));
        return firstFireTimeWithTolerance.before(new DateTime(fireTime).toDate());
    }

}
