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
import org.ikasan.spec.scheduled.event.service.ClusterEventBroadcastChannel;
import org.ikasan.job.orchestration.rest.client.ClusterEventBroadcastRestServiceImpl;
import org.ikasan.job.orchestration.rest.client.ContextMachineRestServiceImpl;
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

    // -------------------------------------------------------------------------
    // Cluster node discovery
    // -------------------------------------------------------------------------

    /** Comma-separated URLs of peer dashboard nodes, excluding this node's own URL. */
    @Value("${ikasan.dashboard.cluster.peer-urls:}")
    private String clusterPeerUrls;

    @Value("${server.port:9090}")
    private int serverPort;

    // -------------------------------------------------------------------------
    // Peer broadcast queue
    // -------------------------------------------------------------------------

    /**
     * Maximum number of pending broadcast tasks per peer node before the oldest is evicted.
     * When a peer is slow or temporarily unreachable, its queue fills up. Evicting the oldest
     * event ensures the peer receives current state rather than a large backlog of stale events
     * once it recovers. Increase this value if peers regularly fall behind during high-throughput
     * periods and queue saturation warnings appear in the log.
     *
     * <p>Property: {@code ikasan.dashboard.cluster.peer-broadcast-queue-capacity} (default 1000)</p>
     */
    @Value("${ikasan.dashboard.cluster.peer-broadcast-queue-capacity:1000}")
    private int peerBroadcastQueueCapacity;

    // -------------------------------------------------------------------------
    // Peer HTTP timeouts
    // -------------------------------------------------------------------------

    /**
     * TCP connection timeout in milliseconds for peer-to-peer broadcast HTTP calls.
     * A short value (3 s default) ensures that a peer that is completely unreachable fails
     * fast, keeping its execution lane free for the next event. Increase if peers are on
     * high-latency networks where connection establishment takes longer than normal.
     *
     * <p>Property: {@code ikasan.dashboard.cluster.http.connect-timeout-ms} (default 3000)</p>
     */
    @Value("${ikasan.dashboard.cluster.http.connect-timeout-ms:3000}")
    private int clusterHttpConnectTimeoutMs;

    /**
     * HTTP response (read) timeout in milliseconds for peer-to-peer broadcast HTTP calls.
     * Once a connection is established, if the peer does not complete the HTTP response within
     * this window the call is abandoned. A value of 10 s (default) allows for transient
     * slowness while preventing a degraded peer from permanently occupying its execution lane.
     *
     * <p>Property: {@code ikasan.dashboard.cluster.http.read-timeout-ms} (default 10000)</p>
     */
    @Value("${ikasan.dashboard.cluster.http.read-timeout-ms:10000}")
    private int clusterHttpReadTimeoutMs;

    // -------------------------------------------------------------------------
    // Per-peer circuit breaker
    // -------------------------------------------------------------------------

    /**
     * Number of consecutive network-level failures (connection refused, timeout, etc.) before
     * the circuit breaker opens for a peer. While the circuit is open, broadcast tasks for
     * that peer are dropped immediately without queuing, preventing a permanently-down peer
     * from filling its queue with doomed work. HTTP error responses (4xx/5xx) do not count
     * towards this threshold because they indicate the peer is reachable.
     *
     * <p>Property: {@code ikasan.dashboard.cluster.circuit-breaker.failure-threshold} (default 5)</p>
     */
    @Value("${ikasan.dashboard.cluster.circuit-breaker.failure-threshold:5}")
    private int circuitBreakerFailureThreshold;

    /**
     * Duration in milliseconds that the circuit breaker stays open before allowing a single
     * probe request through (half-open state). A successful probe resets the failure counter
     * and closes the circuit. A failed probe restarts the open window. The default of 30 s
     * provides a balance between recovery speed and avoiding repeated connection attempts to
     * a peer that is still down.
     *
     * <p>Property: {@code ikasan.dashboard.cluster.circuit-breaker.open-duration-ms} (default 30000)</p>
     */
    @Value("${ikasan.dashboard.cluster.circuit-breaker.open-duration-ms:30000}")
    private long circuitBreakerOpenDurationMs;

    // Retained for @PreDestroy — populated when peerBroadcastChannels() is called.
    private List<ClusterEventBroadcastChannel> channels = Collections.emptyList();

    // =========================================================================
    // Beans
    // =========================================================================

    @Bean
    public ClusterEventController clusterEventController() {
        return new ClusterEventController();
    }

    /**
     * Dedicated {@link CloseableHttpClient} for peer-to-peer cluster broadcasts.
     * Using a separate client instance (with its own {@code RequestConfig}) keeps cluster
     * broadcast timeouts independent from the module-client timeouts used elsewhere in the
     * application. Spring closes the client on context shutdown via {@code destroyMethod}.
     */
    @Bean(destroyMethod = "close")
    public CloseableHttpClient clusterHttpClient() {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(clusterHttpConnectTimeoutMs))
            .build();
        PoolingHttpClientConnectionManager connectionManager =
            PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
            .setResponseTimeout(Timeout.ofMilliseconds(clusterHttpReadTimeoutMs))
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(clusterHttpConnectTimeoutMs))
            .build();
        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();
    }

    /**
     * Creates one {@link PeerBroadcastChannel} per configured peer node. Each channel pairs the
     * peer's REST client with a dedicated single-threaded executor and a per-peer circuit breaker.
     *
     * <p>All nine remote broadcast listener beans share this same list, so each peer has exactly
     * one execution lane across all event types. This preserves event ordering per peer and
     * minimises total thread count (one thread per peer, not one per event-type per peer).</p>
     */
    @Bean
    public List<ClusterEventBroadcastChannel> peerBroadcastChannels(Environment environment,
                                                             CloseableHttpClient clusterHttpClient) {
        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory(clusterHttpClient);
        this.channels = clusterPeerUrls().stream()
            .map(url -> (ClusterEventBroadcastChannel) new PeerBroadcastChannel(
                new ClusterEventBroadcastRestServiceImpl(url, environment, factory),
                peerBroadcastQueueCapacity,
                url,
                circuitBreakerFailureThreshold,
                circuitBreakerOpenDurationMs))
            .collect(Collectors.toList());
        return this.channels;
    }

    /**
     * Shuts down all peer execution lanes on Spring context close, preventing non-daemon
     * threads from keeping the JVM alive after the application stops or between test runs.
     * The {@link CloseableHttpClient} is closed separately by its own {@code destroyMethod}.
     */
    @PreDestroy
    public void shutdown() {
        if (channels != null) {
            channels.forEach(ClusterEventBroadcastChannel::shutdown);
        }
    }

    @Bean
    public List<ContextMachineRestServiceImpl> contextMachineRestServices(Environment environment,
                                                                           CloseableHttpClient clusterHttpClient) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(clusterHttpClient);
        return clusterPeerUrls().stream()
            .map(url -> new ContextMachineRestServiceImpl(url, environment, factory))
            .collect(Collectors.toList());
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

    // =========================================================================
    // Node URL helpers
    // =========================================================================

    /**
     * Parses and sanitises ikasan.dashboard.cluster.peer-urls before any services are created.
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
    List<String> clusterPeerUrls() {
        List<InetAddress> selfAddresses = selfAddresses();
        List<String> urls = Arrays.stream(clusterPeerUrls.split(","))
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(s -> !s.isEmpty())
            .filter(url -> !isSelfUrl(url, selfAddresses))
            .toList();

        List<String> deduped = urls.stream().distinct().collect(Collectors.toList());
        if (deduped.size() < urls.size()) {
            logger.warn("Duplicate or invalid URLs were found in ikasan.dashboard.cluster.peer-urls and have been removed. " +
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
                        "Please remove this node's own URL from ikasan.dashboard.cluster.peer-urls.", url, serverPort);
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
                    "until the host becomes reachable. Please check ikasan.dashboard.cluster.peer-urls.", url);
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
