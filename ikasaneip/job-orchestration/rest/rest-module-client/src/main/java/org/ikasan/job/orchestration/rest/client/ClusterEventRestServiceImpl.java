package org.ikasan.job.orchestration.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.rest.client.ModuleRestService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public class ClusterEventRestServiceImpl extends ModuleRestService {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterEventRestServiceImpl.class);

    public static final String CLUSTER_EVENTS_BASE_URL = "/rest/clusterEvents";
    public static final String CONTEXT_INSTANCE_STATE_CHANGE_PATH  = CLUSTER_EVENTS_BASE_URL + "/context-instance-state-change";
    public static final String SCHEDULER_JOB_STATE_CHANGE_PATH     = CLUSTER_EVENTS_BASE_URL + "/scheduler-job-state-change";
    public static final String CONTEXT_INSTANCE_SAVED_PATH         = CLUSTER_EVENTS_BASE_URL + "/context-instance-saved";
    public static final String CONTEXT_INSTANCE_DLQ_PATH           = CLUSTER_EVENTS_BASE_URL + "/context-instance-dlq";
    public static final String CONTEXT_TEMPLATE_SAVED_PATH         = CLUSTER_EVENTS_BASE_URL + "/context-template-saved";
    public static final String CONTEXT_TEMPLATE_ENABLE_DISABLE_PATH = CLUSTER_EVENTS_BASE_URL + "/context-template-enable-disable";
    public static final String CONTEXT_VIEW_UPDATE_PATH            = CLUSTER_EVENTS_BASE_URL + "/context-view-update";
    public static final String NEW_SCHEDULER_JOB_PATH              = CLUSTER_EVENTS_BASE_URL + "/new-scheduler-job";
    public static final String JOB_LOCK_CACHE_PATH                 = CLUSTER_EVENTS_BASE_URL + "/job-lock-cache";

    private final ObjectMapper objectMapper;

    public ClusterEventRestServiceImpl(Environment environment,
                                       HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        super(environment, httpComponentsClientHttpRequestFactory);
        this.objectMapper = ObjectMapperFactory.newInstance();
    }

    public void broadcastContextInstanceStateChange(String nodeUrl, ContextInstanceStateChangeEvent event) {
        post(nodeUrl, CONTEXT_INSTANCE_STATE_CHANGE_PATH, event, "ContextInstanceStateChangeEvent");
    }

    public void broadcastSchedulerJobStateChange(String nodeUrl, SchedulerJobInstanceStateChangeEvent event) {
        post(nodeUrl, SCHEDULER_JOB_STATE_CHANGE_PATH, event, "SchedulerJobInstanceStateChangeEvent");
    }

    public void broadcastContextInstanceSaved(String nodeUrl, ContextInstance contextInstance) {
        post(nodeUrl, CONTEXT_INSTANCE_SAVED_PATH, contextInstance, "ContextInstance saved");
    }

    public void broadcastContextInstanceDlq(String nodeUrl, ContextInstance contextInstance) {
        post(nodeUrl, CONTEXT_INSTANCE_DLQ_PATH, contextInstance, "ContextInstance DLQ");
    }

    public void broadcastContextTemplateSaved(String nodeUrl, ContextTemplate contextTemplate) {
        post(nodeUrl, CONTEXT_TEMPLATE_SAVED_PATH, contextTemplate, "ContextTemplate saved");
    }

    public void broadcastContextTemplateEnableDisable(String nodeUrl, ContextTemplate contextTemplate) {
        post(nodeUrl, CONTEXT_TEMPLATE_ENABLE_DISABLE_PATH, contextTemplate, "ContextTemplate enable/disable");
    }

    public void broadcastContextViewUpdate(String nodeUrl, String message) {
        post(nodeUrl, CONTEXT_VIEW_UPDATE_PATH, message, "context view update");
    }

    public void broadcastNewSchedulerJob(String nodeUrl, SchedulerJob schedulerJob) {
        post(nodeUrl, NEW_SCHEDULER_JOB_PATH, schedulerJob, "new SchedulerJob");
    }

    public void broadcastJobLockCache(String nodeUrl, JobLockCacheEvent event) {
        post(nodeUrl, JOB_LOCK_CACHE_PATH, event, "JobLockCacheEvent");
    }

    private void post(String nodeUrl, String path, Object payload, String eventDescription) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(json, createHttpHeaders());
            restTemplate.exchange(nodeUrl + path, HttpMethod.POST, entity, Void.class);
            LOG.debug("Cluster broadcast of [{}] sent to node [{}]", eventDescription, nodeUrl);
        } catch (Exception e) {
            LOG.error("Failed to broadcast [{}] to cluster node [{}]", eventDescription, nodeUrl, e);
        }
    }
}
