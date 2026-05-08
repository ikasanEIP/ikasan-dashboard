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

import org.ikasan.job.orchestration.broadcast.*;
import org.ikasan.job.orchestration.rest.client.ClusterEventRestServiceImpl;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
public class ClusterBroadcastAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ClusterBroadcastAutoConfiguration.class);

    @Value("${ikasan.dashboard.cluster.node-urls:}")
    private String clusterNodeUrlsProperty;

    @Value("${server.port:9090}")
    private int serverPort;

    @Bean
    public ClusterEventController clusterEventController() {
        return new ClusterEventController();
    }

    @Bean
    public List<ClusterEventService> clusterEventServices(Environment environment,
                                                          HttpComponentsClientHttpRequestFactory factory) {
        return clusterNodeUrls().stream()
            .map(url -> (ClusterEventService) new ClusterEventRestServiceImpl(url, environment, factory))
            .collect(Collectors.toList());
    }

    @Bean
    public ContextInstanceStateChangeEventRemoteBroadcastListenerImpl contextInstanceStateChangeEventRemoteBroadcastListener(
            List<ClusterEventService> clusterEventServices) {
        ContextInstanceStateChangeEventRemoteBroadcastListenerImpl listener =
            new ContextInstanceStateChangeEventRemoteBroadcastListenerImpl(clusterEventServices);
        ContextInstanceStateChangeEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public SchedulerJobStateChangeEventRemoteBroadcastListenerImpl schedulerJobStateChangeEventRemoteBroadcastListener(
            List<ClusterEventService> clusterEventServices) {
        SchedulerJobStateChangeEventRemoteBroadcastListenerImpl listener =
            new SchedulerJobStateChangeEventRemoteBroadcastListenerImpl(clusterEventServices);
        SchedulerJobStateChangeEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public ContextInstanceSavedEventRemoteBroadcastListenerImpl contextInstanceSavedEventRemoteBroadcastListener(
            List<ClusterEventService> clusterEventServices) {
        ContextInstanceSavedEventRemoteBroadcastListenerImpl listener =
            new ContextInstanceSavedEventRemoteBroadcastListenerImpl(clusterEventServices);
        ContextInstanceSavedEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public ContextInstanceDlqEventRemoteBroadcastListenerImpl contextInstanceDlqEventRemoteBroadcastListener(
            List<ClusterEventService> clusterEventServices) {
        ContextInstanceDlqEventRemoteBroadcastListenerImpl listener =
            new ContextInstanceDlqEventRemoteBroadcastListenerImpl(clusterEventServices);
        ContextInstanceDlqEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public ContextTemplateSavedEventRemoteBroadcastListenerImpl contextTemplateSavedEventRemoteBroadcastListener(
            List<ClusterEventService> clusterEventServices) {
        ContextTemplateSavedEventRemoteBroadcastListenerImpl listener =
            new ContextTemplateSavedEventRemoteBroadcastListenerImpl(clusterEventServices);
        ContextTemplateSavedEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl contextTemplateEnableDisableEventRemoteBroadcastListener(
            List<ClusterEventService> clusterEventServices) {
        ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl listener =
            new ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl(clusterEventServices);
        ContextTemplateEnableDisableEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public ContextViewUpdateEventRemoteBroadcastListenerImpl contextViewUpdateEventRemoteBroadcastListener(
            List<ClusterEventService> clusterEventServices) {
        ContextViewUpdateEventRemoteBroadcastListenerImpl listener =
            new ContextViewUpdateEventRemoteBroadcastListenerImpl(clusterEventServices);
        ContextViewUpdateEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public NewSchedulerJobEventRemoteBroadcastListenerImpl newSchedulerJobEventRemoteBroadcastListener(
            List<ClusterEventService> clusterEventServices) {
        NewSchedulerJobEventRemoteBroadcastListenerImpl listener =
            new NewSchedulerJobEventRemoteBroadcastListenerImpl(clusterEventServices);
        NewSchedulerJobEventBroadcaster.register(listener);
        return listener;
    }

    @Bean
    public JobLockCacheEventRemoteBroadcastListenerImpl jobLockCacheEventRemoteBroadcastListener(
            List<ClusterEventService> clusterEventServices) {
        JobLockCacheEventRemoteBroadcastListenerImpl listener =
            new JobLockCacheEventRemoteBroadcastListenerImpl(clusterEventServices);
        JobLockCacheEventBroadcaster.register(listener);
        return listener;
    }

    /**
     * Parses and sanitises ikasan.dashboard.cluster.node-urls before any services are created.
     * Guards against three misconfiguration scenarios:
     *   1. Self-reference — a URL that resolves to this node on this port would cause every
     *      broadcast event to be sent back to the originating node, triggering an infinite loop.
     *   2. Duplicates — the same peer URL listed more than once (including mixed-case variants
     *      such as "Node2.Example.Com" and "node2.example.com") would result in every event
     *      being dispatched multiple times to the same node.
     *   3. Unresolvable hostnames — a URL whose hostname cannot be resolved is kept in the list
     *      (DNS may be temporarily unavailable at startup) but a warning is logged so the
     *      operator is alerted at startup rather than discovering silent broadcast failures later.
     */
    List<String> clusterNodeUrls() {
        List<InetAddress> selfAddresses = selfAddresses();
        List<String> urls = Arrays.stream(clusterNodeUrlsProperty.split(","))
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(s -> !s.isEmpty())
            .filter(url -> !isSelfUrl(url, selfAddresses))
            .toList();

        List<String> deduped = urls.stream().distinct().collect(Collectors.toList());
        if (deduped.size() < urls.size()) {
            logger.warn("Duplicate or invalid URLs were found in ikasan.dashboard.cluster.node-urls and have been removed. " +
                "Please check your configuration. Effective URLs: {}", deduped);
        }
        deduped.forEach(this::warnIfUnresolvable);
        return deduped;
    }

    /**
     * Returns true if the URL resolves to this node on this node's port.
     * Uses InetAddress.isLoopbackAddress() rather than string-matching specific values such as
     * "localhost" or "127.0.0.1", which covers all loopback variants (127.0.0.x, ::1, etc.)
     * regardless of how the OS resolves "localhost" on the current platform.
     * The port is checked first to avoid a DNS lookup for the common case of a peer on a
     * different port.
     */
    private boolean isSelfUrl(String url, List<InetAddress> selfAddresses) {
        try {
            URI uri = URI.create(url);
            int port = uri.getPort() == -1 ? ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80) : uri.getPort();
            if (port != serverPort) {
                return false;
            }
            InetAddress[] resolved = resolveWithTimeout(uri.getHost());
            for (InetAddress addr : resolved) {
                if (addr.isLoopbackAddress() || selfAddresses.contains(addr)) {
                    logger.error("Cluster node URL [{}] appears to reference this node itself (port {}). " +
                        "This would cause recursive broadcasting and has been excluded from the cluster node list. " +
                        "Please remove this node's own URL from ikasan.dashboard.cluster.node-urls.", url, serverPort);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("Could not parse cluster node URL [{}] — skipping self-reference check: {}", url, e.getMessage());
        }
        return false;
    }

    /**
     * Resolves a hostname to its InetAddress array with a 2-second cap.
     * Without a timeout, a misconfigured or unreachable DNS server could block application
     * startup for the full JVM resolver timeout (typically 10+ seconds per hostname).
     * Returns an empty array if resolution fails or times out.
     */
    private InetAddress[] resolveWithTimeout(String host) {
        try {
            return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return InetAddress.getAllByName(host);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orTimeout(2, TimeUnit.SECONDS)
                .join();
        } catch (Exception e) {
            return new InetAddress[0];
        }
    }

    /**
     * Logs a warning at startup if a peer URL's hostname cannot be resolved.
     * The URL is still retained in the peer list because DNS may be temporarily unavailable;
     * excluding it silently would be worse than keeping it and letting the broadcast error
     * handling report failures at runtime.
     */
    private void warnIfUnresolvable(String url) {
        try {
            URI uri = URI.create(url);
            InetAddress[] resolved = resolveWithTimeout(uri.getHost());
            if (resolved.length == 0) {
                logger.warn("Cluster node URL [{}] could not be resolved. Broadcasting to this node will fail " +
                    "until the host becomes reachable. Please check ikasan.dashboard.cluster.node-urls.", url);
            }
        } catch (Exception e) {
            logger.warn("Could not validate cluster node URL [{}]: {}", url, e.getMessage());
        }
    }

    /**
     * Resolves all InetAddresses for this node's own hostname, used by isSelfUrl() to detect
     * non-loopback self-references (e.g. the machine's FQDN or LAN IP).
     */
    private List<InetAddress> selfAddresses() {
        List<InetAddress> addresses = new ArrayList<>();
        try {
            InetAddress local = InetAddress.getLocalHost();
            addresses.addAll(Arrays.asList(resolveWithTimeout(local.getHostName())));
        } catch (UnknownHostException e) {
            logger.warn("Could not determine local host address for self-reference check: {}", e.getMessage());
        }
        return addresses;
    }
}
