package org.ikasan.job.orchestration.rest.dashboard.cluster;

import org.ikasan.job.orchestration.broadcast.*;
import org.ikasan.job.orchestration.rest.client.ClusterEventRestServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class ClusterBroadcastAutoConfiguration {

    @Value("${ikasan.dashboard.cluster.node-urls:}")
    private String clusterNodeUrlsProperty;

    @Resource
    private ClusterEventRestServiceImpl clusterEventRestService;

    @Bean
    public ClusterEventController clusterEventController() {
        return new ClusterEventController();
    }

    @Bean
    public ContextInstanceStateChangeEventRemoteBroadcastListenerImpl contextInstanceStateChangeEventRemoteBroadcastListener() {
        ContextInstanceStateChangeEventRemoteBroadcastListenerImpl listener =
            new ContextInstanceStateChangeEventRemoteBroadcastListenerImpl(clusterEventRestService, clusterNodeUrls());
        ContextInstanceStateChangeEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public SchedulerJobStateChangeEventRemoteBroadcastListenerImpl schedulerJobStateChangeEventRemoteBroadcastListener() {
        SchedulerJobStateChangeEventRemoteBroadcastListenerImpl listener =
            new SchedulerJobStateChangeEventRemoteBroadcastListenerImpl(clusterEventRestService, clusterNodeUrls());
        SchedulerJobStateChangeEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public ContextInstanceSavedEventRemoteBroadcastListenerImpl contextInstanceSavedEventRemoteBroadcastListener() {
        ContextInstanceSavedEventRemoteBroadcastListenerImpl listener =
            new ContextInstanceSavedEventRemoteBroadcastListenerImpl(clusterEventRestService, clusterNodeUrls());
        ContextInstanceSavedEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public ContextInstanceDlqEventRemoteBroadcastListenerImpl contextInstanceDlqEventRemoteBroadcastListener() {
        ContextInstanceDlqEventRemoteBroadcastListenerImpl listener =
            new ContextInstanceDlqEventRemoteBroadcastListenerImpl(clusterEventRestService, clusterNodeUrls());
        ContextInstanceDlqEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public ContextTemplateSavedEventRemoteBroadcastListenerImpl contextTemplateSavedEventRemoteBroadcastListener() {
        ContextTemplateSavedEventRemoteBroadcastListenerImpl listener =
            new ContextTemplateSavedEventRemoteBroadcastListenerImpl(clusterEventRestService, clusterNodeUrls());
        ContextTemplateSavedEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl contextTemplateEnableDisableEventRemoteBroadcastListener() {
        ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl listener =
            new ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl(clusterEventRestService, clusterNodeUrls());
        ContextTemplateEnableDisableEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public ContextViewUpdateEventRemoteBroadcastListenerImpl contextViewUpdateEventRemoteBroadcastListener() {
        ContextViewUpdateEventRemoteBroadcastListenerImpl listener =
            new ContextViewUpdateEventRemoteBroadcastListenerImpl(clusterEventRestService, clusterNodeUrls());
        ContextViewUpdateEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public NewSchedulerJobEventRemoteBroadcastListenerImpl newSchedulerJobEventRemoteBroadcastListener() {
        NewSchedulerJobEventRemoteBroadcastListenerImpl listener =
            new NewSchedulerJobEventRemoteBroadcastListenerImpl(clusterEventRestService, clusterNodeUrls());
        NewSchedulerJobEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public JobLockCacheEventRemoteBroadcastListenerImpl jobLockCacheEventRemoteBroadcastListener() {
        JobLockCacheEventRemoteBroadcastListenerImpl listener =
            new JobLockCacheEventRemoteBroadcastListenerImpl(clusterEventRestService, clusterNodeUrls());
        JobLockCacheEventBroadcaster.register(listener);
        return listener;
    }

    private List<String> clusterNodeUrls() {
        return Arrays.stream(clusterNodeUrlsProperty.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
}
