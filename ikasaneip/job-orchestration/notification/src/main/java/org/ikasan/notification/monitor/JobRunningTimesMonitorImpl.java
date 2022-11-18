package org.ikasan.notification.monitor;

import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.MonitorType;
import org.ikasan.notification.exception.StopNotificationRunnerException;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.notification.model.Monitor;
import org.ikasan.spec.search.SearchResults;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class JobRunningTimesMonitorImpl extends AbstractMonitorBase<GenericNotificationDetails> implements Monitor<GenericNotificationDetails> {

    private static final Logger LOG = LoggerFactory.getLogger(JobRunningTimesMonitorImpl.class);

    private SchedulerJobInstanceService schedulerJobInstanceService;
    private InternalEventDrivenJobService internalEventDrivenJobService;

    private boolean notificationEnabled;
    private int notificationPollingInterval;

    private List<ScheduledFuture<?>> jobRunningTimesNotificationsExecutors = new ArrayList<>();

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

        jobRunningTimesNotificationsExecutors.clear();
    }

    @Override
    public void invoke(final GenericNotificationDetails status)
    {
        super.invoke(status);
    }

    @Override
    public void register(ContextInstance contextInstance) {
        if(this.notificationEnabled) {
            jobRunningTimesNotificationsExecutors.add(Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(new JobRunningTimesNotificationsRunner(contextInstance), 1, this.notificationPollingInterval, TimeUnit.MINUTES));

            LOG.info("JobRunningTimesMonitor has started monitoring on " + contextInstance.getName());
            LOG.info(jobRunningTimesNotificationsExecutors.size() + " number of Contexts are being monitored now!");
        }
        else {
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
                if (contextInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.COMPLETE.toString())) {
                    throw new StopNotificationRunnerException(contextInstance.getName()+" is already Complete, stopping the JobRunningTimesNotificationsRunner");
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

                                LOG.info("MIN = {}, MAX = {} AND PROCESSED MINS = {} - {} FIRED {} CURRENT {} COMPLETED {} ", internalEventDrivenJobRecord.getInternalEventDrivenJob().getMinExecutionTime(),
                                    internalEventDrivenJobRecord.getInternalEventDrivenJob().getMaxExecutionTime(),
                                    processedTimeInMinutes, processTime, new DateTime().withMillis(fireTime),
                                    new DateTime().withMillis(currentTime), new DateTime().withMillis(completionTime));

                                if (!(internalEventDrivenJobRecord.getInternalEventDrivenJob().getMinExecutionTime() == -1 ||
                                    internalEventDrivenJobRecord.getInternalEventDrivenJob().getMaxExecutionTime() == -1)) {

                                    if (processedTimeInMinutes < internalEventDrivenJobRecord.getInternalEventDrivenJob().getMinExecutionTime() ||
                                        processedTimeInMinutes > internalEventDrivenJobRecord.getInternalEventDrivenJob().getMaxExecutionTime() ) {

                                        GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(internalEventDrivenJobInstance.getAgentName(), contextInstance.getName(), internalEventDrivenJobInstance.getChildContextNames().get(0),
                                            internalEventDrivenJobInstance.getJobName(), contextInstance.getId(), MonitorType.RUNNING_TIME, internalEventDrivenJobInstance.getStatus());
                                        genericNotificationDetails.setMessage("Processing time:"+processTime+", job min. running time:"+internalEventDrivenJobRecord.getInternalEventDrivenJob().getMinExecutionTime()+
                                            ", job max. running time:"+internalEventDrivenJobRecord.getInternalEventDrivenJob().getMaxExecutionTime());

                                        LOG.info(genericNotificationDetails.getMessage());

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
