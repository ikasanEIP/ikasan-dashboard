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
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.ikasan.job.orchestration.rest.dashboard.model.dto.ErrorDto;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.model.SchedulerJobWrapper;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Dashboard application implementing the REST contract
 */
@RequestMapping("/rest")
@RestController
public class SchedulerJobProvisionController
{
    private static Logger logger = LoggerFactory.getLogger(SchedulerJobProvisionController.class);

    private JobProvisionService jobProvisionService;
    private SchedulerJobService schedulerJobService;
    private ObjectMapper mapper;

    public SchedulerJobProvisionController(JobProvisionService jobProvisionService,
                                           SchedulerJobService schedulerJobService)
    {
        this.jobProvisionService = jobProvisionService;
        if (this.jobProvisionService == null)
        {
            throw new IllegalArgumentException("jobProvisionService cannot be null!");
        }

        this.schedulerJobService = schedulerJobService;
        if (this.schedulerJobService == null)
        {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }

        this.mapper = ConcurrentObjectMapperFactory.newInstance();
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("org.ikasan.spec.scheduled.job.model")
            .allowIfSubType("org.ikasan.job.orchestration.model.job")
            .allowIfSubType("org.ikasan.job.orchestration.model.context")
            .allowIfSubType("java.util.concurrent.CopyOnWriteArrayList")
            .allowIfSubType("java.util.ArrayList")
            .allowIfSubType("java.util.HashMap")
            .build();
        this.mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @RequestMapping(method = RequestMethod.PUT,
        value = "/provision/jobs")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity provisionJobs(@RequestBody String schedulerJobs)
    {
        try
        {
            logger.debug(schedulerJobs);

            SchedulerJobWrapper schedulerJobWrapper = this.mapper.readValue(schedulerJobs
                , SchedulerJobWrapper.class);

            logger.info("Attempting to provision {} scheduler jobs.", schedulerJobWrapper.getJobs().size());

            this.jobProvisionService.provisionJobs(schedulerJobWrapper.getJobs(), "system");

            logger.info("Successfully provisioned {} scheduler jobs.", schedulerJobWrapper.getJobs().size());
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to provision scheduler jobs! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET,
        value = "/job/{contextName}/{jobName}")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity provisionJobs(@PathVariable(value = "contextName") String contextName,
                                        @PathVariable(value = "jobName") String jobName)
    {
        try {
            logger.info("Attempting to get job {} for context {}.", jobName, contextName);

            SchedulerJobRecord schedulerJobRecord = this.schedulerJobService.findByContextNameAndJobName(contextName, jobName);

            return new ResponseEntity(schedulerJobRecord.getJob(), HttpStatus.OK);
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to provision scheduler jobs! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }
    }
}
