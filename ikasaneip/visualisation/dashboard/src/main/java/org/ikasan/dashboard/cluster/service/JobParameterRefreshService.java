package org.ikasan.dashboard.cluster.service;

import org.ikasan.spec.scheduled.job.service.SpringCloudConfigRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class JobParameterRefreshService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobParameterRefreshService.class);

    private final SpringCloudConfigRefreshService springCloudConfigRefreshService;
    private final List<String> clusterPeerUrls;

    public JobParameterRefreshService(SpringCloudConfigRefreshService springCloudConfigRefreshService,
                                      @Value("${ikasan.dashboard.cluster.peer-urls:}") String clusterPeerUrls) {
        this.springCloudConfigRefreshService = springCloudConfigRefreshService;
        this.clusterPeerUrls = parseClusterPeerUrls(clusterPeerUrls);
    }

    public void refreshLocalAndPropagateBestEffort() {
        this.springCloudConfigRefreshService.actuatorRefresh();
        propagateToPeersBestEffort();
    }

    private void propagateToPeersBestEffort() {
        if (this.clusterPeerUrls.isEmpty()) {
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "JobParameterRefreshPeers");
            thread.setDaemon(true);
            return thread;
        });

        // Peer refresh is deliberately best-effort. The local leader refresh is the user-visible action;
        // follower refresh helps a standby node take over with fresher parameters, but it must not make the
        // dashboard operation fail if a follower is down, unreachable, or temporarily unauthenticated.
        executor.execute(() -> {
            try {
                this.clusterPeerUrls.forEach(peerUrl -> {
                    try {
                        LOGGER.info("Propagating job parameter refresh to peer [{}]", peerUrl);
                        this.springCloudConfigRefreshService.actuatorRefreshAtUrl(peerUrl);
                    }
                    catch (RestClientException e) {
                        LOGGER.warn("Failed to propagate job parameter refresh to peer [{}]: {}", peerUrl, e.getMessage());
                    }
                });
            }
            finally {
                executor.shutdown();
            }
        });
    }

    private List<String> parseClusterPeerUrls(String clusterPeerUrls) {
        if (clusterPeerUrls == null || clusterPeerUrls.isBlank()) {
            return List.of();
        }

        return Arrays.stream(clusterPeerUrls.split(","))
            .map(String::trim)
            .filter(peerUrl -> !peerUrl.isEmpty())
            .distinct()
            .collect(Collectors.toList());
    }
}
