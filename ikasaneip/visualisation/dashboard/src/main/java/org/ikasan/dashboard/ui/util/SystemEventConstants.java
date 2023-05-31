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
package org.ikasan.dashboard.ui.util;


import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 * @author Ikasan Development Team
 *
 */
public class SystemEventConstants
{
	public static final String DASHBOARD_LOGIN_CONSTANTS = "Dashboard login";
	public static final String DASHBOARD_LOGOUT_CONSTANTS = "Dashboard log out";
	public static final String DASHBOARD_SESSION_EXPIRED_CONSTANTS = "Dashboard session expired";
	public static final String DASHBOARD_PRINCIPAL_ROLE_CHANGED_CONSTANTS = "Principal role change";
    public static final String DASHBOARD_MODULE_ROLE_CHANGE_CONSTANTS = "Module role change";
    public static final String DASHBOARD_JOB_PLAN_ROLE_CHANGE_CONSTANTS = "Job plan role change";
    public static final String DASHBOARD_ROLE_ADDED = "Role added";
    public static final String DASHBOARD_ROLE_DELETED = "Role added";
    public static final String NEW_USER_CREATED = "New user created";

    public static final String NEW_SCHEDULED_JOB_CREATED = "New scheduled job created";
    public static final String SCHEDULED_JOB_DELETED = "Scheduled job deleted";
    public static final String SCHEDULED_JOB_EDIT = "Scheduled job edited";
    public static final String SCHEDULED_JOB_SKIPPED = "Scheduled job skipped";
    public static final String SCHEDULED_JOB_KILLED = "Scheduled job killed";
    public static final String SCHEDULED_JOB_ENABLED = "Scheduled job enabled";
    public static final String SCHEDULED_JOB_HELD = "Scheduled job held";
    public static final String All_SCHEDULED_JOBS_HELD_FOR_JOB_PLAN = "All scheduled jobs held for job plan";
    public static final String All_SCHEDULED_JOBS_ENABLED_FOR_JOB_PLAN = "All scheduled jobs enabled for job plan";
    public static final String All_SCHEDULED_JOBS_RELEASED_FOR_JOB_PLAN = "All scheduled jobs released for job plan";
    public static final String SCHEDULED_JOB_RELEASED = "Scheduled job released";
    public static final String SCHEDULED_JOB_SUBMITTED = "Scheduled job manually submitted";
    public static final String CHILD_JOB_PLAN_ADDED_TO_JOB_PLAN = "Child job plan added to job plan";
    public static final String JOB_PLAN_SAVED = "Job plan saved";
    public static final String SCHEDULED_JOB_RESET = "Scheduled job reset";
    public static final String USER_ADDED_TO_CONTEXT_PROFILE = "User added to context profile";
    public static final String USER_REMOVED_FROM_CONTEXT_PROFILE = "User removed from context profile";
    public static final String CONTEXT_JOB_LOCKS_MODIFICATION = "Job plan job lock modification";
    public static final String CONTEXT_TEMPLATE_SCHEDULED_JOBS_DISABLED = "Job plan all scheduled jobs disabled";
    public static final String CONTEXT_TEMPLATE_SCHEDULED_JOBS_ENABLED = "Job plan all scheduled jobs enabled";
    public static final String CONTEXT_INSTANCE_SCHEDULED_JOBS_DISABLED = "Job plan instance all scheduled jobs disabled";
    public static final String CONTEXT_INSTANCE_SCHEDULED_JOBS_ENABLED = "Job plan instance all scheduled jobs enabled";
    public static final String CONTEXT_INSTANCE_HOLDING_ALL_JOBS = "Job plan instance all scheduled jobs held";
    public static final String CONTEXT_INSTANCE_RELEASING_ALL_JOBS_START = "Job plan instance all scheduled jobs released - start";
    public static final String CONTEXT_INSTANCE_RELEASING_ALL_JOBS_END = "Job plan instance all scheduled jobs released - end";
    public static final String CHILD_CONTEXT_INSTANCE_RELEASING_ALL_JOBS_START = "Job plan instance all scheduled jobs released - start";
    public static final String CHILD_CONTEXT_INSTANCE_RELEASING_ALL_JOBS_END = "Job plan instance all scheduled jobs released - end";
    public static final String CONTEXT_INSTANCE_RESET = "Job plan instance reset";
    public static final String CONTEXT_INSTANCE_MANUALLY_ENDED = "Job plan instance manually ended";
    public static final String CONTEXT_INSTANCE_DURATION_IGNORED = "Job plan instance duration ignored";
    public static final String CONTEXT_INSTANCE_MANUALLY_CREATED = "Job plan instance manually created";
    public static final String JOB_PLAN_DELETED = "Job plan deleted";
    public static final String NEW_JOB_PLAN_CREATED = "New job plan created";
    public static final String JOB_PLAN_MODIFIED = "Job plan modified";
    public static final String JOB_PLAN_ENABLED = "Job plan enabled";
    public static final String JOB_PLAN_DISABLED = "Job plan disabled";

    public static List<String> getSystemEventConstants() {
        List<String> constantValues = Arrays.stream(SystemEventConstants.class.getDeclaredFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()))
            .map(field -> {
                try {
                    return (String) field.get(SystemEventConstants.class);
                } catch (IllegalAccessException e) {
                    // Ignore as we are interested in all string values only!
                    return null;
                }
            })
            .filter(value -> value != null)// filter out if needed
            .collect(Collectors.toList());

        return constantValues;
    }
}
