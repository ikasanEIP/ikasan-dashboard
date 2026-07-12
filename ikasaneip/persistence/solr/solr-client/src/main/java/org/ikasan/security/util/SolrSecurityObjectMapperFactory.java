package org.ikasan.security.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.ikasan.security.model.*;
import org.ikasan.spec.security.model.*;
import org.springframework.security.core.GrantedAuthority;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class SolrSecurityObjectMapperFactory {

    /**
     * Create an JsonMapper instance that can be used in the
     * job orchestration module with all relevant concrete type
     * mappings.
     *
     * @return
     */
    public static JsonMapper newInstance() {
        final var simpleModule = new SimpleModule()
            .addAbstractTypeMapping(Policy.class, SolrPolicyImpl.class)
            .addAbstractTypeMapping(Role.class, SolrRoleImpl.class)
            .addAbstractTypeMapping(RoleModule.class, SolrRoleModuleImpl.class)
            .addAbstractTypeMapping(RoleJobPlan.class, SolrRoleJobPlanImpl.class)
            .addAbstractTypeMapping(AuthenticationMethod.class, SolrAuthenticationMethodImpl.class)
            .addAbstractTypeMapping(IkasanPrincipal.class, SolrIkasanPrincipalImpl.class)
            .addAbstractTypeMapping(User.class, SolrUserImpl.class)
            .addAbstractTypeMapping(GrantedAuthority.class, SolrPolicyImpl.class)
            .addAbstractTypeMapping(List.class, CopyOnWriteArrayList.class)
            .addAbstractTypeMapping(Map.class, ConcurrentHashMap.class)
            .addAbstractTypeMapping(Set.class, CopyOnWriteArraySet.class);

        return JsonMapper.builder().addModule(simpleModule)
            .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL)
                .withValueInclusion(JsonInclude.Include.NON_NULL))
            .configure(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
    }
}
