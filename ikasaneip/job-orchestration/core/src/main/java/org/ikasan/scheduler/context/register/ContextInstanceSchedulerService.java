package org.ikasan.scheduler.context.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.quartz.AbstractDashboardSchedulerService;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.scheduler.core.spec.Context;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.List;

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


    public ContextInstanceSchedulerService(Scheduler scheduler, ScheduledJobFactory scheduledJobFactory
        , ScheduledContextService scheduledContextService, ScheduledContextInstanceService scheduledContextInstanceService
        , SchedulerService schedulerService) {
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

        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void registerJobs() {
        try {
            List<ScheduledContextRecord> scheduledContextRecords
                = (List<ScheduledContextRecord>) this.scheduledContextService.findAll();

            for (ScheduledContextRecord scheduledContextRecord : scheduledContextRecords) {

                Context context = this.objectMapper.readValue(scheduledContextRecord.getContext(), Context.class);
                ContextInstanceRegisterJob job = new ContextInstanceRegisterJob(scheduledContextRecord.getContextName(),
                    context.getTimeWindowStart(), this.scheduledContextService, this.scheduledContextInstanceService, this.schedulerService);
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
            logger.error(String.format("An exception has occurred registering contexts [%s]", ex.getMessage()), ex);
        }
    }

}
