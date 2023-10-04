package org.ikasan.notification.monitor;

import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.MonitorType;
import org.ikasan.notification.exception.StopNotificationRunnerException;
import org.ikasan.notification.factory.NotificationThreadFactory;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.notification.model.Monitor;
import org.ikasan.spec.search.SearchResults;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class JobRunningTimesMonitorImpl extends AbstractMonitorBase<GenericNotificationDetails> implements Monitor<GenericNotificationDetails> {

    private static final Logger LOG = LoggerFactory.getLogger(JobRunningTimesMonitorImpl.class);

    private SchedulerJobInstanceService schedulerJobInstanceService;
    private InternalEventDrivenJobService internalEventDrivenJobService;

    private boolean notificationEnabled;
    private int notificationPollingInterval;
    private Map<String, ScheduledFuture<?>> mapOfRunningJobs = new HashMap<>();
    private Map<String, ScheduledExecutorService> mapOfRunningExecutors = new HashMap<>();

    /**
     * Constructor
     * @param executorService
     */
    public JobRunningTimesMonitorImpl(ExecutorService executorService, SchedulerJobInstanceService schedulerJobInstanceService,
                                      InternalEventDrivenJobService internalEventDrivenJobService, boolean notificationEnabled,
                                      int notificationPollingInterval) {
        super(executorService);
        LOG.info("JobRunningTimesMonitorImpl is being created!");

        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.internalEventDrivenJobService = internalEventDrivenJobService;
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
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new NotificationThreadFactory("JobRunningNotification"));

            // Create the scheduler to be executed
            ScheduledFuture<?> notificationScheduler = executorService.scheduleAtFixedRate(new JobRunningTimesNotificationsRunner(contextInstance), 1, this.notificationPollingInterval, TimeUnit.MINUTES);

            mapOfRunningJobs.put(contextInstance.getId(), notificationScheduler);
            mapOfRunningExecutors.put(contextInstance.getId(), executorService);

            LOG.info("JobRunningTimesMonitor has started monitoring for context {}, instance {}", contextInstance.getName(), contextInstance.getId());
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
                contextInstance.getName(), contextInstance.getId(), mapOfRunningJobs.size());
        } else {
            LOG.info("Notifications are not enabled!");
        }
    }

    protected class JobRunningTimesNotificationsRunner implements Runnable {

        private ContextInstance contextInstance;

        public JobRunningTimesNotificationsRunner(ContextInstance contextInstance) {
            this.contextInstance = contextInstance;
        }

        @Override
        public void run() {
            try {
                if (contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.COMPLETE.toString()) ||
                    contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.ENDED.toString())) {
                    unregister(contextInstance);
                    throw new StopNotificationRunnerException("Context:" + contextInstance.getName() + ", InstanceId: " + contextInstance.getId() +
                        " is " + contextInstance.getStatus().toString() + ", stopping the JobRunningTimesNotificationsRunner");
                }

                if (contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.RUNNING.toString()) ||
                    contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.ERROR.toString()) ||
                    contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.WAITING.toString()) ||
                    contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.RELEASED.toString())) {

                    SearchResults<SchedulerJobInstanceRecord> searchResults = schedulerJobInstanceService.getSchedulerJobInstancesByContextInstanceId(contextInstance.getId(), -1, -1, null, null);
                    SearchResults<InternalEventDrivenJobRecord> jobDetailsResults = internalEventDrivenJobService.findByContext(contextInstance.getName(), -1, -1);

                    Map<String,InternalEventDrivenJobRecord> jobMap = createJobMap(jobDetailsResults);

                    // Time at execution of the notification polling.
                    long currentTime = System.currentTimeMillis();

                    for (SchedulerJobInstanceRecord schedulerJobInstanceRecord : searchResults.getResultList()) {

                        SchedulerJobInstance schedulerJobInstance  = schedulerJobInstanceRecord.getSchedulerJobInstance();

                        if(schedulerJobInstance instanceof InternalEventDrivenJobInstance) {

                            InternalEventDrivenJobInstance internalEventDrivenJobInstance = (InternalEventDrivenJobInstance) schedulerJobInstance;

                            if (internalEventDrivenJobInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.RUNNING.toString()) ||
                                internalEventDrivenJobInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.COMPLETE.toString())) {

                                InternalEventDrivenJobRecord internalEventDrivenJobRecord = jobMap.get(internalEventDrivenJobInstance.getJobName());

                                // Millis representation of time taken to execute
                                long completionTime = internalEventDrivenJobInstance.getScheduledProcessEvent().getCompletionTime();
                                long fireTime = internalEventDrivenJobInstance.getScheduledProcessEvent().getFireTime();
                                long processTime;
                                // If job is running, completionTime will be 0, therefore use the time right now to work out the duration of the processing job.
                                if (completionTime != 0) {
                                    processTime = completionTime - fireTime;
                                } else {
                                    processTime = currentTime - fireTime;
                                }

                                // Convert processTime to minutes as a decimal representation.
                                double processedTimeInMinutes = (double) processTime / 1000.0 / 60.0;
                                // Only check if min and max execution time != -1 - Else ignore notification

                                LOG.debug("MIN = {}, MAX = {} AND PROCESSED MINS = {} - {} FIRED {} CURRENT {} COMPLETED {} ", internalEventDrivenJobRecord.getInternalEventDrivenJob().getMinExecutionTime(),
                                    internalEventDrivenJobRecord.getInternalEventDrivenJob().getMaxExecutionTime(),
                                    processedTimeInMinutes, processTime, new DateTime().withMillis(fireTime),
                                    new DateTime().withMillis(currentTime), new DateTime().withMillis(completionTime));

                                if (!(internalEventDrivenJobRecord.getInternalEventDrivenJob().getMinExecutionTime() == -1 ||
                                    internalEventDrivenJobRecord.getInternalEventDrivenJob().getMaxExecutionTime() == -1)) {

                                    if (processedTimeInMinutes < internalEventDrivenJobRecord.getInternalEventDrivenJob().getMinExecutionTime() ||
                                        processedTimeInMinutes > internalEventDrivenJobRecord.getInternalEventDrivenJob().getMaxExecutionTime() ) {

                                        GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(internalEventDrivenJobInstance.getAgentName(), contextInstance.getName(), internalEventDrivenJobInstance.getChildContextNames().get(0),
                                            internalEventDrivenJobInstance.getJobName(), contextInstance.getId(), MonitorType.RUNNING_TIME, internalEventDrivenJobInstance.getStatus());
                                        genericNotificationDetails.setMessage("Job has been running for over " + TimeUnit.MINUTES.convert(processTime, TimeUnit.MILLISECONDS) + " minutes. " + System.lineSeparator() +
                                            "Job is configured to alert when running time is less than " + internalEventDrivenJobRecord.getInternalEventDrivenJob().getMinExecutionTime()+
                                            "minutes, and max running time " + internalEventDrivenJobRecord.getInternalEventDrivenJob().getMaxExecutionTime());

                                        invoke(genericNotificationDetails);
                                    }
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

        private Map<String,InternalEventDrivenJobRecord> createJobMap(SearchResults<InternalEventDrivenJobRecord> jobDetailsResults) {
            Map<String,InternalEventDrivenJobRecord> resultMap = new HashMap<>();
            for (InternalEventDrivenJobRecord jobRecord : jobDetailsResults.getResultList()) {
                resultMap.put(jobRecord.getJobName(), jobRecord);
            }
            return resultMap;
        }

    }

}
