package org.ikasan.job.orchestration.rest.client;

import org.ikasan.job.orchestration.rest.client.dto.JobDryRunModeDto;
import org.ikasan.rest.client.ModuleRestService;
import org.ikasan.rest.client.SchedulerRestServiceImpl;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
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

public class JobInitiationServiceImpl extends ModuleRestService implements JobInitiationService {

    private static Logger logger = LoggerFactory.getLogger(SchedulerRestServiceImpl.class);

    public static final String SCHEDULER_JOB_INITIATION_URL = "/rest/schedulerJobInitiation";
    public static final String FLOW_SCHEDULE_FIRE_NOW_URL = "/rest/scheduler/{moduleName}/{flowName}";
    public static final String JOB_DRY_RUN_MODE_URL = "/rest/dryRun/jobmode";

    public JobInitiationServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        super(environment, httpComponentsClientHttpRequestFactory);
    }

    @Override
    public void raiseSchedulerJobInitiationEvent(String contextUrl, SchedulerJobInitiationEvent event) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(event, headers);
        String url = contextUrl + SCHEDULER_JOB_INITIATION_URL;

        logger.info("Context URL[{}] Payload[{}] ", url, event);
        restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
    }

    @Override
    public void raiseQuartzSchedulerJob(String contextUrl, String agentName, String jobName) {
        this.triggerJobNow(contextUrl, agentName, jobName);
    }

    @Override
    public void raiseFileEventSchedulerJob(String contextUrl, String agentName, String jobName) {
        this.setJobDryRunMode(contextUrl, jobName, true);
        this.triggerJobNow(contextUrl, agentName, jobName);
        this.setJobDryRunMode(contextUrl, jobName, false);
    }

    private void setJobDryRunMode(String contextUrl, String jobName, boolean isDryRun) {
        JobDryRunModeDto event = new JobDryRunModeDto();
        event.setJobName(jobName);
        event.setIsDryRun(isDryRun);

        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(event, headers);
        String url = contextUrl + JOB_DRY_RUN_MODE_URL;

        logger.info("Context URL[{}] Payload[{}] ", url, event);
        restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
    }

    private void triggerJobNow(String contextUrl, String agentName, String jobName) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        Map<String, String> parameters = new HashMap<>()
        {{put("moduleName",agentName);{put("flowName",jobName);}}};
        String url = contextUrl+FLOW_SCHEDULE_FIRE_NOW_URL;
        try
        {
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class, parameters);
        }
        catch(RestClientException e){
            logger.warn("Issue triggering scheduler flow job [" + url
                + "] with agent ["+agentName+"] " + "] and job ["+jobName +"] "
                + " with response [{"+e.getLocalizedMessage()+"}]");
            throw e;
        }
    }
}
