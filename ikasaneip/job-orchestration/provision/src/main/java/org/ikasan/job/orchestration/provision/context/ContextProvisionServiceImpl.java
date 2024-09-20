package org.ikasan.job.orchestration.provision.context;

import com.esotericsoftware.minlog.Log;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.context.util.ContextDurationUtils;
import org.ikasan.job.orchestration.context.util.CronUtils;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.security.service.SecurityService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.context.model.ContextBundle;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobWrapper;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.ikasan.job.orchestration.context.util.QuartzTimeWindowChecker.withinOperatingWindow;

public class ContextProvisionServiceImpl implements ContextProvisionService {

    private static final Logger LOG = LoggerFactory.getLogger(ContextProvisionServiceImpl.class);

    private final ScheduledContextService scheduledContextService;
    private final ModuleMetaDataService moduleMetadataService;
    private final SchedulerJobService schedulerJobService;
    private final JobProvisionModuleService jobProvisionModuleRestService;
    private final ContextInstanceRegistrationService contextInstanceRegistrationService;
    private final ContextProfileService contextProfileService;
    private final EmailNotificationDetailsService emailNotificationDetailsService;
    private final EmailNotificationContextService emailNotificationContextService;
    private final boolean uploadProvisionJobs;
    private final ContextInstanceSchedulerService contextInstanceSchedulerService;
    private int jobPlanIntervalMultiple;
    private SecurityService securityService;

    public ContextProvisionServiceImpl(ScheduledContextService scheduledContextService,
                                       ModuleMetaDataService moduleMetadataService,
                                       SchedulerJobService schedulerJobService,
                                       JobProvisionModuleService jobProvisionModuleRestService,
                                       ContextInstanceRegistrationService contextInstanceRegistrationService,
                                       ContextProfileService contextProfileService,
                                       EmailNotificationDetailsService emailNotificationDetailsService,
                                       EmailNotificationContextService emailNotificationContextService,
                                       boolean uploadProvisionJobs,
                                       ContextInstanceSchedulerService contextInstanceSchedulerService,
                                       int jobPlanIntervalMultiple,
                                       SecurityService securityService) {

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

        this.emailNotificationContextService = emailNotificationContextService;
        if (this.emailNotificationContextService == null) {
            throw new IllegalArgumentException("emailNotificationContextService cannot be null!");
        }

        this.uploadProvisionJobs = uploadProvisionJobs;

        this.contextInstanceSchedulerService = contextInstanceSchedulerService;
        if (this.contextInstanceSchedulerService == null) {
            throw new IllegalArgumentException("contextInstanceSchedulerService cannot be null!");
        }

        this.jobPlanIntervalMultiple = jobPlanIntervalMultiple;

        this.securityService = securityService;
        if (this.securityService == null) {
            throw new IllegalArgumentException("securityService cannot be null!");
        }
    }

    /**
     * Called when a plan is imported and requested to be provisioned on the agents.
     * @param contextBundle to provision
     */
    public void provisionContext(ContextBundle contextBundle) {
        try {
            // TODO need to expand validate
            final String jobPlanName = contextBundle.getContextTemplate().getName();
            this.validate(contextBundle.getContextTemplate(), contextBundle.getSchedulerJobs());
            // delete all the jobs if they exist
            this.deleteAllJobs(jobPlanName);
            // delete the context profiles
            this.deleteContextProfiles(jobPlanName);
            // delete the email notification associated to the context
            this.deleteEmailNotificationDetailsByContext(jobPlanName);
            this.deleteEmailNotificationContextByContext(jobPlanName);
            // delete any running context instances since this may be a re-import over an existing context.
            contextInstanceRegistrationService.deRegisterByName(jobPlanName);

            // set job participates in lock flag on relevant jobs
            this.setJobsParticipateInJobLock(contextBundle.getContextTemplate(), contextBundle.getSchedulerJobs());

            // Helper method to populate the contextNames collection on each of the scheduler jobs.
            ContextHelper.populateChildContextNamesOnSchedulerJobs(contextBundle.getContextTemplate(),
                contextBundle.getSchedulerJobs());

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
            if(contextBundle.getEmailNotificationContext() != null) {
                this.saveEmailNotificationContext(contextBundle.getEmailNotificationContext());
            }
            if (this.uploadProvisionJobs) {
                provisionJobs(contextBundle.getSchedulerJobs());
            }

            if(!contextBundle.getRoles().isEmpty()) {
                this.securityService.setJobPlanRoles(contextBundle.getContextTemplate().getName()
                    , contextBundle.getRoles());
            }

            // Even though the next start job may be tomorrow, the trigger must be setup
            contextInstanceSchedulerService.registerStartJobAndTrigger(jobPlanName, contextBundle.getContextTemplate().getTimeWindowStart(),
                contextBundle.getContextTemplate().getTimezone());

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

        if(!CronUtils.isDurationGreaterThanNextFireTime(contextTemplate.getTimeWindowStart(),
            contextTemplate.getContextTtlMilliseconds(), this.jobPlanIntervalMultiple)) {
            LOG.warn("The job plan cron expression and duration are not within an acceptable tolerance" +
                ". The job plan is therefore considered invalid!");
            throw new RuntimeException("The job plan cron expression and duration are not within an acceptable tolerance" +
                ". The job plan is therefore considered invalid!");
        }
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
                    .filter(schedulerJob -> schedulerJob.getAgentName().equals(agent.getName()) &&
                        !(schedulerJob instanceof GlobalEventJob)) // Do not provision Global Events as this is not managed by the agent, but through the ContextMachine
                    .collect(Collectors.toList()));

                LOG.info(String.format("Attempting to provision %s jobs on agent[%s]", schedulerJobWrapper.getJobs().size(), agent.getUrl()));
                LOG.info(String.format("Skipping %s global event jobs for the agent[%s] as global event jobs are not required for the agent.",
                    jobsSize - schedulerJobWrapper.getJobs().size(), agent.getUrl()));
                this.jobProvisionModuleRestService.provisionJobs(agent.getUrl(), schedulerJobWrapper);
                LOG.info(String.format("Successfully provisioned %s jobs on agent[%s]", schedulerJobWrapper.getJobs().size(), agent.getUrl()));
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
        jobLocks.forEach(jobLock -> jobLock.getJobs().values()
            .forEach(jobs -> jobs
                .forEach(job -> jobInJobLocks.add(job.getIdentifier()))));

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

    private void saveEmailNotificationContext(EmailNotificationContext emailNotificationContext) {
        this.emailNotificationContextService.saveEmailNotificationContext(emailNotificationContext);
    }

    private void deleteEmailNotificationContextByContext(String contextName) {
        this.emailNotificationContextService.deleteByContextName(contextName);
    }
}
