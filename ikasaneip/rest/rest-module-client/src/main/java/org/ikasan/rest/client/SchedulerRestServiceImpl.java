package org.ikasan.rest.client;

import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SchedulerRestServiceImpl extends ModuleRestService implements SchedulerService {

    Logger logger = LoggerFactory.getLogger(SchedulerRestServiceImpl.class);

    public static final String TRIGGER_URL = "/rest/scheduler";
    public static final String FLOW_SCHEDULE_FIRE_NOW_URL = "/rest/scheduler/{moduleName}/{flowName}";
    public static final String SCHEDULER_JOB_INITIATION_URL = "/rest/schedulerJobInitiation";

    public SchedulerRestServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        super(environment, httpComponentsClientHttpRequestFactory);
    }

    @Override
    public Optional<List<Trigger>> getTriggers(String contextUrl) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        String url = contextUrl+TRIGGER_URL;

        try
        {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

            return Optional.of(response.getBody());
        }
        catch(RestClientException e){
            logger.warn("Issue getting triggers from module [" + url
                + "]  with response [{"+e.getLocalizedMessage()+"}]");
            return Optional.empty();
        }
    }

    @Override
    public boolean triggerFlowNow(String contextUrl, String moduleName, String flowName) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        Map<String, String> parameters = new HashMap<>()
        {{put("moduleName",moduleName);{put("flowName",flowName);}}};
        String url = contextUrl+FLOW_SCHEDULE_FIRE_NOW_URL;
        try
        {
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class, parameters);

            return true;
        }
        catch(RestClientException e){
            logger.warn("Issue querying module activation state [" + url
                + "] with module ["+moduleName+"] "
                + " with response [{"+e.getLocalizedMessage()+"}]");
            return false;
        }
    }

    @Override
    public void raiseSchedulerJobInitiationEvent(String contextUrl, SchedulerJobInitiationEvent event) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(event, headers);
        String url = contextUrl + SCHEDULER_JOB_INITIATION_URL;

        logger.info("Context URL[{}] Payload[{}] ", url, event);
        restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
    }
}
