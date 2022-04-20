package org.ikasan.rest.client;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class LogStreamingServiceRestImplTest {

    private String contextBaseUrl;

    private LogStreamingServiceRestImpl logStreamingService;

    public static MockWebServer mockBackEnd;

    @Before
    public void setup() throws IOException {
        Environment environment = new StandardEnvironment();
        logStreamingService = new LogStreamingServiceRestImpl(environment, new HttpComponentsClientHttpRequestFactory());
        ReflectionTestUtils.setField(logStreamingService, "runFlag", new AtomicBoolean(false));

        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        contextBaseUrl = mockBackEnd.url("").toString();
    }

    @After
    public void tearDown() throws Exception {
        mockBackEnd.shutdown();
    }

    @Test
    public void shouldConstructWithoutError() throws InterruptedException {
        MockResponse mockResponse = new MockResponse()
            .addHeader("Content-Type", "text/event-stream")
            .setResponseCode(200);

        mockBackEnd.enqueue(mockResponse);

        DataConsumer dataConsumer = new DataConsumer();

        logStreamingService.streamLogFile(contextBaseUrl,
            "/logs",
            "src/test/resources/log.sample",
            dataConsumer::dataConsumer,
            dataConsumer::errorConsumer,
            dataConsumer::completedConsumer
        );


        Flux<ServerSentEvent<String>> eventStream = (Flux<ServerSentEvent<String>>) ReflectionTestUtils.getField(logStreamingService, "eventStream");
        StepVerifier.create(eventStream)
            .expectNoEvent(Duration.ofSeconds(1L));

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertEquals("GET", recordedRequest.getMethod());
        assertEquals("/logs?fullFilePath=src/test/resources/log.sample", recordedRequest.getPath());
        assertEquals("/logs", recordedRequest.getRequestUrl().encodedPath());
        assertEquals(contextBaseUrl + "logs?fullFilePath=src/test/resources/log.sample", recordedRequest.getRequestUrl().toString());
    }

    private static class DataConsumer {
        private final Logger log = LoggerFactory.getLogger(DataConsumer.class);

        public void dataConsumer(ServerSentEvent<String> sse) {
            log.info(sse.data());
        }

        public void errorConsumer(Throwable error) {
            log.error(error.getMessage());
        }

        public void completedConsumer() {
            log.info("Complete consumer");
        }
    }

}