package org.ikasan.rest.client;

import org.apache.commons.codec.binary.Base64;
import org.ikasan.rest.client.dto.ReplayRequestDto;
import org.ikasan.spec.module.client.ReplayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;

public class ReplayRestServiceImpl implements ReplayService
{
    Logger logger = LoggerFactory.getLogger(ReplayRestServiceImpl.class);

    protected final static String REPLAY_URL = "/rest/replay";

    private RestTemplate restTemplate;

    public ReplayRestServiceImpl(HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory)
    {
        restTemplate = new RestTemplate(httpComponentsClientHttpRequestFactory);
        JsonMapper mapper = JsonMapper.builder()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .build();

        JacksonJsonHttpMessageConverter jsonHttpMessageConverter = new JacksonJsonHttpMessageConverter(mapper);
        restTemplate.getMessageConverters().add(jsonHttpMessageConverter);
    }

    @Override
    public boolean replay(String contextUrl, String username, String password, String moduleName, String flowName,
                          byte[] event, String actor)
    {
        ReplayRequestDto dto = new ReplayRequestDto(moduleName, flowName, event, actor);
        HttpHeaders headers = createHttpHeaders(username, password);
        HttpEntity entity = new HttpEntity(dto, headers);
        String url = contextUrl + REPLAY_URL;
        try
        {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            if(response.getStatusCode().is2xxSuccessful()) {
                return true;
            }
            else {
                throw new ReplayFailException("Issue replaying event [" + url + "] with module [" + moduleName + "] "
                    + "and flows [" + flowName + "]" + " with response [{" + response.getStatusCode() + "}]");
            }
        }
        catch (RestClientException e)
        {
            logger.warn(
                "Issue replaying event [" + new String(event) + "] [" + url + "] with module [" + moduleName + "] "
                    + "and flows [" + flowName + "]" + " with response [{" + e.getLocalizedMessage() + "}]");
            throw new ReplayFailException("Issue replaying event [" + url + "] with module [" + moduleName + "] "
                + "and flows [" + flowName + "]" + " with response [{" + e.getLocalizedMessage() + "}]", e);
        }
    }

    private HttpHeaders createHttpHeaders(String username, String password)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        if ( username != null && password != null )
        {
            String credentials = username + ":" + password;
            String basicToken = new String(Base64.encodeBase64(credentials.getBytes()));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basicToken);
        }
        return headers;
    }
}
