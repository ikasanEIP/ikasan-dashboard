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

import org.ikasan.rest.dashboard.component.converter.*;
import org.ikasan.rest.dashboard.model.dto.ErrorDto;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for ESB data sharing - exposes entity data query APIs
 * for upstream dashboard to share data with downstream dashboards.
 *
 * This controller delegates to ESBSearchService to retrieve entity data
 * including wiretap, errors, exclusions, replays, topology, and configuration metadata.
 *
 * @see org.ikasan.spec.search.service.ESBSearchService
 */
@RequestMapping("/rest/data-sharing")
@RestController
public class DataSharingController {

    private static Logger logger = LoggerFactory.getLogger(DataSharingController.class);

    private ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService;

    private final IkasanESBDocumentToWiretapEventConverter wiretapEventConverter = new IkasanESBDocumentToWiretapEventConverter();
    private final IkasanESBDocumentToErrorOccurenceConverter esbDocumentToErrorOccurenceConverter
        = new IkasanESBDocumentToErrorOccurenceConverter();
    private final IkasanESBDocumentToExclusionEventConverter esbDocumentToExclusionEventConverter
        = new IkasanESBDocumentToExclusionEventConverter();
    private final IkasanESBDocumentToModuleMetaDataConverter ikasanESBDocumentToModuleMetaDataConverter
        = new IkasanESBDocumentToModuleMetaDataConverter();
    private final IkasanESBDocumentToConfigurationMetaDataConverter ikasanESBDocumentToConfigurationMetaDataConverter
        = new IkasanESBDocumentToConfigurationMetaDataConverter();
    private final IkasanESBDocumentToReplayEventConverter ikasanESBDocumentToReplayEventConverter
        = new IkasanESBDocumentToReplayEventConverter();

    /**
     * Constructor
     *
     * @param esbSearchService the ESB search service for querying entity data
     */
    public DataSharingController(@Qualifier("esbSearchService") ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> esbSearchService) {
        this.esbSearchService = esbSearchService;
        if (this.esbSearchService == null) {
            throw new IllegalArgumentException("esbSearchService cannot be null!");
        }
    }

