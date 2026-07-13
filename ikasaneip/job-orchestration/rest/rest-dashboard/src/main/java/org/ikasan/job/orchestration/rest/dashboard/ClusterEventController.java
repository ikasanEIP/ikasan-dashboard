/*
 * $Id$
 * $URL$
 *
 * ====================================================================
 * Ikasan Enterprise Integration Platform
 *
 * Distributed under the Modified BSD License.
 * Copyright notice: The copyright for this software and a full listing
 * of individual contributors are as shown in the packaged copyright.txt
 * file.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  - Neither the name of the ORGANIZATION nor the names of its contributors may
 *    be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 */
package org.ikasan.job.orchestration.rest.dashboard;

import org.ikasan.job.orchestration.broadcast.*;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.event.ContextInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.event.JobLockCacheEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.job.orchestration.rest.dashboard.model.dto.ErrorDto;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/rest/clusterEvents")
public class ClusterEventController {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterEventController.class);

    private final JsonMapper objectMapper;

    public ClusterEventController() {
        this.objectMapper = ObjectMapperFactory.newInstance();
    }

    @PostMapping("/context-instance-state-change")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity handleContextInstanceStateChange(@RequestBody String eventJson) {
        try {
            ContextInstanceStateChangeEvent event = objectMapper.readValue(eventJson, ContextInstanceStateChangeEventImpl.class);
            ContextInstanceStateChangeEventBroadcaster.instance().localBroadcast(event);
            LOG.debug("Dispatched received cluster ContextInstanceStateChangeEvent to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster ContextInstanceStateChangeEvent", e);
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to process ContextInstanceStateChangeEvent! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("/scheduler-job-state-change")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity handleSchedulerJobStateChange(@RequestBody String eventJson) {
        try {
            SchedulerJobInstanceStateChangeEvent event = objectMapper.readValue(eventJson, SchedulerJobInstanceStateChangeEventImpl.class);
            SchedulerJobStateChangeEventBroadcaster.instance().localBroadcast(event);
            LOG.debug("Dispatched received cluster SchedulerJobInstanceStateChangeEvent to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster SchedulerJobInstanceStateChangeEvent", e);
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to process SchedulerJobInstanceStateChangeEvent! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("/context-instance-saved")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity handleContextInstanceSaved(@RequestBody String eventJson) {
        try {
            ContextInstance contextInstance = objectMapper.readValue(eventJson, ContextInstanceImpl.class);
            ContextInstanceSavedEventBroadcaster.instance().localBroadcast(contextInstance);
            LOG.debug("Dispatched received cluster ContextInstance saved event to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster ContextInstance saved event", e);
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to process ContextInstance saved event! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("/context-instance-dlq")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity handleContextInstanceDlq(@RequestBody String eventJson) {
        try {
            ContextInstance contextInstance = objectMapper.readValue(eventJson, ContextInstanceImpl.class);
            ContextInstanceDlqEventBroadcaster.instance().localBroadcast(contextInstance);
            LOG.debug("Dispatched received cluster ContextInstance DLQ event to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster ContextInstance DLQ event", e);
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to process ContextInstance DLQ event! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("/context-template-saved")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity handleContextTemplateSaved(@RequestBody String eventJson) {
        try {
            ContextTemplate contextTemplate = objectMapper.readValue(eventJson, ContextTemplateImpl.class);
            ContextTemplateSavedEventBroadcaster.instance().localBroadcast(contextTemplate);
            LOG.debug("Dispatched received cluster ContextTemplate saved event to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster ContextTemplate saved event", e);
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to process ContextTemplate saved event! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("/context-template-enable-disable")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity handleContextTemplateEnableDisable(@RequestBody String eventJson) {
        try {
            ContextTemplate contextTemplate = objectMapper.readValue(eventJson, ContextTemplateImpl.class);
            ContextTemplateEnableDisableEventBroadcaster.instance().localBroadcast(contextTemplate);
            LOG.debug("Dispatched received cluster ContextTemplate enable/disable event to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster ContextTemplate enable/disable event", e);
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to process ContextTemplate enable/disable event! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("/context-view-update")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity handleContextViewUpdate(@RequestBody String eventJson) {
        try {
            String message = objectMapper.readValue(eventJson, String.class);
            ContextViewUpdateEventBroadcaster.instance().localBroadcast(message);
            LOG.debug("Dispatched received cluster context view update event to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster context view update event", e);
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to process context view update event! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("/new-scheduler-job")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity handleNewSchedulerJob(@RequestBody String eventJson) {
        try {
            SchedulerJob schedulerJob = objectMapper.readValue(eventJson, SchedulerJobImpl.class);
            NewSchedulerJobEventBroadcaster.instance().localBroadcast(schedulerJob);
            LOG.debug("Dispatched received cluster new SchedulerJob event to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster new SchedulerJob event", e);
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to process new SchedulerJob event! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("/job-lock-cache")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity handleJobLockCache(@RequestBody String eventJson) {
        try {
            JobLockCacheEvent event = objectMapper.readValue(eventJson, JobLockCacheEventImpl.class);
            JobLockCacheEventBroadcaster.instance().localBroadcast(event);
            LOG.debug("Dispatched received cluster JobLockCacheEvent to local listeners");
        } catch (Exception e) {
            LOG.error("Failed to process incoming cluster JobLockCacheEvent", e);
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to process JobLockCacheEvent! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }
}
