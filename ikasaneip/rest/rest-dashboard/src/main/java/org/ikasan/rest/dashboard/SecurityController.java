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

import org.ikasan.rest.dashboard.model.dto.ErrorDto;
import org.ikasan.security.model.Role;
import org.ikasan.security.model.RoleModule;
import org.ikasan.security.service.SecurityService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
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
 * REST controller for managing security-related operations.
 *
 * <p>Provides endpoints for managing role assignments to modules and job plans,
 * enabling fine-grained access control over integration modules and scheduled job plans.
 *
 * @author Ikasan Development Team
 */
@RequestMapping("/rest/security")
@RestController
public class SecurityController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityController.class);

    private final SecurityService securityService;
    private final ModuleMetaDataService moduleMetaDataService;
    private final ScheduledContextService scheduledContextService;

    /**
     * Constructor for SecurityController.
     *
     * @param securityService the security service implementation
     * @param moduleMetaDataService the module metadata service implementation
     * @param scheduledContextService the scheduled context service implementation
     * @throws IllegalArgumentException if securityService, moduleMetaDataService, or scheduledContextService is null
     */
    public SecurityController(SecurityService securityService, ModuleMetaDataService moduleMetaDataService,
                              ScheduledContextService scheduledContextService) {
        if (securityService == null) {
            throw new IllegalArgumentException("securityService cannot be null!");
        }
        if (moduleMetaDataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }
        if (scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.securityService = securityService;
        this.moduleMetaDataService = moduleMetaDataService;
        this.scheduledContextService = scheduledContextService;
    }

    /**
     * Assigns a module to multiple roles.
     *
     * <p>Creates role-module associations for the specified module name and all provided role names.
     * If a role does not exist, it will be logged as a warning and skipped.
     *
     * @param moduleName the name of the module to assign
     * @param roleNames the list of role names to assign the module to
     * @return ResponseEntity with success message or error details
     */
    @RequestMapping(method = RequestMethod.POST, value = "/module/roles")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<?> assignModuleToRoles(
            @RequestParam(name = "moduleName") String moduleName,
            @RequestBody List<String> roleNames) {

        logger.info("Assigning module [{}] to roles {}", moduleName, roleNames);

        try {
            if (moduleName == null || moduleName.trim().isEmpty()) {
                logger.warn("Module name cannot be null or empty");
                return ResponseEntity
                    .badRequest()
                    .body(new ErrorDto("Module name cannot be null or empty"));
            }

            // Validate module exists
            if (moduleMetaDataService.findById(moduleName) == null) {
                String errorMessage = String.format("Module [%s] does not exist", moduleName);
                logger.warn(errorMessage);
                return ResponseEntity
                    .badRequest()
                    .body(new ErrorDto(errorMessage));
            }

            if (roleNames == null || roleNames.isEmpty()) {
                logger.warn("Role names list cannot be null or empty");
                return ResponseEntity
                    .badRequest()
                    .body(new ErrorDto("Role names list cannot be null or empty"));
            }

            // Validate all role names exist
            ResponseEntity<?> validationResponse = validateRoleNames(roleNames);
            if (validationResponse != null) {
                return validationResponse;
            }

            int assignedCount = 0;
            for (String roleName : roleNames) {
                Role role = securityService.findRoleByName(roleName);

                RoleModule roleModule = new RoleModule();
                roleModule.setModuleName(moduleName);
                roleModule.setRole(role);
                roleModule.setCreatedDateTime(new Date());
                roleModule.setUpdatedDateTime(new Date());

                role.getRoleModules().add(roleModule);

                securityService.saveRoleModule(roleModule);
                securityService.saveRole(role);
                assignedCount++;

                logger.info("Successfully assigned module [{}] to role [{}]", moduleName, roleName);
            }

            String message = String.format("Successfully assigned module [%s] to %d role(s)",
                moduleName, assignedCount);
            logger.info(message);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error assigning module [{}] to roles {}", moduleName, roleNames, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDto("Error assigning module to roles: " + e.getMessage()));
        }
    }

    /**
     * Assigns a job plan to multiple roles.
     *
     * <p>Delegates to the SecurityService to create role-job plan associations for the specified
     * job plan name and all provided role names.
     *
     * @param jobPlanName the name of the job plan to assign
     * @param roleNames the list of role names to assign the job plan to
     * @return ResponseEntity with success message or error details
     */
    @RequestMapping(method = RequestMethod.POST, value = "/jobplan/roles")
    @PreAuthorize("hasAnyAuthority('ALL','WebServiceAdmin')")
    public ResponseEntity<?> assignJobPlanToRoles(
            @RequestParam(name = "jobPlanName") String jobPlanName,
            @RequestBody List<String> roleNames) {

        logger.info("Assigning job plan [{}] to roles {}", jobPlanName, roleNames);

        try {
            if (jobPlanName == null || jobPlanName.trim().isEmpty()) {
                logger.warn("Job plan name cannot be null or empty");
                return ResponseEntity
                    .badRequest()
                    .body(new ErrorDto("Job plan name cannot be null or empty"));
            }

            if (roleNames == null || roleNames.isEmpty()) {
                logger.warn("Role names list cannot be null or empty");
                return ResponseEntity
                    .badRequest()
                    .body(new ErrorDto("Role names list cannot be null or empty"));
            }

            // Validate job plan exists
            if (scheduledContextService.findByName(jobPlanName) == null) {
                String errorMessage = String.format("Job plan [%s] does not exist", jobPlanName);
                logger.warn(errorMessage);
                return ResponseEntity.badRequest().body(new ErrorDto(errorMessage));
            }

            // Validate all role names exist
            ResponseEntity<?> validationResponse = validateRoleNames(roleNames);
            if (validationResponse != null) {
                return validationResponse;
            }
            
            securityService.setJobPlanRoles(jobPlanName, roleNames);

            String message = String.format("Successfully assigned job plan [%s] to %d role(s)",
                jobPlanName, roleNames.size());
            logger.info(message);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error assigning job plan [{}] to roles {}", jobPlanName, roleNames, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDto("Error assigning job plan to roles: " + e.getMessage()));
        }
    }

    /**
     * Validates that all role names in the provided list exist in the system.
     *
     * <p>This method checks each role name against the SecurityService to ensure
     * it corresponds to a valid, existing role. If any role names are invalid,
     * it returns a BAD REQUEST response with details of the invalid roles.
     *
     * @param roleNames the list of role names to validate
     * @return ResponseEntity with error details if validation fails, or null if all roles are valid
     */
    private ResponseEntity<?> validateRoleNames(List<String> roleNames) {
        List<String> invalidRoleNames = new ArrayList<>();

        for (String roleName : roleNames) {
            if (roleName == null || roleName.trim().isEmpty()) {
                invalidRoleNames.add(roleName == null ? "null" : "\"" + roleName + "\" (empty)");
            } else if (securityService.findRoleByName(roleName) == null) {
                invalidRoleNames.add(roleName);
            }
        }

        if (!invalidRoleNames.isEmpty()) {
            String errorMessage = String.format("The following roles are not valid: %s",
                String.join(", ", invalidRoleNames));
            logger.warn(errorMessage);
            return ResponseEntity
                .badRequest()
                .body(new ErrorDto(errorMessage));
        }

        return null;
    }
}
