package org.ikasan.job.orchestration.provision.context;

import com.esotericsoftware.minlog.Log;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.register.ContextInstanceEndJob;
import org.ikasan.job.orchestration.context.register.ContextInstanceRegisterJob;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.quartz.AbstractDashboardSchedulerService;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobWrapper;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.ikasan.job.orchestration.context.register.ContextInstanceEndJob.END_JOB_EXTENSION;
import static org.ikasan.job.orchestration.context.util.QuartzTimeWindowChecker.withinOperatingWindow;

public class ContextProvisionServiceImpl extends AbstractDashboardSchedulerService implements ContextProvisionService {

    private static final Logger LOG = LoggerFactory.getLogger(ContextProvisionServiceImpl.class);

    private ScheduledContextService scheduledContextService;
    private ModuleMetaDataService moduleMetadataService;
    private SchedulerJobService schedulerJobService;
    private JobProvisionModuleService jobProvisionModuleRestService;
    private ContextInstanceRegistrationService contextInstanceRegistrationService;
    private ContextProfileService contextProfileService;
    private EmailNotificationDetailsService emailNotificationDetailsService;
    private boolean uploadProvisionJobs;

    public ContextProvisionServiceImpl(Scheduler scheduler,
                                       ScheduledJobFactory scheduledJobFactory,
                                       ScheduledContextService scheduledContextService,
                                       ModuleMetaDataService moduleMetadataService,
                                       SchedulerJobService schedulerJobService,
                                       JobProvisionModuleService jobProvisionModuleRestService,
                                       ContextInstanceRegistrationService contextInstanceRegistrationService,
                                       ContextProfileService contextProfileService,
                                       EmailNotificationDetailsService emailNotificationDetailsService,
                                       boolean uploadProvisionJobs) {

        super(scheduler, scheduledJobFactory);

        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.moduleMetadataService = moduleMetadataService;
        if (this.moduleMetadataService == null) {
            throw new IllegalArgumentException("moduleMetadataService cannot be null!");
        }
        this.schedulerJobService = schedulerJobService;
        if (this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        this.jobProvisionModuleRestService = jobProvisionModuleRestService;
        if (this.jobProvisionModuleRestService == null) {
            throw new IllegalArgumentException("jobProvisionModuleRestService cannot be null!");
        }
        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        if (this.contextInstanceRegistrationService == null) {
            throw new IllegalArgumentException("contextInstanceRegistrationService cannot be null!");
        }
        this.contextProfileService = contextProfileService;
        if (this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }
        this.emailNotificationDetailsService = emailNotificationDetailsService;
        if (this.emailNotificationDetailsService == null) {
            throw new IllegalArgumentException("emailNotificationDetailsService cannot be null!");
        }

        this.uploadProvisionJobs = uploadProvisionJobs;
    }

    @Override
    public void registerJobs() {
        // does nothing here as we just want to add jobs
    }

    public void provisionContext(ContextBundle contextBundle) {
        try {
            // TODO need to expand validate
            this.validate(contextBundle.getContextTemplate(), contextBundle.getSchedulerJobs());
            // delete all the jobs if they exist
            this.deleteAllJobs(contextBundle.getContextTemplate().getName());
            // delete the context profiles
            this.deleteContextProfiles(contextBundle.getContextTemplate().getName());
            // delete the email notification associated to the context
            this.deleteEmailNotificationDetailsByContext(contextBundle.getContextTemplate().getName());
            // set job participates in lock flag on relevant jobs
            this.setJobsParticipateInJobLock(contextBundle.getContextTemplate(), contextBundle.getSchedulerJobs());
            // save the jobs
            this.saveJobs(contextBundle.getSchedulerJobs());
            // saveContext
            this.saveContext(contextBundle.getContextTemplate());
            // provision the context profiles
            if(contextBundle.getContextProfiles() != null &&
                !contextBundle.getContextProfiles().isEmpty()) {
                this.saveContextProfiles(contextBundle.getContextProfiles());
            }
            // provision the email notification associated to the context
            if(contextBundle.getEmailNotificationDetails() != null &&
               !contextBundle.getEmailNotificationDetails().isEmpty()) {
                this.saveEmailNotificationDetails(contextBundle.getEmailNotificationDetails());
            }
            if (this.uploadProvisionJobs) {
                provisionJobs(contextBundle.getSchedulerJobs());
            }
            // register the jobs
            this.registerContext(contextBundle.getContextTemplate());

            if (withinOperatingWindow(contextBundle.getContextTemplate().getTimeWindowStart(), contextBundle.getContextTemplate().getTimeWindowEnd(), new Date())) {
                ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(contextBundle.getContextTemplate().getName());
                if (contextMachine != null) {
                    contextInstanceRegistrationService.deRegister(contextBundle.getContextTemplate().getName());
                }
                // NOTE: this will create a new context machine and instance and initialise it so overwriting existing context machine
                contextInstanceRegistrationService.register(contextBundle.getContextTemplate().getName());
            }
        } catch (Exception e) {
            String message = String.format("Could not upload context and jobs. Error [%s]", e.getMessage());
            LOG.warn(message, e);
            throw new RuntimeException(message);
        }
    }

    private void validate(ContextTemplate contextTemplate, List<SchedulerJob> contextJobs) {
        // todo expand this out
        if (contextTemplate == null) {
            LOG.warn("Context template can not be null");
            throw new RuntimeException("Context template can not be null");
        }
        if (contextJobs == null) {
            LOG.warn("Context jobs can not be null");
            throw new RuntimeException("Context jobs can not be null");
        }
    }

    private void registerContext(ContextTemplate contextTemplate) {
        // overwrites any existing details of registered jobs
        ContextInstanceRegisterJob job = new ContextInstanceRegisterJob(contextTemplate.getName(),
            contextTemplate.getTimeWindowStart(), contextTemplate.getTimezone(), this.contextInstanceRegistrationService);

        ContextInstanceEndJob endJob = new ContextInstanceEndJob(contextTemplate.getName() + END_JOB_EXTENSION,
            contextTemplate.getTimeWindowEnd(), contextTemplate.getTimezone(), this.contextInstanceRegistrationService);

        JobDetail jobDetail = this.scheduledJobFactory.createJobDetail(job, ContextInstanceRegisterJob.class, job.getJobName(), "context");

        JobDetail endJobDetail = this.scheduledJobFactory.createJobDetail(endJob, ContextInstanceEndJob.class, endJob.getJobName(), "context");

        super.dashboardJobDetailsMap.put(job.getJobName(), jobDetail);
        super.dashboardJobDetailsMap.put(endJob.getJobName(), endJobDetail);

        super.dashboardJobsMap.put(jobDetail.getKey().toString(), job);
        super.dashboardJobsMap.put(endJobDetail.getKey().toString(), endJob);
        this.addJob(jobDetail.getKey().getName());
        this.addJob(endJobDetail.getKey().getName());
    }

    private void provisionJobs(List<SchedulerJob> contextJobs) {
        long now = System.currentTimeMillis();
        int jobsSize = contextJobs.size();
        Set<String> uniqueAgentNames = contextJobs.stream().map(SchedulerJob::getAgentName).collect(Collectors.toSet());
        ModuleMetadataSearchResults agents = this.moduleMetadataService
            .find(new ArrayList<>(uniqueAgentNames), ModuleType.SCHEDULER_AGENT, -1, -1);

        List<Exception> exceptions = new ArrayList<>();
        agents.getResultList().forEach(agent -> {
            try {
                SchedulerJobWrapper schedulerJobWrapper = new SchedulerJobWrapperImpl();
                schedulerJobWrapper.setJobs(contextJobs.stream()
                    .filter(schedulerJob -> schedulerJob.getAgentName().equals(agent.getName()))
                    .collect(Collectors.toList()));

                LOG.info(String.format("Attempting to provision %s jobs on agent[%s]", jobsSize, agent.getUrl()));
                this.jobProvisionModuleRestService.provisionJobs(agent.getUrl(), schedulerJobWrapper);
                LOG.info(String.format("Successfully provisioned %s jobs on agent[%s]", jobsSize, agent.getUrl()));
            } catch (Exception e) {
                e.printStackTrace();
                exceptions.add(new RuntimeException(String.format("Agent[%s] Error[%s]", agent.getName(), e.getMessage()), e));
            }
        });
        if (!exceptions.isEmpty()) {
            StringBuffer message = new StringBuffer("\n");
            exceptions.forEach(e -> message.append(e.getMessage()).append("\n"));
            // todo should this throw an exception ?
            Log.warn(message.toString());
        } else {
            LOG.info(String.format("Finished provisioning %s jobs across %s agents. Time taken %s milliseconds.",
                jobsSize, uniqueAgentNames.size(), System.currentTimeMillis() - now));
        }
    }

    private void saveContext(ContextTemplate contextTemplate) {
        // overwrite the existing context service regardless whether it exists
        ScheduledContextRecord scheduledContextRecord = new ScheduledContextRecordImpl();
        scheduledContextRecord.setContextName(contextTemplate.getName());
        scheduledContextRecord.setContext(contextTemplate);
        scheduledContextRecord.setTimestamp(System.currentTimeMillis());

        this.scheduledContextService.save(scheduledContextRecord);
    }

    private void saveJobs(List<SchedulerJob> contextJobs) {
        this.schedulerJobService.save(contextJobs, "system");
    }

    private void setJobsParticipateInJobLock(ContextTemplate contextTemplate, List<SchedulerJob> contextJobs) {
        List<JobLock> jobLocks = contextTemplate.getAllNestedJobLocks();
        Set<String> jobInJobLocks = new HashSet<>();
        jobLocks.forEach(jobLock -> {
            jobLock.getJobs().values().forEach(jobs -> {
                jobs.forEach(job -> jobInJobLocks.add(job.getIdentifier()));
            });
        });

        contextJobs.forEach(job -> {
            if(job instanceof InternalEventDrivenJob) {
                ((InternalEventDrivenJob)job).setParticipatesInLock(jobInJobLocks.contains(job.getIdentifier()));
            }
        });
    }

    private void deleteAllJobs(String contextName) {
        this.schedulerJobService.deleteByContextName(contextName);
    }

    private void saveContextProfiles(List<ContextProfileRecord> contextProfileRecords) {
        this.contextProfileService.save(contextProfileRecords);
    }

    private void deleteContextProfiles(String contextName) {
        this.contextProfileService.deleteByContextName(contextName);
    }

    private void saveEmailNotificationDetails(List<EmailNotificationDetails> getEmailNotificationDetails) {
        this.emailNotificationDetailsService.saveEmailNotificationDetails(getEmailNotificationDetails);
    }

    private void deleteEmailNotificationDetailsByContext(String contextName) {
        this.emailNotificationDetailsService.deleteByContextName(contextName);
    }
}
