package org.ikasan.notification;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TestUtils {

    public static void resetContextMachineCache() {
        ContextMachineCache instance = ContextMachineCache.instance();
        ConcurrentHashMap<String, ContextMachine> contextInstanceByContextNameCache
            = (ConcurrentHashMap<String, ContextMachine>) ReflectionTestUtils.getField(instance, "contextInstanceByContextInstanceIdCache");
        contextInstanceByContextNameCache.clear();

        Set<String> contextNames =
            (Set<String>) ReflectionTestUtils.getField(instance, "contextNames");
        contextNames.clear();
    }

}