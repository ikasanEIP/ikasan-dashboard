package org.ikasan.security.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.ikasan.scheduled.context.model.*;
import org.ikasan.scheduled.event.model.SolrContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduled.event.model.SolrContextualisedSchedulerJobInitiationEventImpl;
import org.ikasan.scheduled.event.model.SolrSchedulerJobInitiationEventImpl;
import org.ikasan.scheduled.instance.model.*;
import org.ikasan.scheduled.job.model.*;
import org.ikasan.scheduled.profile.model.SolrContextProfileImpl;
import org.ikasan.scheduled.profile.model.SolrContextProfileRecordImpl;
import org.ikasan.security.model.*;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.ReplacementPair;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.security.model.*;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class SolrSecurityObjectMapperFactory {

    /**
     * Create an ObjectMapper instance that can be used in the
     * job orchestration module with all relevant concrete type
     * mappings.
     *
     * @return
     */
    public static ObjectMapper newInstance() {
        ObjectMapper objectMapper = new ObjectMapper();
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

        objectMapper.registerModule(simpleModule);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper;
    }
}
