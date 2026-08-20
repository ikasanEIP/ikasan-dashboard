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
import java.util.concurrent.*;
import java.util.function.Consumer;

public class FlowStateCache implements Consumer<FlowState>
{
    private Logger logger = LoggerFactory.getLogger(FlowStateCache.class);

    private ExecutorService executor = Executors.newFixedThreadPool(10
        , new VaadinThreadFactory("FlowStateCache"));

    /**
     * Throttle configuration: minimum time in milliseconds between broadcasts for the same flow.
     * Default is 60000ms (1 minute) to prevent rapid state transitions from causing memory issues.
     */
    private static final long DEFAULT_THROTTLE_INTERVAL_MS = 60000;
    private long throttleIntervalMs = DEFAULT_THROTTLE_INTERVAL_MS;

    /**
     * Tracks the last state for each flow to detect RECOVERING <-> STOPPED <-> RUNNING oscillations.
     * Key: moduleName+flowName, Value: last FlowState
     */
    private ConcurrentHashMap<String, FlowState> lastState = new ConcurrentHashMap<>();

    /**
     * Tracks the timestamp when each flow's last state was recorded.
     * Key: moduleName+flowName, Value: timestamp of last state change
     */
    private ConcurrentHashMap<String, Long> lastStateTime = new ConcurrentHashMap<>();

    /**
     * Tracks whether a flow has entered oscillation mode (seen at least one RECOVERING <-> STOPPED transition).
     * Key: moduleName+flowName, Value: true if oscillating
     */
    private ConcurrentHashMap<String, Boolean> inOscillation = new ConcurrentHashMap<>();

    /**
     * Tracks the last broadcast time for flows oscillating between RECOVERING and STOPPED.
     * Key: moduleName+flowName, Value: timestamp of last broadcast
     */
    private ConcurrentHashMap<String, Long> lastBroadcastTime = new ConcurrentHashMap<>();

    /**
     * Scheduled executor for delayed broadcasts when throttled.
     */
    private ScheduledExecutorService scheduledBroadcastExecutor = Executors.newScheduledThreadPool(2,
        new VaadinThreadFactory("FlowStateBroadcast"));

    /**
     * Tracks pending scheduled broadcasts to avoid scheduling multiple delayed broadcasts for the same flow.
     * Key: moduleName+flowName, Value: ScheduledFuture of pending broadcast
     */
    private ConcurrentHashMap<String, ScheduledFuture<?>> pendingBroadcasts = new ConcurrentHashMap<>();


    private static FlowStateCache INSTANCE = new FlowStateCache();

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
     * Implements throttling to prevent excessive broadcasts during rapid state transitions.
     *
     * @param flowState The FlowState object to be put into the cache
     */
    public void put(FlowState flowState)
    {
        String key = flowState.getModuleName() + flowState.getFlowName();

        logger.debug("{} attempting to put key[{}]", this, key);

        // Only update and broadcast state if state is new
        // or has changed.
        if(!this.cache.containsKey(key) || this.cache.get(key).getState() != flowState.getState()) {
            logger.debug("{} does not contain key[{}]", this, key);

            if(this.cache.containsKey(key)) {
                logger.debug("{} old state[{}] - new state [{}]",this
                    ,this.cache.get(key).getState(), flowState.getState());
            }

            this.cache.put(key, flowState);
            broadcastWithThrottling(flowState, key);
        }
    }

