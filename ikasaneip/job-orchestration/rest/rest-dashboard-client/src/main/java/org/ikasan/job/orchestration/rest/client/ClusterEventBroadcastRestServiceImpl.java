package org.ikasan.job.orchestration.rest.client;

import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public class ClusterEventBroadcastRestServiceImpl extends ClusterPeerRestServiceImpl implements ClusterEventService {

    public static final String CLUSTER_EVENTS_BASE_URL                   = "/rest/clusterEvents";
    public static final String CONTEXT_INSTANCE_STATE_CHANGE_PATH        = CLUSTER_EVENTS_BASE_URL + "/context-instance-state-change";
    public static final String SCHEDULER_JOB_STATE_CHANGE_PATH           = CLUSTER_EVENTS_BASE_URL + "/scheduler-job-state-change";
    public static final String CONTEXT_INSTANCE_SAVED_PATH               = CLUSTER_EVENTS_BASE_URL + "/context-instance-saved";
    public static final String CONTEXT_INSTANCE_DLQ_PATH                 = CLUSTER_EVENTS_BASE_URL + "/context-instance-dlq";
    public static final String CONTEXT_TEMPLATE_SAVED_PATH               = CLUSTER_EVENTS_BASE_URL + "/context-template-saved";
    public static final String CONTEXT_TEMPLATE_ENABLE_DISABLE_PATH      = CLUSTER_EVENTS_BASE_URL + "/context-template-enable-disable";
    public static final String CONTEXT_VIEW_UPDATE_PATH                  = CLUSTER_EVENTS_BASE_URL + "/context-view-update";
    public static final String NEW_SCHEDULER_JOB_PATH                    = CLUSTER_EVENTS_BASE_URL + "/new-scheduler-job";
    public static final String JOB_LOCK_CACHE_PATH                       = CLUSTER_EVENTS_BASE_URL + "/job-lock-cache";

    public ClusterEventBroadcastRestServiceImpl(String baseUrl, Environment environment,
                                                HttpComponentsClientHttpRequestFactory factory) {
        super(baseUrl, environment, factory);
    }

    @Override
    public void broadcastContextInstanceStateChange(ContextInstanceStateChangeEvent event) {
        publish(CONTEXT_INSTANCE_STATE_CHANGE_PATH, event);
    }

    @Override
    public void broadcastSchedulerJobStateChange(SchedulerJobInstanceStateChangeEvent event) {
        publish(SCHEDULER_JOB_STATE_CHANGE_PATH, event);
    }

    @Override
    public void broadcastContextInstanceSaved(ContextInstance contextInstance) {
        publish(CONTEXT_INSTANCE_SAVED_PATH, contextInstance);
    }

    @Override
    public void broadcastContextInstanceDlq(ContextInstance contextInstance) {
        publish(CONTEXT_INSTANCE_DLQ_PATH, contextInstance);
    }

    @Override
    public void broadcastContextTemplateSaved(ContextTemplate contextTemplate) {
        publish(CONTEXT_TEMPLATE_SAVED_PATH, contextTemplate);
    }

    @Override
    public void broadcastContextTemplateEnableDisable(ContextTemplate contextTemplate) {
        publish(CONTEXT_TEMPLATE_ENABLE_DISABLE_PATH, contextTemplate);
    }

    @Override
    public void broadcastContextViewUpdate(String message) {
        publish(CONTEXT_VIEW_UPDATE_PATH, message);
    }

    @Override
    public void broadcastNewSchedulerJob(SchedulerJob schedulerJob) {
        publish(NEW_SCHEDULER_JOB_PATH, schedulerJob);
    }

    @Override
    public void broadcastJobLockCache(JobLockCacheEvent event) {
        publish(JOB_LOCK_CACHE_PATH, event);
    }
}