    /**
     * Query wiretap data for data sharing
     *
     * @param fromTimestamp start timestamp (milliseconds)
     * @param toTimestamp end timestamp (milliseconds)
     * @param moduleNames optional filter by module names
     * @param limit maximum records to return
     * @param offset pagination offset
     * @return ResponseEntity containing wiretap data and metadata
     */
    @RequestMapping(method = RequestMethod.GET, value = "/wiretap")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<Map<String, Object>> queryWiretap(
            @RequestParam("fromTimestamp") long fromTimestamp,
            @RequestParam("toTimestamp") long toTimestamp,
            @RequestParam(value = "moduleNames", required = false) List<String> moduleNames,
            @RequestParam(value = "limit", defaultValue = "1000") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {

        try {
            logger.debug("Query wiretap data - fromTimestamp: {}, toTimestamp: {}, moduleNames: {}, limit: {}, offset: {}",
                    fromTimestamp, toTimestamp, moduleNames, limit, offset);

            List<String> entityTypes = List.of(EntityDao.WIRETAP_TYPE);

            IkasanDocumentSearchResults searchResults = esbSearchService.searchByHarvestReceivedTime(
                    moduleNames != null ? new HashSet<>(moduleNames) : Set.of(),
                    Set.of(), // flow names
                    Set.of(), // component names
                    null, // event id
                    null, // payload content
                    fromTimestamp,
                    toTimestamp,
                    offset,
                    limit,
                    entityTypes,
                    false, // negate query
                    null,  // sort field
                    null   // sort order
            );

            Map<String, Object> response = new HashMap<>();
            response.put("data", searchResults.getResultList().stream()
                .map(wiretapEventConverter::convert)
                .collect(Collectors.toList()));
            response.put("totalCount", searchResults.getTotalNumberOfResults());
            response.put("hasMore", (offset + limit) < searchResults.getTotalNumberOfResults());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error querying wiretap data", e);
            return new ResponseEntity(
                    new ErrorDto("An error occurred querying wiretap data: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Query error data for data sharing
     *
     * @param fromTimestamp start timestamp (milliseconds)
     * @param toTimestamp end timestamp (milliseconds)
     * @param moduleNames optional filter by module names
     * @param limit maximum records to return
     * @param offset pagination offset
     * @return ResponseEntity containing error data and metadata
     */
    @RequestMapping(method = RequestMethod.GET, value = "/errors")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<Map<String, Object>> queryErrors(
            @RequestParam("fromTimestamp") long fromTimestamp,
            @RequestParam("toTimestamp") long toTimestamp,
            @RequestParam(value = "moduleNames", required = false) List<String> moduleNames,
            @RequestParam(value = "limit", defaultValue = "1000") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {

        try {
            logger.debug("Query error data - fromTimestamp: {}, toTimestamp: {}, moduleNames: {}, limit: {}, offset: {}",
                    fromTimestamp, toTimestamp, moduleNames, limit, offset);

            List<String> entityTypes = List.of(EntityDao.ERROR);

            IkasanDocumentSearchResults searchResults = esbSearchService.searchByHarvestReceivedTime(
                    moduleNames != null ? new HashSet<>(moduleNames) : Set.of(),
                    Set.of(), // flow names
                    Set.of(), // component names
                    null, // event id
                    null, // payload content
                    fromTimestamp,
                    toTimestamp,
                    offset,
                    limit,
                    entityTypes,
                    false, // negate query
                    null,  // sort field
                    null   // sort order
            );

            Map<String, Object> response = new HashMap<>();
            response.put("data", searchResults.getResultList().stream()
                .map(this.esbDocumentToErrorOccurenceConverter::convert)
                .toList());
            response.put("totalCount", searchResults.getTotalNumberOfResults());
            response.put("hasMore", (offset + limit) < searchResults.getTotalNumberOfResults());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error querying error data", e);
            return new ResponseEntity(
                    new ErrorDto("An error occurred querying error data: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Query exclusion/hospital data for data sharing
     *
     * @param fromTimestamp start timestamp (milliseconds)
     * @param toTimestamp end timestamp (milliseconds)
     * @param moduleNames optional filter by module names
     * @param limit maximum records to return
     * @param offset pagination offset
     * @return ResponseEntity containing exclusion data and metadata
     */
    @RequestMapping(method = RequestMethod.GET, value = "/exclusions")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<Map<String, Object>> queryExclusions(
            @RequestParam("fromTimestamp") long fromTimestamp,
            @RequestParam("toTimestamp") long toTimestamp,
            @RequestParam(value = "moduleNames", required = false) List<String> moduleNames,
            @RequestParam(value = "limit", defaultValue = "1000") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {

        try {
            logger.debug("Query exclusion data - fromTimestamp: {}, toTimestamp: {}, moduleNames: {}, limit: {}, offset: {}",
                    fromTimestamp, toTimestamp, moduleNames, limit, offset);

            List<String> entityTypes = List.of(EntityDao.EXCLUSION);

            IkasanDocumentSearchResults searchResults = esbSearchService.searchByHarvestReceivedTime(
                    moduleNames != null ? new HashSet<>(moduleNames) : Set.of(),
                    Set.of(), // flow names
                    null, // component names
                    null, // event id
                    null, // payload content
                    fromTimestamp,
                    toTimestamp,
                    offset,
                    limit,
                    entityTypes,
                    false, // negate query
                    null,  // sort field
                    null   // sort order
            );

            Map<String, Object> response = new HashMap<>();
            response.put("data", searchResults.getResultList().stream()
                .map(this.esbDocumentToExclusionEventConverter::convert)
                .toList());
            response.put("totalCount", searchResults.getTotalNumberOfResults());
            response.put("hasMore", (offset + limit) < searchResults.getTotalNumberOfResults());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error querying exclusion data", e);
            return new ResponseEntity(
                    new ErrorDto("An error occurred querying exclusion data: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Query replay data for data sharing
     *
     * @param fromTimestamp start timestamp (milliseconds)
     * @param toTimestamp end timestamp (milliseconds)
     * @param moduleNames optional filter by module names
     * @param limit maximum records to return
     * @param offset pagination offset
     * @return ResponseEntity containing replay data and metadata
     */
    @RequestMapping(method = RequestMethod.GET, value = "/replays")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<Map<String, Object>> queryReplays(
            @RequestParam("fromTimestamp") long fromTimestamp,
            @RequestParam("toTimestamp") long toTimestamp,
            @RequestParam(value = "moduleNames", required = false) List<String> moduleNames,
            @RequestParam(value = "limit", defaultValue = "1000") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {

        try {
            logger.debug("Query replay data - fromTimestamp: {}, toTimestamp: {}, moduleNames: {}, limit: {}, offset: {}",
                    fromTimestamp, toTimestamp, moduleNames, limit, offset);

            List<String> entityTypes = List.of(EntityDao.REPLAY);

            IkasanDocumentSearchResults searchResults = esbSearchService.searchByHarvestReceivedTime(
                    moduleNames != null ? new HashSet<>(moduleNames) : Set.of(),
                    Set.of(), // flow names
                    null, // component names
                    null, // event id
                    null, // payload content
                    fromTimestamp,
                    toTimestamp,
                    offset,
                    limit,
                    entityTypes,
                    false, // negate query
                    null,  // sort field
                    null   // sort order
            );

            Map<String, Object> response = new HashMap<>();
            response.put("data", searchResults.getResultList().stream()
                .map(this.ikasanESBDocumentToReplayEventConverter::convert)
                .toList());
            response.put("totalCount", searchResults.getTotalNumberOfResults());
            response.put("hasMore", (offset + limit) < searchResults.getTotalNumberOfResults());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error querying replay data", e);
            return new ResponseEntity(
                    new ErrorDto("An error occurred querying replay data: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Query topology/module metadata for data sharing
     *
     * @param moduleNames optional filter by module names
     * @param limit maximum records to return
     * @param offset pagination offset
     * @return ResponseEntity containing topology data and metadata
     */
    @RequestMapping(method = RequestMethod.GET, value = "/module-metadata")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<Map<String, Object>> queryTopology(
            @RequestParam(value = "moduleNames") List<String> moduleNames,
            @RequestParam(value = "limit", defaultValue = "1000") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {

        try {
            logger.debug("Query topology data - moduleNames: {}, limit: {}, offset: {}"
                , moduleNames, limit, offset);

            IkasanDocumentSearchResults searchResults = esbSearchService.search(
                moduleNames != null ? new HashSet<>(moduleNames) : Set.of(),
                    offset,
                    limit,
                    null,  // sort field
                    null   // sort order
            );

            Map<String, Object> response = new HashMap<>();
            response.put("data", searchResults.getResultList().stream()
                .map(this.ikasanESBDocumentToModuleMetaDataConverter::convert)
                .toList());
            response.put("totalCount", searchResults.getTotalNumberOfResults());
            response.put("hasMore", (offset + limit) < searchResults.getTotalNumberOfResults());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error querying topology data", e);
            return new ResponseEntity(
                    new ErrorDto("An error occurred querying topology data: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Query configuration metadata for data sharing
     *
     * @param configurationIdentifiers optional filter by module names
     * @param limit maximum records to return
     * @param offset pagination offset
     * @return ResponseEntity containing configuration metadata and metadata
     */
    @RequestMapping(method = RequestMethod.GET, value = "/configuration")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<Map<String, Object>> queryConfiguration(
            @RequestParam(value = "configurationIdentifiers") List<String> configurationIdentifiers,
            @RequestParam(value = "limit", defaultValue = "1000") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {

        try {
            logger.debug("Query configuration data -, configurationIdentifiers: {}, limit: {}, offset: {}",
                    configurationIdentifiers, limit, offset);

            IkasanDocumentSearchResults searchResults = esbSearchService.search(
                configurationIdentifiers != null ? new HashSet<>(configurationIdentifiers) : Set.of(),
                offset,
                limit,
                null,  // sort field
                null   // sort order
            );

            Map<String, Object> response = new HashMap<>();
            response.put("data", searchResults.getResultList().stream()
                .map(this.ikasanESBDocumentToConfigurationMetaDataConverter::convert)
                .toList());
            response.put("totalCount", searchResults.getTotalNumberOfResults());
            response.put("hasMore", (offset + limit) < searchResults.getTotalNumberOfResults());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error querying configuration data", e);
            return new ResponseEntity(
                    new ErrorDto("An error occurred querying configuration data: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ========== COUNT ENDPOINTS ==========

    /**
     * Count wiretap data for data sharing
     *
     * @param fromTimestamp start timestamp (milliseconds)
     * @param toTimestamp end timestamp (milliseconds)
     * @param moduleNames optional filter by module names
     * @return ResponseEntity containing count
     */
    @RequestMapping(method = RequestMethod.GET, value = "/wiretap/count")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<Map<String, Object>> countWiretap(
            @RequestParam("fromTimestamp") long fromTimestamp,
            @RequestParam("toTimestamp") long toTimestamp,
            @RequestParam(value = "moduleNames", required = false) List<String> moduleNames) {

        try {
            logger.debug("Count wiretap data - fromTimestamp: {}, toTimestamp: {}, moduleNames: {}",
                    fromTimestamp, toTimestamp, moduleNames);

            List<String> entityTypes = List.of(EntityDao.WIRETAP_TYPE);

            IkasanDocumentSearchResults searchResults = esbSearchService.searchByHarvestReceivedTime(
                    moduleNames != null ? new HashSet<>(moduleNames) : Set.of(),
                    Set.of(), // flow names
                    Set.of(), // component names
                    null, // event id
                    null, // payload content
                    fromTimestamp,
                    toTimestamp,
                    0, // offset
                    0, // limit
                    entityTypes,
                    false, // negate query
                    null,  // sort field
                    null   // sort order
            );

            Map<String, Object> response = new HashMap<>();
            response.put("count", searchResults.getTotalNumberOfResults());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error counting wiretap data", e);
            return new ResponseEntity(
                    new ErrorDto("An error occurred counting wiretap data: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Count error data for data sharing
     *
     * @param fromTimestamp start timestamp (milliseconds)
     * @param toTimestamp end timestamp (milliseconds)
     * @param moduleNames optional filter by module names
     * @return ResponseEntity containing count
     */
    @RequestMapping(method = RequestMethod.GET, value = "/errors/count")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<Map<String, Object>> countErrors(
            @RequestParam("fromTimestamp") long fromTimestamp,
            @RequestParam("toTimestamp") long toTimestamp,
            @RequestParam(value = "moduleNames", required = false) List<String> moduleNames) {

        try {
            logger.debug("Count error data - fromTimestamp: {}, toTimestamp: {}, moduleNames: {}",
                    fromTimestamp, toTimestamp, moduleNames);

            List<String> entityTypes = List.of(EntityDao.ERROR);

            IkasanDocumentSearchResults searchResults = esbSearchService.searchByHarvestReceivedTime(
                moduleNames != null ? new HashSet<>(moduleNames) : Set.of(),
                Set.of(), // flow names
                Set.of(), // component names
                null, // event id
                null, // payload content
                fromTimestamp,
                toTimestamp,
                0,
                0,
                entityTypes,
                false, // negate query
                null,  // sort field
                null   // sort order
            );

            Map<String, Object> response = new HashMap<>();
            response.put("count", searchResults.getTotalNumberOfResults());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error counting error data", e);
            return new ResponseEntity(
                    new ErrorDto("An error occurred counting error data: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Count exclusion/hospital data for data sharing
     *
     * @param fromTimestamp start timestamp (milliseconds)
     * @param toTimestamp end timestamp (milliseconds)
     * @param moduleNames optional filter by module names
     * @return ResponseEntity containing count
     */
    @RequestMapping(method = RequestMethod.GET, value = "/exclusions/count")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<Map<String, Object>> countExclusions(
            @RequestParam("fromTimestamp") long fromTimestamp,
            @RequestParam("toTimestamp") long toTimestamp,
            @RequestParam(value = "moduleNames", required = false) List<String> moduleNames) {

        try {
            logger.debug("Count exclusion data - fromTimestamp: {}, toTimestamp: {}, moduleNames: {}",
                    fromTimestamp, toTimestamp, moduleNames);

            List<String> entityTypes = List.of(EntityDao.EXCLUSION);

            IkasanDocumentSearchResults searchResults = esbSearchService.searchByHarvestReceivedTime(
                moduleNames != null ? new HashSet<>(moduleNames) : Set.of(),
                Set.of(), // flow names
                null, // component names
                null, // event id
                null, // payload content
                fromTimestamp,
                toTimestamp,
                0,
                0,
                entityTypes,
                false, // negate query
                null,  // sort field
                null   // sort order
            );

            Map<String, Object> response = new HashMap<>();
            response.put("count", searchResults.getTotalNumberOfResults());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error counting exclusion data", e);
            return new ResponseEntity(
                    new ErrorDto("An error occurred counting exclusion data: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Count replay data for data sharing
     *
     * @param fromTimestamp start timestamp (milliseconds)
     * @param toTimestamp end timestamp (milliseconds)
     * @param moduleNames optional filter by module names
     * @return ResponseEntity containing count
     */
    @RequestMapping(method = RequestMethod.GET, value = "/replays/count")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<Map<String, Object>> countReplays(
            @RequestParam("fromTimestamp") long fromTimestamp,
            @RequestParam("toTimestamp") long toTimestamp,
            @RequestParam(value = "moduleNames", required = false) List<String> moduleNames) {

        try {
            logger.debug("Count replay data - fromTimestamp: {}, toTimestamp: {}, moduleNames: {}",
                    fromTimestamp, toTimestamp, moduleNames);

            List<String> entityTypes = List.of(EntityDao.REPLAY);

            IkasanDocumentSearchResults searchResults = esbSearchService.searchByHarvestReceivedTime(
                moduleNames != null ? new HashSet<>(moduleNames) : Set.of(),
                Set.of(), // flow names
                null, // component names
                null, // event id
                null, // payload content
                fromTimestamp,
                toTimestamp,
                0,
                0,
                entityTypes,
                false, // negate query
                null,  // sort field
                null   // sort order
            );

            Map<String, Object> response = new HashMap<>();
            response.put("count", searchResults.getTotalNumberOfResults());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error counting replay data", e);
            return new ResponseEntity(
                    new ErrorDto("An error occurred counting replay data: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
