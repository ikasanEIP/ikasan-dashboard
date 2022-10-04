package org.ikasan.rest.client;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.ikasan.rest.client.dto.BigQueueMessageDto;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

public class BigQueueModuleRestServiceImplTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private BigQueueModuleRestServiceImpl uut;

    private String contextBaseUrl;

    @Before
    public void setup() {
        contextBaseUrl = "http://localhost:" + wireMockRule.port();
        Environment environment = new StandardEnvironment();
        uut = new BigQueueModuleRestServiceImpl(environment, new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    public void test_size_200() {
        stubFor(get(urlEqualTo("/rest/big/queue/size/queue1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withBody("10")
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(200)
            ));
        long result = uut.size(contextBaseUrl,"queue1");
        assertEquals(10L, result);
    }

    @Test
    public void test_size_200_Empty() {
        stubFor(get(urlEqualTo("/rest/big/queue/size/queue1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(200)
            ));
        long result = uut.size(contextBaseUrl,"queue1");
        assertEquals(0L, result);
    }

    @Test
    public void test_size_404() {
        stubFor(get(urlEqualTo("/rest/big/queue/size/queue1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(404)
            ));
        long result = uut.size(contextBaseUrl,"queue1");
        assertEquals(0L, result);
    }

    @Test
    public void test_size_500() {
        stubFor(get(urlEqualTo("/rest/big/queue/size/queue1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(500)
            ));
        long result = uut.size(contextBaseUrl,"queue1");
        assertEquals(0L, result);
    }

    @Test
    public void test_peek_200() {
        stubFor(get(urlEqualTo("/rest/big/queue/peek/queue1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("{\"messageId\":\"uuidAsMessageId\",\"createdTime\":1657509967,\"message\":\"some message\"}")
                .withStatus(200)
            ));
        BigQueueMessage result = uut.peek(contextBaseUrl,"queue1");
        assertEquals("uuidAsMessageId", result.getMessageId());
        assertEquals(1657509967L, result.getCreatedTime());
        assertEquals("some message", result.getMessage());
        assertNull(result.getMessageProperties());
    }

    @Test
    public void test_peek_200_Empty() {
        stubFor(get(urlEqualTo("/rest/big/queue/peek/queue1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(200)
            ));
        BigQueueMessage result = uut.peek(contextBaseUrl,"queue1");
        assertNull(result);
    }

    @Test
    public void test_peek_404() {
        stubFor(get(urlEqualTo("/rest/big/queue/peek/queue1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(404)
            ));
        BigQueueMessage result = uut.peek(contextBaseUrl,"queue1");
        assertNull(result);
    }

    @Test
    public void test_peek_500() {
        stubFor(get(urlEqualTo("/rest/big/queue/peek/queue1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(500)
            ));
        BigQueueMessage result = uut.peek(contextBaseUrl,"queue1");
        assertNull(result);
    }

    @Test
    public void test_message_200() {
        stubFor(get(urlEqualTo("/rest/big/queue/messages/queue1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withBody("[{\"messageId\":\"uuidAsMessageId1\",\"createdTime\":1657509967,\"message\":\"some message 1\"},{\"messageId\":\"uuidAsMessageId2\",\"createdTime\":1657509960,\"message\":\"some message 2\"}]")
                .withStatus(200)
            ));
        List<BigQueueMessageDto> result = uut.getMessages(contextBaseUrl,"queue1");
        assertEquals("uuidAsMessageId1", result.get(0).getMessageId());
        assertEquals(1657509967L, result.get(0).getCreatedTime());
        assertEquals("some message 1", result.get(0).getMessage());
        assertNull(result.get(0).getMessageProperties());

        assertEquals("uuidAsMessageId2", result.get(1).getMessageId());
        assertEquals(1657509960L, result.get(1).getCreatedTime());
        assertEquals("some message 2", result.get(1).getMessage());
        assertNull(result.get(1).getMessageProperties());
    }

    @Test
    public void test_message_200_Empty() {
        stubFor(get(urlEqualTo("/rest/big/queue/messages/queue1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(200)
            ));
        List<BigQueueMessageDto> result = uut.getMessages(contextBaseUrl,"queue1");
        assertNull(result);
    }

    @Test
    public void test_message_404() {
        stubFor(get(urlEqualTo("/rest/big/queue/messages/queue1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(404)
            ));
        List<BigQueueMessageDto> result = uut.getMessages(contextBaseUrl,"queue1");
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_message_500() {
        stubFor(get(urlEqualTo("/rest/big/queue/messages/queue1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(500)
            ));
        List<BigQueueMessageDto> result = uut.getMessages(contextBaseUrl,"queue1");
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_listQueues_200() {
        stubFor(get(urlEqualTo("/rest/big/queue"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withBody("[\"queueName1\",\"queueName2\"]")
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(200)
            ));
        List<String> result = uut.listQueues(contextBaseUrl);
        assertEquals(2, result.size());
        assertEquals("queueName1", result.get(0));
        assertEquals("queueName2", result.get(1));
    }

    @Test
    public void test_listQueues_200_Empty() {
        stubFor(get(urlEqualTo("/rest/big/queue"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(200)
            ));
        List<String> result = uut.listQueues(contextBaseUrl);
        assertNull(result);
    }

    @Test
    public void test_listQueues_404() {
        stubFor(get(urlEqualTo("/rest/big/queue"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(404)
            ));
        List<String> result = uut.listQueues(contextBaseUrl);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_listQueues_500() {
        stubFor(get(urlEqualTo("/rest/big/queue"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(500)
            ));
        List<String> result = uut.listQueues(contextBaseUrl);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_all_size_200() {
        stubFor(get(urlEqualTo("/rest/big/queue/size?includeZeros=true"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withBody("{\"queueName1\":5,\"queueName2\":0}")
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(200)
            ));
        Map<String, Long> result = uut.size(contextBaseUrl,true);
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(5), result.get("queueName1"));
        assertEquals(Long.valueOf(0), result.get("queueName2"));
    }

    @Test
    public void test_all_size_200_includeZero_false() {
        stubFor(get(urlEqualTo("/rest/big/queue/size?includeZeros=false"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withBody("{\"queueName1\":5,\"queueName2\":2}")
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(200)
            ));
        Map<String, Long> result = uut.size(contextBaseUrl,false);
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(5), result.get("queueName1"));
        assertEquals(Long.valueOf(2), result.get("queueName2"));
    }

    @Test
    public void test_all_size_200_Empty() {
        stubFor(get(urlEqualTo("/rest/big/queue/size?includeZeros=true"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(200)
            ));
        Map<String, Long> result = uut.size(contextBaseUrl,true);
        assertNull(result);
    }

    @Test
    public void test_all_size_404() {
        stubFor(get(urlEqualTo("/rest/big/queue/size?includeZeros=true"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(404)
            ));
        Map<String, Long> result = uut.size(contextBaseUrl,true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_all_size_500() {
        stubFor(get(urlEqualTo("/rest/big/queue/size?includeZeros=true"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(500)
            ));
        Map<String, Long> result = uut.size(contextBaseUrl,true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_deleteMessage_200() {
        stubFor(delete(urlEqualTo("/rest/big/queue/delete/queueName1/messId1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(200)
            ));
        boolean result = uut.deleteMessage(contextBaseUrl,"queueName1", "messId1");
        assertTrue(result);
    }

    @Test
    public void test_deleteMessage_404() {
        stubFor(delete(urlEqualTo("/rest/big/queue/delete/queueName1/messId1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(404)
            ));
        boolean result = uut.deleteMessage(contextBaseUrl,"queueName1", "messId1");
        assertFalse(result);
    }

    @Test
    public void test_deleteMessage_500() {
        stubFor(delete(urlEqualTo("/rest/big/queue/delete/queueName1/messId1"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .withStatus(500)
            ));
        boolean result = uut.deleteMessage(contextBaseUrl,"queueName1", "messId1");
        assertFalse(result);
    }
}
