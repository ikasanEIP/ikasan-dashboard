package org.ikasan.notification.monitor;

import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.MonitorType;
import org.ikasan.notification.exception.StopNotificationRunnerException;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.model.Monitor;
import org.ikasan.spec.search.SearchResults;
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
    private SchedulerJobService schedulerJobService;

    private List<ScheduledFuture<?>> jobRunningTimesNotificationsExecutors = new ArrayList<>();

    /**
     * Constructor
     * @param executorService
     */
    public JobRunningTimesMonitorImpl(ExecutorService executorService, SchedulerJobInstanceService schedulerJobInstanceService,
                                      SchedulerJobService schedulerJobService) {
        super(executorService);
        LOG.info("JobRunningTimesMonitorImpl is being created!");

        this.schedulerJobInstanceService = schedulerJobInstanceService;
        this.schedulerJobService = schedulerJobService;

        jobRunningTimesNotificationsExecutors.clear();
    }

    @Override
    public void invoke(final GenericNotificationDetails status)
    {
        super.invoke(status);
    }

    @Override
    public void register(ContextInstance contextInstance) {
        jobRunningTimesNotificationsExecutors.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new JobRunningTimesNotificationsRunner(contextInstance),1,1, TimeUnit.MINUTES));
        LOG.info("JobRunningTimesMonitor has started monitoring on "+contextInstance.getName());
        LOG.info(jobRunningTimesNotificationsExecutors.size() + " number of Contexts are being monitored now!");
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

                    SearchResults<SchedulerJobInstanceRecord> searchResults = schedulerJobInstanceService.getSchedulerJobInstancesByContextName(contextInstance.getName(), -1, -1, null, null);

                    SearchResults<SchedulerJobRecord> jobDetailsResults = schedulerJobService.findByContext(contextInstance.getName(), -1, -1);

                    Map<String,SchedulerJobRecord> jobMap = createJobMap(jobDetailsResults);

                    for (SchedulerJobInstanceRecord schedulerJobInstanceRecord : searchResults.getResultList()) {

                        SchedulerJobInstance schedulerJobInstance  = schedulerJobInstanceRecord.getSchedulerJobInstance();

                        if(schedulerJobInstance instanceof InternalEventDrivenJobInstance) {

                            InternalEventDrivenJobInstance internalEventDrivenJobInstance = (InternalEventDrivenJobInstance) schedulerJobInstance;

                            if (internalEventDrivenJobInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.RUNNING.toString()) ||
                                internalEventDrivenJobInstance.getStatus().toString().equalsIgnoreCase(InstanceStatus.COMPLETE.toString())) {

                                InternalEventDrivenJobRecord internalEventDrivenJobRecord = (InternalEventDrivenJobRecord)jobMap.get(internalEventDrivenJobInstance.getJobName());

                                long processTime = internalEventDrivenJobInstance.getScheduledProcessEvent().getCompletionTime() - internalEventDrivenJobInstance.getScheduledProcessEvent().getFireTime();
                                if (processTime < internalEventDrivenJobRecord.getInternalEventDrivenJob().getMinExecutionTime() ||
                                    processTime > internalEventDrivenJobRecord.getInternalEventDrivenJob().getMaxExecutionTime() ) {

                                    GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(internalEventDrivenJobInstance.getChildContextIds().get(0),
                                        internalEventDrivenJobInstance.getJobName(), contextInstance.getId(), MonitorType.RUNNING_TIME, internalEventDrivenJobInstance.getStatus());
                                    genericNotificationDetails.setMessage("Processing time:"+processTime+", ");

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

        private Map<String,SchedulerJobRecord> createJobMap(SearchResults<SchedulerJobRecord> jobDetailsResults) {
            Map<String,SchedulerJobRecord> resultMap = new HashMap<>();
            for (SchedulerJobRecord jobRecord : jobDetailsResults.getResultList()) {
                resultMap.put(jobRecord.getJobName(), jobRecord);
            }
            return resultMap;
        }

    }

}
