package org.ikasan.job.orchestration.provision.job;

import org.ikasan.job.orchestration.provision.ScheduledProcessConfigurationConstants;
import org.ikasan.job.orchestration.provision.ScheduledProcessConstants;
import org.ikasan.spec.metadata.*;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class JobProvisionServiceImpl implements JobProvisionService {

    Logger logger = LoggerFactory.getLogger(JobProvisionServiceImpl.class);

    private SchedulerJobService schedulerJobService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private ModuleMetaDataService moduleMetaDataService;
    private MetaDataService metaDataRestService;

    private Map<String, List<InternalEventDrivenJob>> internalEventDrivenJobs;
    private Map<String, List<QuartzScheduleDrivenJob>> quartzScheduleDrivenJobs;
    private Map<String, List<FileEventDrivenJob>> fileEventDrivenJobs;
    private List<String> uniqueAgentNames;

    /**
     * Constructor
     *
     * @param configurationRestService
     * @param moduleControlRestService
     * @param moduleMetaDataService
     */
    public JobProvisionServiceImpl(SchedulerJobService schedulerJobService, ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                   ModuleMetaDataService moduleMetaDataService, MetaDataService metaDataRestService) {
        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }

        this.configurationRestService = configurationRestService;
        if(this.configurationRestService == null) {
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }

        this.moduleControlRestService = moduleControlRestService;
        if(this.moduleControlRestService == null) {
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }

        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }

        this.metaDataRestService = metaDataRestService;
        if(this.metaDataRestService == null) {
            throw new IllegalArgumentException("metaDataRestService cannot be null!");
        }

        this.internalEventDrivenJobs = new HashMap<>();
        this.quartzScheduleDrivenJobs = new HashMap<>();
        this.fileEventDrivenJobs = new HashMap<>();
        this.uniqueAgentNames = new ArrayList<>();
    }

    public void provisionJobs(List<SchedulerJob> jobs) {
        this.addSchedulerJob(jobs);
        long now = System.currentTimeMillis();
        logger.info(String.format("Provisioning %s jobs across %s agents", jobs.size(), uniqueAgentNames.size()));
        ModuleMetadataSearchResults agents = this.moduleMetaDataService
            .find(this.uniqueAgentNames, ModuleType.SCHEDULER_AGENT, -1, -1);

        // As it is possible to provision multiple agents as part of the job
        // provisioning process, we will collect exceptions and report issues
        // once attempts to provision all agents are complete.
        List<JobProvisionException> exceptions = new ArrayList<>();

        agents.getResultList().forEach(agent -> {
            try {
                ConfigurationMetaData<List<ConfigurationParameterMetaData>> moduleConfiguration
                    = this.configurationRestService.getModuleConfiguration(agent.getUrl());

                this.populateFlowDefinitions(agent, moduleConfiguration);
                this.populateFlowDefinitionProfiles(agent, moduleConfiguration);

                logger.debug("Module Configuration: " + moduleConfiguration);
                // update the configuration back onto the module.
                this.configurationRestService.storeConfiguration(agent.getUrl(), moduleConfiguration);
                // We need to deactivate and activate the module so the new flow is initialised
                this.changeActivation(agent, "deactivate");
                this.changeActivation(agent, "activate");

                // Refresh the module metadata with the new flows.
                Optional<ModuleMetaData> moduleMetaData = this.metaDataRestService.getModuleMetadata(agent.getUrl(), agent.getName());
                moduleMetaData.ifPresentOrElse(metaData -> {
                    this.configureFileEventJobs(metaData);
                    this.configureQuartzScheduleJobs(metaData);
                    metaData.getFlows().forEach(flowMetaData ->  {
                        this.setStartUpControl(metaData, flowMetaData.getName(), moduleConfiguration);
                        // start the flow
                        this.moduleControlRestService.changeFlowState(agent.getUrl(), agent.getName(), flowMetaData.getName(), "start");
                    });
                }, () -> {
                    throw new JobProvisionException(String.format("Could not refresh module metadata for agent[%s] with url[%s]"
                        , agent.getName(), agent.getUrl()));
                });
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
            // todo need to work out a nice way to deal with exceptions.
            throw new JobProvisionException("");
        }

        logger.info(String.format("Finished provisioning %s jobs across %s agents. Time taken %s milliseconds.", jobs.size(), uniqueAgentNames.size(), System.currentTimeMillis()-now));
    }

    private void addSchedulerJob(List<SchedulerJob> jobs) {
        jobs.forEach(schedulerJob -> {
            if(schedulerJob instanceof InternalEventDrivenJob) {
                if(!this.internalEventDrivenJobs.containsKey(schedulerJob.getAgentName())) {
                    this.internalEventDrivenJobs.put(schedulerJob.getAgentName(), new ArrayList<>());
                }

                this.internalEventDrivenJobs.get(schedulerJob.getAgentName()).add((InternalEventDrivenJob)schedulerJob);
                this.schedulerJobService.saveInternalEventDrivenJob((InternalEventDrivenJob)schedulerJob);
            }
            else if(schedulerJob instanceof FileEventDrivenJob) {
                if(!this.fileEventDrivenJobs.containsKey(schedulerJob.getAgentName())) {
                    this.fileEventDrivenJobs.put(schedulerJob.getAgentName(), new ArrayList<>());
                }
                this.fileEventDrivenJobs.get(schedulerJob.getAgentName()).add((FileEventDrivenJob)schedulerJob);
                this.schedulerJobService.saveFileEventDrivenJob((FileEventDrivenJob)schedulerJob);
            }
            else if(schedulerJob instanceof QuartzScheduleDrivenJob) {
                if(!this.quartzScheduleDrivenJobs.containsKey(schedulerJob.getAgentName())) {
                    this.quartzScheduleDrivenJobs.put(schedulerJob.getAgentName(), new ArrayList<>());
                }
                this.quartzScheduleDrivenJobs.get(schedulerJob.getAgentName()).add((QuartzScheduleDrivenJob)schedulerJob);
                this.schedulerJobService.saveQuartzScheduledJob((QuartzScheduleDrivenJob) schedulerJob);
            }
            else {
                throw new JobProvisionException("Invalid scheduler job!");
            }

            if(!this.uniqueAgentNames.contains(schedulerJob.getAgentName())) {
                this.uniqueAgentNames.add(schedulerJob.getAgentName());
            }
        });
    }

    private void populateFlowDefinitions(ModuleMetaData agent, ConfigurationMetaData<List<ConfigurationParameterMetaData>> moduleConfiguration) {
        moduleConfiguration.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitions"))
            .findFirst().ifPresentOrElse(flowDefinitions -> {
            // Add the new job flow to the map.
            Map<String, String> configurationMap = (Map<String, String>) flowDefinitions.getValue();

            if(this.fileEventDrivenJobs.containsKey(agent.getName())) {
                this.fileEventDrivenJobs.get(agent.getName()).forEach(fileEventDrivenJob
                    ->  {
                    configurationMap.put(fileEventDrivenJob.getJobName(), "MANUAL");

                });
            }

            if(this.quartzScheduleDrivenJobs.containsKey(agent.getName())) {
                this.quartzScheduleDrivenJobs.get(agent.getName()).forEach(fileEventDrivenJob
                    -> configurationMap.put(fileEventDrivenJob.getJobName(), "MANUAL"));
            }

            if(this.internalEventDrivenJobs.containsKey(agent.getName())) {
                this.internalEventDrivenJobs.get(agent.getName()).forEach(fileEventDrivenJob
                    -> configurationMap.put(fileEventDrivenJob.getJobName(), "MANUAL"));
            }

            flowDefinitions.setValue(configurationMap);
        }, () -> {
            throw new JobProvisionException(String.format("Could not find flow definitions from module configuration for agent[%s]", agent));
        });
    }

    private void populateFlowDefinitionProfiles(ModuleMetaData agent, ConfigurationMetaData<List<ConfigurationParameterMetaData>> moduleConfiguration) {
        moduleConfiguration.getParameters().stream()
            .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitionProfiles"))
            .findFirst().ifPresentOrElse(flowDefinitionProfiles -> {
            // Add the new job flow to the map.
            Map<String, String> configurationMap = (Map<String, String>) flowDefinitionProfiles.getValue();

            if(this.fileEventDrivenJobs.containsKey(agent.getName())) {
                this.fileEventDrivenJobs.get(agent.getName()).forEach(fileEventDrivenJob
                    ->  configurationMap.put(fileEventDrivenJob.getJobName(), "FILE"));
            }

            if(this.quartzScheduleDrivenJobs.containsKey(agent.getName())) {
                this.quartzScheduleDrivenJobs.get(agent.getName()).forEach(quartzScheduleDrivenJob
                    -> configurationMap.put(quartzScheduleDrivenJob.getJobName(), "QUARTZ"));
            }

            if(this.internalEventDrivenJobs.containsKey(agent.getName())) {
                this.internalEventDrivenJobs.get(agent.getName()).forEach(fileEventDrivenJob
                    -> configurationMap.put(fileEventDrivenJob.getJobName(), "SCHEDULER_JOB"));
            }

            ;
            flowDefinitionProfiles.setValue(configurationMap);

        }, () -> {
            throw new JobProvisionException(String.format("Could not find flow definition profiles from module configuration for agent[%s]", agent));
        });
    }

    private void configureFileEventJobs(ModuleMetaData agent) {
        if(fileEventDrivenJobs.containsKey(agent.getName())) {
            this.fileEventDrivenJobs.get(agent.getName()).forEach(job -> {
                ConfigurationMetaData<List<ConfigurationParameterMetaData>> fileEventConsumerConfiguration
                    = this.getConfigurationForAgentFlowComponent(agent, job.getJobName(), ScheduledProcessConstants.FILE_CONSUMER);

                this.updateFileConsumerConfiguration(fileEventConsumerConfiguration, job);
                this.configurationRestService.storeConfiguration(agent.getUrl(), fileEventConsumerConfiguration);
            });
        }
    }

    private void configureQuartzScheduleJobs(ModuleMetaData agent) {
        if(quartzScheduleDrivenJobs.containsKey(agent.getName())) {
            this.quartzScheduleDrivenJobs.get(agent.getName()).forEach(job -> {
                ConfigurationMetaData<List<ConfigurationParameterMetaData>> fileEventConsumerConfiguration
                    = this.getConfigurationForAgentFlowComponent(agent, job.getJobName(), ScheduledProcessConstants.SCHEDULED_CONSUMER);

                this.updateScheduleConsumerConfiguration(fileEventConsumerConfiguration, job);
                this.configurationRestService.storeConfiguration(agent.getUrl(), fileEventConsumerConfiguration);
            });
        }
    }


    /**
     * Helper method to call activation endpoint on the scheduler agent.
     *
     * @param action
     */
    private void changeActivation(ModuleMetaData agent, String action) {
        boolean success = this.moduleControlRestService.changeModuleActivationState(agent.getUrl(), agent.getName(), action);
        if (!success) {
            throw new JobProvisionException(String.format("Agent[%s]. Attempting to change module activation state. Could not %s agent[%s]"
                , agent.getName(), action, agent.getUrl()));
        }
    }

    /**
     * Update the scheduled consumer configuration.
     *
     * @param scheduledConsumerConfiguration
     * @param scheduleProcessAggregateConfiguration
     */
    private void updateScheduleConsumerConfiguration(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
        , QuartzScheduleDrivenJob scheduleProcessAggregateConfiguration) {
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.JOB_NAME,
            scheduleProcessAggregateConfiguration.getJobName());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.JOB_GROUP_NAME,
            scheduleProcessAggregateConfiguration.getJobGroup());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.JOB_DESCRIPTION,
            scheduleProcessAggregateConfiguration.getJobDescription());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.CRON_EXPRESSION,
            scheduleProcessAggregateConfiguration.getCronExpression());
        if(scheduleProcessAggregateConfiguration.getTimeZone() != null) {
            this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.TIMEZONE,
                scheduleProcessAggregateConfiguration.getTimeZone());
        }
