package org.ikasan.job.orchestration.provision.job;

import org.ikasan.job.orchestration.provision.ScheduledProcessConfigurationConstants;
import org.ikasan.job.orchestration.provision.ScheduledProcessConstants;
import org.ikasan.spec.metadata.*;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class JobProvisionService {

    Logger logger = LoggerFactory.getLogger(JobProvisionService.class);

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
    public JobProvisionService(ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                               ModuleMetaDataService moduleMetaDataService, MetaDataService metaDataRestService) {
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

    public void addSchedulerJob(SchedulerJob schedulerJob) {
        if(schedulerJob instanceof InternalEventDrivenJob) {
            if(!this.internalEventDrivenJobs.containsKey(schedulerJob.getAgentName())) {
                this.internalEventDrivenJobs.put(schedulerJob.getAgentName(), new ArrayList<>());
            }

            this.internalEventDrivenJobs.get(schedulerJob.getAgentName()).add((InternalEventDrivenJob)schedulerJob);
        }
        else if(schedulerJob instanceof QuartzScheduleDrivenJob) {
            if(!this.quartzScheduleDrivenJobs.containsKey(schedulerJob.getAgentName())) {
                this.quartzScheduleDrivenJobs.put(schedulerJob.getAgentName(), new ArrayList<>());
            }
            this.quartzScheduleDrivenJobs.get(schedulerJob.getAgentName()).add((QuartzScheduleDrivenJob)schedulerJob);
        }
        else if(schedulerJob instanceof FileEventDrivenJob) {
            if(!this.fileEventDrivenJobs.containsKey(schedulerJob.getAgentName())) {
                this.fileEventDrivenJobs.put(schedulerJob.getAgentName(), new ArrayList<>());
            }
            this.fileEventDrivenJobs.get(schedulerJob.getAgentName()).add((FileEventDrivenJob)schedulerJob);
        }
        else {
            throw new JobProvisionException("Invalid scheduler job!");
        }

        if(!this.uniqueAgentNames.contains(schedulerJob.getAgentName())) {
            this.uniqueAgentNames.add(schedulerJob.getAgentName());
        }
    }

    public void provisionJobs() {
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

                logger.info("Module Configuration: " + moduleConfiguration);
                // update the configuration back onto the module.
                this.configurationRestService.storeConfiguration(agent.getUrl(), moduleConfiguration);
                // We need to deactivate and activate the module so the new flow is initialised
                this.changeActivation(agent, "deactivate");
                this.changeActivation(agent, "activate");
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
                this.quartzScheduleDrivenJobs.get(agent.getName()).forEach(fileEventDrivenJob
                    -> configurationMap.put(fileEventDrivenJob.getJobName(), "QUARTZ"));
            }

            if(this.internalEventDrivenJobs.containsKey(agent.getName())) {
                this.internalEventDrivenJobs.get(agent.getName()).forEach(fileEventDrivenJob
                    -> configurationMap.put(fileEventDrivenJob.getJobName(), "SCHEDULER_JOB"));
            }

            ;
            flowDefinitionProfiles.setValue(configurationMap);

        }, () -> {
            throw new JobProvisionException(String.format("Could not find flow definitions from module configuration for agent[%s]", agent));
        });
    }

    private void configureFileEventJobs(ModuleMetaData agent) {
        // Load the required configurations for a scheduled job.
        Optional<ModuleMetaData> moduleMetaData = this.metaDataRestService.getModuleMetadata(agent.getUrl(), agent.getName());

        this.fileEventDrivenJobs.entrySet().forEach(entry -> {
            entry.getValue().forEach(job -> {
                ConfigurationMetaData<List<ConfigurationParameterMetaData>> fileEventConsumerConfiguration = this.getConfigurationForAgentFlowComponent(agent, moduleMetaData,
                    job.getJobName(), ScheduledProcessConstants.SCHEDULED_CONSUMER);

                this.updateFileConsumerConfiguration(fileEventConsumerConfiguration, job);
                this.configurationRestService.storeConfiguration(agent.getUrl(), fileEventConsumerConfiguration);
            });
        });
    }

