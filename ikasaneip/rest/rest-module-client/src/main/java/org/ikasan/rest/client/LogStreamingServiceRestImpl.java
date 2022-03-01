package org.ikasan.rest.client;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.ikasan.spec.module.client.LogStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;

public class LogStreamingServiceRestImpl extends ModuleRestService implements LogStreamingService<ServerSentEvent<String>> {

    private static final Logger LOG = LoggerFactory.getLogger(LogStreamingServiceRestImpl.class);
    private static final String FULL_FILE_PATH = "fullFilePath";
    // vars set here so can reflectively get and set them in tests
    private Flux<ServerSentEvent<String>> eventStream;
    private AtomicBoolean runFlag = new AtomicBoolean(true);

    public LogStreamingServiceRestImpl(Environment environment,
                                       HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        super(environment, httpComponentsClientHttpRequestFactory);
    }

    @Override
    public void streamLogFile(String host,
                              String path,
                              String fullFilePathToLog,
                              Consumer<ServerSentEvent<String>> dataConsumer,
                              Consumer<Throwable> errorConsumer,
                              Runnable completedConsumer) throws InterruptedException {

        try {

            WebClient webClient = getWebClient(host);

            ParameterizedTypeReference<ServerSentEvent<String>> type = new ParameterizedTypeReference<>() {
            };

            eventStream = webClient.get()
                .uri(path + "?" + FULL_FILE_PATH + "=" + fullFilePathToLog)
                .retrieve()
                .bodyToFlux(type);

            eventStream.subscribe(dataConsumer, errorConsumer, completedConsumer);

            while (runFlag.get()) {
                TimeUnit.SECONDS.sleep(1);
            }

        } catch (Exception e) {
            runFlag.set(false);
            LOG.error(e.getMessage());
            throw e;
        }
    }

    private WebClient getWebClient(String host) {
        WebClient webClient;
        List<String> auths = super.createHttpHeaders().get(HttpHeaders.AUTHORIZATION);
        if (auths != null) {
            webClient = WebClient.builder().baseUrl(host).defaultHeader(HttpHeaders.AUTHORIZATION, auths.toArray(new String[0])).build();
        } else {
            webClient = WebClient.builder().baseUrl(host).build();
        }
        return webClient;
    }
}
