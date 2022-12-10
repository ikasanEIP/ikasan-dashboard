package org.ikasan.job.orchestration.context.cache;

import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ContextMachineCache
{
    private Logger logger = LoggerFactory.getLogger(ContextMachineCache.class);

    private static ContextMachineCache INSTANCE;

    public static ContextMachineCache instance()
    {
        if(INSTANCE == null) {
            synchronized (ContextMachineCache.class) {
                if(INSTANCE == null) {
                    INSTANCE = new ContextMachineCache();
                }
            }
        }
        return INSTANCE;
    }

    private ConcurrentHashMap<String, ContextMachine> contextInstanceByContextNameCache;
    private ConcurrentHashMap<String, ContextMachine> contextInstanceByContextInstanceIdCache;

    private ContextMachineCache() {
        this.contextInstanceByContextNameCache = new ConcurrentHashMap<>();
        this.contextInstanceByContextInstanceIdCache = new ConcurrentHashMap<>();
    }

    public void put(ContextMachine contextMachine)
    {
        this.contextInstanceByContextNameCache.put(contextMachine.getContext().getName(), contextMachine);
        this.contextInstanceByContextInstanceIdCache.put(contextMachine.getContext().getId(), contextMachine);
        contextMachine.registerToNotificationMonitors();
    }


    public ContextMachine getByContextName(String contextName)
    {
        logger.debug(String.format("%s attempting to get context using context name[%s]"
            , this, contextName));

        if(contextName == null) return null;

        return this.contextInstanceByContextNameCache.get(contextName);
    }

    public ContextMachine getByContextInstanceId(String contextInstanceId)
    {
        logger.debug(String.format("Attempting to get context using context instance id[%s]"
            , contextInstanceId));

        if(contextInstanceId == null) return null;

        return this.contextInstanceByContextInstanceIdCache.get(contextInstanceId);
    }

    public boolean containsContextName(String contextName)
    {
        if(contextName == null) return false;

        boolean result = this.contextInstanceByContextNameCache.containsKey(contextName);
        logger.debug(String.format("Check contains[%s] - result [%s]"
            , contextName, result));
        return result;
    }

    public boolean containsInstanceIdentifier(String contextInstanceId)
    {
        if(contextInstanceId == null) return false;

        boolean result = this.contextInstanceByContextInstanceIdCache.containsKey(contextInstanceId);

        logger.debug(String.format("Check contains[%s] - result [%s]"
            , contextInstanceId, result));
        return result;
    }

    public Set<String> contextNames() {
        return this.contextInstanceByContextNameCache.keySet();
    }

    public Set<String> contextInstanceIdentifiers() {
        return this.contextInstanceByContextInstanceIdCache.keySet();
    }

    public void remove(ContextMachine contextMachine)
    {
        contextMachine.unregisterToNotificationMonitors();
        this.contextInstanceByContextNameCache.remove(contextMachine.getContext().getName(), contextMachine);
        this.contextInstanceByContextInstanceIdCache.remove(contextMachine.getContext().getId(), contextMachine);
    }

    @Override
    public String toString() {
        StringBuffer cacheContexts = new StringBuffer("ContextMachineCache[");
        this.contextInstanceIdentifiers().forEach(id -> {
            ContextMachine machine = this.getByContextInstanceId(id);
            cacheContexts.append("{contextName[").append(machine.getContext().getName())
                .append("], contextInstanceId[").append(machine.getContext().getId()).append("]} ");
        });

        cacheContexts.append("]");
        return cacheContexts.toString();
    }
}
