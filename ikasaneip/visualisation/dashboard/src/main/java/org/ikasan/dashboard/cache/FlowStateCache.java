package org.ikasan.dashboard.cache;

import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.State;
import org.ikasan.dashboard.ui.util.VaadinThreadFactory;
import org.ikasan.dashboard.ui.visualisation.model.flow.Flow;
import org.ikasan.dashboard.ui.visualisation.model.flow.Module;
import org.ikasan.rest.client.dto.FlowDto;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FlowStateCache implements Consumer<FlowState>
{
    private Logger logger = LoggerFactory.getLogger(FlowStateCache.class);

    private static FlowStateCache INSTANCE;

    private ExecutorService executor = Executors.newFixedThreadPool(10, new VaadinThreadFactory("FlowStateCache"));

    /**
     * This method returns an instance of FlowStateCache. It follows the Singleton pattern
     * to ensure that only one instance of FlowStateCache is created.
     *
     * @return An instance of FlowStateCache
     */
    public static FlowStateCache instance()
    {
        if(INSTANCE == null) {
            synchronized (FlowStateCache.class) {
                if(INSTANCE == null) {
                    INSTANCE = new FlowStateCache();
                }
            }
        }
        return INSTANCE;
    }

    private ConcurrentHashMap<String, FlowState> cache;
    private ModuleControlService moduleControlRestService;
    private ModuleMetaDataService moduleMetaDataService;

    /**
     * FlowStateCache class with a private constructor that initializes a ConcurrentHashMap to store flow states.
     */
    private FlowStateCache()
    {
        cache = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the FlowStateCache by retrieving all flow metadata through the moduleMetaDataService and
     * fetching each corresponding flow state using the moduleControlRestService.
     */
    public void init() {
        if(this.moduleMetaDataService != null && this.moduleControlRestService != null) {
            this.moduleMetaDataService.findAll().forEach((moduleMetaData
                -> moduleMetaData.getFlows().forEach(flowMetaData
                    -> this.get(moduleMetaData, flowMetaData.getName()))));
        }
    }

    /**
     * Puts a FlowState object into the cache if the key is not already present or if the state has changed.
     *
     * @param flowState The FlowState object to be put into the cache
     */
    public void put(FlowState flowState)
    {
        String key = flowState.getModuleName() + flowState.getFlowName();

        logger.debug(String.format("%s attempting to put key[%s]", this, key));

        // Only update and broadcast state if state is new
        // or has changed.
        if(!this.cache.containsKey(key) || this.cache.get(key).getState() != flowState.getState()) {
            logger.debug(String.format("%s does not contain key[%s]", this, key));

            if(this.cache.containsKey(key)) {
                logger.debug(String.format("%s old state[%s] - new state [%s]",this
                    ,this.cache.get(key).getState(), flowState.getState()));
            }

            this.cache.put(key, flowState);
            CacheStateBroadcaster.broadcast(flowState);
        }
    }


    /**
     * Retrieves the FlowState object for the given Module and Flow.
     *
     * @param module The Module for which to retrieve the FlowState.
     * @param flow The Flow for which to retrieve the FlowState.
     * @return The FlowState object associated with the provided Module and Flow.
     */
    public FlowState get(Module module, Flow flow)
    {
        logger.debug(String.format("%s attempting to get module[%s] - flow[%s] - cache value[%s]"
            , this, module, flow.getName(), this.cache.get(module.getName()+flow.getName())));
        if(!this.contains(module, flow)) {
            this.put(new FlowState(module.getName(), flow.getName(), State.getState(State.UNKNOWN)));
            Runnable updateFromSourceRunnable = () -> refreshFromSource
                (module.getName(), flow.getName(), module.getUrl());
            this.executor.execute(updateFromSourceRunnable);
        }

        return this.cache.get(module.getName()+flow.getName());
    }

    /**
     * Retrieves the FlowState object for the given ModuleMetaData and flowName.
     *
     * @param module The ModuleMetaData for which to retrieve the FlowState.
     * @param flowName The name of the flow for which to retrieve the FlowState.
     * @return The FlowState object associated with the provided ModuleMetaData and flow.
     */
    public FlowState get(ModuleMetaData module, String flowName) {
        if(module == null) {
            return null;
        }

        logger.debug(String.format("%s attempting to get module[%s] - flow[%s] - cache value[%s]"
            , this, module.getName(), flowName, this.cache.get(module.getName()+flowName)));
        if(!this.contains(module, flowName)) {
            this.put(new FlowState(module.getName(), flowName, State.getState(State.UNKNOWN)));
            Runnable updateFromSourceRunnable = () -> refreshFromSource
                (module.getName(), flowName, module.getUrl());
            this.executor.execute(updateFromSourceRunnable);
        }

        return this.cache.get(module.getName()+flowName);
    }

    /**
     * Checks if the cache contains a specific Module and Flow.
     *
     * @param module The Module to check in the cache.
     * @param flow The Flow to check in the cache.
     * @return true if the cache contains the Module and Flow, false otherwise.
     */
    public boolean contains(Module module, Flow flow)
    {
        logger.debug(String.format("%s check contains[%s] - result [%s]",this
            , module.getName()+flow.getName(), this.cache.containsKey(module.getName()+flow.getName())));
        return this.cache.containsKey(module.getName()+flow.getName());
    }

    /**
     * Checks if the cache contains a specific ModuleMetaData and flowName.
     *
     * @param module The ModuleMetaData to check in the cache.
     * @param flowName The name of the flow to check in the cache.
     * @return true if the cache contains the ModuleMetaData and flowName, false otherwise.
     */
    public boolean contains(ModuleMetaData module, String flowName)
    {
        if(module == null) {
            return false;
        }
        logger.debug(String.format("%s check contains[%s] - result [%s]",this
            , module.getName()+flowName, this.cache.containsKey(module.getName()+flowName)));
        return this.cache.containsKey(module.getName()+flowName);
    }

    /**
     * Checks if the cache contains a specific module name and flow name.
     *
     * @param moduleName The name of the module to check in the cache.
     * @param flowName The name of the flow to check in the cache.
     * @return true if the cache contains the module name and flow name, false otherwise.
     */
    public boolean contains(String moduleName, String flowName)
    {
        logger.debug(String.format("%s check contains[%s] - result [%s]",this
            , moduleName+flowName, this.cache.containsKey(moduleName+flowName)));
        return this.cache.containsKey(moduleName+flowName);
    }

    @Override
    public void accept(FlowState flowState)
    {
        logger.debug(String.format("%s Received state change[%s]",this
            , flowState));
        this.put(flowState);
    }

    /**
     * Sets the ModuleControlService for interacting with module controls.
     *
     * @param moduleControlRestService The ModuleControlService to be set.
     */
    public void setModuleControlRestService(ModuleControlService moduleControlRestService)
    {
        this.moduleControlRestService = moduleControlRestService;
    }

    /**
     * Sets the ModuleMetaDataService to be used by this object.
     *
     * @param moduleMetaDataService The ModuleMetaDataService to be set
     */
    public void setModuleMetaDataService(ModuleMetaDataService moduleMetaDataService) {
        this.moduleMetaDataService = moduleMetaDataService;
    }

    /**
     * Refreshes the flow state from the specified data source based on the module name, flow name, and context URL.
     *
     * @param moduleName The name of the module for which to refresh the flow state.
     * @param flowName The name of the flow for which to refresh the state.
     * @param contextUrl The URL of the context from which to fetch the flow state.
     */
    private void refreshFromSource(String moduleName, String flowName, String contextUrl)
    {
        Optional<FlowDto> flowDto;

        logger.debug(String.format("%s Refresh from source[%s]-[%s]-[%s]",this
            , contextUrl, moduleName, flowName));

        flowDto = this.moduleControlRestService.getFlowState(contextUrl, moduleName, flowName);

        flowDto.ifPresentOrElse(dto -> {
            FlowState state = new FlowState(moduleName, flowName, State.getState(flowDto.get().getState()));
            logger.debug(String.format("%s Putting state-[%s]",FlowStateCache.instance()
                , state));
            FlowStateCache.instance().put(state);
        }, () -> {
            logger.debug(String.format("Could not load flow state for module[%s], flow[%s] using URL[%s].", moduleName, flowName, contextUrl));
            FlowState state = new FlowState(moduleName, flowName, State.getState(State.UNKNOWN));
            logger.debug(String.format("%s Putting state-[%s]",FlowStateCache.instance()
                , state));
            FlowStateCache.instance().put(state);
        });
    }

    public void teardown() {
        this.executor.shutdown();
        try {
            if (!executor.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        }
        catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
