package org.ikasan.scheduler.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduler.core.model.context.ContextTemplate;
import org.ikasan.scheduler.core.model.instance.ContextInstance;

public class ContextService {
    private ObjectMapper objectMapper;

    public ContextService() {
        this.objectMapper = new ObjectMapper();
    }

    public ContextTemplate getContext(String context) throws JsonProcessingException {
        return objectMapper.readValue(context, ContextTemplate.class);
    }

    public ContextInstance getContextInstance(String context) throws JsonProcessingException {
        return objectMapper.readValue(context, ContextInstance.class);
    }

    public String getContextInstanceString(ContextInstance context) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
    }
}
