package org.ikasan.scheduler.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduler.core.model.context.ContextTemplateImpl;
import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.ikasan.scheduler.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

public class ContextService {
    private ObjectMapper objectMapper;

    public ContextService() {
        this.objectMapper = ObjectMapperFactory.newInstance();
    }

    public ContextTemplate getContext(String context) throws JsonProcessingException {
        return objectMapper.readValue(context, ContextTemplateImpl.class);
    }

    public String getContextString(ContextTemplate context) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
    }

    public ContextInstance getContextInstance(String context) throws JsonProcessingException {
        return objectMapper.readValue(context, ContextInstanceImpl.class);
    }

    public String getContextInstanceString(ContextInstance context) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
    }
}
