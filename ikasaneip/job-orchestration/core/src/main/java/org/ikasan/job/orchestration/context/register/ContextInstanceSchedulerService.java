package org.ikasan.job.orchestration.context.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.quartz.AbstractDashboardSchedulerService;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.search.SearchResults;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

public class ContextInstanceSchedulerService extends AbstractDashboardSchedulerService {
    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(ContextInstanceSchedulerService.class);

    /**
     * Scheduler
     */
    private ScheduledContextService scheduledContextService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ObjectMapper objectMapper;
    private SchedulerService schedulerService;
    private InternalEventDrivenJobService internalEventDrivenJobService;
    private String queueDirectory;


    public ContextInstanceSchedulerService(Scheduler scheduler, ScheduledJobFactory scheduledJobFactory
        , ScheduledContextService scheduledContextService, ScheduledContextInstanceService scheduledContextInstanceService
        , SchedulerService schedulerService, InternalEventDrivenJobService internalEventDrivenJobService, String queueDirectory) {
        super(scheduler, scheduledJobFactory);

        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.schedulerService = schedulerService;
        if (this.schedulerService == null) {
            throw new IllegalArgumentException("schedulerService cannot be null!");
        }
        this.internalEventDrivenJobService = internalEventDrivenJobService;
        if(this.internalEventDrivenJobService == null) {
            throw new IllegalArgumentException("internalEventDrivenJobService cannot be null!");
        }
        this.queueDirectory = queueDirectory;
        if(this.queueDirectory == null) {
            throw new IllegalArgumentException("queueDirectory cannot be null!");
        }

        this.objectMapper = ObjectMapperFactory.newInstance();;
    }

    @PostConstruct
    public void registerJobs() {
        try {
            SearchResults<ScheduledContextRecord> scheduledContextRecords
                = (SearchResults<ScheduledContextRecord>) this.scheduledContextService.findAll();

            for (ScheduledContextRecord scheduledContextRecord : scheduledContextRecords.getResultList()) {

                ContextInstanceRegisterJob job = new ContextInstanceRegisterJob(scheduledContextRecord.getContextName(),
                    scheduledContextRecord.getContext().getTimeWindowStart(), this.scheduledContextService
                    , this.scheduledContextInstanceService, this.schedulerService, this.internalEventDrivenJobService
                    , this.queueDirectory);
                JobDetail jobDetail = this.scheduledJobFactory.createJobDetail
                    (job, ContextInstanceRegisterJob.class, job.getJobName(), "context");

                super.dashboardJobDetailsMap.put(job.getJobName(), jobDetail);
                super.dashboardJobsMap.put(jobDetail.getKey().toString(), job);

            }

            for (JobDetail jobDetail : super.dashboardJobDetailsMap.values()) {
                logger.info(String.format("Registering context instance job[%s]", jobDetail.getKey().getName()));
                this.addJob(jobDetail.getKey().getName());
            }
        }
        catch (Exception ex) {
            // todo need to add some notifications here
            logger.error(String.format("An exception has occurred registering contexts [%s]", ex.getMessage()), ex);
        }
    }

}
