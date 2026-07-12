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


import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsImpl;
import org.ikasan.job.orchestration.rest.dashboard.model.scheduled.EmailNotificationDetailsRecordRestImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.rest.dashboard.model.dto.ErrorDto;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsWrapper;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static tools.jackson.databind.DefaultTyping.NON_FINAL;

/**
 * @author Ikasan Development Team
 */
@RequestMapping("/rest")
@RestController
public class EmailNotificationDetailsController
{
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationDetailsController.class);

    private JsonMapper mapper;
    private EmailNotificationDetailsService emailNotificationDetailsService;

    public EmailNotificationDetailsController(EmailNotificationDetailsService emailNotificationDetailsService)
    {
        this.emailNotificationDetailsService = emailNotificationDetailsService;
        if(this.emailNotificationDetailsService == null) {
            throw new IllegalArgumentException("emailNotificationDetailsService cannot be null!");
        }
        this.mapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
    }

    @RequestMapping(method = RequestMethod.PUT,
        value = "/emailNotificationDetails/save")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity saveEmailNotificationDetails(@RequestBody String emailNotificationDetailsJsonPayload)
    {
        try
        {
            EmailNotificationDetails emailNotificationDetails = this.mapper.readValue(emailNotificationDetailsJsonPayload, EmailNotificationDetailsImpl.class);

            EmailNotificationDetailsRecord record = new EmailNotificationDetailsRecordRestImpl();
            record.setEmailNotificationDetails(emailNotificationDetails);
            record.setModifiedTimestamp(new Date().getTime());

            this.emailNotificationDetailsService.save(record);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to perform a save of EmailNotificationDetails! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity( HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT,
        value = "/emailNotificationDetails/saveAll")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity saveEmailNotificationDetailsAll(@RequestBody String emailNotificationDetailsWrapper)
    {
        try
        {
            PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("org.ikasan.spec.scheduled.notification.model")
                .allowIfSubType("org.ikasan.job.orchestration.model.notification")
                .allowIfSubType("java.util.ArrayList")
                .allowIfSubType("java.util.HashMap")
                .build();
            JsonMapper objectMapper = ObjectMapperFactory.newInstance();
            objectMapper = objectMapper.rebuild()
                .activateDefaultTyping(ptv, NON_FINAL)
                .build();

            EmailNotificationDetailsWrapper wrapper = objectMapper.readValue(emailNotificationDetailsWrapper, EmailNotificationDetailsWrapper.class);

            List<EmailNotificationDetailsRecord> records = new ArrayList<>();

            for (EmailNotificationDetails details : wrapper.getEmailNotificationDetails()) {
                EmailNotificationDetailsRecord record = new EmailNotificationDetailsRecordRestImpl();
                record.setEmailNotificationDetails(details);
                record.setModifiedTimestamp(new Date().getTime());
                records.add(record);
            }

            this.emailNotificationDetailsService.save(records);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to perform a save of EmailNotificationDetails! Error message ["
                    + e.getMessage() + "]"), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity( HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = {"/emailNotificationDetails/get/{contextName}/{limit}/{offset}"})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity getAllEmailNotificationByContextName(@PathVariable(value = "contextName") String contextName,
                                                               @PathVariable(value = "limit") int limit,
                                                               @PathVariable(value = "offset") int offset) {
        SearchResults<EmailNotificationDetailsRecord> notificationResults = emailNotificationDetailsService.findByContextName(contextName, limit, offset);

        JsonMapper objectMapper = ObjectMapperFactory.newInstance();
        objectMapper = objectMapper.rebuild().enable(SerializationFeature.INDENT_OUTPUT).build(); // Export with pretty lines
        try {
            String jsonString = objectMapper.writeValueAsString(notificationResults);
            if (jsonString == null || "".equals(jsonString)) {
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity(jsonString, HttpStatus.OK);
            }
        } catch (JacksonException e) {
            logger.error("Error converting to JSON", e);
            return new ResponseEntity("Error converting to JSON", HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = {"/emailNotificationDetails/getById/{jobName}/{childContextName}/{monitorType}"})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity getEmailNotificationByJobNameAndMonitorType(@PathVariable(value = "jobName") String jobName,
                                                                      @PathVariable(value = "childContextName") String childContextName,
                                                                      @PathVariable(value = "monitorType") String monitorType) {
        EmailNotificationDetailsRecord notificationResults = emailNotificationDetailsService.findByJobNameAndMonitorType(jobName, childContextName, monitorType);

        JsonMapper objectMapper = ObjectMapperFactory.newInstance();
        objectMapper = objectMapper.rebuild().enable(SerializationFeature.INDENT_OUTPUT).build();
        try {
            String jsonString = objectMapper.writeValueAsString(notificationResults);
            if (jsonString == null || "".equals(jsonString)) {
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity(jsonString, HttpStatus.OK);
            }
        } catch (JacksonException e) {
            logger.error("Error converting to JSON", e);
            return new ResponseEntity("Error converting to JSON", HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(method = RequestMethod.DELETE,
        value = "/emailNotificationDetails/deleteByContextName/{contextName}")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity deleteByContextName(@PathVariable(value = "contextName") String contextName) {
        try {
            emailNotificationDetailsService.deleteByContextName(contextName);
            return new ResponseEntity(HttpStatus.OK);

        } catch (Exception e) {
            String message = String.format("Got exception trying to delete all notification for the context [%s]. Error [%s]", contextName, e.getMessage());
            logger.warn(message);
            return new ResponseEntity(message, HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(method = RequestMethod.DELETE,
        value = "/emailNotificationDetails/deleteById/{jobName}/{childContextName}/{monitorType}")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity deleteByJobNameAndMonitorType(@PathVariable(value = "jobName") String jobName,
                                                        @PathVariable(value = "childContextName") String childContextName,
                                                        @PathVariable(value = "monitorType") String monitorType) {
        try {
            emailNotificationDetailsService.deleteByJobNameAndMonitorType(jobName, childContextName, monitorType);
            return new ResponseEntity(HttpStatus.OK);

        } catch (Exception e) {
            String message = String.format("Got exception trying to delete notification for the jobName [%s], " +
                "childContextName [%s] and monitorType [%s]. Error [%s]", jobName, childContextName, monitorType, e.getMessage());
            logger.warn(message);
            return new ResponseEntity(message, HttpStatus.BAD_REQUEST);
        }
    }
}