//        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.PASS_THROUGH_PROPERTIES,
//            scheduleProcessAggregateConfiguration.getPassthroughProperties());
    }

    /**
     * Update the file consumer configuration.
     *
     * @param scheduledConsumerConfiguration
     * @param fileEventDrivenJob
     */
    private void updateFileConsumerConfiguration(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
        , FileEventDrivenJob fileEventDrivenJob) {
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.JOB_NAME,
            fileEventDrivenJob.getJobName());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.JOB_GROUP_NAME,
            fileEventDrivenJob.getJobGroup());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.JOB_DESCRIPTION,
            fileEventDrivenJob.getJobDescription());
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.CRON_EXPRESSION,
            fileEventDrivenJob.getCronExpression());
        if(fileEventDrivenJob.getTimeZone() != null) {
            this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.TIMEZONE,
                fileEventDrivenJob.getTimeZone());
        }
        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.FILENAMES,
            List.of(fileEventDrivenJob.getFilePath()));
    }

    private void setStartUpControl(ModuleMetaData agent, String jobName, ConfigurationMetaData<List<ConfigurationParameterMetaData>> moduleConfiguration) {
            // Now that all configurations are applied we need to set up the startup type and restart the flow
            String startupType = "AUTOMATIC";
            moduleConfiguration.getParameters().stream()
                .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitions"))
                .findFirst().ifPresentOrElse(flowDefinitions -> {
                // Add the new job flow to the map.
                Map<String, String> configurationMap = (Map<String, String>) flowDefinitions.getValue();
                configurationMap.replace(jobName, startupType);
                flowDefinitions.setValue(configurationMap);

                logger.debug("Module Configuration: " + moduleConfiguration);
                // update the configuration back onto the module.
                this.configurationRestService.storeConfiguration(agent.getUrl(), moduleConfiguration);
            }, () -> {
                throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s] " +
                    "when attempting to update start up control.", agent));
            });

            this.moduleControlRestService.changeFlowStartupType(agent.getUrl(), agent.getName(), jobName
                , startupType, "Scheduler flow requires automatic startup.");
    }

    /**
     * General method to set parameters on a configuration meta data.
     *
     * @param params
     * @param paramName
     * @param value
     */
    private void setConfigurationParameterMetaDataValue(ConfigurationMetaData<List<ConfigurationParameterMetaData>> params
        , String paramName, Object value) {
        params.getParameters().stream()
            .filter(param -> param.getName().equals(paramName))
            .findFirst()
            .ifPresentOrElse(conf -> conf.setValue(value), () -> logger.warn(String.format("Failed to set configuration parameter[%s]" +
                ", value[%s], configuration[%s]", paramName, value, params)));
    }

    /**
     * Helper method to get a specific component configuration from the module.
     *
     * @param flow
     * @param component
     * @return
     */
    private ConfigurationMetaData getConfigurationForAgentFlowComponent(ModuleMetaData agent, String flow, String component) {
        AtomicReference<ConfigurationMetaData> configurationMetaData = new AtomicReference<>();

        agent.getFlows().stream()
            .filter(flowMetaData -> flowMetaData.getName().equals(flow))
            .findFirst().ifPresentOrElse(flowMetaData -> {
            flowMetaData.getFlowElements().stream()
                .filter(flowElementMetaData -> flowElementMetaData.getComponentName().equals(component))
                .findFirst().ifPresentOrElse(id -> configurationMetaData.set(configurationRestService
                    .getConfiguredResourceConfiguration(agent.getUrl(), agent.getName(), flow, component))
                , () -> {
                    throw new RuntimeException(String.format("Could not load configuration metadata for agent[%s], flow[%s], component[%s] at url[%s]!"
                        , agent.getName(), flow, component, agent.getUrl()));
                });
        }, () -> {
            throw new RuntimeException(String.format("Could not load flow for agent[%s], flow[%s], component[%s] at url[%s]!"
                , agent.getName(), flow, component, agent.getUrl()));
        });

        return configurationMetaData.get();
    }
}
