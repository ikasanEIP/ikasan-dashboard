package org.ikasan.scheduler.context.cache;

import org.ikasan.scheduler.core.machine.ContextMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private ConcurrentHashMap<String, ContextMachine> cache;

    private ContextMachineCache() {
        cache = new ConcurrentHashMap<>();
    }

    public void put(String contextName, ContextMachine contextMachine)
    {
        logger.debug(String.format("%s attempting to put key[%s]", this, contextName));

        this.cache.put(contextName, contextMachine);
    }


    public ContextMachine get(String contextName)
    {
        logger.debug(String.format("%s attempting to get context[%s]"
            , this, contextName));

        return this.cache.get(contextName);
    }

    public boolean contains(String contextName)
    {
        logger.debug(String.format("%s check contains[%s] - result [%s]",this
            , contextName));
        return this.cache.containsKey(contextName);
    }
}
