package org.ikasan.rest.client.dto;

import org.ikasan.spec.bigqueue.message.BigQueueMessage;

import java.io.Serializable;
import java.util.Map;

public class BigQueueMessageDto<T> implements BigQueueMessage<T>, Serializable {

    private String messageId;
    private long createdTime;
    private T message;
    private Map<String, String> messageProperties;

    @Override
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public String getMessageId() {
        return this.messageId;
    }

    @Override
    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    @Override
    public long getCreatedTime() {
        return this.createdTime;
    }

    @Override
    public void setMessage(T message) {
        this.message = message;
    }

    @Override
    public T getMessage() {
        return this.message;
    }

    @Override
    public void setMessageProperties(Map<String, String> messageProperties) {
        this.messageProperties = messageProperties;
    }

    @Override
    public Map<String, String> getMessageProperties() {
        return this.messageProperties;
    }
}
