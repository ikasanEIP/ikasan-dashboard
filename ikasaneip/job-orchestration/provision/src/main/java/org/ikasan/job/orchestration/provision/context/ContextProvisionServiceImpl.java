package org.ikasan.job.orchestration.provision.context;

import com.esotericsoftware.minlog.Log;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.context.util.CronUtils;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.job.orchestration.provision.job.JobProvisionException;
import org.ikasan.job.orchestration.provision.job.JobProvisionLockException;
import org.ikasan.job.orchestration.rest.client.dto.ErrorDto;
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
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.ikasan.scheduled.instance.model.SolrContextInstanceSearchFilterImpl;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

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
    private final ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;
    private final ScheduledContextInstanceService scheduledContextInstanceService;
    private int jobPlanIntervalMultiple;
    private SecurityService securityService;
    private final ConcurrentHashMap<String, ReentrantLock> agentLocks;

    /**
     * Constructor
     *
     * @param scheduledContextService The service for managing scheduled contexts
     * @param moduleMetadataService The service for managing module metadata
     * @param schedulerJobService The service for managing scheduler jobs
     * @param jobProvisionModuleRestService The service for managing job provision modules
     * @param contextInstanceRegistrationService The service for managing context instance registration
     * @param contextProfileService The service for managing context profiles
     * @param emailNotificationDetailsService The service for managing email notification details
     * @param emailNotificationContextService The service for managing email notification contexts
     * @param uploadProvisionJobs Flag indicating whether provision jobs should be uploaded
     * @param contextInstanceSchedulerService The service for context instance scheduling
     * @param scheduledContextInstanceService The service for managing scheduled context instances
     * @param jobPlanIntervalMultiple The multiple for the job plan interval
     * @param securityService The security service for managing security-related operations
     */
    public ContextProvisionServiceImpl(ScheduledContextService scheduledContextService,
                                       ModuleMetaDataService moduleMetadataService,
                                       SchedulerJobService schedulerJobService,
                                       JobProvisionModuleService jobProvisionModuleRestService,
                                       ContextInstanceRegistrationService contextInstanceRegistrationService,
                                       ContextProfileService contextProfileService,
                                       EmailNotificationDetailsService emailNotificationDetailsService,
                                       EmailNotificationContextService emailNotificationContextService,
                                       boolean uploadProvisionJobs,
                                       ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService,
                                       ScheduledContextInstanceService scheduledContextInstanceService,
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

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }

        this.jobPlanIntervalMultiple = jobPlanIntervalMultiple;

        this.securityService = securityService;
        if (this.securityService == null) {
            throw new IllegalArgumentException("securityService cannot be null!");
        }

        this.agentLocks = new ConcurrentHashMap<>();
    }

    /**
     * Called when a plan is imported and requested to be provisioned on the agents.
     * @param contextBundle to provision
     */
    public void provisionContext(ContextBundle contextBundle) {

        synchronized(this) {
            List<String> agents = ContextHelper.getAllAgents(contextBundle.getContextTemplate());
            agents.forEach(agent -> {
                if (!agentLocks.containsKey(agent)) {
                    ReentrantLock agentLock = new ReentrantLock();
                    agentLock.lock();
                    agentLocks.put(agent, agentLock);
                } else if (agentLocks.containsKey(agent) && !agentLocks.get(agent).isLocked()) {
                    agentLocks.get(agent).lock();
                } else {
                    throw new JobProvisionLockException(String.format("Cannot provision job plan[%s] as another process has " +
                        "locked the agent[%s] for modification.", contextBundle.getContextTemplate().getName(), agent));
                }
            });
        }

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

            if(!contextBundle.getContextTemplate().isDelayAgentSynchronisationUntilNextInstance()) {
                // delete any running context instances since this may be a re-import over an existing context.
                contextInstanceRegistrationService.deRegisterByName(jobPlanName, this.contextInstanceSchedulerService);
            }

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

            if (this.uploadProvisionJobs && !contextBundle.getContextTemplate().isDelayAgentSynchronisationUntilNextInstance()) {
                provisionJobs(contextBundle.getSchedulerJobs(), contextBundle.getContextTemplate());
            }

            if(contextBundle.getContextTemplate().isDelayAgentSynchronisationUntilNextInstance()) {
                // Any prepared instance have become redundant and will be recreated to
                // reflect the newly provisioned context template (job plan) in the registerStartJobAndTrigger
                // method below.
                this.removePrepared(contextBundle.getContextTemplate().getName());
            }

            if(!contextBundle.getRoles().isEmpty()) {
                this.securityService.setJobPlanRoles(contextBundle.getContextTemplate().getName()
                    , contextBundle.getRoles());
            }

            // Even though the next start job may be tomorrow, the trigger must be setup
            contextInstanceSchedulerService.registerStartJobAndTrigger(contextBundle.getContextTemplate(),
                contextBundle.getContextTemplate().getTimezone());

        }
        catch (JobProvisionLockException e) {
            throw e;
        }
        catch (Exception e) {
            String message = String.format("Could not upload context and jobs. Error [%s]", e.getMessage());
            LOG.error(message, e);
            throw new JobProvisionException(message, e);
        }
        finally {
            synchronized (this) {
                List<String> agents = ContextHelper.getAllAgents(contextBundle.getContextTemplate());
                agents.forEach(agent -> agentLocks.get(agent).unlock());
            }
        }
    }

    /**
     * Validates the provided context template and list of scheduler jobs.
     *
     * @param contextTemplate The context template to validate
     * @param contextJobs The list of scheduler jobs to validate
     */
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


    /**
     * Removes all prepared context instances with the given contextName.
     * This method searches for prepared context instances with the provided contextName,
     * and then deletes them by their contextInstanceId.
     *
     * @param contextName the name of the context for which prepared instances should be removed
     */
    protected void removePrepared(String contextName) {
        ContextInstanceSearchFilter filter = new SolrContextInstanceSearchFilterImpl();
        filter.setStatus(InstanceStatus.PREPARED.name());
        filter.setContextInstanceNames(Collections.singletonList(contextName));

        SearchResults<ScheduledContextInstanceRecord> results = this.scheduledContextInstanceService
            .getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        results.getResultList().forEach(scheduledContextInstanceRecord ->
            this.scheduledContextInstanceService.deleteById(scheduledContextInstanceRecord.getContextInstanceId()));
    }


    /**
     * Provision jobs on specified agent URL based on the context jobs and template.
     *
     * @param contextJobs List of SchedulerJob objects to be provisioned
     * @param contextTemplate ContextTemplate object representing the context
     */
    private void provisionJobs(List<SchedulerJob> contextJobs, ContextTemplate contextTemplate) {
        long now = System.currentTimeMillis();
        int jobsSize = contextJobs.size();
        Set<String> uniqueAgentNames = contextJobs.stream().map(SchedulerJob::getAgentName).collect(Collectors.toSet());
        ModuleMetadataSearchResults agents = this.moduleMetadataService
            .find(new ArrayList<>(uniqueAgentNames), ModuleType.SCHEDULER_AGENT, -1, -1);

        List<String> jobIdentifiersInJobPlan = ContextHelper.getAllJobs(contextTemplate).stream()
            .map(job -> job.getIdentifier())
            .collect(Collectors.toList());

        List<Exception> exceptions = new ArrayList<>();
        agents.getResultList().forEach(agent -> {
            try {
                SchedulerJobWrapper schedulerJobWrapper = new SchedulerJobWrapperImpl();
                schedulerJobWrapper.setJobs(contextJobs.stream()
                    .filter(schedulerJob -> schedulerJob.getAgentName().equals(agent.getName()) &&
                        !(schedulerJob instanceof GlobalEventJob) && // Do not provision Global Events as this is not managed by the agent, but through the ContextMachine
                        !(schedulerJob instanceof LocalEventJob) && // Do not provision Local Events as this is not managed by the agent, but through the ContextMachine
                        !(schedulerJob instanceof ContextStartJob) && // Do not provision ContextStartJobs as they is not managed by the agent, but through the ContextMachine
                        !(schedulerJob instanceof ContextTerminalJob) && // Do not provision ContextTerminalJobs as they is not managed by the agent, but through the ContextMachine
                        !(schedulerJob.isTemplateJob() != null && schedulerJob.isTemplateJob() == true) && // Do not provision template jobs as they are not managed by the agent, but through the ContextMachine
                        jobIdentifiersInJobPlan.contains(schedulerJob.getIdentifier())) // We only provision jobs in the job plan
                    .collect(Collectors.toList()));

                LOG.info(String.format("Attempting to provision %s jobs on agent[%s]", schedulerJobWrapper.getJobs().size(), agent.getUrl()));
                LOG.info(String.format("Skipping %s global event jobs for the agent[%s] as global event jobs are not required for the agent.",
                    jobsSize - schedulerJobWrapper.getJobs().size(), agent.getUrl()));
                this.jobProvisionModuleRestService.provisionJobs(agent.getUrl(), schedulerJobWrapper);
                LOG.info(String.format("Successfully provisioned %s jobs on agent[%s]", schedulerJobWrapper.getJobs().size(), agent.getUrl()));
            }
            catch (JobProvisionLockException e) {
                LOG.error(String.format("Agent[%s] Error[%s]", agent.getName(), e.getMessage()), e);
                throw e;
            }
            catch (Exception e) {
                LOG.error(String.format("Agent[%s] Error[%s]", agent.getName(), e.getMessage()), e);
                this.determineIfLockExceptionAndRaiseAccordingly(e);
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

    /**
     * Determines if the given exception is a lock-related exception. If the cause of the exception is an instance
     * of HttpClientErrorException, it attempts to read the response body as an ErrorDto to check if the error code
     * corresponds to a lock acquisition error. If a lock acquisition error is detected, a JobProvisionLockException
     * is thrown with the error message and the original exception as the cause.
     *
     * @param e The exception to check for lock-related issues.
     */
    private void determineIfLockExceptionAndRaiseAccordingly(Exception e) {
        if(e.getCause() != null && e.getCause() instanceof HttpClientErrorException) {
            try {
                ErrorDto errorDto = new ObjectMapper().readValue(((HttpClientErrorException) e.getCause()).getResponseBodyAsString(), ErrorDto.class);
                if(errorDto.getErrorCode() != null && errorDto.getErrorCode().equals("LOCK_ACQUISITION_ERROR")) {
                    throw new JobProvisionLockException(errorDto.getErrorMessage(), e);
                }
            } catch (JsonProcessingException ex) {
                // Ignore as exception could not be determined if it was a lock related exception!
            }
        }
    }

    /**
     * Saves the provided context information into the system.
     * If the context requires agent synchronisation on the next instance, it is marked so.
     * The context information is then saved as a ScheduledContextRecord.
     *
     * @param contextTemplate the template representing the context to be saved
     */
    private void saveContext(ContextTemplate contextTemplate) {
        // As the context has been provisioned, we need to mark the plan
        // as requiring synchronisation with the agent next time the job
        // plan starts.
        if(contextTemplate.isDelayAgentSynchronisationUntilNextInstance()) {
            contextTemplate.setRequiresAgentSynchronisation(true);
        }
        // overwrite the existing context regardless whether it exists
        ScheduledContextRecord scheduledContextRecord = new ScheduledContextRecordImpl();
        scheduledContextRecord.setContextName(contextTemplate.getName());
        scheduledContextRecord.setContext(contextTemplate);
        scheduledContextRecord.setTimestamp(System.currentTimeMillis());

        this.scheduledContextService.save(scheduledContextRecord);
    }

    /**
     * Saves the provided list of SchedulerJob objects into the system.
     * The jobs are saved using the SchedulerJobService with the specified username "system".
     *
     * @param contextJobs the list of SchedulerJob objects to be saved
     */
    private void saveJobs(List<SchedulerJob> contextJobs) {
        this.schedulerJobService.save(contextJobs, "system");
    }

    /**
     * Sets the flag indicating whether each job in the given list of SchedulerJobs participates in a job lock.
     *
     * @param contextTemplate the context template containing job locks
     * @param contextJobs the list of SchedulerJobs to update participation in job lock flag
     */
    private void setJobsParticipateInJobLock(ContextTemplate contextTemplate, List<SchedulerJob> contextJobs) {
        List<JobLock> jobLocks = contextTemplate.getAllNestedJobLocks();
        Set<String> jobInJobLocks = new HashSet<>();
        jobLocks.forEach(jobLock -> {
            if(jobLock.getJobs() != null) {
                jobLock.getJobs().values()
                    .forEach(jobs -> jobs
                        .forEach(job -> jobInJobLocks.add(job.getIdentifier())));
            }
        });

        contextJobs.forEach(job -> {
            if(job instanceof InternalEventDrivenJob) {
                ((InternalEventDrivenJob)job).setParticipatesInLock(jobInJobLocks.contains(job.getIdentifier()));
            }
        });
    }

    /**
     * Deletes all jobs associated with the provided contextName.
     *
     * @param contextName the name of the context for which all jobs should be deleted
     */
    private void deleteAllJobs(String contextName) {
        this.schedulerJobService.deleteByContextName(contextName);
    }

    /**
     * Saves the provided list of context profile records into the system.
     * This method delegates the saving operation to the context profile service.
     *
     * @param contextProfileRecords the list of ContextProfileRecord objects to be saved
     */
    private void saveContextProfiles(List<ContextProfileRecord> contextProfileRecords) {
        this.contextProfileService.save(contextProfileRecords);
    }

    /**
     * Deletes all context profiles associated with the provided context name.
     *
     * @param contextName the name of the context for which context profiles should be deleted
     */
    private void deleteContextProfiles(String contextName) {
        this.contextProfileService.deleteByContextName(contextName);
    }

    /**
     * Saves the email notification details in the system.
     *
     * @param getEmailNotificationDetails the list of EmailNotificationDetails to be saved
     */
    private void saveEmailNotificationDetails(List<EmailNotificationDetails> getEmailNotificationDetails) {
        this.emailNotificationDetailsService.saveEmailNotificationDetails(getEmailNotificationDetails);
    }

    /**
     * Deletes email notification details associated with the specified context name.
     *
     * @param contextName the name of the context for which email notification details should be deleted
     */
    private void deleteEmailNotificationDetailsByContext(String contextName) {
        this.emailNotificationDetailsService.deleteByContextName(contextName);
    }

    /**
     * Saves the email notification context by delegating the operation to the EmailNotificationContextService.
     *
     * @param emailNotificationContext the email notification context to be saved
     */
    private void saveEmailNotificationContext(EmailNotificationContext emailNotificationContext) {
        this.emailNotificationContextService.saveEmailNotificationContext(emailNotificationContext);
    }

    /**
     * Deletes the email notification context by the provided context name.
     *
     * @param contextName the name of the context for which the email notification context should be deleted
     */
    private void deleteEmailNotificationContextByContext(String contextName) {
        this.emailNotificationContextService.deleteByContextName(contextName);
    }
}
