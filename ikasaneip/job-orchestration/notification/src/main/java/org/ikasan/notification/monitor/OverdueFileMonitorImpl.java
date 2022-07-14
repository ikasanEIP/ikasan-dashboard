package org.ikasan.notification.monitor;

import org.apache.commons.lang3.time.DateUtils;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.MonitorType;
import org.ikasan.notification.exception.StopNotificationRunnerException;
import org.ikasan.spec.scheduled.event.service.ContextMachineUpdateBroadcaster;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
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
import java.util.function.Consumer;

public class OverdueFileMonitorImpl extends AbstractMonitorBase<GenericNotificationDetails> implements Monitor<GenericNotificationDetails>, Consumer<ContextInstance> {

    private static final Logger LOG = LoggerFactory.getLogger(OverdueFileMonitorImpl.class);

    private SchedulerJobService schedulerJobService;

    private List<ScheduledFuture<?>> overdueFileNotificationsExecutors = new ArrayList<>();

    private Integer fileArrivalToleranceInMinutes;

    private ContextMachineUpdateBroadcaster contextMachineUpdateBroadcaster;

    /**
     * Constructor
     * @param fileArrivalToleranceInMinutes
     * @param executorService
     */
    public OverdueFileMonitorImpl(Integer fileArrivalToleranceInMinutes, ExecutorService executorService, SchedulerJobService schedulerJobService, ContextMachineUpdateBroadcaster contextMachineUpdateBroadcaster) {
        super(executorService);
        LOG.info("OverdueFileMonitorImpl is being created!");

        this.fileArrivalToleranceInMinutes = fileArrivalToleranceInMinutes;
        this.schedulerJobService = schedulerJobService;

        overdueFileNotificationsExecutors.clear();
        for ( Object contextName : ContextMachineCache.instance().contextNames() ) {
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextName((String) contextName);
            overdueFileNotificationsExecutors.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new OverdueFileNotificationsRunner(contextMachine),1,1, TimeUnit.MINUTES));
        }
        LOG.info(overdueFileNotificationsExecutors.size() + " number of Contexts are being monitored now!");

        contextMachineUpdateBroadcaster.register(this);
        LOG.info("Registered to ContextMachineUpdateBroadcaster");
    }

    @Override
    public void invoke(final GenericNotificationDetails status)
    {
        super.invoke(status);
    }

    @Override
    public void accept(ContextInstance contextInstance) {
        overdueFileNotificationsExecutors.add(Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new OverdueFileNotificationsRunner(ContextMachineCache.instance().getByContextInstanceId(contextInstance.getId())),1,1, TimeUnit.MINUTES));
        LOG.info("Started monitoring of "+contextInstance.getName());
    }

    protected class OverdueFileNotificationsRunner implements Runnable {

        private ContextMachine contextMachine;

        public OverdueFileNotificationsRunner(ContextMachine contextMachine) {
            this.contextMachine = contextMachine;
        }

        @Override
        public void run() {
            try {
                if (contextMachine.getContext().getStatus().toString().equalsIgnoreCase(InstanceStatus.COMPLETE.toString())) {
                    throw new StopNotificationRunnerException(contextMachine.getContext().getName()+" is already Complete, stopping the OverdueFileNotificationsRunner");
                }

                if (contextMachine.getContext().getStatus().toString().equalsIgnoreCase(InstanceStatus.RUNNING.toString()) ||
                    contextMachine.getContext().getStatus().toString().equalsIgnoreCase(InstanceStatus.ERROR.toString()) ||
                    contextMachine.getContext().getStatus().toString().equalsIgnoreCase(InstanceStatus.WAITING.toString()) ||
                    contextMachine.getContext().getStatus().toString().equalsIgnoreCase(InstanceStatus.RELEASED.toString())) {

                    DateTime dateTime = new DateTime().withHourOfDay(1).withMinuteOfHour(0).withSecondOfMinute(0);

                    SearchResults<SchedulerJobRecord> searchResults = schedulerJobService.findByContext(contextMachine.getContext().getName(), 10000, 0);

                    for (SchedulerJobRecord schedulerJobRecord : searchResults.getResultList()) {

                        SchedulerJob job  = schedulerJobRecord.getJob();

                        if(job instanceof FileEventDrivenJob) {

                            FileEventDrivenJob fileEventDrivenJob = (FileEventDrivenJob) job;
                            if (isJobOverdued(dateTime.toDate(), fileEventDrivenJob.getCronExpression())) {
                                GenericNotificationDetails genericNotificationDetails = new GenericNotificationDetails(fileEventDrivenJob.getChildContextIds().get(0),
                                    fileEventDrivenJob.getJobName(), contextMachine.getContext().getId(), MonitorType.OVERDUE, InstanceStatus.ERROR);

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
