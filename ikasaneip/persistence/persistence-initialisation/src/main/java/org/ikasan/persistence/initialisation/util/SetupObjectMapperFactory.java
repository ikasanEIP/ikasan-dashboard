package org.ikasan.persistence.initialisation.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.ikasan.persistence.initialisation.model.DashboardSetupItemImpl;
import org.ikasan.spec.persistence.model.DashboardSetupItem;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class SetupObjectMapperFactory {

    /**
     * Create an JsonMapper instance that can be used in the
     * job orchestration module with all relevant concrete type
     * mappings.
     *
     * @return
     */
    public static JsonMapper newInstance() {
        final var simpleModule = new SimpleModule()
            .addAbstractTypeMapping(DashboardSetupItem.class, DashboardSetupItemImpl.class)
            .addAbstractTypeMapping(List.class, CopyOnWriteArrayList.class)
            .addAbstractTypeMapping(Map.class, ConcurrentHashMap.class)
            .addAbstractTypeMapping(Set.class, CopyOnWriteArraySet.class);

        return JsonMapper.builder().addModule(simpleModule)
            .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL)
                .withValueInclusion(JsonInclude.Include.NON_NULL))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
    }
}
