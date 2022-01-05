package org.ikasan.scheduler.context.cache;

import org.ikasan.scheduler.core.machine.ContextMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    }


    public ContextMachine getByContextName(String contextName)
    {
        logger.debug(String.format("%s attempting to get context using context name[%s]"
            , this, contextName));

        return this.contextInstanceByContextNameCache.get(contextName);
    }

    public ContextMachine getByContextInstanceId(String contextName)
    {
        logger.debug(String.format("%s attempting to get context using context instance id[%s]"
            , this, contextName));

        return this.contextInstanceByContextInstanceIdCache.get(contextName);
    }

    public boolean containsContextName(String contextName)
    {
        boolean result = this.contextInstanceByContextNameCache.containsKey(contextName);
        logger.debug(String.format("%s check contains[%s] - result [%s]",this
            , contextName, result));
        return result;
    }

    public boolean containsInstanceIdentifier(String contextInstanceId)
    {
        boolean result = this.contextInstanceByContextInstanceIdCache.containsKey(contextInstanceId);

        logger.debug(String.format("%s check contains[%s] - result [%s]",this
            , contextInstanceId, result));
        return result;
    }

    public Set contextNames() {
        return this.contextInstanceByContextNameCache.keySet();
    }
    public Set contextInstanceIdentifiers() {
        return this.contextInstanceByContextInstanceIdCache.keySet();
    }
}
