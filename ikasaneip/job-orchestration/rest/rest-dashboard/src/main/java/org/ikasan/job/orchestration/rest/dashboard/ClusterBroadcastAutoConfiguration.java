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

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.ikasan.job.orchestration.broadcast.*;
import org.ikasan.job.orchestration.broadcast.listener.*;
import org.ikasan.job.orchestration.rest.client.delegate.ContextMachineRestServiceDelegate;
import org.ikasan.spec.scheduled.event.service.ClusterEventBroadcastChannel;
import org.ikasan.job.orchestration.rest.client.ClusterEventBroadcastRestServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
public class ClusterBroadcastAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ClusterBroadcastAutoConfiguration.class);

    // =========================================================================
    // Beans
    // =========================================================================

    @Bean
    public ClusterEventController clusterEventController() {
        return new ClusterEventController();
    }

    @Bean
    public ContextInstanceStateChangeEventRemoteBroadcastListenerImpl contextInstanceStateChangeEventRemoteBroadcastListener(
            List<ClusterEventBroadcastChannel> peerBroadcastChannels) {
        ContextInstanceStateChangeEventRemoteBroadcastListenerImpl listener =
            new ContextInstanceStateChangeEventRemoteBroadcastListenerImpl(peerBroadcastChannels);
        ContextInstanceStateChangeEventBroadcaster.instance().setRemoteListener(listener);
        return listener;
    }

    @Bean
    public SchedulerJobStateChangeEventRemoteBroadcastListenerImpl schedulerJobStateChangeEventRemoteBroadcastListener(
            List<ClusterEventBroadcastChannel> peerBroadcastChannels) {
        SchedulerJobStateChangeEventRemoteBroadcastListenerImpl listener =
            new SchedulerJobStateChangeEventRemoteBroadcastListenerImpl(peerBroadcastChannels);
        SchedulerJobStateChangeEventBroadcaster.instance().setRemoteListener(listener);
        return listener;
    }

    @Bean
    public ContextInstanceSavedEventRemoteBroadcastListenerImpl contextInstanceSavedEventRemoteBroadcastListener(
            List<ClusterEventBroadcastChannel> peerBroadcastChannels) {
        ContextInstanceSavedEventRemoteBroadcastListenerImpl listener =
            new ContextInstanceSavedEventRemoteBroadcastListenerImpl(peerBroadcastChannels);
        ContextInstanceSavedEventBroadcaster.instance().setRemoteListener(listener);
        return listener;
    }

    @Bean
    public ContextInstanceDlqEventRemoteBroadcastListenerImpl contextInstanceDlqEventRemoteBroadcastListener(
            List<ClusterEventBroadcastChannel> peerBroadcastChannels) {
        ContextInstanceDlqEventRemoteBroadcastListenerImpl listener =
            new ContextInstanceDlqEventRemoteBroadcastListenerImpl(peerBroadcastChannels);
        ContextInstanceDlqEventBroadcaster.instance().setRemoteListener(listener);
        return listener;
    }

    @Bean
    public ContextTemplateSavedEventRemoteBroadcastListenerImpl contextTemplateSavedEventRemoteBroadcastListener(
            List<ClusterEventBroadcastChannel> peerBroadcastChannels) {
        ContextTemplateSavedEventRemoteBroadcastListenerImpl listener =
            new ContextTemplateSavedEventRemoteBroadcastListenerImpl(peerBroadcastChannels);
        ContextTemplateSavedEventBroadcaster.instance().setRemoteListener(listener);
        return listener;
    }

    @Bean
    public ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl contextTemplateEnableDisableEventRemoteBroadcastListener(
            List<ClusterEventBroadcastChannel> peerBroadcastChannels) {
        ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl listener =
            new ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl(peerBroadcastChannels);
        ContextTemplateEnableDisableEventBroadcaster.instance().setRemoteListener(listener);
        return listener;
    }

    @Bean
    public ContextViewUpdateEventRemoteBroadcastListenerImpl contextViewUpdateEventRemoteBroadcastListener(
            List<ClusterEventBroadcastChannel> peerBroadcastChannels) {
        ContextViewUpdateEventRemoteBroadcastListenerImpl listener =
            new ContextViewUpdateEventRemoteBroadcastListenerImpl(peerBroadcastChannels);
        ContextViewUpdateEventBroadcaster.instance().setRemoteListener(listener);
        return listener;
    }

    @Bean
    public NewSchedulerJobEventRemoteBroadcastListenerImpl newSchedulerJobEventRemoteBroadcastListener(
            List<ClusterEventBroadcastChannel> peerBroadcastChannels) {
        NewSchedulerJobEventRemoteBroadcastListenerImpl listener =
            new NewSchedulerJobEventRemoteBroadcastListenerImpl(peerBroadcastChannels);
        NewSchedulerJobEventBroadcaster.instance().setRemoteListener(listener);
        return listener;
    }

    @Bean
    public JobLockCacheEventRemoteBroadcastListenerImpl jobLockCacheEventRemoteBroadcastListener(
            List<ClusterEventBroadcastChannel> peerBroadcastChannels) {
        JobLockCacheEventRemoteBroadcastListenerImpl listener =
            new JobLockCacheEventRemoteBroadcastListenerImpl(peerBroadcastChannels);
        JobLockCacheEventBroadcaster.instance().setRemoteListener(listener);
        return listener;
    }
}
