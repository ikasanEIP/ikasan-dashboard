package org.ikasan.orchestration.service.utils;

import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.topology.metadata.model.ModuleMetaDataImpl;

import java.util.List;

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
}
