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

import javax.annotation.Resource;

import org.ikasan.job.orchestration.rest.dashboard.context.reset.ContextResetController;
import org.ikasan.job.orchestration.rest.dashboard.context.status.ContextStatusServiceController;
import org.ikasan.rest.dashboard.JwtAuthenticationController;
import org.ikasan.rest.dashboard.JwtAuthenticationEntryPoint;
import org.ikasan.rest.dashboard.JwtRequestFilter;
import org.ikasan.rest.dashboard.JwtTokenUtil;
import org.ikasan.security.service.UserService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.scheduled.context.service.ContextStatusService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.scheduled.reset.ContextResetService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;

import com.leansoft.bigqueue.IBigQueue;

@Configuration
public class IkasanRestAutoConfiguration {

    @Resource
    private BatchInsert scheduledProcessEventBatchInsert;

    @Resource
    private IBigQueue inboundQueue;

    @Resource
    private JobProvisionService jobProvisionService;

    @Resource
    private ContextStatusService contextStatusService;

    @Resource
    private ContextResetService contextResetService;

    @Bean
    public ScheduledProcessEventController scheduledProcessEventController() {
        return new ScheduledProcessEventController(this.scheduledProcessEventBatchInsert, this.inboundQueue);
    }

    @Bean
    SchedulerJobProvisionController schedulerJobProvisionController() {
        return new SchedulerJobProvisionController(this.jobProvisionService);
    }

    @Bean
    public ContextStatusServiceController contextStatusServiceController() {
        return new ContextStatusServiceController(this.contextStatusService);
    }

    @Bean
    public ContextResetController contextResetController() {
        return new ContextResetController(this.contextResetService);
    }

    @Bean
    public JwtAuthenticationController jwtAuthenticationController(AuthenticationManager authenticationManager,
                                                                   JwtTokenUtil jwtTokenUtil, UserService userService) {
        return new JwtAuthenticationController(authenticationManager, jwtTokenUtil, userService);
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    public JwtRequestFilter jwtRequestFilter(UserService userService, JwtTokenUtil jwtTokenUtil) {
        return new JwtRequestFilter(userService, jwtTokenUtil);
    }

    @Bean
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil();
    }
}
