package org.ikasan.job.orchestration.provision.job;

import org.ikasan.job.orchestration.model.job.SchedulerJobWrapperImpl;
import org.ikasan.job.orchestration.rest.JobProvisionModuleRestServiceImpl;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JobProvisionServiceImpl implements JobProvisionService {

    Logger logger = LoggerFactory.getLogger(JobProvisionServiceImpl.class);

    private SchedulerJobService schedulerJobService;
    private JobProvisionModuleRestServiceImpl jobProvisionModuleRestService;
    private ModuleMetaDataService moduleMetaDataService;

    /**
     * Constructor
     *
     * @param schedulerJobService
     * @param moduleMetaDataService
     * @param jobProvisionModuleRestService
     */
    public JobProvisionServiceImpl(SchedulerJobService schedulerJobService, ModuleMetaDataService moduleMetaDataService,
                                   JobProvisionModuleRestServiceImpl jobProvisionModuleRestService) {
        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }

        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }

        this.jobProvisionModuleRestService = jobProvisionModuleRestService;
        if(this.jobProvisionModuleRestService == null) {
            throw new IllegalArgumentException("jobProvisionModuleRestService cannot be null!");
        }
    }

    public void provisionJobs(List<SchedulerJob> jobs) {
        long now = System.currentTimeMillis();
        List<String> uniqueAgentNames = this.getUniqueAgentNames(jobs);
        logger.info(String.format("Provisioning %s jobs across %s agents", jobs.size(), uniqueAgentNames.size()));
        ModuleMetadataSearchResults agents = this.moduleMetaDataService
            .find(uniqueAgentNames, ModuleType.SCHEDULER_AGENT, -1, -1);

        // As it is possible to provision multiple agents as part of the job
        // provisioning process, we will collect exceptions and report issues
        // once attempts to provision all agents are complete.
        List<JobProvisionException> exceptions = new ArrayList<>();

        agents.getResultList().forEach(agent -> {
            try {
                SchedulerJobWrapper schedulerJobWrapper = new SchedulerJobWrapperImpl();
                schedulerJobWrapper.setJobs(getJobsForAgent(agent.getName(), jobs));

                this.jobProvisionModuleRestService.provisionJobs(agent.getUrl(), schedulerJobWrapper);

                persistJobs(agent.getName(), jobs);
            }
            catch (JobProvisionException e) {
                e.printStackTrace();
                exceptions.add(e);
            }
            catch (Exception e) {
                e.printStackTrace();
                exceptions.add(new JobProvisionException(String.format("Agent[%s] Error[%s]", agent.getName(), e.getMessage()),e));
            }
        });

        if(!exceptions.isEmpty()) {
            StringBuffer message = new StringBuffer("\n");
            exceptions.forEach(e -> message.append(e.getMessage()).append("\n"));

            throw new JobProvisionException(message.toString());
        }

        logger.info(String.format("Finished provisioning %s jobs across %s agents. Time taken %s milliseconds.", jobs.size(), uniqueAgentNames.size(), System.currentTimeMillis()-now));
    }

    private List<SchedulerJob> getJobsForAgent(String agentName, List<SchedulerJob> jobs) {
        return jobs.stream()
            .filter(schedulerJob -> schedulerJob.getAgentName().equals(agentName))
            .collect(Collectors.toList());
    }

    private void persistJobs(String agentName, List<SchedulerJob> jobs) {
        this.schedulerJobService.deleteByAgentName(agentName);

        List<InternalEventDrivenJob> internalEventDrivenJobs = new ArrayList<>();
        List<FileEventDrivenJob> fileEventDrivenJobs = new ArrayList<>();
        List<QuartzScheduleDrivenJob> quartzScheduleDrivenJobs = new ArrayList<>();
        jobs.forEach(job ->{
            if(job instanceof InternalEventDrivenJob) {
                internalEventDrivenJobs.add((InternalEventDrivenJob)job);
            }
            else if(job instanceof FileEventDrivenJob) {
                fileEventDrivenJobs.add((FileEventDrivenJob)job);
            }
            else if(job instanceof QuartzScheduleDrivenJob) {
                quartzScheduleDrivenJobs.add((QuartzScheduleDrivenJob)job);
            }
        });

        if(!internalEventDrivenJobs.isEmpty()) {
            this.schedulerJobService.saveInternalEventDrivenJobs(internalEventDrivenJobs);
        }

        if(!fileEventDrivenJobs.isEmpty()) {
            this.schedulerJobService.saveFileEventDrivenJobs(fileEventDrivenJobs);
        }

        if(!quartzScheduleDrivenJobs.isEmpty()) {
            this.schedulerJobService.saveQuartzScheduledJobs(quartzScheduleDrivenJobs);
        }
    }

    private List<String> getUniqueAgentNames(List<SchedulerJob> jobs) {
        ArrayList<String> uniqueAgentNames = new ArrayList();
        jobs.forEach(schedulerJob -> {
            if(!uniqueAgentNames.contains(schedulerJob.getAgentName())) {
                uniqueAgentNames.add(schedulerJob.getAgentName());
            }
        });

        return uniqueAgentNames;
    }
}
