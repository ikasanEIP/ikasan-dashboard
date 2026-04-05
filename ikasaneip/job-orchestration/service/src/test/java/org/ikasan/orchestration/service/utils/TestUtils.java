package org.ikasan.orchestration.service.utils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachineImpl;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.topology.metadata.model.ModuleMetaDataImpl;
import org.springframework.test.util.ReflectionTestUtils;

public class TestUtils {
    public static final String AGENT_URL = "/agent/url/";

    public static ModuleMetaData createModuleMetaData(String id) {
        ModuleMetaDataImpl moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName("AGENT-"+id);
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
        param.setDefaultValue("defaultValue");
        param.setValue(value);
        return param;
    }

    public static void resetContextMachineCache() {
        ContextMachineCache instance = ContextMachineCache.instance();
        ConcurrentHashMap<String, ContextMachineImpl> contextInstanceByContextNameCache
            = (ConcurrentHashMap<String, ContextMachineImpl>) ReflectionTestUtils.getField(instance, "contextInstanceByContextInstanceIdCache");
        contextInstanceByContextNameCache.clear();

        Set<String> contextNames =
            (Set<String>) ReflectionTestUtils.getField(instance, "contextNames");
        contextNames.clear();
    }

}
