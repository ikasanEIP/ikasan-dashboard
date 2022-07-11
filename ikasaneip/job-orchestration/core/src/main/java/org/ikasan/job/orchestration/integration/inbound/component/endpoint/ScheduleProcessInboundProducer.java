package org.ikasan.job.orchestration.integration.inbound.component.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.component.endpoint.bigqueue.message.BigQueueMessageImpl;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.integration.inbound.component.endpoint.configuration.ScheduleProcessInboundProducerConfiguration;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.component.endpoint.EndpointException;
import org.ikasan.spec.component.endpoint.Producer;
import org.ikasan.spec.configuration.ConfiguredResource;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleProcessInboundProducer implements Producer<String>, ConfiguredResource<ScheduleProcessInboundProducerConfiguration> {

    private Logger logger = LoggerFactory.getLogger(ScheduleProcessInboundProducer.class);

    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private ScheduleProcessInboundProducerConfiguration configuration;
    private String configurationId;

    @Override
    public void invoke(String payload) throws EndpointException {
        try {
            BigQueueMessage bigQueueMessage = objectMapper.readValue(payload, BigQueueMessageImpl.class);
            String message = (String) bigQueueMessage.getMessage();

            ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent
                = objectMapper.readValue(message, ContextualisedScheduledProcessEventImpl.class);
            ContextMachine contextMachine = ContextMachineCache.instance()
                .getByContextName(contextualisedScheduledProcessEvent.getContextId());

            // put the payload straight onto the queue as it is a big message already created by ScheduledProcessEventController
            contextMachine.eventReceived(payload);
        }
        catch (Exception e) {
            if(this.configuration.isIgnoreErrors()) {
                logger.info("Ignoring error [{}] for payload [{}]", e.getMessage(), payload);
            }
            else {
                throw new EndpointException(e);
            }
        }
    }

    @Override
    public String getConfiguredResourceId() {
        return configurationId;
    }

    @Override
    public void setConfiguredResourceId(String configurationId) {
        this.configurationId = configurationId;
    }

    @Override
    public ScheduleProcessInboundProducerConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public void setConfiguration(ScheduleProcessInboundProducerConfiguration scheduleProcessInboundProducerConfiguration) {
        this.configuration = scheduleProcessInboundProducerConfiguration;
    }
}
