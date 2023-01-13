package org.ikasan.job.orchestration.rest.client;

import org.ikasan.job.orchestration.rest.client.dto.JobDryRunModeDto;
import org.ikasan.rest.client.ModuleRestService;
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

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JobInitiationServiceImpl extends ModuleRestService implements JobInitiationService {

    private static final Logger LOG = LoggerFactory.getLogger(JobInitiationServiceImpl.class);

    public static final String SCHEDULER_JOB_INITIATION_URL = "/rest/schedulerJobInitiation";
    public static final String FLOW_SCHEDULE_FIRE_NOW_URL = "/rest/scheduler/{moduleName}/{flowName}/{correlationId}";
    public static final String JOB_DRY_RUN_MODE_URL = "/rest/dryRun/jobmode";

    private final int fileJobSubmissionWaitTimeSeconds;

    public JobInitiationServiceImpl(Environment environment, HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory
        , int fileJobSubmissionWaitTimeSeconds) {
        super(environment, httpComponentsClientHttpRequestFactory);
        this.fileJobSubmissionWaitTimeSeconds = fileJobSubmissionWaitTimeSeconds;
    }

    @Override
    public void raiseSchedulerJobInitiationEvent(String contextUrl, SchedulerJobInitiationEvent event) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(event, headers);
        String url = contextUrl + SCHEDULER_JOB_INITIATION_URL;

        LOG.info("Context URL[{}] Payload[{}] ", url, event);
        restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
    }

    @Override
    public void raiseQuartzSchedulerJob(String contextUrl, String agentName, String jobName, String correlationId) {
        this.triggerJobNow(contextUrl, agentName, jobName, correlationId);
    }

    @Override
    public void raiseFileEventSchedulerJob(String contextUrl, String agentName, String jobName, String correlationId) {
        this.setJobDryRunMode(contextUrl, jobName, true);
        this.triggerJobNow(contextUrl, agentName, jobName, correlationId);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
                try {
                    this.setJobDryRunMode(contextUrl, jobName, false);
                }
                catch (RestClientException e) {
                    LOG.error("An error has occurred setting job dry run to false", e);
                }
            }
        , this.fileJobSubmissionWaitTimeSeconds, TimeUnit.SECONDS);
        scheduler.shutdown();
    }

    private void setJobDryRunMode(String contextUrl, String jobName, boolean isDryRun) {
        JobDryRunModeDto event = new JobDryRunModeDto();
        event.setJobName(jobName);
        event.setIsDryRun(isDryRun);

        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(event, headers);
        String url = contextUrl + JOB_DRY_RUN_MODE_URL;

        LOG.info("Context URL[{}] Payload[{}] ", url, event);
        restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
    }

    private void triggerJobNow(String contextUrl, String agentName, String jobName, String correlationId) {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity entity = new HttpEntity(headers);
        Map<String, String> parameters = Map.of("moduleName",agentName, "flowName", jobName, "correlationId", correlationId);
        String url = contextUrl+FLOW_SCHEDULE_FIRE_NOW_URL;
        LOG.info("Context URL[{}] Payload[{}] ", url, parameters);
        try
        {
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class, parameters);
        }
        catch(RestClientException e){
            LOG.warn("Issue triggering scheduler flow job [" + url
                + "] with agent ["+agentName+"] " + "] and job ["+jobName +"] "
                + " with response [{"+e.getLocalizedMessage()+"}]");
            throw e;
        }
    }
}
