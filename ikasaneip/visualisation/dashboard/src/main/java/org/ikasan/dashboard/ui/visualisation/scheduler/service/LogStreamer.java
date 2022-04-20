package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import java.util.function.Consumer;

import org.ikasan.spec.module.client.LogStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;

public class LogStreamer {
    private static final Logger LOG = LoggerFactory.getLogger(LogStreamer.class);

    private final LogStreamingService<ServerSentEvent<String>> logStreamingService;
    private final Consumer<ServerSentEvent<String>> logConsumer;
    private final Consumer<Throwable> errorConsumer;
    private final Runnable completedRunner;
    private final String host;
    private final String endPoint;
    private final String logFile;

    public LogStreamer(LogStreamingService<ServerSentEvent<String>> logStreamingService,
                       Consumer<ServerSentEvent<String>> logConsumer,
                       Consumer<Throwable> errorConsumer,
                       Runnable completedRunner,
                       String host,
                       String endPoint,
                       String logFile) {

        this.logStreamingService = logStreamingService;
        this.logConsumer = logConsumer;
        this.errorConsumer = errorConsumer;
        this.completedRunner = completedRunner;
        this.host = host;
        this.endPoint = endPoint;
        this.logFile = logFile;
    }

    public void stream() {
        try {
            logStreamingService.streamLogFile(host, endPoint, logFile, logConsumer, errorConsumer, completedRunner);
        } catch (Exception e) {
            LOG.error("Could not stream logging. Error: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
