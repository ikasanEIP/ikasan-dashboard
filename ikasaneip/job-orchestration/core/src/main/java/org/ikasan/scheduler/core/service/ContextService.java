package org.ikasan.scheduler.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduler.core.model.context.ContextTemplate;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.spec.Context;

public class ContextService {
    private ObjectMapper objectMapper;

    public ContextService() {
        this.objectMapper = new ObjectMapper();
    }

    public ContextTemplate getContext(String context) throws JsonProcessingException {
        return objectMapper.readValue(context, ContextTemplate.class);
    }

    public String getContextString(Context context) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
    }

    public ContextInstance getContextInstance(String context) throws JsonProcessingException {
        return objectMapper.readValue(context, ContextInstance.class);
    }

    public String getContextInstanceString(ContextInstance context) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
    }
}
