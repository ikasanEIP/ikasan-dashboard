package org.ikasan.job.orchestration.rest.dashboard.model.dto;

public class MessageIdDto {

    private String messageId;

    public MessageIdDto() {}

    public MessageIdDto(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
}
