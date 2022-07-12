package org.ikasan.job.orchestration.rest.client;

import org.ikasan.rest.client.ModuleRestService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

public class JobUtilsServiceImpl extends ModuleRestService implements JobUtilsService {

    private Logger logger = LoggerFactory.getLogger(JobUtilsServiceImpl.class);

    public static final String KILL_URL = "/rest/jobUtils/kill/{pid}?destroy={forcibly}";

    public JobUtilsServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        super(environment, httpComponentsClientHttpRequestFactory);
    }

    @Override
    public void killJob(String contextUrl, long pid, boolean forcibly) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        String url = contextUrl+KILL_URL;

        Map<String, String> parameters = new HashMap<String, String>()
        {{put("pid", Long.toString(pid));put("forcibly", Boolean.toString(forcibly));}};

        try
        {
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class, parameters);
        }
        catch(RestClientException e){
            logger.warn("Issue killing job with pid [" + pid
                + "] with destroy ["+forcibly+"]"
                + " with response [{"+e.getLocalizedMessage()+"}]");
            throw e;
        }
    }
}
