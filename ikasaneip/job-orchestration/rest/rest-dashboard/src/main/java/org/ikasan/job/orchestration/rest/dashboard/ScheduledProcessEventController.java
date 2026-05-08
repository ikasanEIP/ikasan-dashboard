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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.component.endpoint.bigqueue.builder.BigQueueMessageBuilder;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.rest.dashboard.model.dto.ErrorDto;
import org.ikasan.job.orchestration.rest.dashboard.model.scheduled.ScheduledProcessEventImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ikasan.quartz.AbstractDashboardSchedulerService.CONTEXT_INSTANCE_ID;

/**
 * Module application implementing the REST contract
 */
@RequestMapping("/rest")
@RestController
public class ScheduledProcessEventController
{
    private static Logger logger = LoggerFactory.getLogger(ScheduledProcessEventController.class);

    private ObjectMapper mapper;

    protected IBigQueue inboundQueue;

    private GlobalEventService globalEventService;

    /**
     * Constructs an instance of ScheduledProcessEventController.
     *
     * @param inboundQueue the queue used for processing inbound events; must not be null
     * @param globalEventService the service responsible for handling global events; must not be null
     *
     * @throws IllegalArgumentException if any of the provided parameters is null
     */
    public ScheduledProcessEventController(IBigQueue inboundQueue, GlobalEventService globalEventService)
    {
        this.inboundQueue = inboundQueue;
        if (this.inboundQueue == null)
        {
            throw new IllegalArgumentException("inboundQueue cannot be null!");
        }
        this.globalEventService = globalEventService;
        if (this.globalEventService == null)
        {
            throw new IllegalArgumentException("globalEventService cannot be null!");
        }
        this.mapper = ObjectMapperFactory.newInstance();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @RequestMapping(method = RequestMethod.PUT,
        value = "/event/scheduled")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity scheduledEvent(@RequestBody String scheduledProcessEventPayload)
    {
        try
        {
            logger.debug("Received - {}", scheduledProcessEventPayload);

            BigQueueMessage bigQueueMessage =
                new BigQueueMessageBuilder()
                    .withMessage(scheduledProcessEventPayload)
                    .withMessageProperties(getProperties(scheduledProcessEventPayload))
                    .build();

            this.inboundQueue.enqueue(mapper.writeValueAsBytes(bigQueueMessage));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to perform a batch insert of ScheduledProcessEvents! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * Allows an external service to issue a PUT request with the name of a Global Job Event to be sent to
     * all running Context Machines
     * @param globalEventJobName
     * @return http status, OK or BAD_REQUEST
     */
    @RequestMapping(method = RequestMethod.PUT,
        value = "/event/globalScheduled")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity globalScheduledEvent(@RequestBody String globalEventJobName) {

        try {
            logger.info("Received Global Event Job Name from external sources - {}", globalEventJobName);
            this.globalEventService.raiseGlobalEventJob(globalEventJobName);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to send global event to all active context instances ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    private Map<String, String> getProperties(String scheduledProcessEventPayload) {
        Map<String, String> map = new HashMap<>();
        try {
            ContextualisedScheduledProcessEvent scheduledProcessEvent
                = mapper.readValue(scheduledProcessEventPayload, ContextualisedScheduledProcessEventImpl.class);
            if (scheduledProcessEvent.getContextName() != null) {
                map.put("contextName", scheduledProcessEvent.getContextName());
            }
            if (scheduledProcessEvent.getContextInstanceId() != null) {
                map.put(CONTEXT_INSTANCE_ID, scheduledProcessEvent.getContextInstanceId());
            }
        } catch (Exception e) {
            logger.warn("Could not deserialise payload " + scheduledProcessEventPayload);
        }
        return map;
    }
}
