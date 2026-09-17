package org.ikasan.mongo.persistence.setup.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.ikasan.mongo.persistence.setup.model.MongoDashboardSetupItemImpl;
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

/**
 * Factory for creating configured JsonMapper instances for MongoDB setup serialization.
 */
public class MongoSetupObjectMapperFactory {

    /**
     * Create a JsonMapper instance that can be used in the
     * setup module with all relevant concrete type mappings.
     *
     * @return configured JsonMapper instance
     */
    public static JsonMapper newInstance() {
        final var simpleModule = new SimpleModule()
            .addAbstractTypeMapping(DashboardSetupItem.class, MongoDashboardSetupItemImpl.class)
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
