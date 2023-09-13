package org.ikasan.job.orchestration.context.cache;

import org.apache.commons.lang3.StringUtils;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    /** Get all Context Instances in the cache */
    public ConcurrentHashMap<String, ContextMachine> getContextInstanceByContextInstanceIdCache() {
        return contextInstanceByContextInstanceIdCache;
    }

    private final ConcurrentHashMap<String, ContextMachine> contextInstanceByContextInstanceIdCache;
    private final Set<String> contextNames;

    private ContextMachineCache() {
        this.contextInstanceByContextInstanceIdCache = new ConcurrentHashMap<>();
        this.contextNames = new HashSet<>();
    }

    public void put(ContextMachine contextMachine)
    {
        // Note, we can now have multiple instances per plan, so the ContextNameCache will contain the latest only.
        this.contextInstanceByContextInstanceIdCache.put(contextMachine.getContext().getId(), contextMachine);
        this.contextNames.add(contextMachine.getContext().getName());
        if (!InstanceStatus.PREPARED.equals(contextMachine.getContext().getStatus())) {
            contextMachine.registerToNotificationMonitors();
        }
    }

    /**
     * @todo @Mick
     * Now that we have the potential to have multiple instances per plan, we can't rely on contextName/planName
     * to return a single ContextMachine ... however the REST status API currently needs this.
     *
     * Within the next few dev days I expect we will make a decision on how best to handle this e.g. have the rest
     * services return lists (since a single call for planName status could return multiple ContextMachines) or
     * we just return the first and assume multi-instance is sufficiently rare (suspect this will not be the case)
     *
     * @param contextName / planName to lookup
     * @return the first context machine that has the contextName / planName
     */
    public ContextMachine getFirstByContextName(final String contextName) {
        final List<ContextMachine> deleteMeSoon = getAllByContextName(contextName);
        if (deleteMeSoon != null && ! deleteMeSoon.isEmpty()) {
            return deleteMeSoon.get(0);
        } else {
            return null;
        }
    }

    /**
     * A single plan can have multiple instances
     *
     * @param contextName / planName to find
     * @return all the instances that are for the given plan
     */
    public List<ContextMachine> getAllByContextName(String contextName)
    {
        logger.debug(String.format("%s attempting to get context using context name[%s]"
            , this, contextName));

        if(contextName == null) return null;

        return this.contextInstanceByContextInstanceIdCache.values().stream()
            .filter(contextMachine -> contextMachine.getContext().getName().equals(contextName))
            .collect(Collectors.toList());
    }

    /**
     * This returns plans that are running and not in a PREPARED state
     *
     * @param contextName / planName to find
     * @return all running instances that are for the given plan
     */
    public List<ContextMachine> getAllRunningByContextName(String contextName) {
        List<ContextMachine> contextMachines = getAllByContextName(contextName);
        contextMachines.removeIf(contextMachine -> contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED));
        return contextMachines;
    }

    public ContextMachine getByContextInstanceId(String contextInstanceId)
    {
        logger.debug(String.format("Attempting to get context using context instance id[%s]"
            , contextInstanceId));

        if(contextInstanceId == null) return null;

        return this.contextInstanceByContextInstanceIdCache.get(contextInstanceId);
    }

    /**
     * Gets a list of ContextInstances based on which environmentGroup they belong to.
     * @param environmentGroup To check the cache
     * @param ignoreEnvironmentGroup set to true to target all active context instance regardless of what group it belongs to.
     * @return list of contextInstanceId in the cache that belongs to the environmentGroup
     */
    public List<String> getListOfContextInstanceIdByEnvironmentGroup(String environmentGroup, boolean ignoreEnvironmentGroup) {

        List<String> contextInstanceIdList = new ArrayList<>();
        contextInstanceByContextInstanceIdCache.forEach((contextInstance, contextMachine) -> {
                // ignoreEnvironmentGroup = true when ignore the environment group and return all context instances
                if (ignoreEnvironmentGroup) {
                    contextInstanceIdList.add(contextInstance);
                } else if (StringUtils.equalsIgnoreCase(contextMachine.getContext().getEnvironmentGroup(), environmentGroup)) {
                    contextInstanceIdList.add(contextInstance);
                }
            });
        logger.debug("Found {} context instances. environmentGroup=[{}], ignoreEnvironmentGroup=[{}]",
            contextInstanceIdList.size(), environmentGroup, ignoreEnvironmentGroup);
        return contextInstanceIdList;
    }


    public boolean containsInstanceIdentifier(String contextInstanceId)
    {
        if(contextInstanceId == null) return false;

        boolean result = this.contextInstanceByContextInstanceIdCache.containsKey(contextInstanceId);

        logger.debug(String.format("Check contains[%s] - result [%s]"
            , contextInstanceId, result));
        return result;
    }

    /**
     * A copy (so as not to break encapsulation) of the complete set of context names
     * @return a set of context names currently dealt with by this machine.
     */
    public Set<String> contextNames() {
        return Set.copyOf(this.contextNames);
    }

    public Set<String> contextInstanceIdentifiers() {
        return this.contextInstanceByContextInstanceIdCache.keySet();
    }

    public void remove(ContextMachine contextMachine)
    {
        contextMachine.unregisterToNotificationMonitors();
        this.contextInstanceByContextInstanceIdCache.remove(contextMachine.getContext().getId(), contextMachine);
        contextNames.remove(contextMachine.getContext().getName());
    }

    /**
     * This is intended to support testability and remove the need for reflective access
     */
    public void resetAllCache() {
        contextInstanceByContextInstanceIdCache.clear();
        contextNames.clear();
    }

    /**
     * This is intended to support testability and remove the need for reflective access
     */
    public boolean cacheIsEmpty() {
        return contextInstanceByContextInstanceIdCache.isEmpty() && contextNames.isEmpty() ;
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
