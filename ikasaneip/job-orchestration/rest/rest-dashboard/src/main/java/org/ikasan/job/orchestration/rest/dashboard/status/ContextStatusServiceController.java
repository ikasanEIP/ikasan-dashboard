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

package org.ikasan.job.orchestration.rest.dashboard.status;

import java.util.Map;

import org.ikasan.job.orchestration.rest.dashboard.model.dto.ErrorDto;
import org.ikasan.spec.scheduled.context.service.ContextStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/rest/context/status")
@RestController
public class ContextStatusServiceController {

    private static Logger LOG = LoggerFactory.getLogger(ContextStatusServiceController.class);

    private ContextStatusService contextStatusService;

    public ContextStatusServiceController(ContextStatusService contextStatusService) {

        if (contextStatusService == null) {
            throw new IllegalArgumentException("contextStatusService cannot be null!");
        }

        this.contextStatusService = contextStatusService;
    }


    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity getContextStatus(@RequestParam Map<String, String> params) {
        String instanceName = params.get("instanceName");
        String contextName = params.get("contextName");

        String contextNameStatus;

        try {
            contextNameStatus = contextStatusService.getContextStatus(instanceName, contextName);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            String errorMessage =
                String.format("An error has occurred attempting to get status for instance %s and context %s", instanceName, contextName);
            return new ResponseEntity(
                new ErrorDto(errorMessage + "! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }

        String infoMessage = String.format("Got status %s for instance %s and context %s", contextNameStatus, instanceName, contextName);
        LOG.info(infoMessage);

        return new ResponseEntity(contextNameStatus, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/job")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity getContextStatusForJob(@RequestParam Map<String, String> params) {
        String instanceName = params.get("instanceName");
        String contextName = params.get("contextName");
        String jobIdentifier = params.get("jobIdentifier");

        String contextNameStatus;

        try {
            contextNameStatus = contextStatusService.getContextStatusForJob(instanceName, contextName, jobIdentifier);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            String errorMessage = String.format("An error has occurred attempting to get status for instance %s, context %s, jobIdentifier %s!",
                instanceName, contextName, jobIdentifier);
            return new ResponseEntity(
                new ErrorDto(errorMessage + " Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }

        String infoMessage = String.format("Got status %s for instance %s and context %s and jobIdentifier %s",
            contextNameStatus, instanceName, contextName, jobIdentifier);
        LOG.info(infoMessage);

        return new ResponseEntity(contextNameStatus, HttpStatus.OK);
    }
}