    /**
     * Broadcasts a flow state change with throttling to prevent memory issues from rapid state transitions.
     * Only throttles transitions between RECOVERING, STOPPED, and RUNNING states, which cause excessive broadcasts.
     * All other state transitions are broadcast immediately.
     *
     * @param flowState The FlowState to broadcast
     * @param key The cache key (moduleName+flowName)
     */
    private void broadcastWithThrottling(FlowState flowState, String key) {
        FlowState previousState = lastState.get(key);
        State currentState = flowState.getState();
        long currentTime = System.currentTimeMillis();

        // Check if current state is RECOVERING, STOPPED, or RUNNING (potential oscillation states)
        boolean isOscillationState = (currentState == State.RECOVERING_STATE ||
                                      currentState == State.STOPPED_STATE ||
                                      currentState == State.RUNNING_STATE);

        // Check time since last state change
        Long previousStateTime = lastStateTime.get(key);
        long timeSinceLastState = (previousStateTime != null) ? (currentTime - previousStateTime) : Long.MAX_VALUE;

        // Check if previous state was also RECOVERING, STOPPED, or RUNNING (and different from current)
        // AND the transition occurred within the oscillation detection window
        boolean isOscillationTransition = previousState != null &&
            (previousState.getState() == State.RECOVERING_STATE ||
             previousState.getState() == State.STOPPED_STATE ||
             previousState.getState() == State.RUNNING_STATE) &&
            previousState.getState() != currentState &&
            isOscillationState;// &&
            //timeSinceLastState <= oscillationWindowMs;

        // Update last state and time for future oscillation detection
        lastState.put(key, flowState);
        lastStateTime.put(key, currentTime);

        // Check if we're already in oscillation mode
        Boolean wasInOscillation = inOscillation.get(key);
        boolean alreadyOscillating = (wasInOscillation != null && wasInOscillation);

        // If this is an oscillation transition, mark as in oscillation
        if (isOscillationTransition) {
            inOscillation.put(key, true);
            logger.debug("{} Detected oscillation for key[{}], time since last state: {}ms", this, key, timeSinceLastState);
        } else if (!isOscillationState) {
            // If we transition to a non-oscillation state, clear oscillation flag
            inOscillation.remove(key);
            logger.debug("{} Cleared oscillation flag for key[{}] - transitioned to non-oscillation state", this, key);
        }

        // Apply throttling only if we're ALREADY in oscillation mode
        if (isOscillationTransition && alreadyOscillating) {
            // We're in an ongoing oscillation - apply throttling
            Long lastBroadcast = lastBroadcastTime.get(key);

            if (lastBroadcast != null && (currentTime - lastBroadcast) < throttleIntervalMs) {
                // Too soon since last broadcast - schedule a delayed broadcast
                long delay = throttleIntervalMs - (currentTime - lastBroadcast);

                logger.debug("{} Throttling RECOVERING<->STOPPED<->RUNNING oscillation for key[{}], scheduling delayed broadcast in {}ms",
                    this, key, delay);

                // Cancel any existing pending broadcast for this flow
                ScheduledFuture<?> existingFuture = pendingBroadcasts.get(key);
                if (existingFuture != null && !existingFuture.isDone()) {
                    existingFuture.cancel(false);
                    logger.debug("{} Cancelled existing pending broadcast for key[{}]", this, key);
                }

                // Schedule new delayed broadcast with the latest state
                ScheduledFuture<?> future = scheduledBroadcastExecutor.schedule(() -> {
                    logger.debug("{} Executing delayed broadcast for key[{}]", this, key);
                    CacheStateBroadcaster.broadcast(flowState);
                    lastBroadcastTime.put(key, System.currentTimeMillis());
                    pendingBroadcasts.remove(key);
                }, delay, TimeUnit.MILLISECONDS);

                pendingBroadcasts.put(key, future);
            } else {
                // Enough time has passed - broadcast immediately
                logger.debug("{} Broadcasting throttled state immediately (throttle expired) for key[{}]", this, key);
                CacheStateBroadcaster.broadcast(flowState);
                lastBroadcastTime.put(key, currentTime);

                // Cancel any pending broadcast since we just broadcast the latest state
                ScheduledFuture<?> existingFuture = pendingBroadcasts.remove(key);
                if (existingFuture != null && !existingFuture.isDone()) {
                    existingFuture.cancel(false);
                }
            }
        } else {
            // First oscillation transition or non-oscillating state - broadcast immediately
            logger.debug("{} Broadcasting state change immediately for key[{}]", this, key);
            CacheStateBroadcaster.broadcast(flowState);

            // Keep track of broadcast time
            lastBroadcastTime.put(key, System.currentTimeMillis());

            // Cancel any pending broadcast since we just broadcast the latest state
            ScheduledFuture<?> existingFuture = pendingBroadcasts.remove(key);
            if (existingFuture != null && !existingFuture.isDone()) {
                existingFuture.cancel(false);
            }
        }
    }

