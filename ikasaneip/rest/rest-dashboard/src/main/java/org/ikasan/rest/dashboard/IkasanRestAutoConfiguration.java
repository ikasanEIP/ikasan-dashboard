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

import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.component.endpoint.bigqueue.service.BigQueueDirectoryManagementServiceImpl;
import org.ikasan.rest.dashboard.service.bigqueue.BigQueueDashboardServiceImpl;
import org.ikasan.spec.bigqueue.service.BigQueueDirectoryManagementService;
import org.ikasan.spec.cache.FlowStateCacheAdapter;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.metrics.MetricsService;
import org.ikasan.spec.module.client.BigQueueModuleService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.security.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.context.SecurityContextRepository;

import javax.annotation.Resource;

@Configuration
public class IkasanRestAutoConfiguration
{

    @Value("${scheduled.job.context.queue.directory:.}")
    private String queueDir;

    @Value("${jwt.request.filter.user.cache.timeout.seconds:300}")
    private int jwtRequestFilterUserCacheTimeoutSeconds;

    @Value("${user.service.user.cache.timeout.seconds:300}")
    private int userServiceUserCacheTimeoutSeconds;

    @Resource(name="errorOccurrenceBatchInsert")
    private BatchInsert errorOccurrenceBatchInsert;

    @Resource(name="replayEventBatchInsert")
    private BatchInsert replayEventBatchInsert;

    @Resource(name="exclusionEventBatchInsert")
    private BatchInsert exclusionEventBatchInsert;

    @Resource(name="wiretapEventBatchInsert")
    private BatchInsert wiretapEventBatchInsert;

    @Resource(name="systemEventBatchInsert")
    private BatchInsert systemEventBatchInsert;

    @Resource(name="moduleMetadataBatchInsert")
    private BatchInsert moduleMetadataBatchInsert;

    @Resource(name="configurationMetadataBatchInsert")
    private BatchInsert configurationMetadataBatchInsert;

    @Resource(name="flowInvocationMetricBatchInsert")
    private BatchInsert flowInvocationMetricBatchInsert;


    @Resource
    private ContextParametersInstanceService contextParametersInstanceService;

    @Resource
    @Qualifier("moduleMetadataService")
    private ModuleMetaDataService moduleMetadataService;

    @Resource
    private MetricsService metricsService;

    @Resource
    private BigQueueModuleService bigQueueModuleService;

    @Autowired(required = false)
    private IBigQueue inboundQueue;

    @Resource
    private FlowStateCacheAdapter cacheAdapter;

    @Bean
    public ReplayController replayApplication()
    {
        return new ReplayController(this.replayEventBatchInsert);
    }

    @Bean
    public WiretapController wiretapController()
    {
        return new WiretapController(this.wiretapEventBatchInsert);
    }

    @Bean
    public ErrorController errorApplication()
    {
        return new ErrorController(this.errorOccurrenceBatchInsert);
    }

    @Bean
    public ExclusionController exclusionApplication()
    {
        return new ExclusionController(this.exclusionEventBatchInsert);
    }

    @Bean
    public MetaDataController metaDataApplication()
    {
        return new MetaDataController(this.moduleMetadataBatchInsert,
            this.configurationMetadataBatchInsert);
    }

    @Bean
    public SystemEventController systemEventController()
    {
        return new SystemEventController(this.systemEventBatchInsert);
    }

    @Bean
    public ModulesController modulesController()
    {
        return new ModulesController(moduleMetadataService);
    }

    @Bean
    public MetricsController metricsApplication() {
        return new MetricsController(this.flowInvocationMetricBatchInsert, this.metricsService);
    }

    @Bean
    public UserController userController(UserService userService)
    {
        return new UserController(userService, this.userServiceUserCacheTimeoutSeconds);
    }

    @Bean
    public NotifierController notifierControllerApplication()
    {
        return new NotifierController(this.cacheAdapter);
    }

    @Bean
    @ConditionalOnProperty(value="is.ikasan.enterprise.scheduler.instance", havingValue = "true")
    public BigQueueDashboardController bigQueueManagementController() {
        return new BigQueueDashboardController();
    }

    @Bean
    @ConditionalOnProperty(value="is.ikasan.enterprise.scheduler.instance", havingValue = "true")
    public BigQueueDirectoryManagementService bigQueueDirectoryManagementService() {
        return new BigQueueDirectoryManagementServiceImpl(new BigQueueDashboardServiceImpl(inboundQueue), this.queueDir);
    }

    @Bean
    @ConditionalOnProperty(value="is.ikasan.enterprise.scheduler.instance", havingValue = "true")
    public BigQueueModuleController bigQueueModuleController() {
        return new BigQueueModuleController(bigQueueModuleService, moduleMetadataService);
    }

    @Bean
    public JwtAuthenticationController jwtAuthenticationController(AuthenticationManager authenticationManager,
                                                                   JwtTokenUtil jwtTokenUtil, UserService userService) {
        return new JwtAuthenticationController(authenticationManager, jwtTokenUtil, userService);
    }

    @Bean("jwtAuthenticationEntryPoint")
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    public JwtRequestFilter jwtRequestFilter(UserService userService, JwtTokenUtil jwtTokenUtil
        , @Lazy SecurityContextRepository securityContextRepository) {
        return new JwtRequestFilter(userService, jwtTokenUtil
            , securityContextRepository, jwtRequestFilterUserCacheTimeoutSeconds);
    }

    @Bean
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil();
    }

    @Bean
    public JobContextController jobContextController() {
        return new JobContextController(contextParametersInstanceService);
    }
}
