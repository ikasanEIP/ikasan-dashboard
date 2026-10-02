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
package org.ikasan.rest.dashboard;

import org.ikasan.esb.service.support.DirectoryZipUtil;
import org.ikasan.esb.service.systemevent.SystemEventSearchFilterImpl;
import org.ikasan.rest.dashboard.model.dto.ErrorDto;
import org.ikasan.rest.dashboard.model.systemevent.SystemEventImpl;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventRecord;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Module application implementing the REST contract
 */
@RequestMapping("/rest")
@RestController
public class SystemEventController
{
    private static Logger logger = LoggerFactory.getLogger(SystemEventController.class);

    private BatchInsert<SystemEvent> batchInsert;

    private SystemEventSearchService systemEventSearchService;

    private final JsonMapper mapper = JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();

    public SystemEventController(BatchInsert<SystemEvent> batchInsert,
                                 SystemEventSearchService systemEventSearchService)
    {
        this.batchInsert = batchInsert;
        if (this.batchInsert == null)
        {
            throw new IllegalArgumentException("BatchInsert cannot be null!");
        }
        this.systemEventSearchService = systemEventSearchService;
        if (this.systemEventSearchService == null)
        {
            throw new IllegalArgumentException("systemEventSearchService cannot be null!");
        }
    }

    @RequestMapping(method = RequestMethod.PUT,
        value = "/harvest/systemevents")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity harvestSystemEvents(@RequestBody String systemEventsJsonPayload)
    {
        try
        {
            logger.debug(systemEventsJsonPayload);
            List<SystemEvent> systemEvents = this.mapper.readValue(systemEventsJsonPayload
                , mapper.getTypeFactory().constructCollectionType(List.class, SystemEventImpl.class));
            this.batchInsert.insert(systemEvents);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new ResponseEntity(
                new ErrorDto("An error has occurred attempting to perform a batch insert of SystemEvents! Error message ["
                    + e.getMessage() + "]"),
                HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = {"/systemevents/last24hours/zip"}, produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<StreamingResponseBody> systemEventZip() {
        // Get last 24 hours system events
        SystemEventSearchFilter systemEventSearchFilter = new SystemEventSearchFilterImpl();
        systemEventSearchFilter.setStartTime(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        systemEventSearchFilter.setEndTime(System.currentTimeMillis());
        try {
            AtomicInteger counter = new AtomicInteger(1);
            Map<String, String> systemEvents = this.systemEventSearchService
                .findByFilter(systemEventSearchFilter, -1, -1, null, null).getResultList()
                .stream().map(systemEvent -> ((SystemEventRecord)systemEvent).getPayload())
                .collect(Collectors.toMap(systemEvent -> "systemEvent-"+counter.incrementAndGet()+".json"
                    , Function.identity()));

            StreamingResponseBody body = outputStream ->
                DirectoryZipUtil.zipFileContents(systemEvents, outputStream);

            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment;filename=systemEvents-"
                    + System.currentTimeMillis() + ".zip")
                .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(body);
        } catch (Exception e) {
            logger.error("A general error has occurred when trying to download the zipped system events from [{}], to [{}]!"
                , systemEventSearchFilter.getStartTime(), systemEventSearchFilter.getEndTime(), e);
            return ResponseEntity
                .internalServerError()
                .header("Content-Disposition", "attachment;filename=error.txt")
                .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(outputStream -> {
                    String errorMsg = String.format("A general error has occurred when trying to download the zipped system events from [%s], to [%s]!"
                        , systemEventSearchFilter.getStartTime(), systemEventSearchFilter.getEndTime());
                    outputStream.write(errorMsg.getBytes());
                    outputStream.close();
                });
        }
    }
}
