package org.ikasan.job.orchestration.provision.job;

import org.ikasan.job.orchestration.model.context.ContextParameterImpl;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class JobProvisionServiceImpl implements JobProvisionService {

    Logger logger = LoggerFactory.getLogger(JobProvisionServiceImpl.class);
    private SchedulerJobService schedulerJobService;
    private JobProvisionModuleService jobProvisionModuleRestService;
    private ModuleMetaDataService moduleMetaDataService;

    /**
     * Constructor
     *
     * @param schedulerJobService
     * @param moduleMetaDataService
     * @param jobProvisionModuleService
     */
    public JobProvisionServiceImpl(SchedulerJobService schedulerJobService, ModuleMetaDataService moduleMetaDataService,
                                   JobProvisionModuleService jobProvisionModuleService) {
        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }

        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }

        this.jobProvisionModuleRestService = jobProvisionModuleService;
        if(this.jobProvisionModuleRestService == null) {
            throw new IllegalArgumentException("jobProvisionModuleRestService cannot be null!");
        }
    }

    @Override
    public void provisionJobs(List<SchedulerJob> jobs, String actor) {
        long now = System.currentTimeMillis();
        List<String> uniqueAgentNames = this.getUniqueAgentNames(jobs);
        logger.info(String.format("Provisioning %s jobs across %s agents", jobs.size(), uniqueAgentNames.size()));
        ModuleMetadataSearchResults agents = this.moduleMetaDataService
            .find(uniqueAgentNames, ModuleType.SCHEDULER_AGENT, -1, -1);

        AtomicBoolean containsGlobalEvents = new AtomicBoolean(false);
        jobs.forEach(job -> {
            if(job instanceof GlobalEventJob) {
                containsGlobalEvents.set(true);
            }
        });

        int global = 0;
        if(containsGlobalEvents.get()) {
            global++;
        }

        if(uniqueAgentNames.size() != agents.getResultList().size() + global) {
            StringBuffer missingAgents = new StringBuffer();

            uniqueAgentNames.forEach(agentName -> {
                if(agents.getResultList().stream()
                    .filter(moduleMetaData -> moduleMetaData.getName().equals(agentName))
                    .collect(Collectors.toList()).size() == 0) {
                    missingAgents.append(agentName).append(", ");
                }
            });

            String missingAgentFinal = missingAgents.toString().trim();
            if(missingAgentFinal.length() > 0 && missingAgentFinal.endsWith(",")) {
                missingAgentFinal = missingAgentFinal.substring(0, missingAgentFinal.length()-1);
            }

            throw new JobProvisionException(String.format("An attempt to provision jobs has failed. The following unknown agents were encountered - %s", missingAgentFinal));
        }

        // As it is possible to provision multiple agents as part of the job
        // provisioning process, we will collect exceptions and report issues
        // once attempts to provision all agents are complete.
        List<JobProvisionException> exceptions = new ArrayList<>();

        agents.getResultList().forEach(agent -> {
            try {
                // Do not provision Global Events as this is not managed by the agent, but through the ContextMachine
                List<SchedulerJob> agentJobs = new ArrayList<>();
                jobs.forEach(schedulerJob -> {
                    if (!(schedulerJob instanceof GlobalEventJob)) {
                        agentJobs.add(schedulerJob);
                    }
                });

                SchedulerJobWrapper schedulerJobWrapper = new SchedulerJobWrapperImpl();
                schedulerJobWrapper.setJobs(getJobsForAgent(agent.getName(), agentJobs));

                logger.info(String.format("Attempting to provision %s jobs on agent[%s]", agentJobs.size(), agent.getUrl()));
                logger.info(String.format("Skipping %s global event jobs for the agent[%s] as global event jobs are not required for the agent.",
                    jobs.size() - agentJobs.size(), agent.getUrl()));
                this.jobProvisionModuleRestService.provisionJobs(agent.getUrl(), schedulerJobWrapper);
                persistJobs(jobs, actor); // Make sure the Global Events are persist
                logger.info(String.format("Successfully provisioned %s jobs on agent[%s]", agentJobs.size(), agent.getUrl()));
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

    @Override
    public void removeJobs(String contextName) {
        long now = System.currentTimeMillis();
        SearchResults<SchedulerJobRecord> searchResults = this.schedulerJobService.findByContext(contextName, -1, -1);

        List<SchedulerJob> schedulerJobs = new ArrayList<>();

        searchResults.getResultList().forEach(schedulerJobRecord -> schedulerJobs.add(schedulerJobRecord.getJob()));

        List<String> uniqueAgentNames = this.getUniqueAgentNames(schedulerJobs);

        List<ModuleMetaData> agents;

        if(uniqueAgentNames.size() > 0) {
            agents = this.moduleMetaDataService
                .find(uniqueAgentNames, ModuleType.SCHEDULER_AGENT, -1, -1).getResultList();
        }
        else {
            agents = new ArrayList<>();
        }

        // As it is possible to provision multiple agents as part of the job
        // provisioning process, we will collect exceptions and report issues
        // once attempts to provision all agents are complete.
        List<JobProvisionException> exceptions = new ArrayList<>();

        agents.forEach(agent -> {
            try {
                logger.info(String.format("Attempting to remove jobs for context[%s] jobs on agent[%s]", contextName, agent.getUrl()));
                this.jobProvisionModuleRestService.removeJobsForContext(agent.getUrl(), contextName);
                logger.info(String.format("Successfully removed jobs for context[%s] jobs on agent[%s]", contextName, agent.getUrl()));
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

        logger.info(String.format("Finished removing jobs for %s across %s agents. Time taken %s milliseconds."
            , contextName, uniqueAgentNames.size(), System.currentTimeMillis()-now));
    }

    private List<SchedulerJob> getJobsForAgent(String agentName, List<SchedulerJob> jobs) {
        return this.convertJobs(jobs.stream()
            .filter(schedulerJob -> schedulerJob.getAgentName().equals(agentName))
            .collect(Collectors.toList()));
    }

    private List<SchedulerJob> convertJobs(List<SchedulerJob> jobs) {
        return jobs.stream()
            .map(job -> {
                if (job instanceof InternalEventDrivenJob) {
                    InternalEventDrivenJobImpl internalEventDrivenJob = new InternalEventDrivenJobImpl();
                    internalEventDrivenJob.setIdentifier(job.getIdentifier());
                    internalEventDrivenJob.setCommandLine(((InternalEventDrivenJob) job).getCommandLine());
                    internalEventDrivenJob.setContextParameters(((InternalEventDrivenJob) job).getContextParameters()
                        .stream()
                        .map(p -> {
                            ContextParameterImpl contextParameter = new ContextParameterImpl();
                            contextParameter.setName(p.getName());
                            contextParameter.setDefaultValue(p.getDefaultValue());

                            return contextParameter;
                        }).collect(Collectors.toList()));
                    internalEventDrivenJob.setDaysOfWeekToRun(((InternalEventDrivenJob) job).getDaysOfWeekToRun());
                    internalEventDrivenJob.setMaxExecutionTime(((InternalEventDrivenJob) job).getMaxExecutionTime());
                    internalEventDrivenJob.setMinExecutionTime(((InternalEventDrivenJob) job).getMinExecutionTime());
                    internalEventDrivenJob.setSuccessfulReturnCodes(((InternalEventDrivenJob) job).getSuccessfulReturnCodes());
                    internalEventDrivenJob.setWorkingDirectory(((InternalEventDrivenJob) job).getWorkingDirectory());
                    internalEventDrivenJob.setAgentName(job.getAgentName());
                    internalEventDrivenJob.setChildContextNames(job.getChildContextNames());
                    internalEventDrivenJob.setContextName(job.getContextName());
                    internalEventDrivenJob.setChildContextNames(job.getChildContextNames());
                    internalEventDrivenJob.setStartupControlType(job.getStartupControlType());
                    internalEventDrivenJob.setJobName(job.getJobName());
                    internalEventDrivenJob.setJobDescription(job.getJobDescription());
                    internalEventDrivenJob.setTargetResidingContextOnly(((InternalEventDrivenJob) job).isTargetResidingContextOnly());

                    return internalEventDrivenJob;
                } else if (job instanceof FileEventDrivenJob) {
                    FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
                    fileEventDrivenJob.setContextName(job.getContextName());
                    fileEventDrivenJob.setDirectoryDepth(((FileEventDrivenJob) job).getDirectoryDepth());
                    fileEventDrivenJob.setEncoding(((FileEventDrivenJob) job).getEncoding());
                    fileEventDrivenJob.setFilenames(((FileEventDrivenJob) job).getFilenames());
                    fileEventDrivenJob.setFilePath(((FileEventDrivenJob) job).getFilePath());
                    fileEventDrivenJob.setIgnoreFileRenameWhilstScanning(((FileEventDrivenJob) job).isIgnoreFileRenameWhilstScanning());
                    fileEventDrivenJob.setIncludeHeader(((FileEventDrivenJob) job).isIncludeHeader());
                    fileEventDrivenJob.setIncludeTrailer(((FileEventDrivenJob) job).isIncludeTrailer());
                    fileEventDrivenJob.setLogMatchedFilenames(((FileEventDrivenJob) job).isLogMatchedFilenames());
                    fileEventDrivenJob.setMinFileAgeSeconds(((FileEventDrivenJob) job).getMinFileAgeSeconds());
                    fileEventDrivenJob.setMoveDirectory(((FileEventDrivenJob) job).getMoveDirectory());
                    fileEventDrivenJob.setSortAscending(((FileEventDrivenJob) job).isSortAscending());
                    fileEventDrivenJob.setSortByModifiedDateTime(((FileEventDrivenJob) job).isSortByModifiedDateTime());
                    fileEventDrivenJob.setAgentName(job.getAgentName());
                    fileEventDrivenJob.setChildContextNames(job.getChildContextNames());
                    fileEventDrivenJob.setCronExpression(((FileEventDrivenJob) job).getCronExpression());
                    fileEventDrivenJob.setEager(((FileEventDrivenJob) job).isEager());
                    fileEventDrivenJob.setIdentifier(job.getIdentifier());
                    fileEventDrivenJob.setIgnoreMisfire(((FileEventDrivenJob) job).isIgnoreMisfire());
                    fileEventDrivenJob.setJobGroup(((FileEventDrivenJob) job).getJobGroup());
                    fileEventDrivenJob.setMaxEagerCallbacks(((FileEventDrivenJob) job).getMaxEagerCallbacks());
                    fileEventDrivenJob.setTimeZone(((FileEventDrivenJob) job).getTimeZone());
                    fileEventDrivenJob.setPassthroughProperties(((FileEventDrivenJob) job).getPassthroughProperties());
                    fileEventDrivenJob.setPersistentRecovery(((FileEventDrivenJob) job).isPersistentRecovery());
                    fileEventDrivenJob.setRecoveryTolerance(((FileEventDrivenJob) job).getRecoveryTolerance());
                    fileEventDrivenJob.setStartupControlType(job.getStartupControlType());
                    fileEventDrivenJob.setJobName(job.getJobName());

                    return fileEventDrivenJob;
                } else if (job instanceof GlobalEventJob) {
                    GlobalEventJob globalEventJob = new GlobalEventJobImpl();
                    globalEventJob.setAgentName(job.getAgentName());
                    globalEventJob.setChildContextNames(job.getChildContextNames());
                    globalEventJob.setContextName(job.getContextName());
                    globalEventJob.setIdentifier(job.getIdentifier());
                    globalEventJob.setJobDescription(job.getJobDescription());
                    globalEventJob.setJobName(job.getJobName());
                    globalEventJob.setStartupControlType(job.getStartupControlType());

                    return globalEventJob;
                } else {
                    QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
                    quartzScheduleDrivenJob.setContextName(job.getContextName());
                    quartzScheduleDrivenJob.setCronExpression(((QuartzScheduleDrivenJob) job).getCronExpression());
                    quartzScheduleDrivenJob.setEager(((QuartzScheduleDrivenJob) job).isEager());
                    quartzScheduleDrivenJob.setIgnoreMisfire(((QuartzScheduleDrivenJob) job).isIgnoreMisfire());
                    quartzScheduleDrivenJob.setJobGroup(((QuartzScheduleDrivenJob) job).getJobGroup());
                    quartzScheduleDrivenJob.setMaxEagerCallbacks(((QuartzScheduleDrivenJob) job).getMaxEagerCallbacks());
                    quartzScheduleDrivenJob.setPassthroughProperties(((QuartzScheduleDrivenJob) job).getPassthroughProperties());
                    quartzScheduleDrivenJob.setPersistentRecovery(((QuartzScheduleDrivenJob) job).isPersistentRecovery());
                    quartzScheduleDrivenJob.setRecoveryTolerance(((QuartzScheduleDrivenJob) job).getRecoveryTolerance());
                    quartzScheduleDrivenJob.setStartupControlType(job.getStartupControlType());
                    quartzScheduleDrivenJob.setJobName(job.getJobName());
                    quartzScheduleDrivenJob.setJobDescription(job.getJobDescription());
                    quartzScheduleDrivenJob.setIdentifier(job.getIdentifier());
                    quartzScheduleDrivenJob.setChildContextNames(job.getChildContextNames());
                    quartzScheduleDrivenJob.setAgentName(job.getAgentName());
                    quartzScheduleDrivenJob.setTimeZone(((QuartzScheduleDrivenJob) job).getTimeZone());

                    return quartzScheduleDrivenJob;
                }
            })
            .collect(Collectors.toList());
    }

    private void persistJobs(List<SchedulerJob> jobs, String actor) {
        Set<String> contextNames = jobs.stream().map(SchedulerJob::getContextName).collect(Collectors.toSet());
        for (String contextName : contextNames) {
            this.schedulerJobService.deleteByContextName(contextName);
        }

        List<InternalEventDrivenJob> internalEventDrivenJobs = new ArrayList<>();
        List<FileEventDrivenJob> fileEventDrivenJobs = new ArrayList<>();
        List<QuartzScheduleDrivenJob> quartzScheduleDrivenJobs = new ArrayList<>();
        List<GlobalEventJob> globalEventJobs = new ArrayList<>();
        jobs.forEach(job -> {
            if(job instanceof InternalEventDrivenJob) {
                internalEventDrivenJobs.add((InternalEventDrivenJob)job);
            }
            else if(job instanceof FileEventDrivenJob) {
                fileEventDrivenJobs.add((FileEventDrivenJob)job);
            }
            else if(job instanceof QuartzScheduleDrivenJob) {
                quartzScheduleDrivenJobs.add((QuartzScheduleDrivenJob)job);
            }
            else if(job instanceof GlobalEventJob) {
                globalEventJobs.add((GlobalEventJob)job);
            }
        });

        if(!internalEventDrivenJobs.isEmpty()) {
            this.schedulerJobService.saveInternalEventDrivenJobs(internalEventDrivenJobs, actor);
        }

        if(!fileEventDrivenJobs.isEmpty()) {
            this.schedulerJobService.saveFileEventDrivenJobs(fileEventDrivenJobs, actor);
        }

        if(!quartzScheduleDrivenJobs.isEmpty()) {
            this.schedulerJobService.saveQuartzScheduledJobs(quartzScheduleDrivenJobs, actor);
        }

        if(!globalEventJobs.isEmpty()) {
            this.schedulerJobService.saveGlobalEventJobs(globalEventJobs, actor);
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