    /**
     * Sets the throttle interval in milliseconds. This controls the minimum time between broadcasts
     * for the same flow to prevent memory issues during rapid state transitions.
     *
     * @param throttleIntervalMs The throttle interval in milliseconds (must be > 0)
     */
    public void setThrottleIntervalMs(long throttleIntervalMs) {
        if (throttleIntervalMs > 0) {
            this.throttleIntervalMs = throttleIntervalMs;
            logger.info("FlowStateCache throttle interval set to {}ms", throttleIntervalMs);
        } else {
            logger.warn("Invalid throttle interval {}ms, must be > 0", throttleIntervalMs);
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
        logger.debug("{} attempting to get module[{}] - flow[{}] - cache value[{}]"
            , this, module.getName(), flow.getName(), this.cache.get(module.getName()+flow.getName()));
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

        logger.debug("{} attempting to get module[{}] - flow[{}] - cache value[{}]"
            , this, module.getName(), flowName, this.cache.get(module.getName()+flowName));
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
        logger.debug("{} check contains[{}] - result [{{}]",this
            , module.getName()+flow.getName(), this.cache.containsKey(module.getName()+flow.getName()));
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
        logger.debug("{} check contains[{}] - result [{{}]",this
            , module.getName()+flowName, this.cache.containsKey(module.getName()+flowName));
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
        logger.debug("{} check contains[{}] - result [{{}]",this
            , moduleName+flowName, this.cache.containsKey(moduleName+flowName));
        return this.cache.containsKey(moduleName+flowName);
    }

    @Override
    public void accept(FlowState flowState)
    {
        logger.debug("{} Received state change[{}]",this
            , flowState);
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

        logger.debug("{} Refresh from source[{}]-[{}]-[{}]",this
            , contextUrl, moduleName, flowName);

        flowDto = this.moduleControlRestService.getFlowState(contextUrl, moduleName, flowName);

        flowDto.ifPresentOrElse(dto -> {
            FlowState state = new FlowState(moduleName, flowName, State.getState(flowDto.get().getState()));
            logger.debug("{} Putting state-[{}]",FlowStateCache.instance()
                , state);
            FlowStateCache.instance().put(state);
        }, () -> {
            logger.debug("Could not load flow state for module[{}], flow[{}] using URL[{}]."
                , moduleName, flowName, contextUrl);
            FlowState state = new FlowState(moduleName, flowName, State.getState(State.UNKNOWN));
            logger.debug("{} Putting state-[{}]",FlowStateCache.instance()
                , state);
            FlowStateCache.instance().put(state);
        });
    }

    public void teardown() {
        // Shutdown main executor
        this.executor.shutdown();
        try {
            if (!executor.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        }
        catch (InterruptedException e) {
            executor.shutdownNow();
        }

        // Shutdown scheduled broadcast executor
        this.scheduledBroadcastExecutor.shutdown();
        try {
            if (!scheduledBroadcastExecutor.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                scheduledBroadcastExecutor.shutdownNow();
            }
        }
        catch (InterruptedException e) {
            scheduledBroadcastExecutor.shutdownNow();
        }

        // Clear tracking maps
        lastState.clear();
        lastStateTime.clear();
        inOscillation.clear();
        lastBroadcastTime.clear();
        pendingBroadcasts.clear();
    }
}
