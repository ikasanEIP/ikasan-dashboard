package org.ikasan.orchestration.service.utils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.topology.metadata.model.ModuleMetaDataImpl;
import org.springframework.test.util.ReflectionTestUtils;

public class TestUtils {
    public static final String AGENT_URL = "/agent/url/";

    public static ModuleMetaData createModuleMetaData(String id) {
        ModuleMetaDataImpl moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setUrl(AGENT_URL + id);
        return moduleMetaData;
    }

    public static List<ContextParameterInstance> createParams() {
        return List.of(
            createParam("BusinessDate", "20220428"),
            createParam("ErrorSearch", "someValue"),
            createParam("UseBusinessDate", "1")
        );
    }

    public static ContextParameterInstanceImpl createParam(String name, String value) {
        ContextParameterInstanceImpl param = new ContextParameterInstanceImpl();
        param.setName(name);
        param.setType("java.lang.String");
        param.setValue(value);
        return param;
    }

    public static void resetContextMachineCache() {
        ContextMachineCache instance = ContextMachineCache.instance();
        ConcurrentHashMap<String, ContextMachine> contextInstanceByContextNameCache
            = (ConcurrentHashMap<String, ContextMachine>) ReflectionTestUtils.getField(instance, "contextInstanceByContextNameCache");
        contextInstanceByContextNameCache.clear();

        ConcurrentHashMap<String, ContextMachine> contextInstanceByContextInstanceIdCache =
            (ConcurrentHashMap<String, ContextMachine>) ReflectionTestUtils.getField(instance, "contextInstanceByContextInstanceIdCache");
        contextInstanceByContextInstanceIdCache.clear();
    }

}