//    /**
//     * This method interacts with with agent in order to create a new scheduler agent flow or update an existing flow and associated job.
//     *
//     * @param scheduleProcessAggregateConfiguration
//     */
//    public void createOrUpdateScheduledJob(ModuleMetaData agent, ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
//        // Get the module configuration from the module.
//        ConfigurationMetaData<List<ConfigurationParameterMetaData>> moduleConfiguration
//            = this.configurationRestService.getModuleConfiguration(agent.getUrl());
//
//        try {
//
//            if (moduleConfiguration == null) {
//                throw new RuntimeException(String.format("Could not find module configuration for agent[%s]", agent.getName()));
//            }
//
//            logger.debug("Module Configuration: " + moduleConfiguration);
//
//            if (this.editMode == EditMode.NEW) {
//                // Get the flowDefinitions from the configuration metadata.
//                moduleConfiguration.getParameters().stream()
//                    .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitions"))
//                    .findFirst().ifPresentOrElse(flowDefinitions -> {
//                    // Add the new job flow to the map.
//                    Map<String, String> configurationMap = (Map<String, String>) flowDefinitions.getValue();
//                    configurationMap.put(scheduleProcessAggregateConfiguration.getJobName(), "MANUAL");
//                    flowDefinitions.setValue(configurationMap);
//
//                    logger.info("Module Configuration: " + moduleConfiguration);
//                    // update the configuration back onto the module.
//                    this.configurationRestService.storeConfiguration(agent.getUrl(), moduleConfiguration);
//                }, () -> {
//                    throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s]", agent));
//                });
//
//
//                // We need to deactivate and activate the module so the new flow is initialised
//                this.changeActivation(agent, "deactivate");
//                this.changeActivation(agent, "activate");
//            }
//
//            /// Load the required configurations for a scheduled job.
//            Optional<ModuleMetaData> moduleMetaData = this.metaDataRestService.getModuleMetadata(agent.getUrl(), agent.getName());
//
//            ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration = this.getConfigurationForAgentFlowComponent(moduleMetaData,
//                this.jobNameTf.getValue(), ScheduledProcessConstants.SCHEDULED_CONSUMER);
//            ConfigurationMetaData<List<ConfigurationParameterMetaData>> blackoutRouterConfiguration = this.getConfigurationForAgentFlowComponent(moduleMetaData,
//                this.jobNameTf.getValue(), ScheduledProcessConstants.BLACKOUT_ROUTER);
//            ConfigurationMetaData<List<ConfigurationParameterMetaData>> processExecutionBrokerConfiguration = this.getConfigurationForAgentFlowComponent(moduleMetaData,
//                this.jobNameTf.getValue(), ScheduledProcessConstants.PROCESS_EXECUTION_BROKER);
//
//            // Update all the configurations with the configurations provided in the form.
//            this.updateScheduleConsumerConfiguration(scheduledConsumerConfiguration, scheduleProcessAggregateConfiguration);
//            this.updateBlackoutRouterConfiguration(blackoutRouterConfiguration, scheduleProcessAggregateConfiguration);
//            this.updateProcessExecutionBrokerConfiguration(processExecutionBrokerConfiguration, scheduleProcessAggregateConfiguration);
//
//            // Save all the configurations back to the agent.
//            logger.debug(scheduledConsumerConfiguration.toString());
//            if(!this.configurationRestService.storeConfiguration(agent.getUrl(), scheduledConsumerConfiguration)) {
//                throw new RuntimeException(String.format("Could not store scheduled consumer configuration [%s]", scheduledConsumerConfiguration));
//            }
//            this.scheduledProcessManagementService.saveConfiguration(scheduledConsumerConfiguration);
//
//            logger.debug(blackoutRouterConfiguration.toString());
//            if(!this.configurationRestService.storeConfiguration(agent.getUrl(), blackoutRouterConfiguration)) {
//                throw new RuntimeException(String.format("Could not store blackout router configuration [%s]", blackoutRouterConfiguration));
//            }
//            this.scheduledProcessManagementService.saveConfiguration(blackoutRouterConfiguration);
//
//            logger.debug(processExecutionBrokerConfiguration.toString());
//            if(!this.configurationRestService.storeConfiguration(agent.getUrl(), processExecutionBrokerConfiguration)) {
//                throw new RuntimeException(String.format("Could not store process execution configuration [%s]", blackoutRouterConfiguration));
//            }
//            this.scheduledProcessManagementService.saveConfiguration(processExecutionBrokerConfiguration);
//
//
//            if(this.startAutomaticCb.getValue()) {
//                // Now that all configurations are applied we need to set up the startup type and restart the flow
//                String startupType = this.startAutomaticCb.getValue() ? "AUTOMATIC" : "MANUAL";
//                moduleConfiguration.getParameters().stream()
//                    .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitions"))
//                    .findFirst().ifPresentOrElse(flowDefinitions -> {
//                    // Add the new job flow to the map.
//                    Map<String, String> configurationMap = (Map<String, String>) flowDefinitions.getValue();
//                    configurationMap.replace(scheduleProcessAggregateConfiguration.getJobName(), startupType);
//                    flowDefinitions.setValue(configurationMap);
//
//                    logger.info("Module Configuration: " + moduleConfiguration);
//                    // update the configuration back onto the module.
//                    this.configurationRestService.storeConfiguration(agent.getUrl(), moduleConfiguration);
//                }, () -> {
//                    throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s] " +
//                        "when attempting to update start up control.", agent));
//                });
//
//                this.moduleControlRestService.changeFlowStartupType(agent.getUrl(), agent.getName(), scheduleProcessAggregateConfiguration.getJobName()
//                    , startupType, "Scheduler flow requires automatic startup.");
//            }
//
//            // In order for the configuration to be applied the flow must be stopped and started.
//            this.moduleControlRestService.changeFlowState(agent.getUrl(), agent.getName(), scheduleProcessAggregateConfiguration.getJobName(), "stop");
//            this.moduleControlRestService.changeFlowState(agent.getUrl(), agent.getName(), scheduleProcessAggregateConfiguration.getJobName(), "start");
//        }
//        catch (Exception e) {
//            // If any exceptions occur we are going to remove the job that we attempted to create.
//            if(moduleConfiguration != null) {
//                moduleConfiguration.getParameters().stream()
//                    .filter(configurationParameterMetaData -> configurationParameterMetaData.getName().equals("flowDefinitions"))
//                    .findFirst().ifPresentOrElse(flowDefinitions -> {
//                    // Add the new job flow to the map.
//                    Map<String, String> configurationMap = (Map<String, String>) flowDefinitions.getValue();
//                    configurationMap.remove(scheduleProcessAggregateConfiguration.getJobName());
//                    flowDefinitions.setValue(configurationMap);
//
//                    logger.info("Module Configuration: " + moduleConfiguration);
//                    // update the configuration back onto the module.
//                    this.configurationRestService.storeConfiguration(agent.getUrl(), moduleConfiguration);
//                }, () -> {
//                    throw new RuntimeException(String.format("Could not find flow definitions from module configuration for agent[%s]", agent));
//                });
//
//
//                // We need to deactivate and activate the module so the new flow is removed when initialisation occurs.
//                this.changeActivation(agent,"deactivate");
//                this.changeActivation(agent,"activate");
//            }
//
//            throw e;
//        }
//    }

    /**
     * Helper method to call activation endpoint on the scheduler agent.
     *
     * @param action
     */
    private void changeActivation(ModuleMetaData agent, String action) {
        boolean success = this.moduleControlRestService.changeModuleActivationState(agent.getUrl(), agent.getName(), action);
        if (!success) {
            throw new JobProvisionException(String.format("Agent[%s]. Attempting to change module activation state. Could not %s agent[%s]"
                , agent.getName(), action, agent));
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
     * @param scheduleProcessAggregateConfiguration
     */
    private void updateFileConsumerConfiguration(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
        , FileEventDrivenJob scheduleProcessAggregateConfiguration) {
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
    }
//
//    /**
//     * Update the execution broker configuration.
//     *
//     * @param scheduledConsumerConfiguration
//     * @param scheduleProcessAggregateConfiguration
//     */
//    private void updateProcessExecutionBrokerConfiguration(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
//        , ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
//        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.COMMAND_LINE,
//            scheduleProcessAggregateConfiguration.getCommandLine());
//        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.WORKING_DIRECTORY,
//            scheduleProcessAggregateConfiguration.getWorkingDirectory());
//        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.SUCCESSFUL_RETURN_CODES,
//            scheduleProcessAggregateConfiguration.getSuccessfulReturnCodes());
//        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.SECONDS_TO_WAIT_FOR_PROCESS_TO_START,
//            scheduleProcessAggregateConfiguration.getSecondsToWaitForProcessStart());
//        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.STD_ERR,
//            scheduleProcessAggregateConfiguration.getStdErr());
//        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.STD_OUT,
//            scheduleProcessAggregateConfiguration.getStdOut());
//        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.RETRY_ON_FAIL,
//            scheduleProcessAggregateConfiguration.isRetryOnFail());
//    }
//
//    /**
//     * Update the blackout router configuration.
//     *
//     * @param scheduledConsumerConfiguration
//     * @param scheduleProcessAggregateConfiguration
//     */
//    private void updateBlackoutRouterConfiguration(ConfigurationMetaData<List<ConfigurationParameterMetaData>> scheduledConsumerConfiguration
//        , ScheduledProcessAggregateConfiguration scheduleProcessAggregateConfiguration) {
//        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.CRON_EXPRESSIONS,
//            scheduleProcessAggregateConfiguration.getBlackoutCronExpressions());
//        this.setConfigurationParameterMetaDataValue(scheduledConsumerConfiguration, ScheduledProcessConfigurationConstants.DATE_TIME_RANGES,
//            scheduleProcessAggregateConfiguration.getBlackoutDateTimeRanges());
//    }

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
     * @param agentOptional
     * @param flow
     * @param component
     * @return
     */
    private ConfigurationMetaData getConfigurationForAgentFlowComponent(ModuleMetaData agent, Optional<ModuleMetaData> agentOptional, String flow, String component) {
        AtomicReference<ConfigurationMetaData> configurationMetaData = new AtomicReference<>();

        agentOptional.ifPresentOrElse(metaData -> {
            metaData.getFlows().stream()
                .filter(flowMetaData -> flowMetaData.getName().equals(flow))
                .findFirst().ifPresentOrElse(flowMetaData -> {
                flowMetaData.getFlowElements().stream()
                    .filter(flowElementMetaData -> flowElementMetaData.getComponentName().equals(component))
                    .findFirst().ifPresentOrElse(id -> configurationMetaData.set(configurationRestService
                        .getConfiguredResourceConfiguration(metaData.getUrl(), metaData.getName(), flow, component))
                    , () -> {
                        throw new RuntimeException(String.format("Could not load configuration metadata for agent[%s], flow[%s], component[%s] at url[%s]!"
                            , metaData.getName(), flow, component, metaData.getUrl()));
                    });
            }, () -> {
                throw new RuntimeException(String.format("Could not load flow for agent[%s], flow[%s], component[%s] at url[%s]!"
                    , metaData.getName(), flow, component, metaData.getUrl()));
            });

        }, () -> {
            throw new RuntimeException(String.format("Could not load module metadata for agent[%s] at url[%s]!", agent.getName(), agent.getUrl()));
        });


        return configurationMetaData.get();
    }
}
