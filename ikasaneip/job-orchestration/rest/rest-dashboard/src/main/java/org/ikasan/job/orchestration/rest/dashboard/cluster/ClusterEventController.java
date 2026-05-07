package org.ikasan.job.orchestration.rest.dashboard.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.broadcast.*;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.event.ContextInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.event.JobLockCacheEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/clusterEvents")
public class ClusterEventController {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterEventController.class);

    private final ObjectMapper objectMapper;

    public ClusterEventController() {
        this.objectMapper = ObjectMapperFactory.newInstance();
    }

    @PostMapping("/context-instance-state-change")
    public ResponseEntity<Void> handleContextInstanceStateChange(@RequestBody String eventJson) {
        try {
            ContextInstanceStateChangeEvent event = objectMapper.readValue(eventJson, ContextInstanceStateChangeEventImpl.class);
            ContextInstanceStateChangeEventBroadcaster.localBroadcast(event);
            LOG.debug("Dispatched received cluster ContextInstanceStateChangeEvent to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster ContextInstanceStateChangeEvent", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/scheduler-job-state-change")
    public ResponseEntity<Void> handleSchedulerJobStateChange(@RequestBody String eventJson) {
        try {
            SchedulerJobInstanceStateChangeEvent event = objectMapper.readValue(eventJson, SchedulerJobInstanceStateChangeEventImpl.class);
            SchedulerJobStateChangeEventBroadcaster.localBroadcast(event);
            LOG.debug("Dispatched received cluster SchedulerJobInstanceStateChangeEvent to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster SchedulerJobInstanceStateChangeEvent", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/context-instance-saved")
    public ResponseEntity<Void> handleContextInstanceSaved(@RequestBody String eventJson) {
        try {
            ContextInstance contextInstance = objectMapper.readValue(eventJson, ContextInstanceImpl.class);
            ContextInstanceSavedEventBroadcaster.localBroadcast(contextInstance);
            LOG.debug("Dispatched received cluster ContextInstance saved event to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster ContextInstance saved event", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/context-instance-dlq")
    public ResponseEntity<Void> handleContextInstanceDlq(@RequestBody String eventJson) {
        try {
            ContextInstance contextInstance = objectMapper.readValue(eventJson, ContextInstanceImpl.class);
            ContextInstanceDlqEventBroadcaster.localBroadcast(contextInstance);
            LOG.debug("Dispatched received cluster ContextInstance DLQ event to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster ContextInstance DLQ event", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/context-template-saved")
    public ResponseEntity<Void> handleContextTemplateSaved(@RequestBody String eventJson) {
        try {
            ContextTemplate contextTemplate = objectMapper.readValue(eventJson, ContextTemplateImpl.class);
            ContextTemplateSavedEventBroadcaster.localBroadcast(contextTemplate);
            LOG.debug("Dispatched received cluster ContextTemplate saved event to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster ContextTemplate saved event", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/context-template-enable-disable")
    public ResponseEntity<Void> handleContextTemplateEnableDisable(@RequestBody String eventJson) {
        try {
            ContextTemplate contextTemplate = objectMapper.readValue(eventJson, ContextTemplateImpl.class);
            ContextTemplateEnableDisableEventBroadcaster.localBroadcast(contextTemplate);
            LOG.debug("Dispatched received cluster ContextTemplate enable/disable event to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster ContextTemplate enable/disable event", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/context-view-update")
    public ResponseEntity<Void> handleContextViewUpdate(@RequestBody String eventJson) {
        try {
            String message = objectMapper.readValue(eventJson, String.class);
            ContextViewUpdateEventBroadcaster.localBroadcast(message);
            LOG.debug("Dispatched received cluster context view update event to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster context view update event", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/new-scheduler-job")
    public ResponseEntity<Void> handleNewSchedulerJob(@RequestBody String eventJson) {
        try {
            SchedulerJob schedulerJob = objectMapper.readValue(eventJson, SchedulerJobImpl.class);
            NewSchedulerJobEventBroadcaster.localBroadcast(schedulerJob);
            LOG.debug("Dispatched received cluster new SchedulerJob event to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster new SchedulerJob event", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/job-lock-cache")
    public ResponseEntity<Void> handleJobLockCache(@RequestBody String eventJson) {
        try {
            JobLockCacheEvent event = objectMapper.readValue(eventJson, JobLockCacheEventImpl.class);
            JobLockCacheEventBroadcaster.localBroadcast(event);
            LOG.debug("Dispatched received cluster JobLockCacheEvent to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster JobLockCacheEvent", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
