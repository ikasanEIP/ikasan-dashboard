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
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsImpl;
import org.ikasan.job.orchestration.rest.dashboard.model.scheduled.EmailNotificationDetailsRecordRestImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.rest.dashboard.model.dto.ErrorDto;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsWrapper;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Ikasan Development Team
 */
@RequestMapping("/rest")
@RestController
public class EmailNotificationDetailsController
{
    private static Logger logger = LoggerFactory.getLogger(EmailNotificationDetailsController.class);

    private ObjectMapper mapper;
    private EmailNotificationDetailsService emailNotificationDetailsService;

    public EmailNotificationDetailsController(EmailNotificationDetailsService emailNotificationDetailsService)
    {
        this.emailNotificationDetailsService = emailNotificationDetailsService;
        if(this.emailNotificationDetailsService == null) {
            throw new IllegalArgumentException("emailNotificationDetailsService cannot be null!");
        }
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
            ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
            objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

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

    // TODO should we add a delete??? Maybe not required
}
