package org.ikasan.rest.client;

import org.ikasan.spec.module.client.DownloadLogFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DownloadLogFileServiceImpl extends ModuleRestService implements DownloadLogFileService {
    private static final Logger LOG = LoggerFactory.getLogger(DownloadLogFileServiceImpl.class);

    private final static String GET_LIST_LOG_FILES_URL = "/rest/logs/listLogFiles?maxFileSize={maxFileSize}";
    private final static String GET_DOWNLOAD_LOG_FILES_URL = "/rest/logs/downloadLogFile?maxFileSize={maxFileSize}&fullFilePath={fullFilePath}";
    private long maxFileSizeInBytes;

    public DownloadLogFileServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory, long maxFileSizeInBytes) {
        super(environment, httpComponentsClientHttpRequestFactory);
        this.maxFileSizeInBytes = maxFileSizeInBytes;
    }

    @Override
    public Map<String, String> listLogFiles(String contextUrl) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        String url = contextUrl + GET_LIST_LOG_FILES_URL;
        Map<String, String> parameters = new HashMap<>() {{  put("maxFileSize", String.valueOf(maxFileSizeInBytes)); }};
        try {
            ResponseEntity<Map<String, String>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {}, parameters);
            return responseEntity.getBody();
        }
        catch(RestClientException e){
            String error = "Issue getting the list of logs files when calling url ["+url+"]. Likely module is not compatible or module is not responsive.";
            LOG.warn(error);
            LOG.debug(e.getLocalizedMessage());
            throw new RestClientException(error);
        }
    }

    @Override
    public byte[] downloadLogFile(String contextUrl, String fullFilePath) {
        HttpHeaders headers = createOctetStreamHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        String url = contextUrl + GET_DOWNLOAD_LOG_FILES_URL;
        Map<String, String> parameters = new HashMap<>() {{ put("fullFilePath", fullFilePath); put("maxFileSize", String.valueOf(maxFileSizeInBytes)); }};
        try {
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {}, parameters);
            return responseEntity.getBody();
        }
        catch(RestClientException e){
            String error = "Issue getting the log file when calling url ["+url+"]. Likely module is not compatible or module is not responsive.";
            LOG.warn(error);
            LOG.debug(e.getLocalizedMessage());
            throw new RestClientException(error);
        }
    }

    private HttpHeaders createOctetStreamHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
        if ( basicToken != null ) {
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basicToken);
        }
        return headers;
    }
}
