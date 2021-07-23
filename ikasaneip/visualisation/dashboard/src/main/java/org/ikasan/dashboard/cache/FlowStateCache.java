package org.ikasan.dashboard.cache;

import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.FlowStateBroadcaster;
import org.ikasan.dashboard.broadcast.State;
import org.ikasan.dashboard.ui.visualisation.model.flow.Flow;
import org.ikasan.dashboard.ui.visualisation.model.flow.Module;
import org.ikasan.rest.client.ModuleControlRestServiceImpl;
import org.ikasan.rest.client.dto.FlowDto;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class FlowStateCache implements Consumer<FlowState>
{
    private Logger logger = LoggerFactory.getLogger(FlowStateCache.class);

    private static FlowStateCache INSTANCE;

    private ExecutorService executor = Executors.newFixedThreadPool(10);

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
    private ModuleControlRestServiceImpl moduleControlRestService;
    private ModuleMetaDataService moduleMetaDataService;

    private FlowStateCache()
    {
        cache = new ConcurrentHashMap<>();
        FlowStateBroadcaster.register(this);
    }

    public void init() {
        if(this.moduleMetaDataService != null && this.moduleControlRestService != null) {
            this.moduleMetaDataService.findAll().forEach((moduleMetaData
                -> moduleMetaData.getFlows().forEach(flowMetaData
                    -> this.get(moduleMetaData, flowMetaData.getName()))));
        }
    }

    public void put(FlowState flowState)
    {
        String key = flowState.getModuleName() + flowState.getFlowName();

        logger.info(String.format("%s attempting to put key[%s]", this, key));

        // Only update and broadcast state if state is new
        // or has changed.
        if(!this.cache.containsKey(key) || this.cache.get(key).getState() != flowState.getState()) {
            logger.info(String.format("%s does not contain key[%s]", this, key));

            if(this.cache.containsKey(key)) {
                logger.info(String.format("%s old state[%s] - new state [%s]",this
                    ,this.cache.get(key).getState(), flowState.getState()));
            }

            this.cache.put(key, flowState);
            CacheStateBroadcaster.broadcast(flowState);
        }
    }


    public FlowState get(Module module, Flow flow)
    {
        logger.info(String.format("%s attempting to get module[%s] - flow[%s] - cache value[%s]"
            , this, module, flow.getName(), this.cache.get(module.getName()+flow.getName())));
        if(!this.contains(module, flow)) {
            this.put(new FlowState(module.getName(), flow.getName(), State.getState(State.UNKNOWN)));
            Runnable updateFromSourceRunnable = () -> refreshFromSource
                (module.getName(), flow.getName(), module.getUrl());
            this.executor.execute(updateFromSourceRunnable);
        }

        return this.cache.get(module.getName()+flow.getName());
    }

    public FlowState get(ModuleMetaData module, String flowName) {
        if(module == null) {
            return null;
        }

        logger.info(String.format("%s attempting to get module[%s] - flow[%s] - cache value[%s]"
            , this, module.getName(), flowName, this.cache.get(module.getName()+flowName)));
        if(!this.contains(module, flowName)) {
            this.put(new FlowState(module.getName(), flowName, State.getState(State.UNKNOWN)));
            Runnable updateFromSourceRunnable = () -> refreshFromSource
                (module.getName(), flowName, module.getUrl());
            this.executor.execute(updateFromSourceRunnable);
        }

        return this.cache.get(module.getName()+flowName);
    }

    public boolean contains(Module module, Flow flow)
    {
        logger.info(String.format("%s check contains[%s] - result [%s]",this
            , module.getName()+flow.getName(), this.cache.containsKey(module.getName()+flow.getName())));
        return this.cache.containsKey(module.getName()+flow.getName());
    }

    public boolean contains(ModuleMetaData module, String flowName)
    {
        if(module == null) {
            return false;
        }
        logger.info(String.format("%s check contains[%s] - result [%s]",this
            , module.getName()+flowName, this.cache.containsKey(module.getName()+flowName)));
        return this.cache.containsKey(module.getName()+flowName);
    }

    public boolean contains(String moduleName, String flowName)
    {
        logger.info(String.format("%s check contains[%s] - result [%s]",this
            , moduleName+flowName, this.cache.containsKey(moduleName+flowName)));
        return this.cache.containsKey(moduleName+flowName);
    }

    @Override
    public void accept(FlowState flowState)
    {
        logger.info(String.format("%s Received state change[%s]",this
            , flowState));
        this.put(flowState);
    }

    public void setModuleControlRestService(ModuleControlRestServiceImpl moduleControlRestService)
    {
        this.moduleControlRestService = moduleControlRestService;
    }

    public void setModuleMetaDataService(ModuleMetaDataService moduleMetaDataService) {
        this.moduleMetaDataService = moduleMetaDataService;
    }

    private void refreshFromSource(String moduleName, String flowName, String contextUrl)
    {
        Optional<FlowDto> flowDto;

        logger.info(String.format("%s Refresh from source[%s]-[%s]-[%s]",this
            , contextUrl, moduleName, flowName));

        flowDto = this.moduleControlRestService.getFlowState(contextUrl, moduleName, flowName);

        flowDto.ifPresentOrElse(dto -> {
            FlowState state = new FlowState(moduleName, flowName, State.getState(flowDto.get().getState()));
            logger.info(String.format("%s Putting state-[%s]",FlowStateCache.instance()
                , state));
            FlowStateCache.instance().put(state);
        }, () -> {
            logger.warn(String.format("Could not load flow state for module[%s], flow[%s] using URL[%s].", moduleName, flowName, contextUrl));
            FlowState state = new FlowState(moduleName, flowName, State.getState(State.UNKNOWN));
            logger.info(String.format("%s Putting state-[%s]",FlowStateCache.instance()
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
